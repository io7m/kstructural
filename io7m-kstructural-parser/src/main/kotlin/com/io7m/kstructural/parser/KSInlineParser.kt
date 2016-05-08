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
import com.io7m.kstructural.core.KSElement.KSInline
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineFootnoteReference
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineImage
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineLink
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineListOrdered
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineListUnordered
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineTable
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineTerm
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineText
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineVerbatim
import com.io7m.kstructural.core.KSElement.KSInline.KSListItem
import com.io7m.kstructural.core.KSElement.KSInline.KSSize
import com.io7m.kstructural.core.KSElement.KSInline.KSTableBody
import com.io7m.kstructural.core.KSElement.KSInline.KSTableBodyCell
import com.io7m.kstructural.core.KSElement.KSInline.KSTableBodyRow
import com.io7m.kstructural.core.KSElement.KSInline.KSTableHead
import com.io7m.kstructural.core.KSElement.KSInline.KSTableHeadColumnName
import com.io7m.kstructural.core.KSElement.KSInline.KSTableSummary
import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSLexicalType
import com.io7m.kstructural.core.KSLink
import com.io7m.kstructural.core.KSLinkContent
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.parser.KSExpression.KSExpressionList
import com.io7m.kstructural.parser.KSExpression.KSExpressionQuoted
import com.io7m.kstructural.parser.KSExpression.KSExpressionSymbol
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

    val footnote_ref_name =
      KSExpressionMatch.exactSymbol("footnote-ref")
    val footnote_ref =
      KSExpressionMatch.allOfList(listOf(footnote_ref_name, symbol_or_string))

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

    val list_item_name =
      KSExpressionMatch.exactSymbol("item")
    val list_item =
      KSExpressionMatch.prefixOfList(listOf(list_item_name))

    val list_ordered_name =
      KSExpressionMatch.exactSymbol("list-ordered")
    val list_ordered =
      KSExpressionMatch.prefixOfList(listOf(list_ordered_name))

    val list_unordered_name =
      KSExpressionMatch.exactSymbol("list-unordered")
    val list_unordered =
      KSExpressionMatch.prefixOfList(listOf(list_unordered_name))

    val summary_name =
      KSExpressionMatch.exactSymbol("summary")
    val summary =
      KSExpressionMatch.prefixOfList(listOf(summary_name))

    val head_name =
      KSExpressionMatch.exactSymbol("head")
    val head =
      KSExpressionMatch.prefixOfList(listOf(head_name))

    val body_name =
      KSExpressionMatch.exactSymbol("body")
    val body =
      KSExpressionMatch.prefixOfList(listOf(body_name))

    val name_name =
      KSExpressionMatch.exactSymbol("name")
    val name =
      KSExpressionMatch.prefixOfList(listOf(name_name))

    val row_name =
      KSExpressionMatch.exactSymbol("row")
    val row =
      KSExpressionMatch.prefixOfList(listOf(row_name))

    val cell_name =
      KSExpressionMatch.exactSymbol("cell")
    val cell =
      KSExpressionMatch.prefixOfList(listOf(cell_name))

    val table_name =
      KSExpressionMatch.exactSymbol("table")
    val table =
      KSExpressionMatch.prefixOfList(listOf(table_name, summary, body))
    val table_type =
      KSExpressionMatch.prefixOfList(listOf(table_name, summary, type, body))
    val table_head =
      KSExpressionMatch.prefixOfList(listOf(table_name, summary, head, body))
    val table_head_type =
      KSExpressionMatch.prefixOfList(listOf(table_name, summary, type, head, body))
  }

  override fun parse(
    e : KSExpression) : KSResult<KSInline<Unit>, KSParseError> {
    return parseInlineAny(e)
  }

  private fun <A : Any> parseError(
    e : KSLexicalType, m : String) : KSResult<A, KSParseError> =
    KSResult.fail<A, KSParseError>(KSParseError(e.position, m))

  private fun <A : Any> failedToMatchResult(
    e : KSExpression,
    m : List<KSExpressionMatch>) : KSResult<A, KSParseError> =
    KSResult.fail(failedToMatch(e, m))

  private fun failedToMatch(
    e : KSExpression,
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

  private fun parseAttributeType(e : KSExpressionList) : String {
    Assertive.require(e.elements.size == 2)
    Assertive.require(e.elements[0] is KSExpressionSymbol)
    Assertive.require(e.elements[1] is KSExpressionSymbol)
    return (e.elements[1] as KSExpressionSymbol).text
  }

  private fun parseAttributeTarget(e : KSExpressionList) : String {
    Assertive.require(e.elements.size == 2)
    Assertive.require(e.elements[0] is KSExpressionSymbol)
    val target = e.elements[1]
    return when (target) {
      is KSExpressionList   -> throw UnreachableCodeException()
      is KSExpressionSymbol -> target.text
      is KSExpressionQuoted -> target.text
    }
  }

  private fun parseAttributeTargetAsURI(
    e : KSExpressionList) : KSResult<URI, KSParseError> {
    val text = parseAttributeTarget(e)
    return try {
      KSResult.succeed(URI(text))
    } catch (x : URISyntaxException) {
      parseError(e, "Invalid URI: " + x.message)
    }
  }

  private fun parseAttributeSize(
    e : KSExpressionList)
    : KSResult<KSSize, KSParseError> {
    Assertive.require(e.elements.size == 3)
    Assertive.require(e.elements[0] is KSExpressionSymbol)
    Assertive.require(e.elements[1] is KSExpressionSymbol)
    Assertive.require(e.elements[2] is KSExpressionSymbol)

    return try {
      val w = BigInteger((e.elements[1] as KSExpressionSymbol).text)
      val h = BigInteger((e.elements[2] as KSExpressionSymbol).text)

      if (w.compareTo(BigInteger.ZERO) < 0) {
        return parseError(e.elements[1], "Width is negative")
      }
      if (h.compareTo(BigInteger.ZERO) < 0) {
        return parseError(e.elements[2], "Height is negative")
      }

      return KSResult.succeed(KSSize(w, h))
    } catch (x : NumberFormatException) {
      return parseError(e, "Invalid width or height: " + x.message)
    }
  }

  private fun parseInlineImage(
    e : KSExpressionList)
    : KSResult<KSInlineImage<Unit>, KSParseError> {
    when {
      KSExpressionMatch.matches(e, CommandMatchers.image_with_type_size) -> {
        Assertive.require(e.elements.size >= 5)

        val act_target = parseAttributeTargetAsURI(
          e.elements[1] as KSExpressionList)
        val type = Optional.of(
          parseAttributeType(e.elements[2] as KSExpressionList))
        val act_size = parseAttributeSize(
          e.elements[3] as KSExpressionList)
        val texts =
          e.elements.subList(4, e.elements.size)
        val act_content =
          KSResult.listMap({ t -> parseInlineText(t) }, texts)

        return act_size flatMap { size ->
          act_content flatMap { content ->
            act_target flatMap { target ->
              KSResult.succeed<KSInlineImage<Unit>, KSParseError>(
                KSInlineImage(
                  e.position, Unit, type, target, Optional.of(size), content))
            }
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.image_with_type)      -> {
        Assertive.require(e.elements.size >= 4)

        val act_target = parseAttributeTargetAsURI(
          e.elements[1] as KSExpressionList)
        val type = Optional.of(
          parseAttributeType(e.elements[2] as KSExpressionList))
        val size =
          Optional.empty<KSSize>()
        val texts =
          e.elements.subList(3, e.elements.size)
        val act_content =
          KSResult.listMap({ t -> parseInlineText(t) }, texts)

        return act_content flatMap { content ->
          act_target flatMap { target ->
            KSResult.succeed<KSInlineImage<Unit>, KSParseError>(
              KSInlineImage(e.position, Unit, type, target, size, content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.image_with_size)      -> {
        Assertive.require(e.elements.size >= 4)

        val act_target =
          parseAttributeTargetAsURI(e.elements[1] as KSExpressionList)
        val type =
          Optional.empty<String>()
        val act_size = parseAttributeSize(
          e.elements[2] as KSExpressionList)
        val texts =
          e.elements.subList(3, e.elements.size)
        val act_content =
          KSResult.listMap({ t -> parseInlineText(t) }, texts)

        return act_size flatMap { size ->
          act_content flatMap { content ->
            act_target flatMap { target ->
              KSResult.succeed<KSInlineImage<Unit>, KSParseError>(
                KSInlineImage(
                  e.position, Unit, type, target, Optional.of(size), content))
            }
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.image)                -> {
        Assertive.require(e.elements.size >= 3)
        val act_target =
          parseAttributeTargetAsURI(e.elements[1] as KSExpressionList)
        val type =
          Optional.empty<String>()
        val size =
          Optional.empty<KSSize>()
        val texts =
          e.elements.subList(2, e.elements.size)
        val act_content =
          KSResult.listMap({ t -> parseInlineText(t) }, texts)

        return act_content flatMap { content ->
          act_target flatMap { target ->
            KSResult.succeed<KSInlineImage<Unit>, KSParseError>(
              KSInlineImage(e.position, Unit, type, target, size, content))
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
    e : KSExpression) : KSResult<KSInlineText<Unit>, KSParseError> {
    return when (e) {
      is KSExpressionList   -> {
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
      is KSExpressionSymbol ->
        KSResult.succeed(KSInlineText(e.position, Unit, e.text))
      is KSExpressionQuoted ->
        KSResult.succeed(KSInlineText(e.position, Unit, e.text))
    }
  }

  private fun parseInlineVerbatim(
    e : KSExpressionList)
    : KSResult<KSInlineVerbatim<Unit>, KSParseError> {
    when {
      KSExpressionMatch.matches(e, CommandMatchers.verbatim_type) -> {
        Assertive.require(e.elements.size == 3)
        val type =
          parseAttributeType(e.elements[1] as KSExpressionList)
        val content =
          (e.elements[2] as KSExpressionQuoted).text
        return KSResult.succeed(
          KSInlineVerbatim(e.position, Unit, Optional.of(type), content))
      }
      KSExpressionMatch.matches(e, CommandMatchers.verbatim)      -> {
        Assertive.require(e.elements.size == 2)
        val content =
          (e.elements[1] as KSExpressionQuoted).text
        return KSResult.succeed(
          KSInlineVerbatim(e.position, Unit, Optional.empty(), content))
      }
    }

    return failedToMatchResult(e,
      listOf(CommandMatchers.verbatim_type, CommandMatchers.verbatim))
  }

  private fun parseLinkInternal(
    e : KSExpressionList)
    : KSResult<KSLink.KSLinkInternal<Unit>, KSParseError> {

    if (KSExpressionMatch.matches(e, CommandMatchers.link)) {
      Assertive.require(e.elements.size >= 3)
      val texts = e.elements.subList(2, e.elements.size)
      Assertive.require(texts.size >= 1)

      val target =
        parseAttributeTarget(e.elements[1] as KSExpressionList)
      val kid =
        KSID(e.elements[1].position, target, Unit)
      val content =
        KSResult.listMap({ t -> parseLinkContent(t) }, texts)
      return content flatMap { cs ->
        KSResult.succeed<KSLink.KSLinkInternal<Unit>, KSParseError>(
          KSLink.KSLinkInternal(e.position, kid, cs))
      }
    } else {
      return failedToMatchResult(e, listOf(CommandMatchers.link))
    }
  }

  private fun parseLinkExternal(
    e : KSExpressionList)
    : KSResult<KSLink.KSLinkExternal<Unit>, KSParseError> {

    if (KSExpressionMatch.matches(e, CommandMatchers.link_ext)) {
      Assertive.require(e.elements.size >= 3)
      val texts = e.elements.subList(2, e.elements.size)
      Assertive.require(texts.size >= 1)

      val target = parseAttributeTarget(e.elements[1] as KSExpressionList)
      try {
        val uri = URI(target)
        val content = KSResult.listMap({ t -> parseLinkContent(t) }, texts)
        return content flatMap { cs ->
          KSResult.succeed<KSLink.KSLinkExternal<Unit>, KSParseError>(
            KSLink.KSLinkExternal(e.position, uri, cs))
        }
      } catch (x : URISyntaxException) {
        return parseError(e, "Invalid URI: " + x.message)
      }

    } else {
      return failedToMatchResult(e, listOf(CommandMatchers.link_ext))
    }
  }

  private fun parseInlineToLinkContent(
    e : KSInline<Unit>) : KSResult<KSLinkContent<Unit>, KSParseError> {
    return when (e) {
      is KSInlineText                       ->
        KSResult.succeed(KSLinkContent.KSLinkText(e.position, Unit, e))

      is KSInlineImage                      ->
        KSResult.succeed(KSLinkContent.KSLinkImage(e.position, Unit, e))

      is KSInlineLink                       ->
        parseError(e, "Link elements cannot appear inside link elements")

      is KSInlineVerbatim                   ->
        parseError(e, "Verbatim elements cannot appear inside link elements")

      is KSInlineTerm                       ->
        parseError(e, "Term elements cannot appear inside link elements")

      is KSInline.KSInlineListOrdered       ->
        parseError(e, "List elements cannot appear inside link elements")

      is KSInline.KSInlineListUnordered     ->
        parseError(e, "List elements cannot appear inside link elements")

      is KSInline.KSInlineTable             ->
        parseError(e, "Table elements cannot appear inside link elements")

      is KSInline.KSInlineFootnoteReference ->
        parseError(e, "Footnote references cannot appear inside link elements")
    }
  }

  private fun parseLinkContent(
    e : KSExpression) : KSResult<KSLinkContent<Unit>, KSParseError> {
    return when (e) {
      is KSExpressionList   ->
        parseInlineAny(e) flatMap { result -> parseInlineToLinkContent(result) }
      is KSExpressionSymbol ->
        KSResult.succeed(KSLinkContent.KSLinkText(
          e.position, Unit, KSInlineText(e.position, Unit, e.text)))
      is KSExpressionQuoted ->
        KSResult.succeed(KSLinkContent.KSLinkText(
          e.position, Unit, KSInlineText(e.position, Unit, e.text)))
    }
  }

  private fun parseInlineLinkInternal(
    e : KSExpressionList)
    : KSResult<KSInlineLink<Unit>, KSParseError> {
    return parseLinkInternal(e) flatMap {
      link ->
      KSResult.succeed<KSInlineLink<Unit>, KSParseError>(
        KSInlineLink(e.position, Unit, link))
    }
  }

  private fun parseInlineLinkExternal(
    e : KSExpressionList)
    : KSResult<KSInlineLink<Unit>, KSParseError> {
    return parseLinkExternal(e) flatMap {
      link ->
      KSResult.succeed<KSInlineLink<Unit>, KSParseError>(
        KSInlineLink(e.position, Unit, link))
    }
  }

  private fun parseInlineTerm(
    e : KSExpressionList)
    : KSResult<KSInlineTerm<Unit>, KSParseError> {
    when {
      KSExpressionMatch.matches(e, CommandMatchers.term_type) -> {
        Assertive.require(e.elements.size >= 3)
        val texts =
          e.elements.subList(2, e.elements.size)
        Assertive.require(texts.size >= 1)
        val type =
          parseAttributeType(e.elements[1] as KSExpressionList)
        val content =
          KSResult.listMap({ t -> parseInlineText(t) }, texts)
        return content flatMap { cs ->
          KSResult.succeed<KSInlineTerm<Unit>, KSParseError>(
            KSInlineTerm(e.position, Unit, Optional.of(type), cs))
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.term)      -> {
        Assertive.require(e.elements.size >= 2)
        val texts =
          e.elements.subList(1, e.elements.size)
        Assertive.require(texts.size >= 1)
        val content =
          KSResult.listMap({ t -> parseInlineText(t) }, texts)
        return content flatMap { cs ->
          KSResult.succeed<KSInlineTerm<Unit>, KSParseError>(
            KSInlineTerm(e.position, Unit, Optional.empty(), cs))
        }
      }
    }

    return failedToMatchResult(
      e, listOf(CommandMatchers.term_type, CommandMatchers.term))
  }

  private fun parseInlineListOrdered(
    e : KSExpressionList)
    : KSResult<KSInlineListOrdered<Unit>, KSParseError> {

    when {
      KSExpressionMatch.matches(e, CommandMatchers.list_ordered) -> {
        Assertive.require(e.elements.size >= 1)
        val texts = e.elements.subList(1, e.elements.size)
        val content = KSResult.listMap({ t -> parseListItem(t) }, texts)
        return content flatMap { cs ->
          KSResult.succeed<KSInlineListOrdered<Unit>, KSParseError>(
            KSInlineListOrdered(e.position, Unit, cs))
        }
      }
    }

    return failedToMatchResult(e, listOf(CommandMatchers.list_ordered))
  }

  private fun parseInlineListUnordered(
    e : KSExpressionList)
    : KSResult<KSInlineListUnordered<Unit>, KSParseError> {

    when {
      KSExpressionMatch.matches(e, CommandMatchers.list_unordered) -> {
        Assertive.require(e.elements.size >= 1)
        val texts = e.elements.subList(1, e.elements.size)
        val content = KSResult.listMap({ t -> parseListItem(t) }, texts)
        return content flatMap { cs ->
          KSResult.succeed<KSInlineListUnordered<Unit>, KSParseError>(
            KSInlineListUnordered(e.position, Unit, cs))
        }
      }
    }

    return failedToMatchResult(e, listOf(CommandMatchers.list_unordered))
  }

  private fun parseListItem(
    e : KSExpression) : KSResult<KSListItem<Unit>, KSParseError> {
    when {
      KSExpressionMatch.matches(e, CommandMatchers.list_item) -> {
        e as KSExpressionList
        Assertive.require(e.elements.size >= 1)
        val contents = e.elements.subList(1, e.elements.size)
        Assertive.require(contents.size >= 1)
        val act_content =
          KSResult.listMap({ ce -> parseInlineAny(ce) }, contents)
        return act_content flatMap { cs ->
          KSResult.succeed<KSListItem<Unit>, KSParseError>(
            KSListItem(e.position, Unit, cs))
        }
      }
    }

    return failedToMatchResult(e, listOf(CommandMatchers.list_item))
  }

  private fun parseInlineTable(
    e : KSExpressionList)
    : KSResult<KSInlineTable<Unit>, KSParseError> {

    when {
      KSExpressionMatch.matches(e, CommandMatchers.table_head_type) -> {
        Assertive.require(e.elements.size == 5)

        val act_summary = parseTableSummary(e.elements[1])
        val type = parseAttributeType(e.elements[2] as KSExpressionList)
        val act_head = parseTableHead(e.elements[3] as KSExpressionList)
        val act_body = parseTableBody(e.elements[4] as KSExpressionList)

        return act_summary flatMap { summary ->
          act_head flatMap { head ->
            act_body flatMap { body ->
              val opt_type = Optional.of(type)
              val opt_head = Optional.of(head)
              KSResult.succeed<KSInlineTable<Unit>, KSParseError>(KSInlineTable(
                e.position, Unit, opt_type, summary, opt_head, body))
            }
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.table_head)      -> {
        Assertive.require(e.elements.size == 4)

        val act_summary = parseTableSummary(e.elements[1])
        val act_head = parseTableHead(e.elements[2] as KSExpressionList)
        val act_body = parseTableBody(e.elements[3] as KSExpressionList)

        return act_summary flatMap { summary ->
          act_head flatMap { head ->
            act_body flatMap { body ->
              val opt_type = Optional.empty<String>()
              val opt_head = Optional.of(head)
              KSResult.succeed<KSInlineTable<Unit>, KSParseError>(KSInlineTable(
                e.position, Unit, opt_type, summary, opt_head, body))
            }
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.table_type)      -> {
        Assertive.require(e.elements.size == 4)

        val act_summary = parseTableSummary(e.elements[1])
        val type = parseAttributeType(e.elements[2] as KSExpressionList)
        val act_body = parseTableBody(e.elements[3] as KSExpressionList)

        return act_summary flatMap { summary ->
          act_body flatMap { body ->
            val opt_type = Optional.of(type)
            val opt_head = Optional.empty<KSTableHead<Unit>>()
            KSResult.succeed<KSInlineTable<Unit>, KSParseError>(KSInlineTable(
              e.position, Unit, opt_type, summary, opt_head, body))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.table)           -> {
        Assertive.require(e.elements.size == 3)

        val act_summary = parseTableSummary(e.elements[1])
        val act_body = parseTableBody(e.elements[2] as KSExpressionList)

        return act_summary flatMap { summary ->
          act_body flatMap { body ->
            val opt_type = Optional.empty<String>()
            val opt_head = Optional.empty<KSTableHead<Unit>>()
            KSResult.succeed<KSInlineTable<Unit>, KSParseError>(KSInlineTable(
              e.position, Unit, opt_type, summary, opt_head, body))
          }
        }
      }
    }

    return failedToMatchResult(e, listOf(
      CommandMatchers.table,
      CommandMatchers.table_type,
      CommandMatchers.table_head,
      CommandMatchers.table_head_type))
  }

  private fun parseTableBody(
    e : KSExpressionList) : KSResult<KSTableBody<Unit>, KSParseError> {
    when {
      KSExpressionMatch.matches(e, CommandMatchers.body) -> {
        Assertive.require(e.elements.size >= 1)
        val contents = e.elements.subList(1, e.elements.size)
        val act_content =
          KSResult.listMap({ ce -> parseTableBodyRow(ce) }, contents)
        return act_content flatMap { cs ->
          KSResult.succeed<KSTableBody<Unit>, KSParseError>(
            KSTableBody(e.position, Unit, cs))
        }
      }
    }

    return failedToMatchResult(e, listOf(CommandMatchers.body))
  }

  private fun parseTableBodyRow(
    e : KSExpression) : KSResult<KSTableBodyRow<Unit>, KSParseError> {
    when {
      KSExpressionMatch.matches(e, CommandMatchers.row) -> {
        e as KSExpressionList
        Assertive.require(e.elements.size >= 1)
        val contents = e.elements.subList(1, e.elements.size)
        val act_content =
          KSResult.listMap({ ce -> parseTableBodyCell(ce) }, contents)
        return act_content flatMap { cs ->
          KSResult.succeed<KSTableBodyRow<Unit>, KSParseError>(
            KSTableBodyRow(e.position, Unit, cs))
        }
      }
    }

    return failedToMatchResult(e, listOf(CommandMatchers.row))
  }

  private fun parseTableBodyCell(
    e : KSExpression) : KSResult<KSTableBodyCell<Unit>, KSParseError> {
    when {
      KSExpressionMatch.matches(e, CommandMatchers.cell) -> {
        e as KSExpressionList
        Assertive.require(e.elements.size >= 1)
        val contents = e.elements.subList(1, e.elements.size)
        val act_content =
          KSResult.listMap({ ce -> parseInlineAny(ce) }, contents)
        return act_content flatMap { cs ->
          KSResult.succeed<KSTableBodyCell<Unit>, KSParseError>(
            KSTableBodyCell(e.position, Unit, cs))
        }
      }
    }

    return failedToMatchResult(e, listOf(CommandMatchers.cell))
  }

  private fun parseTableHead(
    e : KSExpressionList) : KSResult<KSTableHead<Unit>, KSParseError> {
    when {
      KSExpressionMatch.matches(e, CommandMatchers.head) -> {
        Assertive.require(e.elements.size >= 1)
        val contents = e.elements.subList(1, e.elements.size)
        val act_content =
          KSResult.listMap({ ce -> parseTableColumnName(ce) }, contents)
        return act_content flatMap { cs ->
          KSResult.succeed<KSTableHead<Unit>, KSParseError>(
            KSTableHead(e.position, Unit, cs))
        }
      }
    }

    return failedToMatchResult(e, listOf(CommandMatchers.head))
  }

  private fun parseTableColumnName(
    e : KSExpression) : KSResult<KSTableHeadColumnName<Unit>, KSParseError> {
    when {
      KSExpressionMatch.matches(e, CommandMatchers.name) -> {
        e as KSExpressionList
        Assertive.require(e.elements.size >= 1)
        val contents = e.elements.subList(1, e.elements.size)
        Assertive.require(contents.size >= 1)
        val act_content =
          KSResult.listMap({ ce -> parseInlineText(ce) }, contents)
        return act_content flatMap { cs ->
          KSResult.succeed<KSTableHeadColumnName<Unit>, KSParseError>(
            KSTableHeadColumnName(e.position, Unit, cs))
        }
      }
    }

    return failedToMatchResult(e, listOf(CommandMatchers.name))
  }

  private fun parseTableSummary(
    e : KSExpression) : KSResult<KSTableSummary<Unit>, KSParseError> {
    when {
      KSExpressionMatch.matches(e, CommandMatchers.summary) -> {
        e as KSExpressionList
        Assertive.require(e.elements.size >= 1)
        val contents = e.elements.subList(1, e.elements.size)
        Assertive.require(contents.size >= 1)
        val act_content =
          KSResult.listMap({ ce -> parseInlineText(ce) }, contents)
        return act_content flatMap { cs ->
          KSResult.succeed<KSTableSummary<Unit>, KSParseError>(
            KSTableSummary(e.position, Unit, cs))
        }
      }
    }

    return failedToMatchResult(e, listOf(CommandMatchers.summary))
  }

  private fun parseInlineFootnoteReference(
    e : KSExpressionList)
    : KSResult<KSInlineFootnoteReference<Unit>, KSParseError> {

    when {
      KSExpressionMatch.matches(e, CommandMatchers.footnote_ref) -> {
        Assertive.require(e.elements.size == 2)
        val target = parseAttributeTarget(e)
        val kid = KSID(e.elements[1].position, target, Unit)
        return KSResult.succeed(KSInlineFootnoteReference(e.position, Unit, kid))
      }
    }

    return failedToMatchResult(e, listOf(CommandMatchers.footnote_ref))
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
    m.put("footnote-ref", ElementParser("footnote-ref", { parseInlineFootnoteReference(it) }))
    m.put("image", ElementParser("image", { parseInlineImage(it) }))
    m.put("list-ordered", ElementParser("list-ordered", { parseInlineListOrdered(it) }))
    m.put("list-unordered", ElementParser("list-unordered", { parseInlineListUnordered(it) }))
    m.put("table", ElementParser("table", { parseInlineTable(it) }))
    return m
  }

  private data class ElementParser(
    val name : String,
    val parser : (KSExpressionList) -> KSResult<KSInline<Unit>, KSParseError>)

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

  private fun commandName(e : KSExpressionList) : String {
    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpressionSymbol)
    return (e.elements[0] as KSExpressionSymbol).text
  }

  private val isInlineElement =
    KSExpressionMatch.prefixOfList(
      listOf(KSExpressionMatch.MatchSymbol(
        { s -> parsers.containsKey(s) },
        parserDescriptions)))

  private fun parseInlineAny(
    e : KSExpression) : KSResult<KSInline<Unit>, KSParseError> {
    return when (e) {
      is KSExpressionQuoted ->
        KSResult.succeed(KSInlineText(e.position, Unit, e.text))
      is KSExpressionSymbol ->
        KSResult.succeed(KSInlineText(e.position, Unit, e.text))
      is KSExpressionList   -> {
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
