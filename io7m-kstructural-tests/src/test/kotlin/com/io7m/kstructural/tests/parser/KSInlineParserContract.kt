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

import com.io7m.kstructural.core.KSElement.KSInline.KSInlineFootnoteReference
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineImage
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineLink
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineListOrdered
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineListUnordered
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineTable
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineTerm
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineText
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineVerbatim
import com.io7m.kstructural.core.KSElement.KSInline.*
import com.io7m.kstructural.core.KSElement.KSInline.KSSize
import com.io7m.kstructural.core.KSID
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

    r as KSSuccess<KSInlineText<*>, KSParseError>
    Assert.assertEquals("x", r.result.text)
  }

  @Test fun testInlineTextQuoted() {
    val pp = newParserForString("\"x\"")
    val r = pp.p.parse(pp.s())

    r as KSSuccess<KSInlineText<*>, KSParseError>
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

    i as KSSuccess<KSInlineTerm<*>, KSParseError>
    Assert.assertEquals("x", i.result.content[0].text)
    Assert.assertEquals(Optional.empty<String>(), i.result.type)
  }

  @Test fun testInlineTermType() {
    val pp = newParserForString("[term [type y] x]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineTerm<*>, KSParseError>
    Assert.assertEquals("x", i.result.content[0].text)
    Assert.assertEquals(Optional.of("y"), i.result.type)
  }

  @Test fun testInlineTermQuoted() {
    val pp = newParserForString("[term \"x\"]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineTerm<*>, KSParseError>
    Assert.assertEquals("x", i.result.content[0].text)
    Assert.assertEquals(Optional.empty<String>(), i.result.type)
  }

  @Test fun testInlineTermQuotedType() {
    val pp = newParserForString("[term [type y] \"x\"]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineTerm<*>, KSParseError>
    Assert.assertEquals("x", i.result.content[0].text)
    Assert.assertEquals(Optional.of("y"), i.result.type)
  }

  @Test fun testInlineVerbatim() {
    val pp = newParserForString("[verbatim \"x\"]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineVerbatim<*>, KSParseError>
    Assert.assertEquals("x", i.result.text)
  }

  @Test fun testInlineVerbatimType() {
    val pp = newParserForString("[verbatim [type y] \"x\"]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineVerbatim<*>, KSParseError>
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

    i as KSSuccess<KSInlineLink<*>, KSParseError>
    val l = i.result.actual as KSLink.KSLinkInternal

    val lt = l.content[0] as KSLinkContent.KSLinkText<*>
    Assert.assertEquals(KSID(Optional.empty(), "x", Unit), l.target)
    Assert.assertEquals("y", lt.actual.text)
  }

  @Test fun testInlineLinkInternalQuoted() {
    val pp = newParserForString("[link [target \"x\"] \"y\"]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineLink<*>, KSParseError>
    val l = i.result.actual as KSLink.KSLinkInternal

    val lt = l.content[0] as KSLinkContent.KSLinkText<*>
    Assert.assertEquals(KSID(Optional.empty(), "x", Unit), l.target)
    Assert.assertEquals("y", lt.actual.text)
  }

  @Test fun testInlineLinkInternalImage() {
    val pp = newParserForString("[link [target \"x\"] (image [target \"q\"] y)]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineLink<*>, KSParseError>
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

    i as KSSuccess<KSInlineLink<*>, KSParseError>
    val l = i.result.actual as KSLink.KSLinkExternal

    val lt = l.content[0] as KSLinkContent.KSLinkText<*>
    Assert.assertEquals("http://example.com", l.target.toString())
    Assert.assertEquals("y", lt.actual.text)
  }

  @Test fun testInlineLinkExternalQuoted() {
    val pp = newParserForString("[link-ext [target \"http://example.com\"] \"y\"]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineLink<*>, KSParseError>
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

    i as KSSuccess<KSInlineImage<*>, KSParseError>
    Assert.assertEquals("x", i.result.target.toString())
    Assert.assertEquals(Optional.empty<String>(), i.result.type)
    Assert.assertEquals(Optional.empty<KSSize>(), i.result.size)
    Assert.assertEquals("y", i.result.content[0].text)
  }

  @Test fun testInlineImageType() {
    val pp = newParserForString("[image [target \"x\"] [type y] z]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineImage<*>, KSParseError>
    Assert.assertEquals("x", i.result.target.toString())
    Assert.assertEquals("y", i.result.type.get())
    Assert.assertEquals(Optional.empty<KSSize>(), i.result.size)
    Assert.assertEquals("z", i.result.content[0].text)
  }

  @Test fun testInlineImageTypeSize() {
    val pp = newParserForString("[image [target \"x\"] [type y] [size 100 200] z]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineImage<*>, KSParseError>
    Assert.assertEquals("x", i.result.target.toString())
    Assert.assertEquals("y", i.result.type.get())
    Assert.assertEquals(Optional.of(
      KSSize(
        BigInteger.valueOf(100L),
        BigInteger.valueOf(200L))), i.result.size)
    Assert.assertEquals("z", i.result.content[0].text)
  }

  @Test fun testInlineImageSize() {
    val pp = newParserForString("[image [target \"x\"] [size 100 200] z]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineImage<*>, KSParseError>
    Assert.assertEquals("x", i.result.target.toString())
    Assert.assertEquals(Optional.empty<String>(), i.result.type)
    Assert.assertEquals(Optional.of(
      KSSize(
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

    i as KSSuccess<KSInlineListOrdered<*>, KSParseError>
    Assert.assertEquals(1, i.result.content.size)

    val ii = i.result.content[0]
    Assert.assertEquals("x", (ii.content[0] as KSInlineText).text)
  }

  @Test fun testInlineListOrderedEmpty() {
    val pp = newParserForString("[list-ordered]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineListOrdered<*>, KSParseError>
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

    i as KSSuccess<KSInlineListUnordered<*>, KSParseError>
    Assert.assertEquals(1, i.result.content.size)

    val ii = i.result.content[0]
    Assert.assertEquals("x", (ii.content[0] as KSInlineText).text)
  }

  @Test fun testInlineListUnorderedEmpty() {
    val pp = newParserForString("[list-unordered]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineListUnordered<*>, KSParseError>
    Assert.assertEquals(0, i.result.content.size)
  }

  @Test fun testInlineListUnorderedError() {
    val pp = newParserForString("[list-unordered z]")
    val i = pp.p.parse(pp.s())

    i as KSFailure
  }

  @Test fun testInlineTableError() {
    val pp = newParserForString("[table]")
    val i = pp.p.parse(pp.s())

    i as KSFailure
  }

  @Test fun testInlineTableSummaryError() {
    val pp = newParserForString("[table [summary [term q]] [body]]")
    val i = pp.p.parse(pp.s())

    i as KSFailure
  }

  @Test fun testInlineTableSummaryBodyError() {
    val pp = newParserForString("[table [summary s] [body q]]")
    val i = pp.p.parse(pp.s())

    i as KSFailure
  }

  @Test fun testInlineTableSummaryBodyErrorCell() {
    val pp = newParserForString("[table [summary s] [body [row x]]]")
    val i = pp.p.parse(pp.s())

    i as KSFailure
  }

  @Test fun testInlineTableSummaryHeadBodyError() {
    val pp = newParserForString("[table [summary s] [head x] [body]]")
    val i = pp.p.parse(pp.s())

    i as KSFailure
  }

  @Test fun testInlineTableSummaryHeadBodyNameError() {
    val pp = newParserForString("[table [summary s] [head [name [term z]]] [body]]")
    val i = pp.p.parse(pp.s())

    i as KSFailure
  }

  @Test fun testInlineTableSummaryBody() {
    val pp = newParserForString("[table [summary s] [body]]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertFalse(i.result.type.isPresent)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(0, i.result.body.rows.size)
  }

  @Test fun testInlineTableSummaryBodyRow() {
    val pp = newParserForString("[table [summary s] [body [row]]]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertFalse(i.result.type.isPresent)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(1, i.result.body.rows.size)
    Assert.assertEquals(0, i.result.body.rows[0].cells.size)
  }

  @Test fun testInlineTableSummaryBodyRowCell() {
    val pp = newParserForString("[table [summary s] [body [row [cell]]]]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertFalse(i.result.type.isPresent)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(1, i.result.body.rows.size)
    Assert.assertEquals(1, i.result.body.rows[0].cells.size)
    Assert.assertEquals(0, i.result.body.rows[0].cells[0].content.size)
  }

  @Test fun testInlineTableSummaryHeadBody() {
    val pp = newParserForString("[table [summary s] [head] [body]]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertFalse(i.result.type.isPresent)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(0, i.result.body.rows.size)
    Assert.assertEquals(0, i.result.head.get().column_names.size)
  }

  @Test fun testInlineTableSummaryHeadNamesBody() {
    val pp = newParserForString("[table [summary s] [head [name x]] [body]]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertFalse(i.result.type.isPresent)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(0, i.result.body.rows.size)
    Assert.assertEquals(1, i.result.head.get().column_names.size)
    Assert.assertEquals("x", i.result.head.get().column_names[0].content[0].text)
  }

  @Test fun testInlineTableSummaryTypeBody() {
    val pp = newParserForString("[table [summary s] [type t] [body]]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertEquals(Optional.of("t"), i.result.type)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(0, i.result.body.rows.size)
  }

  @Test fun testInlineTableSummaryTypeBodyRow() {
    val pp = newParserForString("[table [summary s] [type t] [body [row]]]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertEquals(Optional.of("t"), i.result.type)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(1, i.result.body.rows.size)
    Assert.assertEquals(0, i.result.body.rows[0].cells.size)
  }

  @Test fun testInlineTableSummaryTypeBodyRowCell() {
    val pp = newParserForString("[table [summary s] [type t] [body [row [cell]]]]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertEquals(Optional.of("t"), i.result.type)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(1, i.result.body.rows.size)
    Assert.assertEquals(1, i.result.body.rows[0].cells.size)
    Assert.assertEquals(0, i.result.body.rows[0].cells[0].content.size)
  }

  @Test fun testInlineTableSummaryTypeHeadBody() {
    val pp = newParserForString("[table [summary s] [type t] [head] [body]]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertEquals(Optional.of("t"), i.result.type)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(0, i.result.body.rows.size)
    Assert.assertEquals(0, i.result.head.get().column_names.size)
  }

  @Test fun testInlineTableSummaryTypeHeadNamesBody() {
    val pp = newParserForString("[table [summary s] [type t] [head [name x]] [body]]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertEquals(Optional.of("t"), i.result.type)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(0, i.result.body.rows.size)
    Assert.assertEquals(1, i.result.head.get().column_names.size)
    Assert.assertEquals("x", i.result.head.get().column_names[0].content[0].text)
  }

  @Test fun testInlineFootnoteReference() {
    val pp = newParserForString("[footnote-ref \"x\"]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineFootnoteReference<*>, KSParseError>
    val l = i.result as KSInlineFootnoteReference<Unit>

    Assert.assertEquals(KSID(Optional.empty(), "x", Unit), l.target)
  }

  @Test fun testInlineFootnoteReferenceError() {
    val pp = newParserForString("[footnote-ref]")
    val i = pp.p.parse(pp.s())

    i as KSFailure
  }

  @Test fun testInlineInclude() {
    val pp = newParserForString("[include \"x\"]")
    val i = pp.p.parse(pp.s())

    i as KSSuccess<KSInlineInclude<*>, KSParseError>
    Assert.assertEquals("x", i.result.file.text)
  }

  @Test fun testInlineIncludeError() {
    val pp = newParserForString("[include [x]]")
    val i = pp.p.parse(pp.s())
    i as KSFailure
  }
}
