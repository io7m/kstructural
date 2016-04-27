package com.io7m.kstructural.parser

import com.io7m.jstructural.compact.KSExpressionMatch
import com.io7m.junreachable.UnreachableCodeException
import com.io7m.kstructural.core.KSBlock
import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSInline
import com.io7m.kstructural.core.KSLexicalType
import com.io7m.kstructural.core.KSSubsectionContent
import org.valid4j.Assertive
import java.net.URI
import java.net.URISyntaxException
import java.util.HashMap
import java.util.Optional

/*
 * Copyright © 2016 <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

class KSBlockParser private constructor(
  private val inlines : KSInlineParserType) : KSBlockParserType {

  companion object {

    fun get(p : KSInlineParserType) : KSBlockParserType =
      KSBlockParser(p)

    private fun failedToMatch(
      e : KSExpression.KSExpressionList,
      m : List<KSExpressionMatch>) : KSParseError {

      val sb = StringBuilder()
      sb.append("Input did not match expected form.")
      sb.append(System.lineSeparator())
      sb.append("  Expected one of: ")
      sb.append(System.lineSeparator())

      for (i in 0 .. m.size - 1) {
        sb.append("    ")
        sb.append(m[i])
        sb.append(System.lineSeparator())
      }

      sb.append("  Received: ")
      sb.append(System.lineSeparator())
      sb.append("    ")
      sb.append(e)
      sb.append(System.lineSeparator())
      return KSParseError(e.position, sb.toString())
    }

    private fun <A : Any> failedToMatchResult(
      e : KSExpression.KSExpressionList,
      m : List<KSExpressionMatch>) : KSParseResult<A> =
      KSParseResult.fail(failedToMatch(e, m))

    private fun <A : Any> parseError(e : KSLexicalType, m : String) : KSParseResult<A> =
      KSParseResult.fail<A>(KSParseError(e.position, m))

    private fun parseAttributeType(e : KSExpression.KSExpressionList) : String {
      Assertive.require(e.elements.size == 2)
      Assertive.require(e.elements[0] is KSExpression.KSExpressionSymbol)
      Assertive.require(e.elements[1] is KSExpression.KSExpressionSymbol)
      return (e.elements[1] as KSExpression.KSExpressionSymbol).text
    }

    private fun parseAttributeID(e : KSExpression.KSExpressionList) : KSID<Unit> {
      Assertive.require(e.elements.size == 2)
      Assertive.require(e.elements[0] is KSExpression.KSExpressionSymbol)
      Assertive.require(e.elements[1] is KSExpression.KSExpressionSymbol)
      return KSID(e.position, (e.elements[1] as KSExpression.KSExpressionSymbol).text, Unit)
    }

    private fun parseAttributeTarget(e : KSExpression.KSExpressionList) : String {
      Assertive.require(e.elements.size == 2)
      Assertive.require(e.elements[0] is KSExpression.KSExpressionSymbol)
      val target = e.elements[1]
      return when (target) {
        is KSExpression.KSExpressionList   -> throw UnreachableCodeException()
        is KSExpression.KSExpressionSymbol -> target.text
        is KSExpression.KSExpressionQuoted -> target.text
      }
    }
  }

  private object CommandMatchers {

    val symbol =
      KSExpressionMatch.anySymbol()
    val string =
      KSExpressionMatch.anyString()
    val symbol_or_string =
      KSExpressionMatch.oneOf(listOf(symbol, string))
    val any =
      KSExpressionMatch.anything()

    val id_name =
      KSExpressionMatch.exactSymbol("id")
    val id =
      KSExpressionMatch.allOfList(listOf(id_name, symbol))

    val type_name =
      KSExpressionMatch.exactSymbol("type")
    val type =
      KSExpressionMatch.allOfList(listOf(type_name, symbol))

    val para_name =
      KSExpressionMatch.exactSymbol("paragraph")
    val para_any =
      KSExpressionMatch.prefixOfList(listOf(para_name))
    val para_with_id =
      KSExpressionMatch.prefixOfList(listOf(para_name, id))
    val para_with_id_type =
      KSExpressionMatch.prefixOfList(listOf(para_name, id, type))
    val para_with_type_id =
      KSExpressionMatch.prefixOfList(listOf(para_name, type, id))
    val para_with_type =
      KSExpressionMatch.prefixOfList(listOf(para_name, type))

    val title_name =
      KSExpressionMatch.exactSymbol("title")
    val title =
      KSExpressionMatch.prefixOfList(listOf(title_name, symbol_or_string))

    val subsection_name =
      KSExpressionMatch.exactSymbol("subsection")
    val subsection_none =
      KSExpressionMatch.prefixOfList(listOf(subsection_name, title))
    val subsection_with_id =
      KSExpressionMatch.prefixOfList(listOf(subsection_name, title, id))
    val subsection_with_id_type =
      KSExpressionMatch.prefixOfList(listOf(subsection_name, title, id, type))
    val subsection_with_type_id =
      KSExpressionMatch.prefixOfList(listOf(subsection_name, title, type, id))
    val subsection_with_type =
      KSExpressionMatch.prefixOfList(listOf(subsection_name, title, type))
  }

  private fun parseAttributeTitle(
    e : KSExpression.KSExpressionList) : KSParseResult<List<KSInline.KSInlineText<Unit>>> {
    Assertive.require(e.elements.size >= 2)
    Assertive.require(e.elements[0] is KSExpression.KSExpressionSymbol)
    val texts = e.elements.subList(1, e.elements.size)
    return parseInlineTexts(texts)
  }

  private fun parseAttributeTargetAsURI(
    e : KSExpression.KSExpressionList) : KSParseResult<URI> {
    val text = parseAttributeTarget(e)
    return try {
      KSParseResult.succeed(URI(text))
    } catch (x : URISyntaxException) {
      parseError(e, "Invalid URI: " + x.message)
    }
  }

  private fun parseBlockPara(
    e : KSExpression.KSExpressionList) : KSParseResult<KSBlock.KSBlockParagraph<Unit>> {
    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpression.KSExpressionSymbol)

    when {
      KSExpressionMatch.matches(e, CommandMatchers.para_with_id_type) -> {
        Assertive.require(e.elements.size >= 3)
        val id =
          parseAttributeID(e.elements[1] as KSExpression.KSExpressionList)
        val type =
          parseAttributeType(e.elements[2] as KSExpression.KSExpressionList)
        val rest =
          e.elements.subList(3, e.elements.size)
        val act_content =
          parseInlines(rest)

        return act_content flatMap { content ->
          KSParseResult.succeed(newBlockParagraph(
            content, e, Optional.of(id), Optional.of(type)))
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.para_with_type_id) -> {
        Assertive.require(e.elements.size >= 3)
        val type =
          parseAttributeType(e.elements[1] as KSExpression.KSExpressionList)
        val id =
          parseAttributeID(e.elements[2] as KSExpression.KSExpressionList)
        val rest =
          e.elements.subList(3, e.elements.size)
        val act_content =
          parseInlines(rest)

        return act_content flatMap { content ->
          KSParseResult.succeed(newBlockParagraph(
            content, e, Optional.of(id), Optional.of(type)))
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.para_with_id)      -> {
        Assertive.require(e.elements.size >= 2)
        val id =
          parseAttributeID(e.elements[1] as KSExpression.KSExpressionList)
        val rest =
          e.elements.subList(2, e.elements.size)
        val act_content =
          parseInlines(rest)

        return act_content flatMap { content ->
          KSParseResult.succeed(newBlockParagraph(
            content, e, Optional.of(id), Optional.empty()))
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.para_with_type)    -> {
        Assertive.require(e.elements.size >= 2)
        val type =
          parseAttributeType(e.elements[1] as KSExpression.KSExpressionList)
        val rest =
          e.elements.subList(2, e.elements.size)
        val act_content =
          parseInlines(rest)

        return act_content flatMap { content ->
          KSParseResult.succeed(newBlockParagraph(
            content, e, Optional.empty(), Optional.of(type)))
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.para_any)          -> {
        Assertive.require(e.elements.size >= 1)
        val rest =
          e.elements.subList(1, e.elements.size)
        val act_content =
          parseInlines(rest)

        return act_content flatMap { content ->
          KSParseResult.succeed(newBlockParagraph(
            content, e, Optional.empty(), Optional.empty()))
        }
      }
    }

    return failedToMatchResult(e, listOf(
      CommandMatchers.para_any,
      CommandMatchers.para_with_id,
      CommandMatchers.para_with_id_type,
      CommandMatchers.para_with_type_id,
      CommandMatchers.para_with_type))
  }

  private fun newBlockParagraph(
    content : List<KSInline<Unit>>,
    e : KSExpression.KSExpressionList,
    id : Optional<KSID<Unit>>,
    type : Optional<String>) : KSBlock.KSBlockParagraph<Unit> {
    return KSBlock.KSBlockParagraph(e.position, Unit, type, id, content)
  }

  private fun parseBlockSubsection(
    e : KSExpression.KSExpressionList) : KSParseResult<KSBlock.KSBlockSubsection<Unit>> {
    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpression.KSExpressionSymbol)

    when {
      KSExpressionMatch.matches(e, CommandMatchers.subsection_with_type_id) -> {
        Assertive.require(e.elements.size >= 4)
        val act_title =
          parseAttributeTitle(e.elements[1] as KSExpression.KSExpressionList)
        val type =
          parseAttributeType(e.elements[2] as KSExpression.KSExpressionList)
        val id =
          parseAttributeID(e.elements[3] as KSExpression.KSExpressionList)
        val rest =
          e.elements.subList(4, e.elements.size)
        val act_content =
          parseSubsectionContents(rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSParseResult.succeed(KSBlock.KSBlockSubsection(
              e.position,
              Unit,
              title = title,
              id = Optional.of(id),
              type = Optional.of(type),
              content = content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.subsection_with_id_type) -> {
        Assertive.require(e.elements.size >= 4)
        val act_title =
          parseAttributeTitle(e.elements[1] as KSExpression.KSExpressionList)
        val id =
          parseAttributeID(e.elements[2] as KSExpression.KSExpressionList)
        val type =
          parseAttributeType(e.elements[3] as KSExpression.KSExpressionList)
        val rest =
          e.elements.subList(4, e.elements.size)
        val act_content =
          parseSubsectionContents(rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSParseResult.succeed(KSBlock.KSBlockSubsection(
              e.position,
              Unit,
              title = title,
              id = Optional.of(id),
              type = Optional.of(type),
              content = content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.subsection_with_type) -> {
        Assertive.require(e.elements.size >= 3)
        val act_title =
          parseAttributeTitle(e.elements[1] as KSExpression.KSExpressionList)
        val type =
          parseAttributeType(e.elements[2] as KSExpression.KSExpressionList)
        val rest =
          e.elements.subList(3, e.elements.size)
        val act_content =
          parseSubsectionContents(rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSParseResult.succeed(KSBlock.KSBlockSubsection(
              e.position,
              Unit,
              title = title,
              id = Optional.empty(),
              type = Optional.of(type),
              content = content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.subsection_with_id) -> {
        Assertive.require(e.elements.size >= 3)
        val act_title =
          parseAttributeTitle(e.elements[1] as KSExpression.KSExpressionList)
        val id =
          parseAttributeID(e.elements[2] as KSExpression.KSExpressionList)
        val rest =
          e.elements.subList(3, e.elements.size)
        val act_content =
          parseSubsectionContents(rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSParseResult.succeed(KSBlock.KSBlockSubsection(
              e.position,
              Unit,
              title = title,
              id = Optional.of(id),
              type = Optional.empty(),
              content = content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.subsection_none) -> {
        Assertive.require(e.elements.size >= 2)
        val act_title =
          parseAttributeTitle(e.elements[1] as KSExpression.KSExpressionList)
        val rest =
          e.elements.subList(2, e.elements.size)
        val act_content =
          parseSubsectionContents(rest)

        return act_title flatMap { title ->
          act_content flatMap { content ->
            KSParseResult.succeed(KSBlock.KSBlockSubsection(
              e.position,
              Unit,
              title = title,
              id = Optional.empty(),
              type = Optional.empty(),
              content = content))
          }
        }
      }
    }

    return failedToMatchResult(e, listOf(
      CommandMatchers.subsection_none,
      CommandMatchers.subsection_with_id,
      CommandMatchers.subsection_with_id_type,
      CommandMatchers.subsection_with_type,
      CommandMatchers.subsection_with_type_id))
  }

  private fun anyToSubsectionContent(
    e : KSBlock<Unit>) : KSParseResult<KSSubsectionContent<Unit>> {
    return when (e) {
      is KSBlock.KSBlockSection,
      is KSBlock.KSBlockSubsection,
      is KSBlock.KSBlockPart       -> {
        val sb = StringBuilder()
        sb.append("Expected subsection content.")
        sb.append(System.lineSeparator())
        sb.append("  Expected: Subsection content")
        sb.append(System.lineSeparator())
        sb.append("  Received: ")
        sb.append(e)
        sb.append(System.lineSeparator())
        parseError(e, sb.toString())
      }
      is KSBlock.KSBlockParagraph  ->
        KSParseResult.succeed(KSSubsectionContent.KSSubsectionParagraph(e))
    }
  }

  private fun parseSubsectionContent(
    e : KSExpression) : KSParseResult<KSSubsectionContent<Unit>> {
    return parseBlockAny(e) flatMap { k -> anyToSubsectionContent(k) }
  }

  private fun parseInlineTexts(
    e : List<KSExpression>) : KSParseResult<List<KSInline.KSInlineText<Unit>>> {
    return KSParseResult.map({ k ->
      inlines.parse(k) flatMap { r ->
        when (r) {
          is KSInline.KSInlineText ->
            KSParseResult.succeed(r)
          is KSInline.KSInlineLink,
          is KSInline.KSInlineVerbatim,
          is KSInline.KSInlineTerm,
          is KSInline.KSInlineImage -> {
            val sb = StringBuilder()
            sb.append("Expected text.")
            sb.append(System.lineSeparator())
            sb.append("  Expected: text")
            sb.append(System.lineSeparator())
            sb.append("  Received: ")
            sb.append(e)
            sb.append(System.lineSeparator())
            parseError(r, sb.toString())
          }
        }
      }
    }, e)
  }

  private fun parseInlines(
    e : List<KSExpression>) : KSParseResult<List<KSInline<Unit>>> {
    return KSParseResult.map({ k ->
      val r = this.inlines.parse(k)
      r flatMap { z -> KSParseResult.succeed(z) }
    }, e)
  }

  private fun parseSubsectionContents(
    e : List<KSExpression>) : KSParseResult<List<KSSubsectionContent<Unit>>> {
    return KSParseResult.map({ k -> parseSubsectionContent(k) }, e)
  }

  private val parsers : Map<String, ElementParser> =
    makeParsers()
  private val parserDescriptions : String =
    makeMapDescription(parsers)

  private fun makeParsers() : Map<String, ElementParser> {
    val m = HashMap<String, ElementParser>()
    m.put("paragraph", ElementParser("paragraph", { e -> parseBlockPara(e) }))
    m.put("subsection", ElementParser("subsection", { e -> parseBlockSubsection(e) }))
    return m
  }

  private data class ElementParser(
    val name : String,
    val parser : (KSExpression.KSExpressionList) -> KSParseResult<out KSBlock<Unit>>)

  private fun makeMapDescription(m : Map<String, Any>) : String {
    val sb = StringBuilder()
    sb.append("{")
    val iter = m.keys.iterator()
    while (iter.hasNext()) {
      sb.append(iter.next())
      if (iter.hasNext()) {
        sb.append(" | ")
      }
    }
    sb.append("}")
    return sb.toString()
  }

  private fun elementName(e : KSExpression.KSExpressionList) : String {
    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpression.KSExpressionSymbol)
    return (e.elements[0] as KSExpression.KSExpressionSymbol).text
  }

  private val isElement =
    KSExpressionMatch.prefixOfList(
      listOf(KSExpressionMatch.MatchSymbol(
        { s -> parsers.containsKey(s) },
        parserDescriptions)))

  private fun parseBlockAny(e : KSExpression) : KSParseResult<out KSBlock<Unit>> {
    if (!KSExpressionMatch.matches(e, isElement)) {
      val sb = StringBuilder()
      sb.append("Expected a block command.")
      sb.append(System.lineSeparator())
      sb.append("  Expected: ")
      sb.append(isElement)
      sb.append(System.lineSeparator())
      sb.append("  Received: ")
      sb.append(e)
      sb.append(System.lineSeparator())
      return parseError(e, sb.toString())
    }

    val el = e as KSExpression.KSExpressionList
    val name = elementName(el)
    Assertive.require(parsers.containsKey(name))
    val ic = parsers.get(name)!!
    Assertive.require(ic.name == name)
    return ic.parser.invoke(el)
  }

  override fun parse(e : KSExpression) : KSParseResult<out KSBlock<Unit>> {
    return parseBlockAny(e)
  }
}
