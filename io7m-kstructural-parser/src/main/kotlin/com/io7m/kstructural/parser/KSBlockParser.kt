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

package com.io7m.kstructural.parser

import com.io7m.junreachable.UnreachableCodeException
import com.io7m.kstructural.core.KSBlock
import com.io7m.kstructural.core.KSBlock.KSBlockParagraph
import com.io7m.kstructural.core.KSBlock.KSBlockPart
import com.io7m.kstructural.core.KSBlock.KSBlockSection
import com.io7m.kstructural.core.KSBlock.KSBlockSubsection
import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSInline
import com.io7m.kstructural.core.KSInline.KSInlineImage
import com.io7m.kstructural.core.KSInline.KSInlineLink
import com.io7m.kstructural.core.KSInline.KSInlineTerm
import com.io7m.kstructural.core.KSInline.KSInlineText
import com.io7m.kstructural.core.KSInline.KSInlineVerbatim
import com.io7m.kstructural.core.KSLexicalType
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.core.KSSubsectionContent
import org.valid4j.Assertive
import java.net.URI
import java.net.URISyntaxException
import java.util.HashMap
import java.util.Optional

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
      m : List<KSExpressionMatch>) : KSResult<A, KSParseError> =
      KSResult.fail(failedToMatch(e, m))

    private fun <A : Any> parseError(e : KSLexicalType, m : String) : KSResult<A, KSParseError> =
      KSResult.fail<A, KSParseError>(KSParseError(e.position, m))

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
    e : KSExpression.KSExpressionList) : KSResult<List<KSInlineText<Unit>>, KSParseError> {
    Assertive.require(e.elements.size >= 2)
    Assertive.require(e.elements[0] is KSExpression.KSExpressionSymbol)
    val texts = e.elements.subList(1, e.elements.size)
    return parseInlineTexts(texts)
  }

  private fun parseAttributeTargetAsURI(
    e : KSExpression.KSExpressionList) : KSResult<URI, KSParseError> {
    val text = parseAttributeTarget(e)
    return try {
      KSResult.succeed(URI(text))
    } catch (x : URISyntaxException) {
      parseError(e, "Invalid URI: " + x.message)
    }
  }

  private fun parseBlockPara(
    e : KSExpression.KSExpressionList)
    : KSResult<KSBlockParagraph<Unit>, KSParseError> {
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
          KSResult.succeed<KSBlockParagraph<Unit>, KSParseError>(
            newBlockParagraph(content, e, Optional.of(id), Optional.of(type)))
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
          KSResult.succeed<KSBlockParagraph<Unit>, KSParseError>(
            newBlockParagraph(content, e, Optional.of(id), Optional.of(type)))
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
          KSResult.succeed<KSBlockParagraph<Unit>, KSParseError>(
            newBlockParagraph(content, e, Optional.of(id), Optional.empty()))
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
          KSResult.succeed<KSBlockParagraph<Unit>, KSParseError>(
            newBlockParagraph(content, e, Optional.empty(), Optional.of(type)))
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.para_any)          -> {
        Assertive.require(e.elements.size >= 1)
        val rest =
          e.elements.subList(1, e.elements.size)
        val act_content =
          parseInlines(rest)

        return act_content flatMap { content ->
          KSResult.succeed<KSBlockParagraph<Unit>, KSParseError>(
            newBlockParagraph(content, e, Optional.empty(), Optional.empty()))
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
    type : Optional<String>) : KSBlockParagraph<Unit> {
    return KSBlockParagraph(e.position, Unit, type, id, content)
  }

  private fun parseBlockSubsection(
    e : KSExpression.KSExpressionList)
    : KSResult<KSBlockSubsection<Unit>, KSParseError> {
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
            KSResult.succeed<KSBlockSubsection<Unit>, KSParseError>(
              KSBlockSubsection(
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
            KSResult.succeed<KSBlockSubsection<Unit>, KSParseError>(
              KSBlockSubsection(
              e.position,
              Unit,
              title = title,
              id = Optional.of(id),
              type = Optional.of(type),
              content = content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.subsection_with_type)    -> {
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
            KSResult.succeed<KSBlockSubsection<Unit>, KSParseError>(
              KSBlockSubsection(
              e.position,
              Unit,
              title = title,
              id = Optional.empty(),
              type = Optional.of(type),
              content = content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.subsection_with_id)      -> {
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
            KSResult.succeed<KSBlockSubsection<Unit>, KSParseError>(
              KSBlockSubsection(
              e.position,
              Unit,
              title = title,
              id = Optional.of(id),
              type = Optional.empty(),
              content = content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.subsection_none)         -> {
        Assertive.require(e.elements.size >= 2)
        val act_title =
          parseAttributeTitle(e.elements[1] as KSExpression.KSExpressionList)
        val rest =
          e.elements.subList(2, e.elements.size)
        val act_content =
          parseSubsectionContents(rest)

        return act_title flatMap { title ->
          act_content flatMap { content ->
            KSResult.succeed<KSBlockSubsection<Unit>, KSParseError>(
              KSBlockSubsection(
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
    e : KSBlock<Unit>) : KSResult<KSSubsectionContent<Unit>, KSParseError> {
    return when (e) {
      is KSBlock.KSDocument,
      is KSBlockSection,
      is KSBlockSubsection,
      is KSBlockPart       -> {
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
      is KSBlockParagraph  ->
        KSResult.succeed(KSSubsectionContent.KSSubsectionParagraph(e))
    }
  }

  private fun parseSubsectionContent(
    e : KSExpression) : KSResult<KSSubsectionContent<Unit>, KSParseError> {
    return parseBlockAny(e) flatMap { k -> anyToSubsectionContent(k) }
  }

  private fun parseInlineTexts(
    e : List<KSExpression>) : KSResult<List<KSInlineText<Unit>>, KSParseError> {
    return KSResult.map({ k ->
      inlines.parse(k) flatMap { r ->
        when (r) {
          is KSInlineText ->
            KSResult.succeed<KSInlineText<Unit>, KSParseError>(r)
          is KSInlineLink,
          is KSInlineVerbatim,
          is KSInlineTerm,
          is KSInlineImage -> {
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
    e : List<KSExpression>) : KSResult<List<KSInline<Unit>>, KSParseError> {
    return KSResult.map({ k ->
      val r = this.inlines.parse(k)
      r flatMap { z -> KSResult.succeed<KSInline<Unit>, KSParseError>(z) }
    }, e)
  }

  private fun parseSubsectionContents(
    e : List<KSExpression>)
    : KSResult<List<KSSubsectionContent<Unit>>, KSParseError> {
    return KSResult.map({ k -> parseSubsectionContent(k) }, e)
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
    val parser : (KSExpression.KSExpressionList) -> KSResult<out KSBlock<Unit>, KSParseError>)

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

  private fun parseBlockAny(
    e : KSExpression) : KSResult<out KSBlock<Unit>, KSParseError> {
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

  override fun parse(
    e : KSExpression) : KSResult<out KSBlock<Unit>, KSParseError> {
    return parseBlockAny(e)
  }
}
