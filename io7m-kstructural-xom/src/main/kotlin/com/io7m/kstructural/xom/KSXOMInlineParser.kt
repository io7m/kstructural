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

package com.io7m.kstructural.xom

import com.io7m.jlexing.core.LexicalPositionType
import com.io7m.kstructural.core.KSElement.KSInline
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineFootnoteReference
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineImage
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineInclude
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
import com.io7m.kstructural.core.KSLink
import com.io7m.kstructural.core.KSLinkContent
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSParseContextType
import com.io7m.kstructural.core.KSParseError
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.core.KSType
import com.io7m.kstructural.schema.KSSchemaNamespaces
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.valid4j.Assertive
import java.math.BigInteger
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Path
import java.util.Optional

class KSXOMInlineParser private constructor() : KSXOMInlineParserType {

  private val SQUARE = true

  override fun parse(
    context : KSParseContextType,
    node : Node) : KSResult<KSInline<KSParse>, KSParseError> {

    if (node is Text) {
      return parseInlineText(context, node)
    }

    if (node is Element) {
      return parseElement(context, node)
    }

    val sb = StringBuilder()
    sb.append("Unexpected element.")
    sb.append(System.lineSeparator())
    sb.append("  Expected: An element, or text")
    sb.append(System.lineSeparator())
    sb.append("  Received: ")
    sb.append(node)
    sb.append(System.lineSeparator())
    return fail(KSParseError(no_lex, sb.toString()))
  }

  private fun parseElement(
    context : KSParseContextType,
    element : Element) : KSResult<KSInline<KSParse>, KSParseError> {

    return when (element.localName) {
      "image"          -> parseElementImage(context, element)
      "term"           -> parseElementTerm(context, element)
      "verbatim"       -> parseElementVerbatim(context, element)
      "link"           -> parseElementLink(context, element)
      "link-external"  -> parseElementLinkExternal(context, element)
      "list-ordered"   -> parseElementListOrdered(context, element)
      "list-unordered" -> parseElementListUnordered(context, element)
      "table"          -> parseElementTable(context, element)
      "footnote-ref"   -> parseElementFootnoteReference(context, element)
      else             ->
        fail(KSParseError(no_lex, "Unrecognized element: " + element.localName))
    }
  }

  private fun parseElementFootnoteReference(
    context : KSParseContextType,
    element : Element)
    : KSResult<KSInlineFootnoteReference<KSParse>, KSParseError> {
    Assertive.require(element.localName == "footnote-ref")

    val ta = element.getAttribute("target", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    return parseID(context, ta.value).flatMap { id ->
      succeed(KSInlineFootnoteReference(no_lex, SQUARE, KSParse(context), id))
    }
  }

  private fun parseElementTable(
    context : KSParseContextType,
    element : Element)
    : KSResult<KSInlineTable<KSParse>, KSParseError> {
    Assertive.require(element.localName == "table")

    val kp = KSParse(context)
    val act_type = parseType(context, element)
    val summary = parseSummary(context, element)

    val eh = element.getFirstChildElement(
      "head", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    val act_head = if (eh != null) {
      parseElementTableHead(context, eh).flatMap { head ->
        succeed(Optional.of(head))
      }
    } else {
      succeed(Optional.empty())
    }

    val eb = element.getFirstChildElement(
      "body", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    val act_body : KSResult<KSTableBody<KSParse>, KSParseError> =
      if (eb != null) {
        parseElementTableBody(context, eb)
      } else {
        KSResult.fail(KSParseError(no_lex, "No table body provided"))
      }

    return act_head.flatMap { head ->
      act_body.flatMap { body ->
        act_type.flatMap { type ->
          succeed(KSInlineTable(no_lex, SQUARE, kp, type, summary, head, body))
        }
      }
    }
  }

  private fun parseSummary(
    context : KSParseContextType,
    element : Element)
    : KSTableSummary<KSParse> {
    val tt = element.getAttribute(
      "summary", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    val kp = KSParse(context)
    return KSTableSummary(
      no_lex,
      false,
      kp,
      listOf(KSInlineText(no_lex, SQUARE, kp, false, tt.value)))
  }

  private fun parseElementTableBody(
    context : KSParseContextType,
    element : Element)
    : KSResult<KSTableBody<KSParse>, KSParseError> {
    Assertive.require(element.localName == "body")

    val act_rows = KSResult.listMap(
      { c -> parseTableRow(context, c) }, listOfChildElements(element))

    return act_rows.flatMap { rows ->
      succeed(KSTableBody(no_lex, SQUARE, KSParse(context), rows))
    }
  }

  private fun parseTableRow(
    context : KSParseContextType,
    element : Element)
    : KSResult<KSTableBodyRow<KSParse>, KSParseError> {
    Assertive.require(element.localName == "row")

    val act_cells = KSResult.listMap(
      { c -> parseTableCell(context, c) }, listOfChildElements(element))

    return act_cells.flatMap { cells ->
      succeed(KSTableBodyRow(no_lex, SQUARE, KSParse(context), cells))
    }
  }

  private fun parseTableCell(
    context : KSParseContextType,
    element : Element)
    : KSResult<KSTableBodyCell<KSParse>, KSParseError> {
    Assertive.require(element.localName == "cell")

    val act_content = KSResult.listMap(
      { c -> parse(context, c) },
      KSXOMTokenizer.tokenizeNodes(listOfChildren(element)))

    return act_content.flatMap { content ->
      succeed(KSTableBodyCell(no_lex, SQUARE, KSParse(context), content))
    }
  }

  private fun parseElementTableHead(
    context : KSParseContextType,
    element : Element)
    : KSResult<KSTableHead<KSParse>, KSParseError> {
    Assertive.require(element.localName == "head")

    val act_names = KSResult.listMap(
      { c -> parseTableColumnName(context, c) }, listOfChildElements(element))

    return act_names.flatMap { names ->
      succeed(KSTableHead(no_lex, SQUARE, KSParse(context), names))
    }
  }

  private fun parseTableColumnName(
    context : KSParseContextType,
    element : Element)
    : KSResult<KSTableHeadColumnName<KSParse>, KSParseError> {
    Assertive.require(element.localName == "name")

    val act_content = KSResult.listMap(
      { e -> parseInlineText(context, e) }, listOfChildren(element))
    return act_content.flatMap { content ->
      succeed(KSTableHeadColumnName(no_lex, SQUARE, KSParse(context), content))
    }
  }

  private fun parseElementListOrdered(
    context : KSParseContextType,
    element : Element)
    : KSResult<KSInlineListOrdered<KSParse>, KSParseError> {
    Assertive.require(element.localName == "list-ordered")

    val kp = KSParse(context)
    val act_content = KSResult.listMap(
      { e -> parseListItem(context, e) }, listOfChildElements(element))

    return act_content.flatMap { content ->
      succeed(KSInlineListOrdered(no_lex, SQUARE, kp, content))
    }
  }

  private fun parseElementListUnordered(
    context : KSParseContextType,
    element : Element)
    : KSResult<KSInlineListUnordered<KSParse>, KSParseError> {
    Assertive.require(element.localName == "list-unordered")

    val kp = KSParse(context)
    val act_content = KSResult.listMap(
      { e -> parseListItem(context, e) }, listOfChildElements(element))

    return act_content.flatMap { content ->
      succeed(KSInlineListUnordered(no_lex, SQUARE, kp, content))
    }
  }

  private fun parseListItem(
    context : KSParseContextType,
    e : Element)
    : KSResult<KSListItem<KSParse>, KSParseError> {

    val fail = {
      val sb = StringBuilder()
      sb.append("Unexpected element.")
      sb.append(System.lineSeparator())
      sb.append("  Expected: A list item")
      sb.append(System.lineSeparator())
      sb.append("  Received: ")
      sb.append(e)
      sb.append(System.lineSeparator())
      KSResult.fail<KSListItem<KSParse>, KSParseError>(
        KSParseError(no_lex, sb.toString()))
    }

    return if (e.localName == "item") {
      val kp = KSParse(context)
      val act_content = KSResult.listMap(
        { e -> parse(context, e) },
        KSXOMTokenizer.tokenizeNodes(listOfChildren(e)))

      return act_content.flatMap { content ->
        succeed(KSListItem(no_lex, SQUARE, kp, content))
      }
    } else {
      fail.invoke()
    }
  }

  private fun parseID(
    context : KSParseContextType,
    text : String)
    : KSResult<KSID<KSParse>, KSParseError> {
    return if (KSID.isValidID(text)) {
      KSResult.succeed<KSID<KSParse>, KSParseError>(
        KSID.create(no_lex, text, KSParse(context)))
    } else {
      val sb = StringBuilder()
      sb.append("Invalid identifier.")
      sb.append(System.lineSeparator())
      sb.append("  Received: ")
      sb.append(text)
      sb.append(System.lineSeparator())
      KSResult.fail(KSParseError(no_lex, sb.toString()))
    }
  }

  private fun parseLink(
    context : KSParseContextType,
    element : Element)
    : KSResult<KSLink.KSLinkInternal<KSParse>, KSParseError> {
    Assertive.require(element.localName == "link")

    val kp = KSParse(context)
    val ta = element.getAttribute("target", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    val act_target = parseID(context, ta.value)
    val act_content = KSResult.listMap(
      { e -> parseLinkContent(context, e) }, listOfChildren(element))

    return act_target.flatMap { id ->
      act_content.flatMap { content ->
        succeed(KSLink.KSLinkInternal(no_lex, id, content))
      }
    }
  }

  private fun parseLinkExternal(
    context : KSParseContextType,
    element : Element)
    : KSResult<KSLink.KSLinkExternal<KSParse>, KSParseError> {
    Assertive.require(element.localName == "link-external")

    val act_target = parseTargetURI(element)
    val act_content = KSResult.listMap(
      { e -> parseLinkContent(context, e) }, listOfChildren(element))

    return act_target.flatMap { target ->
      act_content.flatMap { content ->
        succeed(KSLink.KSLinkExternal(no_lex, target, content))
      }
    }
  }

  private fun parseElementLink(
    context : KSParseContextType,
    element : Element)
    : KSResult<KSInlineLink<KSParse>, KSParseError> {
    return parseLink(context, element).flatMap { link ->
      val kp = KSParse(context)
      succeed(KSInlineLink(link.position, false, kp, link))
    }
  }

  private fun parseElementLinkExternal(
    context : KSParseContextType,
    element : Element)
    : KSResult<KSInlineLink<KSParse>, KSParseError> {
    return parseLinkExternal(context, element).flatMap { link ->
      val kp = KSParse(context)
      succeed(KSInlineLink(link.position, false, kp, link))
    }
  }

  private fun parseElementTerm(
    context : KSParseContextType,
    element : Element)
    : KSResult<KSInline<KSParse>, KSParseError> {
    Assertive.require(element.localName == "term")

    val act_type = parseType(context, element)
    val act_content = KSResult.listMap(
      { e -> parseInlineText(context, e) }, listOfChildren(element))

    return act_content.flatMap { content ->
      act_type.flatMap { type ->
        succeed(KSInlineTerm(no_lex, SQUARE, KSParse(context), type, content))
      }
    }
  }

  private fun parseElementVerbatim(
    context : KSParseContextType,
    element : Element)
    : KSResult<KSInlineVerbatim<KSParse>, KSParseError> {
    Assertive.require(element.localName == "verbatim")

    val act_type = parseType(context, element)
    val kp = KSParse(context)
    val content = KSInlineText(no_lex, SQUARE, kp, true, element.value)
    return act_type.flatMap { type ->
      succeed(KSInlineVerbatim(no_lex, SQUARE, kp, type, content))
    }
  }

  private fun parseElementImage(
    context : KSParseContextType,
    element : Element)
    : KSResult<KSInlineImage<KSParse>, KSParseError> {
    Assertive.require(element.localName == "image")

    val act_type = parseType(context, element)
    val act_target = parseTargetURI(element)
    val act_size = parseSize(element)
    val act_content = KSResult.listMap(
      { e -> parseInlineText(context, e) }, listOfChildren(element))

    return act_target.flatMap { target ->
      act_size.flatMap { size ->
        act_content.flatMap { content ->
          act_type.flatMap { type ->
            succeed(KSInlineImage(
              no_lex, SQUARE, KSParse(context), type, target, size, content))
          }
        }
      }
    }
  }

  private fun checkLinkContent(
    e : KSInline<KSParse>)
    : KSResult<KSLinkContent<KSParse>, KSParseError> =
    when (e) {
      is KSInlineText    ->
        KSResult.succeed(KSLinkContent.KSLinkText(e.position, e.data, e))
      is KSInlineImage   ->
        KSResult.succeed(KSLinkContent.KSLinkImage(e.position, e.data, e))

      is KSInlineLink,
      is KSInlineVerbatim,
      is KSInlineTerm,
      is KSInlineFootnoteReference,
      is KSInlineListOrdered,
      is KSInlineListUnordered,
      is KSInlineTable,
      is KSInlineInclude -> {
        val sb = StringBuilder()
        sb.append("Unexpected element.")
        sb.append(System.lineSeparator())
        sb.append("  Expected: Link content")
        sb.append(System.lineSeparator())
        sb.append("  Received: ")
        sb.append(e)
        sb.append(System.lineSeparator())
        KSResult.fail(KSParseError(no_lex, sb.toString()))
      }
    }

  private fun parseLinkContent(
    context : KSParseContextType,
    e : Node)
    : KSResult<KSLinkContent<KSParse>, KSParseError> {
    return parse(context, e).flatMap { content -> checkLinkContent(content) }
  }

  private fun parseInlineText(
    context : KSParseContextType,
    e : Node)
    : KSResult<KSInlineText<KSParse>, KSParseError> {
    if (e is Text) {
      return succeed(
        KSInlineText(no_lex, SQUARE, KSParse(context), false, e.value))
    }

    val sb = StringBuilder()
    sb.append("Unexpected element.")
    sb.append(System.lineSeparator())
    sb.append("  Expected: text")
    sb.append(System.lineSeparator())
    sb.append("  Received: ")
    sb.append(e)
    sb.append(System.lineSeparator())
    return KSResult.fail(KSParseError(no_lex, sb.toString()))
  }

  private fun listOfChildren(element : Element) : List<Node> {
    val xs = mutableListOf<Node>()
    for (i in 0 .. element.childCount - 1) {
      xs.add(element.getChild(i))
    }
    return xs
  }

  private fun listOfChildElements(element : Element) : List<Element> {
    val xs = mutableListOf<Element>()
    for (i in 0 .. element.childCount - 1) {
      val ec = element.getChild(i)
      if (ec is Element) xs.add(ec)
    }
    return xs
  }

  private fun parseSize(
    element : Element)
    : KSResult<Optional<KSSize>, KSParseError> {
    val tw = element.getAttribute("width", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    val th = element.getAttribute("height", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    return if (tw != null) {
      try {
        succeed(Optional.of(KSSize(BigInteger(tw.value), BigInteger(th.value))))
      } catch (x : NumberFormatException) {
        KSResult.fail<Optional<KSSize>, KSParseError>(
          KSParseError(no_lex, "Invalid width or size: " + x.message))
      }
    } else {
      succeed(Optional.empty())
    }
  }

  private fun parseTargetURI(
    element : Element) : KSResult<URI, KSParseError> {
    val ta = element.getAttribute(
      "target", KSSchemaNamespaces.NAMESPACE_URI_TEXT)

    if (ta == null) {
      val sb = StringBuilder()
      sb.append("Missing target attribute.")
      sb.append(System.lineSeparator())
      sb.append("  Received: ")
      sb.append(element)
      sb.append(System.lineSeparator())
      return KSResult.fail<URI, KSParseError>(
        KSParseError(no_lex, sb.toString()))
    }

    return try {
      succeed(URI(ta.value))
    } catch (x : URISyntaxException) {
      val sb = StringBuilder()
      sb.append("Invalid URI.")
      sb.append(System.lineSeparator())
      sb.append("  Received: ")
      sb.append(ta.value)
      sb.append(System.lineSeparator())
      sb.append("  Error:    ")
      sb.append(x.message)
      sb.append(System.lineSeparator())
      sb.append("  Reason:   ")
      sb.append(x.reason)
      sb.append(System.lineSeparator())
      KSResult.fail<URI, KSParseError>(KSParseError(no_lex, sb.toString()))
    }
  }

  private fun parseType(
    context : KSParseContextType,
    element : Element)
    : KSResult<Optional<KSType<KSParse>>, KSParseError> {
    val ta = element.getAttribute("type", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    return if (ta != null) {
      if (KSType.isValidType(ta.value)) {
        KSResult.succeed(Optional.of(
          KSType.create(no_lex, ta.value, KSParse(context))))
      } else {
        val sb = StringBuilder()
        sb.append("Invalid type name.")
        sb.append(System.lineSeparator())
        sb.append("  Received: ")
        sb.append(ta.value)
        sb.append(System.lineSeparator())
        KSResult.fail(KSParseError(no_lex, sb.toString()))
      }
    } else {
      KSResult.succeed(Optional.empty())
    }
  }

  companion object {

    private val no_lex : Optional<LexicalPositionType<Path>> = Optional.empty()

    private fun <T : Any> succeed(x : T) : KSResult<T, KSParseError> =
      KSResult.succeed<T, KSParseError>(x)

    private fun fail(x : KSParseError) : KSResult<KSInline<KSParse>, KSParseError> =
      KSResult.fail(x)

    fun create() : KSXOMInlineParserType =
      KSXOMInlineParser()
  }

}