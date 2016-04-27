/*
 * Copyright © 2016 <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DIKSLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.kstructural.parser

import com.io7m.junreachable.UnreachableCodeException
import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSInline
import com.io7m.kstructural.core.KSLexicalType
import com.io7m.kstructural.core.KSLink
import com.io7m.kstructural.core.KSLinkContent
import org.valid4j.Assertive
import java.math.BigInteger
import java.net.URI
import java.net.URISyntaxException
import java.util.HashMap
import java.util.Optional

object KSInlineParser : KSInlineParserType {

  private object CommandMatchers {

    val symbol =
      KSExpressionMatch.anySymbol()
    val string =
      KSExpressionMatch.anyString()
    val symbol_or_string =
      KSExpressionMatch.oneOf(listOf(symbol, string))

    val id_name =
      KSExpressionMatch.exactSymbol("id")
    val id =
      KSExpressionMatch.allOfList(listOf(id_name, symbol))

    val type_name =
      KSExpressionMatch.exactSymbol("type")
    val type =
      KSExpressionMatch.allOfList(listOf(type_name, symbol))

    val target_name =
      KSExpressionMatch.exactSymbol("target")
    val target =
      KSExpressionMatch.allOfList(listOf(target_name, symbol_or_string))

    val size_name =
      KSExpressionMatch.exactSymbol("size")
    val size =
      KSExpressionMatch.allOfList(listOf(size_name, symbol, symbol))

    val image_name =
      KSExpressionMatch.exactSymbol("image")
    val image =
      KSExpressionMatch.prefixOfList(listOf(image_name, target, symbol_or_string))
    val image_with_type =
      KSExpressionMatch.prefixOfList(listOf(image_name, target, type, symbol_or_string))
    val image_with_size =
      KSExpressionMatch.prefixOfList(listOf(image_name, target, size, symbol_or_string))
    val image_with_type_size =
      KSExpressionMatch.prefixOfList(listOf(image_name, target, type, size, symbol_or_string))

    val term_name =
      KSExpressionMatch.exactSymbol("term")
    val term_type =
      KSExpressionMatch.prefixOfList(listOf(term_name, type, symbol_or_string))
    val term =
      KSExpressionMatch.prefixOfList(listOf(term_name, symbol_or_string))

    val verbatim_name =
      KSExpressionMatch.exactSymbol("verbatim")
    val verbatim =
      KSExpressionMatch.prefixOfList(listOf(verbatim_name, KSExpressionMatch.anyString()))
    val verbatim_type =
      KSExpressionMatch.prefixOfList(listOf(verbatim_name, type, KSExpressionMatch.anyString()))

    val link_name =
      KSExpressionMatch.exactSymbol("link")
    val link =
      KSExpressionMatch.prefixOfList(listOf(link_name, target))

    val link_ext_name =
      KSExpressionMatch.exactSymbol("link-ext")
    val link_ext =
      KSExpressionMatch.prefixOfList(listOf(link_ext_name, target))
  }

  override fun parse(e : KSExpression) : KSParseResult<out KSInline<Unit>> {
    return parseInlineAny(e)
  }

  private fun <A : Any> parseError(e : KSLexicalType, m : String) : KSParseResult<A> =
    KSParseResult.fail<A>(KSParseError(e.position, m))

  private fun <A : Any> failedToMatchResult(
    e : KSExpression.KSExpressionList,
    m : List<KSExpressionMatch>) : KSParseResult<A> =
    KSParseResult.fail(failedToMatch(e, m))

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

  private fun parseAttributeType(e : KSExpression.KSExpressionList) : String {
    Assertive.require(e.elements.size == 2)
    Assertive.require(e.elements[0] is KSExpression.KSExpressionSymbol)
    Assertive.require(e.elements[1] is KSExpression.KSExpressionSymbol)
    return (e.elements[1] as KSExpression.KSExpressionSymbol).text
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

  private fun parseAttributeTargetAsURI(
    e : KSExpression.KSExpressionList) : KSParseResult<URI> {
    val text = parseAttributeTarget(e)
    return try {
      KSParseResult.succeed(URI(text))
    } catch (x : URISyntaxException) {
      parseError(e, "Invalid URI: " + x.message)
    }
  }

  private fun parseAttributeSize(
    e : KSExpression.KSExpressionList) : KSParseResult<KSInline.KSSize> {
    Assertive.require(e.elements.size == 3)
    Assertive.require(e.elements[0] is KSExpression.KSExpressionSymbol)
    Assertive.require(e.elements[1] is KSExpression.KSExpressionSymbol)
    Assertive.require(e.elements[2] is KSExpression.KSExpressionSymbol)

    return try {
      val w = BigInteger((e.elements[1] as KSExpression.KSExpressionSymbol).text)
      val h = BigInteger((e.elements[2] as KSExpression.KSExpressionSymbol).text)

      if (w.compareTo(BigInteger.ZERO) < 0) {
        return parseError(e.elements[1], "Width is negative")
      }
      if (h.compareTo(BigInteger.ZERO) < 0) {
        return parseError(e.elements[2], "Height is negative")
      }

      return KSParseResult.succeed(KSInline.KSSize(w, h))
    } catch (x : NumberFormatException) {
      return parseError(e, "Invalid width or height: " + x.message)
    }
  }

  private fun parseInlineImage(
    e : KSExpression.KSExpressionList) : KSParseResult<KSInline.KSInlineImage<Unit>> {
    when {
      KSExpressionMatch.matches(e, CommandMatchers.image_with_type_size) -> {
        Assertive.require(e.elements.size >= 5)

        val act_target = parseAttributeTargetAsURI(
          e.elements[1] as KSExpression.KSExpressionList)
        val type = Optional.of(
          parseAttributeType(e.elements[2] as KSExpression.KSExpressionList))
        val act_size = parseAttributeSize(
          e.elements[3] as KSExpression.KSExpressionList)
        val texts =
          e.elements.subList(4, e.elements.size)
        val act_content =
          KSParseResult.map({ t -> parseInlineText(t) }, texts)

        return act_size flatMap { size ->
          act_content flatMap { content ->
            act_target flatMap { target ->
              KSParseResult.succeed(
                KSInline.KSInlineImage(
                  e.position, Unit, type, target, Optional.of(size), content))
            }
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.image_with_type)      -> {
        Assertive.require(e.elements.size >= 4)

        val act_target = parseAttributeTargetAsURI(
          e.elements[1] as KSExpression.KSExpressionList)
        val type = Optional.of(
          parseAttributeType(e.elements[2] as KSExpression.KSExpressionList))
        val size =
          Optional.empty<KSInline.KSSize>()
        val texts =
          e.elements.subList(3, e.elements.size)
        val act_content =
          KSParseResult.map({ t -> parseInlineText(t) }, texts)

        return act_content flatMap { content ->
          act_target flatMap { target ->
            KSParseResult.succeed(
              KSInline.KSInlineImage(
                e.position, Unit, type, target, size, content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.image_with_size)      -> {
        Assertive.require(e.elements.size >= 4)

        val act_target =
          parseAttributeTargetAsURI(e.elements[1] as KSExpression.KSExpressionList)
        val type =
          Optional.empty<String>()
        val act_size = parseAttributeSize(
          e.elements[2] as KSExpression.KSExpressionList)
        val texts =
          e.elements.subList(3, e.elements.size)
        val act_content =
          KSParseResult.map({ t -> parseInlineText(t) }, texts)

        return act_size flatMap { size ->
          act_content flatMap { content ->
            act_target flatMap { target ->
              KSParseResult.succeed(
                KSInline.KSInlineImage(
                  e.position, Unit, type, target, Optional.of(size), content))
            }
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.image)                -> {
        Assertive.require(e.elements.size >= 3)
        val act_target =
          parseAttributeTargetAsURI(e.elements[1] as KSExpression.KSExpressionList)
        val type =
          Optional.empty<String>()
        val size =
          Optional.empty<KSInline.KSSize>()
        val texts =
          e.elements.subList(2, e.elements.size)
        val act_content =
          KSParseResult.map({ t -> parseInlineText(t) }, texts)

        return act_content flatMap { content ->
          act_target flatMap { target ->
            KSParseResult.succeed(
              KSInline.KSInlineImage(
                e.position, Unit, type, target, size, content))
          }
        }
      }
    }

    return failedToMatchResult(e, listOf(
      CommandMatchers.image,
      CommandMatchers.image_with_size,
      CommandMatchers.image_with_type,
      CommandMatchers.image_with_type_size))
  }

  private fun parseInlineText(
    e : KSExpression) : KSParseResult<KSInline.KSInlineText<Unit>> {
    return when (e) {
      is KSExpression.KSExpressionList   -> {
        val sb = StringBuilder()
        sb.append("Expected text, but received an inline command.")
        sb.append(System.lineSeparator())
        sb.append("  Expected: Text")
        sb.append(System.lineSeparator())
        sb.append("  Received: ")
        sb.append(e)
        sb.append(System.lineSeparator())
        parseError(e, sb.toString())
      }
      is KSExpression.KSExpressionSymbol ->
        KSParseResult.succeed(KSInline.KSInlineText(e.position, Unit, e.text))
      is KSExpression.KSExpressionQuoted ->
        KSParseResult.succeed(KSInline.KSInlineText(e.position, Unit, e.text))
    }
  }

  private fun parseInlineVerbatim(
    e : KSExpression.KSExpressionList) : KSParseResult<KSInline.KSInlineVerbatim<Unit>> {
    when {
      KSExpressionMatch.matches(e, CommandMatchers.verbatim_type) -> {
        Assertive.require(e.elements.size == 3)
        val type =
          parseAttributeType(e.elements[1] as KSExpression.KSExpressionList)
        val content =
          (e.elements[2] as KSExpression.KSExpressionQuoted).text
        return KSParseResult.succeed(
          KSInline.KSInlineVerbatim(e.position, Unit, Optional.of(type), content))
      }
      KSExpressionMatch.matches(e, CommandMatchers.verbatim)      -> {
        Assertive.require(e.elements.size == 2)
        val content =
          (e.elements[1] as KSExpression.KSExpressionQuoted).text
        return KSParseResult.succeed(
          KSInline.KSInlineVerbatim(e.position, Unit, Optional.empty(), content))
      }
    }

    return failedToMatchResult(e, listOf(CommandMatchers.verbatim_type, CommandMatchers.verbatim))
  }

  private fun parseLinkInternal(
    e : KSExpression.KSExpressionList) : KSParseResult<KSLink.KSLinkInternal<Unit>> {

    if (KSExpressionMatch.matches(e, CommandMatchers.link)) {
      Assertive.require(e.elements.size >= 3)
      val texts = e.elements.subList(2, e.elements.size)
      Assertive.require(texts.size >= 1)

      val target =
        parseAttributeTarget(e.elements[1] as KSExpression.KSExpressionList)
      val kid =
        KSID(e.elements[1].position, target, Unit)
      val content =
        KSParseResult.map({ t -> parseLinkContent(t) }, texts)
      return content flatMap { cs ->
        KSParseResult.succeed(KSLink.KSLinkInternal(e.position, kid, cs))
      }
    } else {
      return failedToMatchResult(e, listOf(CommandMatchers.link))
    }
  }

  private fun parseLinkExternal(
    e : KSExpression.KSExpressionList) : KSParseResult<KSLink.KSLinkExternal<Unit>> {

    if (KSExpressionMatch.matches(e, CommandMatchers.link_ext)) {
      Assertive.require(e.elements.size >= 3)
      val texts = e.elements.subList(2, e.elements.size)
      Assertive.require(texts.size >= 1)

      val target = parseAttributeTarget(e.elements[1] as KSExpression.KSExpressionList)
      try {
        val uri = URI(target)
        val content = KSParseResult.map({ t -> parseLinkContent(t) }, texts)
        return content flatMap { cs ->
          KSParseResult.succeed(KSLink.KSLinkExternal(e.position, uri, cs))
        }
      } catch (x : URISyntaxException) {
        return parseError(e, "Invalid URI: " + x.message)
      }

    } else {
      return failedToMatchResult(e, listOf(CommandMatchers.link_ext))
    }
  }

  private fun parseInlineToLinkContent(
    e : KSInline<Unit>) : KSParseResult<KSLinkContent<Unit>> {
    return when (e) {
      is KSInline.KSInlineText     ->
        KSParseResult.succeed(KSLinkContent.KSLinkText(e.position, Unit, e))

      is KSInline.KSInlineImage    ->
        KSParseResult.succeed(KSLinkContent.KSLinkImage(e.position, Unit, e))

      is KSInline.KSInlineLink     ->
        parseError(e, "Link elements cannot appear inside link elements")

      is KSInline.KSInlineVerbatim ->
        parseError(e, "Verbatim elements cannot appear inside link elements")

      is KSInline.KSInlineTerm     ->
        parseError(e, "Term elements cannot appear inside link elements")
    }
  }

  private fun parseLinkContent(
    e : KSExpression) : KSParseResult<KSLinkContent<Unit>> {
    return when (e) {
      is KSExpression.KSExpressionList   ->
        parseInlineAny(e) flatMap { result -> parseInlineToLinkContent(result) }
      is KSExpression.KSExpressionSymbol ->
        KSParseResult.succeed(KSLinkContent.KSLinkText(
          e.position, Unit, KSInline.KSInlineText(e.position, Unit, e.text)))
      is KSExpression.KSExpressionQuoted ->
        KSParseResult.succeed(KSLinkContent.KSLinkText(
          e.position, Unit, KSInline.KSInlineText(e.position, Unit, e.text)))
    }
  }

  private fun parseInlineLinkInternal(
    e : KSExpression.KSExpressionList) : KSParseResult<KSInline.KSInlineLink<Unit>> {
    return parseLinkInternal(e) flatMap {
      link ->
      KSParseResult.succeed(KSInline.KSInlineLink(e.position, Unit, link))
    }
  }

  private fun parseInlineLinkExternal(
    e : KSExpression.KSExpressionList) : KSParseResult<KSInline.KSInlineLink<Unit>> {
    return parseLinkExternal(e) flatMap {
      link ->
      KSParseResult.succeed(KSInline.KSInlineLink(e.position, Unit, link))
    }
  }

  private fun parseInlineTerm(
    e : KSExpression.KSExpressionList) : KSParseResult<KSInline.KSInlineTerm<Unit>> {
    when {
      KSExpressionMatch.matches(e, CommandMatchers.term_type) -> {
        Assertive.require(e.elements.size >= 3)
        val texts =
          e.elements.subList(2, e.elements.size)
        Assertive.require(texts.size >= 1)
        val type =
          parseAttributeType(e.elements[1] as KSExpression.KSExpressionList)
        val content =
          KSParseResult.map({ t -> parseInlineText(t) }, texts)
        return content flatMap { cs ->
          KSParseResult.succeed(
            KSInline.KSInlineTerm(e.position, Unit, Optional.of(type), cs))
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.term)      -> {
        Assertive.require(e.elements.size >= 2)
        val texts =
          e.elements.subList(1, e.elements.size)
        Assertive.require(texts.size >= 1)
        val content =
          KSParseResult.map({ t -> parseInlineText(t) }, texts)
        return content flatMap { cs ->
          KSParseResult.succeed(
            KSInline.KSInlineTerm(e.position, Unit, Optional.empty(), cs))
        }
      }
    }

    return failedToMatchResult(
      e, listOf(CommandMatchers.term_type, CommandMatchers.term))
  }

  private val parsers : Map<String, ElementParser> =
    makeParsers()
  private val parserDescriptions : String =
    makeMapDescription(parsers)

  private fun makeParsers() : Map<String, ElementParser> {
    val m = HashMap<String, ElementParser>()
    m.put("term", ElementParser("term", { parseInlineTerm(it) }))
    m.put("verbatim", ElementParser("verbatim", { parseInlineVerbatim(it) }))
    m.put("link-ext", ElementParser("link-ext", { parseInlineLinkExternal(it) }))
    m.put("link", ElementParser("link", { parseInlineLinkInternal(it) }))
    m.put("image", ElementParser("image", { parseInlineImage(it) }))
    return m
  }

  private data class ElementParser(
    val name : String,
    val parser : (KSExpression.KSExpressionList) -> KSParseResult<out KSInline<Unit>>)

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

  private fun commandName(e : KSExpression.KSExpressionList) : String {
    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpression.KSExpressionSymbol)
    return (e.elements[0] as KSExpression.KSExpressionSymbol).text
  }

  private val isInlineElement =
    KSExpressionMatch.prefixOfList(
      listOf(KSExpressionMatch.MatchSymbol(
        { s -> parsers.containsKey(s) },
        parserDescriptions)))

  private fun parseInlineAny(
    e : KSExpression) : KSParseResult<out KSInline<Unit>> {
    return when (e) {
      is KSExpression.KSExpressionQuoted ->
        KSParseResult.succeed(KSInline.KSInlineText(e.position, Unit, e.text))
      is KSExpression.KSExpressionSymbol ->
        KSParseResult.succeed(KSInline.KSInlineText(e.position, Unit, e.text))
      is KSExpression.KSExpressionList   -> {
        if (!KSExpressionMatch.matches(e, isInlineElement)) {
          val sb = StringBuilder()
          sb.append("Expected an inline element.")
          sb.append(System.lineSeparator())
          sb.append("  Expected: ")
          sb.append(isInlineElement)
          sb.append(System.lineSeparator())
          sb.append("  Received: ")
          sb.append(e)
          sb.append(System.lineSeparator())
          return parseError(e, sb.toString())
        }

        val name = commandName(e)
        Assertive.require(parsers.containsKey(name))
        val ic = parsers.get(name)!!
        Assertive.require(ic.name == name)
        return ic.parser.invoke(e)
      }
    }
  }
}
