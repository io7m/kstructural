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
import com.io7m.kstructural.core.KSElement.KSInline.KSSize
import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSLink
import com.io7m.kstructural.core.KSLinkContent
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSParseContextType
import com.io7m.kstructural.core.KSParseError
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.schema.KSXMLNamespace
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.slf4j.LoggerFactory
import org.valid4j.Assertive
import java.math.BigInteger
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Path
import java.util.Optional

class KSXOMInlineParser private constructor() : KSXOMInlineParserType {

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
      "image"         -> parseElementImage(context, element)
      "term"          -> parseElementTerm(context, element)
      "verbatim"      -> parseElementVerbatim(context, element)
      "link"          -> parseElementLink(context, element)
      "link-external" -> parseElementLinkExternal(context, element)
      else            ->
        fail(KSParseError(no_lex, "Unrecognized element: " + element.localName))
    }
  }

  private fun parseLink(
    context : KSParseContextType,
    element : Element) : KSResult<KSLink.KSLinkInternal<KSParse>, KSParseError> {
    Assertive.require(element.localName == "link")

    val kp = KSParse(context)
    val ta = element.getAttribute("target", KSXMLNamespace.NAMESPACE_URI_TEXT)
    val target = KSID<KSParse>(no_lex, ta.value, kp)
    val act_content = KSResult.listMap(
      { e -> parseLinkContent(context, e) }, listOfChildren(element))

    return act_content.flatMap { content ->
      succeed(KSLink.KSLinkInternal(no_lex, target, content))
    }
  }

  private fun parseLinkExternal(
    context : KSParseContextType,
    element : Element) : KSResult<KSLink.KSLinkExternal<KSParse>, KSParseError> {
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
    element : Element) : KSResult<KSInlineLink<KSParse>, KSParseError> {
    return parseLink(context, element).flatMap { link ->
      val kp = KSParse(context)
      succeed(KSInlineLink(link.position, false, kp, link))
    }
  }

  private fun parseElementLinkExternal(
    context : KSParseContextType,
    element : Element) : KSResult<KSInlineLink<KSParse>, KSParseError> {
    return parseLinkExternal(context, element).flatMap { link ->
      val kp = KSParse(context)
      succeed(KSInlineLink(link.position, false, kp, link))
    }
  }

  private fun parseElementTerm(
    context : KSParseContextType,
    element : Element) : KSResult<KSInline<KSParse>, KSParseError> {
    Assertive.require(element.localName == "term")

    val type = parseType(element)
    val act_content = KSResult.listMap(
      { e -> parseInlineText(context, e) }, listOfChildren(element))

    return act_content.flatMap { content ->
      succeed(KSInlineTerm(
        no_lex, false, KSParse(context), type, content))
    }
  }

  private fun parseElementVerbatim(
    context : KSParseContextType,
    element : Element) : KSResult<KSInlineVerbatim<KSParse>, KSParseError> {
    Assertive.require(element.localName == "verbatim")

    val type = parseType(element)
    val kp = KSParse(context)
    val content = KSInlineText(no_lex, false, kp, true, element.value)
    return succeed(KSInlineVerbatim(no_lex, false, kp, type, content))
  }

  private fun parseElementImage(
    context : KSParseContextType,
    element : Element) : KSResult<KSInlineImage<KSParse>, KSParseError> {
    Assertive.require(element.localName == "image")

    val type = parseType(element)
    val act_target = parseTargetURI(element)
    val act_size = parseSize(element)
    val act_content = KSResult.listMap(
      { e -> parseInlineText(context, e) }, listOfChildren(element))

    return act_target.flatMap { target ->
      act_size.flatMap { size ->
        act_content.flatMap { content ->
          succeed(KSInlineImage(
            no_lex, false, KSParse(context), type, target, size, content))
        }
      }
    }
  }

  private fun checkLinkContent(
    e : KSInline<KSParse>) : KSResult<KSLinkContent<KSParse>, KSParseError> =
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
    e : Node) : KSResult<KSLinkContent<KSParse>, KSParseError> {
    return parse(context, e).flatMap { content -> checkLinkContent(content) }
  }

  private fun parseInlineText(
    context : KSParseContextType,
    e : Node) : KSResult<KSInlineText<KSParse>, KSParseError> {
    if (e is Text) {
      return succeed(
        KSInlineText(no_lex, false, KSParse(context), false, e.value))
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

  private fun parseSize(
    element : Element) : KSResult<Optional<KSSize>, KSParseError> {
    val tw = element.getAttribute("width", KSXMLNamespace.NAMESPACE_URI_TEXT)
    val th = element.getAttribute("height", KSXMLNamespace.NAMESPACE_URI_TEXT)
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

  private fun parseTargetURI(element : Element) : KSResult<URI, KSParseError> {
    return try {
      val ta = element.getAttribute("target", KSXMLNamespace.NAMESPACE_URI_TEXT)
      succeed(URI(ta.value))
    } catch (x : URISyntaxException) {
      KSResult.fail<URI, KSParseError>(
        KSParseError(no_lex, "Invalid URI: " + x.reason))
    }
  }

  private fun parseType(element : Element) : Optional<String> {
    val ta = element.getAttribute("type", KSXMLNamespace.NAMESPACE_URI_TEXT)
    return if (ta != null) {
      Optional.of(ta.value)
    } else {
      Optional.empty()
    }
  }

  companion object {

    private val LOG = LoggerFactory.getLogger(KSXOMInlineParser::class.java)

    private val no_lex : Optional<LexicalPositionType<Path>> = Optional.empty()

    private fun <T : Any> succeed(x : T) : KSResult<T, KSParseError> =
      KSResult.succeed<T, KSParseError>(x)

    private fun fail(x : KSParseError) : KSResult<KSInline<KSParse>, KSParseError> =
      KSResult.fail(x)

    fun create() : KSXOMInlineParserType =
      KSXOMInlineParser()
  }

}