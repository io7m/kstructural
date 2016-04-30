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

package com.io7m.kstructural.tests.parser

import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSInline
import com.io7m.kstructural.core.KSInline.KSInlineText
import com.io7m.kstructural.core.KSLink
import com.io7m.kstructural.core.KSLinkContent
import com.io7m.kstructural.core.KSResult.KSFailure
import com.io7m.kstructural.core.KSResult.KSSuccess
import com.io7m.kstructural.parser.KSExpression
import com.io7m.kstructural.parser.KSInlineParserType
import com.io7m.kstructural.parser.KSParseError
import org.junit.Assert
import org.junit.Test
import java.math.BigInteger
import java.util.Optional


abstract class KSInlineParserContract {

  protected abstract fun newParserForString(text : String) : Parser

  data class Parser(
    val p : KSInlineParserType,
    val s : () -> KSExpression)

  @Test fun testInlineText() {
    val pp = newParserForString("x")
    val r = pp.p.parse(pp.s())

    r as KSSuccess<KSInline.KSInlineText<*>, KSParseError>
    Assert.assertEquals("x", r.result.text)
  }

  @Test fun testInlineTextQuoted() {
    val pp = newParserForString("\"x\"")
    val r = pp.p.parse(pp.s())

    r as KSSuccess<KSInline.KSInlineText<*>, KSParseError>
    Assert.assertEquals("x", r.result.text)
  }

  @Test fun testInlineTermError() {
    val pp = newParserForString("[term]")
    val r = pp.p.parse(pp.s())
    r as KSFailure
  }

  @Test fun testInlineTermTypeError() {
    val pp = newParserForString("[term [type]]")
    val r = pp.p.parse(pp.s())
    r as KSFailure
  }

  @Test fun testInlineTermNestedError() {
    val pp = newParserForString("[term x [term y]]")
    val r = pp.p.parse(pp.s())
    r as KSFailure
  }

  @Test fun testInlineTerm() {
    val pp = newParserForString("[term x]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInline.KSInlineTerm<*>, KSParseError>
    Assert.assertEquals("x", i.result.content[0].text)
    Assert.assertEquals(Optional.empty<String>(), i.result.type)
  }

  @Test fun testInlineTermType() {
    val pp = newParserForString("[term [type y] x]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInline.KSInlineTerm<*>, KSParseError>
    Assert.assertEquals("x", i.result.content[0].text)
    Assert.assertEquals(Optional.of("y"), i.result.type)
  }

  @Test fun testInlineTermQuoted() {
    val pp = newParserForString("[term \"x\"]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInline.KSInlineTerm<*>, KSParseError>
    Assert.assertEquals("x", i.result.content[0].text)
    Assert.assertEquals(Optional.empty<String>(), i.result.type)
  }

  @Test fun testInlineTermQuotedType() {
    val pp = newParserForString("[term [type y] \"x\"]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInline.KSInlineTerm<*>, KSParseError>
    Assert.assertEquals("x", i.result.content[0].text)
    Assert.assertEquals(Optional.of("y"), i.result.type)
  }

  @Test fun testInlineVerbatim() {
    val pp = newParserForString("[verbatim \"x\"]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInline.KSInlineVerbatim<*>, KSParseError>
    Assert.assertEquals("x", i.result.text)
  }

  @Test fun testInlineVerbatimType() {
    val pp = newParserForString("[verbatim [type y] \"x\"]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInline.KSInlineVerbatim<*>, KSParseError>
    Assert.assertEquals("x", i.result.text)
    Assert.assertEquals("y", i.result.type.get())
  }

  @Test fun testInlineVerbatimError() {
    val pp = newParserForString("[verbatim [x]]")
    val i = pp.p.parse(pp.s())
    i as KSFailure
  }

  @Test fun testInlineLinkInternal() {
    val pp = newParserForString("[link [target \"x\"] y]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInline.KSInlineLink<*>, KSParseError>
    val l = i.result.actual as KSLink.KSLinkInternal

    val lt = l.content[0] as KSLinkContent.KSLinkText<*>
    Assert.assertEquals(KSID(Optional.empty(), "x", Unit), l.target)
    Assert.assertEquals("y", lt.actual.text)
  }

  @Test fun testInlineLinkInternalQuoted() {
    val pp = newParserForString("[link [target \"x\"] \"y\"]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInline.KSInlineLink<*>, KSParseError>
    val l = i.result.actual as KSLink.KSLinkInternal

    val lt = l.content[0] as KSLinkContent.KSLinkText<*>
    Assert.assertEquals(KSID(Optional.empty(), "x", Unit), l.target)
    Assert.assertEquals("y", lt.actual.text)
  }

  @Test fun testInlineLinkInternalImage() {
    val pp = newParserForString("[link [target \"x\"] (image [target \"q\"] y)]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInline.KSInlineLink<*>, KSParseError>
    val l = i.result.actual as KSLink.KSLinkInternal

    val lt = l.content[0] as KSLinkContent.KSLinkImage<*>
    Assert.assertEquals(KSID(Optional.empty(), "x", Unit), l.target)
    Assert.assertEquals("q", lt.actual.target.toString())
  }

  @Test fun testInlineLinkInternalError0() {
    val pp = newParserForString("[link]")
    val i = pp.p.parse(pp.s())
    i as KSFailure
  }

  @Test fun testInlineLinkInternalError1() {
    val pp = newParserForString("[link x y]")
    val i = pp.p.parse(pp.s())
    i as KSFailure
  }

  @Test fun testInlineLinkInternalError2() {
    val pp = newParserForString("[link [target \"x\"] [x]]")
    val i = pp.p.parse(pp.s())
    i as KSFailure
  }

  @Test fun testInlineLinkInternalErrorNestedLink() {
    val pp = newParserForString("[link (target \"x\") q (link [target \"y\"] z)]")
    val i = pp.p.parse(pp.s())
    i as KSFailure
  }

  @Test fun testInlineLinkInternalErrorNestedVerbatim() {
    val pp = newParserForString("[link (target \"x\") q (verbatim \"x\")]")
    val i = pp.p.parse(pp.s())
    i as KSFailure
  }

  @Test fun testInlineLinkInternalErrorNestedTerm() {
    val pp = newParserForString("[link (target \"x\") q (term \"x\")]")
    val i = pp.p.parse(pp.s())
    i as KSFailure
  }

  @Test fun testInlineLinkExternal() {
    val pp = newParserForString("[link-ext [target \"http://example.com\"] y]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInline.KSInlineLink<*>, KSParseError>
    val l = i.result.actual as KSLink.KSLinkExternal

    val lt = l.content[0] as KSLinkContent.KSLinkText<*>
    Assert.assertEquals("http://example.com", l.target.toString())
    Assert.assertEquals("y", lt.actual.text)
  }

  @Test fun testInlineLinkExternalQuoted() {
    val pp = newParserForString("[link-ext [target \"http://example.com\"] \"y\"]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInline.KSInlineLink<*>, KSParseError>
    val l = i.result.actual as KSLink.KSLinkExternal

    val lt = l.content[0] as KSLinkContent.KSLinkText<*>
    Assert.assertEquals("http://example.com", l.target.toString())
    Assert.assertEquals("y", lt.actual.text)
  }

  @Test fun testInlineLinkExternalError0() {
    val pp = newParserForString("[link-ext [target \" \"] x]")
    val i = pp.p.parse(pp.s())
    i as KSFailure
  }

  @Test fun testInlineLinkExternalError1() {
    val pp = newParserForString("[link-ext [target \"http://example.com\"] [x]]")
    val i = pp.p.parse(pp.s())
    i as KSFailure
  }

  @Test fun testInlineLinkExternalErrorNestedLink() {
    val pp = newParserForString("[link-ext (target \"x\") q (link [target \"y\"] z)]")
    val i = pp.p.parse(pp.s())
    i as KSFailure
  }

  @Test fun testInlineLinkExternalErrorEmpty() {
    val pp = newParserForString("[link-ext]")
    val i = pp.p.parse(pp.s())
    i as KSFailure
  }

  @Test fun testInlineLinkExternalErrorNestedVerbatim() {
    val pp = newParserForString("[link-ext (target \"x\") q (verbatim \"x\")]")
    val i = pp.p.parse(pp.s())
    i as KSFailure
  }

  @Test fun testInlineLinkExternalErrorNestedTerm() {
    val pp = newParserForString("[link-ext (target \"x\") q (term \"x\")]")
    val i = pp.p.parse(pp.s())
    i as KSFailure
  }

  @Test fun testInlineImage() {
    val pp = newParserForString("[image [target \"x\"] y]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInline.KSInlineImage<*>, KSParseError>
    Assert.assertEquals("x", i.result.target.toString())
    Assert.assertEquals(Optional.empty<String>(), i.result.type)
    Assert.assertEquals(Optional.empty<KSInline.KSSize>(), i.result.size)
    Assert.assertEquals("y", i.result.content[0].text)
  }

  @Test fun testInlineImageType() {
    val pp = newParserForString("[image [target \"x\"] [type y] z]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInline.KSInlineImage<*>, KSParseError>
    Assert.assertEquals("x", i.result.target.toString())
    Assert.assertEquals("y", i.result.type.get())
    Assert.assertEquals(Optional.empty<KSInline.KSSize>(), i.result.size)
    Assert.assertEquals("z", i.result.content[0].text)
  }

  @Test fun testInlineImageTypeSize() {
    val pp = newParserForString("[image [target \"x\"] [type y] [size 100 200] z]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInline.KSInlineImage<*>, KSParseError>
    Assert.assertEquals("x", i.result.target.toString())
    Assert.assertEquals("y", i.result.type.get())
    Assert.assertEquals(Optional.of(
      KSInline.KSSize(
        BigInteger.valueOf(100L),
        BigInteger.valueOf(200L))), i.result.size)
    Assert.assertEquals("z", i.result.content[0].text)
  }

  @Test fun testInlineImageSize() {
    val pp = newParserForString("[image [target \"x\"] [size 100 200] z]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInline.KSInlineImage<*>, KSParseError>
    Assert.assertEquals("x", i.result.target.toString())
    Assert.assertEquals(Optional.empty<String>(), i.result.type)
    Assert.assertEquals(Optional.of(
      KSInline.KSSize(
        BigInteger.valueOf(100L),
        BigInteger.valueOf(200L))), i.result.size)
    Assert.assertEquals("z", i.result.content[0].text)
  }

  @Test fun testInlineImageError() {
    val pp = newParserForString("[image y]")
    val i = pp.p.parse(pp.s())
    i as KSFailure
  }

  @Test fun testInlineImageErrorBadTarget() {
    val pp = newParserForString("[image [target \" \"] z]")
    val i = pp.p.parse(pp.s())
    i as KSFailure
  }

  @Test fun testInlineImageErrorBadWidth() {
    val pp = newParserForString("[image [target \"x\"] [size x 100] y]")
    val i = pp.p.parse(pp.s())
    i as KSFailure
  }

  @Test fun testInlineImageErrorBadWidthNegative() {
    val pp = newParserForString("[image [target \"x\"] [size -100 100] y]")
    val i = pp.p.parse(pp.s())
    i as KSFailure
  }

  @Test fun testInlineImageErrorBadHeight() {
    val pp = newParserForString("[image [target \"x\"] [size 100 x] y]")
    val i = pp.p.parse(pp.s())
    i as KSFailure
  }

  @Test fun testInlineImageErrorBadHeightNegative() {
    val pp = newParserForString("[image [target \"x\"] [size 100 -100] y]")
    val i = pp.p.parse(pp.s())
    i as KSFailure
  }

  @Test fun testInlineIncludeError0() {
    val pp = newParserForString("[include x]")
    val i = pp.p.parse(pp.s())
    i as KSFailure
  }

  @Test fun testInlineIncludeError1() {
    val pp = newParserForString("[include]")
    val i = pp.p.parse(pp.s())
    i as KSFailure
  }

  @Test fun testInlineListOrdered() {
    val pp = newParserForString("[list-ordered [item x]]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInline.KSInlineListOrdered<*>, KSParseError>
    Assert.assertEquals(1, i.result.content.size)

    val ii = i.result.content[0]
    Assert.assertEquals("x", (ii.content[0] as KSInlineText).text)
  }

  @Test fun testInlineListOrderedEmpty() {
    val pp = newParserForString("[list-ordered]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInline.KSInlineListOrdered<*>, KSParseError>
    Assert.assertEquals(0, i.result.content.size)
  }

  @Test fun testInlineListOrderedError() {
    val pp = newParserForString("[list-ordered z]")
    val i = pp.p.parse(pp.s())

    i as KSFailure
  }

  @Test fun testInlineListUnordered() {
    val pp = newParserForString("[list-unordered [item x]]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInline.KSInlineListUnordered<*>, KSParseError>
    Assert.assertEquals(1, i.result.content.size)

    val ii = i.result.content[0]
    Assert.assertEquals("x", (ii.content[0] as KSInlineText).text)
  }

  @Test fun testInlineListUnorderedEmpty() {
    val pp = newParserForString("[list-unordered]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInline.KSInlineListUnordered<*>, KSParseError>
    Assert.assertEquals(0, i.result.content.size)
  }

  @Test fun testInlineListUnorderedError() {
    val pp = newParserForString("[list-unordered z]")
    val i = pp.p.parse(pp.s())

    i as KSFailure
  }
}
