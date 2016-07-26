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

package com.io7m.kstructural.tests.parser.canon

import com.io7m.kstructural.core.KSElement.KSInline.KSInlineFootnoteReference
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineImage
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
import com.io7m.kstructural.core.KSParseContext
import com.io7m.kstructural.core.KSParseContextType
import com.io7m.kstructural.core.KSParseError
import com.io7m.kstructural.core.KSResult.KSFailure
import com.io7m.kstructural.core.KSResult.KSSuccess
import com.io7m.kstructural.parser.KSExpression
import com.io7m.kstructural.parser.canon.KSCanonInlineParserType
import org.apache.commons.io.IOUtils
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.Files
import java.util.Optional


abstract class KSCanonInlineParserContract {

  protected abstract fun newParserForStringAndContext(
    context : KSParseContextType, text : String) : Parser

  protected abstract fun newParserForString(text : String) : Parser

  protected abstract fun newFilesystem() : FileSystem

  private var filesystem : FileSystem? = null

  @Before fun setupFilesystem() : Unit {
    this.filesystem = newFilesystem()
  }

  @After fun tearDownFilesystem() : Unit {
    this.filesystem!!.close()
  }

  protected fun defaultFile() = filesystem!!.getPath("file.txt")

  protected fun rootDirectory() = filesystem!!.rootDirectories.first()!!
  
  data class Parser(
    val p : KSCanonInlineParserType,
    val s : () -> KSExpression)

  @Test fun testInlineText() {
    val pp = newParserForString("x")
    val r = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    r as KSSuccess<KSInlineText<*>, KSParseError>
    Assert.assertEquals("x", r.result.text)
  }

  @Test fun testInlineTextQuoted() {
    val pp = newParserForString("\"x\"")
    val r = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    r as KSSuccess<KSInlineText<*>, KSParseError>
    Assert.assertEquals("x", r.result.text)
  }

  @Test fun testInlineTermTypeError() {
    val pp = newParserForString("[term [type]]")
    val r = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    r as KSFailure
  }

  @Test fun testInlineTermTypeErrorInvalud() {
    val pp = newParserForString("[term [type -]]")
    val r = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    r as KSFailure
  }

  @Test fun testInlineTermNestedError() {
    val pp = newParserForString("[term x [term y]]")
    val r = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    r as KSFailure
  }

  @Test fun testInlineTerm() {
    val pp = newParserForString("[term x]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineTerm<*>, KSParseError>
    Assert.assertEquals("x", i.result.content[0].text)
    Assert.assertEquals(Optional.empty<String>(), i.result.type)
  }

  @Test fun testInlineTermType() {
    val pp = newParserForString("[term [type y] x]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineTerm<*>, KSParseError>
    Assert.assertEquals("x", i.result.content[0].text)
    Assert.assertEquals("y", i.result.type.get().value)
  }

  @Test fun testInlineTermQuoted() {
    val pp = newParserForString("[term \"x\"]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineTerm<*>, KSParseError>
    Assert.assertEquals("x", i.result.content[0].text)
    Assert.assertEquals(Optional.empty<String>(), i.result.type)
  }

  @Test fun testInlineTermQuotedType() {
    val pp = newParserForString("[term [type y] \"x\"]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineTerm<*>, KSParseError>
    Assert.assertEquals("x", i.result.content[0].text)
    Assert.assertEquals("y", i.result.type.get().value)
  }

  @Test fun testInlineTermInclude() {
    val file = filesystem!!.getPath("other.txt").toAbsolutePath()
    Files.newOutputStream(file).use { f ->
      IOUtils.write("hello", f, StandardCharsets.UTF_8)
      f.flush()
    }

    val pp = newParserForString("[term [include \"other.txt\"]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineTerm<*>, KSParseError>
    Assert.assertEquals("hello", i.result.content[0].text)
    Assert.assertEquals(Optional.empty<String>(), i.result.type)
  }

  @Test fun testInlineVerbatim() {
    val pp = newParserForString("[verbatim \"x\"]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineVerbatim<*>, KSParseError>
    Assert.assertEquals("x", i.result.text.text)
  }

  @Test fun testInlineVerbatimType() {
    val pp = newParserForString("[verbatim [type y] \"x\"]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineVerbatim<*>, KSParseError>
    Assert.assertEquals("x", i.result.text.text)
    Assert.assertEquals("y", i.result.type.get().value)
  }

  @Test fun testInlineVerbatimInclude() {
    val file = filesystem!!.getPath("other.txt").toAbsolutePath()
    Files.newOutputStream(file).use { f ->
      IOUtils.write("hello", f, StandardCharsets.UTF_8)
      f.flush()
    }

    val pp = newParserForString("[verbatim (include \"other.txt\")]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineVerbatim<*>, KSParseError>
    Assert.assertEquals("hello", i.result.text.text)
  }

  @Test fun testInlineVerbatimError() {
    val pp = newParserForString("[verbatim [x]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    i as KSFailure
  }

  @Test fun testInlineVerbatimTypeInclude() {
    val file = filesystem!!.getPath("other.txt").toAbsolutePath()
    Files.newOutputStream(file).use { f ->
      IOUtils.write("hello", f, StandardCharsets.UTF_8)
      f.flush()
    }

    val pp = newParserForString("[verbatim (type t) (include \"other.txt\")]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineVerbatim<*>, KSParseError>
    Assert.assertEquals("hello", i.result.text.text)
    Assert.assertEquals("t", i.result.type.get().value)
  }

  @Test fun testInlineLinkInternalErrorInvalidID() {
    val pp = newParserForString("[link [target \"&\"] y]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSFailure
  }

  @Test fun testInlineLinkInternal() {
    val pp = newParserForString("[link [target \"x\"] y]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineLink<*>, KSParseError>
    val l = i.result.actual as KSLink.KSLinkInternal

    val lt = l.content[0] as KSLinkContent.KSLinkText<*>
    Assert.assertEquals(KSID.create(Optional.empty(), "x", Unit), l.target)
    Assert.assertEquals("y", lt.actual.text)
  }

  @Test fun testInlineLinkInternalQuoted() {
    val pp = newParserForString("[link [target \"x\"] \"y\"]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineLink<*>, KSParseError>
    val l = i.result.actual as KSLink.KSLinkInternal

    val lt = l.content[0] as KSLinkContent.KSLinkText<*>
    Assert.assertEquals(KSID.create(Optional.empty(), "x", Unit), l.target)
    Assert.assertEquals("y", lt.actual.text)
  }

  @Test fun testInlineLinkInternalImage() {
    val pp = newParserForString("[link [target \"x\"] (image [target \"q\"] y)]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineLink<*>, KSParseError>
    val l = i.result.actual as KSLink.KSLinkInternal

    val lt = l.content[0] as KSLinkContent.KSLinkImage<*>
    Assert.assertEquals(KSID.create(Optional.empty(), "x", Unit), l.target)
    Assert.assertEquals("q", lt.actual.target.toString())
  }

  @Test fun testInlineLinkInternalError0() {
    val pp = newParserForString("[link]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    i as KSFailure
  }

  @Test fun testInlineLinkInternalError1() {
    val pp = newParserForString("[link x y]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    i as KSFailure
  }

  @Test fun testInlineLinkInternalError2() {
    val pp = newParserForString("[link [target \"x\"] [x]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    i as KSFailure
  }

  @Test fun testInlineLinkInternalErrorNestedLink() {
    val pp = newParserForString("[link (target \"x\") q (link [target \"y\"] z)]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    i as KSFailure
  }

  @Test fun testInlineLinkInternalErrorNestedVerbatim() {
    val pp = newParserForString("[link (target \"x\") q (verbatim \"x\")]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    i as KSFailure
  }

  @Test fun testInlineLinkInternalErrorNestedTerm() {
    val pp = newParserForString("[link (target \"x\") q (term \"x\")]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    i as KSFailure
  }

  @Test fun testInlineLinkExternal() {
    val pp = newParserForString("[link-ext [target \"http://example.com\"] y]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineLink<*>, KSParseError>
    val l = i.result.actual as KSLink.KSLinkExternal

    val lt = l.content[0] as KSLinkContent.KSLinkText<*>
    Assert.assertEquals("http://example.com", l.target.toString())
    Assert.assertEquals("y", lt.actual.text)
  }

  @Test fun testInlineLinkExternalQuoted() {
    val pp = newParserForString("[link-ext [target \"http://example.com\"] \"y\"]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineLink<*>, KSParseError>
    val l = i.result.actual as KSLink.KSLinkExternal

    val lt = l.content[0] as KSLinkContent.KSLinkText<*>
    Assert.assertEquals("http://example.com", l.target.toString())
    Assert.assertEquals("y", lt.actual.text)
  }

  @Test fun testInlineLinkExternalError0() {
    val pp = newParserForString("[link-ext [target \" \"] x]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    i as KSFailure
  }

  @Test fun testInlineLinkExternalError1() {
    val pp = newParserForString("[link-ext [target \"http://example.com\"] [x]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    i as KSFailure
  }

  @Test fun testInlineLinkExternalErrorNestedLink() {
    val pp = newParserForString("[link-ext (target \"x\") q (link [target \"y\"] z)]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    i as KSFailure
  }

  @Test fun testInlineLinkExternalErrorEmpty() {
    val pp = newParserForString("[link-ext]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    i as KSFailure
  }

  @Test fun testInlineLinkExternalErrorNestedVerbatim() {
    val pp = newParserForString("[link-ext (target \"x\") q (verbatim \"x\")]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    i as KSFailure
  }

  @Test fun testInlineLinkExternalErrorNestedTerm() {
    val pp = newParserForString("[link-ext (target \"x\") q (term \"x\")]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    i as KSFailure
  }

  @Test fun testInlineImage() {
    val pp = newParserForString("[image [target \"x\"] y]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineImage<*>, KSParseError>
    Assert.assertEquals("x", i.result.target.toString())
    Assert.assertEquals(Optional.empty<String>(), i.result.type)
    Assert.assertEquals(Optional.empty<KSSize>(), i.result.size)
    Assert.assertEquals("y", i.result.content[0].text)
  }

  @Test fun testInlineImageType() {
    val pp = newParserForString("[image [target \"x\"] [type y] z]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineImage<*>, KSParseError>
    Assert.assertEquals("x", i.result.target.toString())
    Assert.assertEquals("y", i.result.type.get().value)
    Assert.assertEquals(Optional.empty<KSSize>(), i.result.size)
    Assert.assertEquals("z", i.result.content[0].text)
  }

  @Test fun testInlineImageTypeSize() {
    val pp = newParserForString("[image [target \"x\"] [type y] [size 100 200] z]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineImage<*>, KSParseError>
    Assert.assertEquals("x", i.result.target.toString())
    Assert.assertEquals("y", i.result.type.get().value)
    Assert.assertEquals(Optional.of(
      KSSize(
        BigInteger.valueOf(100L),
        BigInteger.valueOf(200L))), i.result.size)
    Assert.assertEquals("z", i.result.content[0].text)
  }

  @Test fun testInlineImageSize() {
    val pp = newParserForString("[image [target \"x\"] [size 100 200] z]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

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
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    i as KSFailure
  }

  @Test fun testInlineImageErrorBadTarget() {
    val pp = newParserForString("[image [target \" \"] z]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    i as KSFailure
  }

  @Test fun testInlineImageErrorBadWidth() {
    val pp = newParserForString("[image [target \"x\"] [size x 100] y]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    i as KSFailure
  }

  @Test fun testInlineImageErrorBadWidthNegative() {
    val pp = newParserForString("[image [target \"x\"] [size -100 100] y]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    i as KSFailure
  }

  @Test fun testInlineImageErrorBadHeight() {
    val pp = newParserForString("[image [target \"x\"] [size 100 x] y]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    i as KSFailure
  }

  @Test fun testInlineImageErrorBadHeightNegative() {
    val pp = newParserForString("[image [target \"x\"] [size 100 -100] y]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    i as KSFailure
  }

  @Test fun testInlineIncludeError0() {
    val pp = newParserForString("[include x]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    i as KSFailure
  }

  @Test fun testInlineIncludeError1() {
    val pp = newParserForString("[include]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    i as KSFailure
  }

  @Test fun testInlineListOrdered() {
    val pp = newParserForString("[list-ordered [item x]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineListOrdered<*>, KSParseError>
    Assert.assertEquals(1, i.result.content.size)

    val ii = i.result.content[0]
    Assert.assertEquals("x", (ii.content[0] as KSInlineText).text)
  }

  @Test fun testInlineListOrderedEmpty() {
    val pp = newParserForString("[list-ordered]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineListOrdered<*>, KSParseError>
    Assert.assertEquals(0, i.result.content.size)
  }

  @Test fun testInlineListOrderedError() {
    val pp = newParserForString("[list-ordered z]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSFailure
  }

  @Test fun testInlineListUnordered() {
    val pp = newParserForString("[list-unordered [item x]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineListUnordered<*>, KSParseError>
    Assert.assertEquals(1, i.result.content.size)

    val ii = i.result.content[0]
    Assert.assertEquals("x", (ii.content[0] as KSInlineText).text)
  }

  @Test fun testInlineListUnorderedEmpty() {
    val pp = newParserForString("[list-unordered]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineListUnordered<*>, KSParseError>
    Assert.assertEquals(0, i.result.content.size)
  }

  @Test fun testInlineListUnorderedError() {
    val pp = newParserForString("[list-unordered z]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSFailure
  }

  @Test fun testInlineTableError() {
    val pp = newParserForString("[table]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSFailure
  }

  @Test fun testInlineTableSummaryError() {
    val pp = newParserForString("[table [summary [term q]] [body]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSFailure
  }

  @Test fun testInlineTableSummaryBodyError() {
    val pp = newParserForString("[table [summary s] [body q]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSFailure
  }

  @Test fun testInlineTableSummaryBodyErrorCell() {
    val pp = newParserForString("[table [summary s] [body [row x]]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSFailure
  }

  @Test fun testInlineTableSummaryHeadBodyError() {
    val pp = newParserForString("[table [summary s] [head x] [body]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSFailure
  }

  @Test fun testInlineTableSummaryHeadBodyNameError() {
    val pp = newParserForString("[table [summary s] [head [name [term z]]] [body]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSFailure
  }

  @Test fun testInlineTableSummaryBody() {
    val pp = newParserForString("[table [summary s] [body]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertFalse(i.result.type.isPresent)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(0, i.result.body.rows.size)
  }

  @Test fun testInlineTableBug() {
    val pp = newParserForString("""
[table
  [summary s]
  [head
    [name ]]
  [body]]
""")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertFalse(i.result.type.isPresent)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertTrue(i.result.head.isPresent)
    Assert.assertEquals(0, i.result.body.rows.size)
  }

  @Test fun testInlineTableSummaryBodyRow() {
    val pp = newParserForString("[table [summary s] [body [row]]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertFalse(i.result.type.isPresent)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(1, i.result.body.rows.size)
    Assert.assertEquals(0, i.result.body.rows[0].cells.size)
  }

  @Test fun testInlineTableSummaryBodyRowType() {
    val pp = newParserForString("[table [summary s] [body [row [type q]]]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertFalse(i.result.type.isPresent)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(1, i.result.body.rows.size)
    Assert.assertEquals(0, i.result.body.rows[0].cells.size)
    Assert.assertEquals("q", i.result.body.rows[0].type.get().value)
    Assert.assertEquals(0, i.result.body.rows[0].cells.size)
  }

  @Test fun testInlineTableSummaryBodyRowTypeCell() {
    val pp = newParserForString("[table [summary s] [body [row [type q] [cell]]]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertFalse(i.result.type.isPresent)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(1, i.result.body.rows.size)
    Assert.assertEquals(1, i.result.body.rows[0].cells.size)
    Assert.assertEquals("q", i.result.body.rows[0].type.get().value)
    Assert.assertEquals(0, i.result.body.rows[0].cells[0].content.size)
  }

  @Test fun testInlineTableSummaryBodyRowCell() {
    val pp = newParserForString("[table [summary s] [body [row [cell]]]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertFalse(i.result.type.isPresent)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(1, i.result.body.rows.size)
    Assert.assertEquals(1, i.result.body.rows[0].cells.size)
    Assert.assertEquals(0, i.result.body.rows[0].cells[0].content.size)
  }

  @Test fun testInlineTableSummaryHeadBody() {
    val pp = newParserForString("[table [summary s] [head] [body]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertFalse(i.result.type.isPresent)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(0, i.result.body.rows.size)
    Assert.assertEquals(0, i.result.head.get().column_names.size)
  }

  @Test fun testInlineTableSummaryHeadNamesBody() {
    val pp = newParserForString("[table [summary s] [head [name x]] [body]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertFalse(i.result.type.isPresent)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(0, i.result.body.rows.size)
    Assert.assertEquals(1, i.result.head.get().column_names.size)
    Assert.assertEquals("x", i.result.head.get().column_names[0].content[0].text)
  }

  @Test fun testInlineTableSummaryTypeBody() {
    val pp = newParserForString("[table [summary s] [type t] [body]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertEquals("t", i.result.type.get().value)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(0, i.result.body.rows.size)
  }

  @Test fun testInlineTableSummaryTypeBodyRow() {
    val pp = newParserForString("[table [summary s] [type t] [body [row]]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertEquals("t", i.result.type.get().value)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(1, i.result.body.rows.size)
    Assert.assertEquals(0, i.result.body.rows[0].cells.size)
  }

  @Test fun testInlineTableSummaryTypeBodyRowCell() {
    val pp = newParserForString("[table [summary s] [type t] [body [row [cell x]]]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertEquals("t", i.result.type.get().value)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(1, i.result.body.rows.size)
    Assert.assertEquals(1, i.result.body.rows[0].cells.size)
    Assert.assertEquals(1, i.result.body.rows[0].cells[0].content.size)
  }

  @Test fun testInlineTableSummaryTypeBodyRowCellType() {
    val pp = newParserForString("[table [summary s] [type t] [body [row [cell [type q]]]]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertEquals("t", i.result.type.get().value)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(1, i.result.body.rows.size)
    Assert.assertEquals(1, i.result.body.rows[0].cells.size)
    Assert.assertEquals(0, i.result.body.rows[0].cells[0].content.size)
    Assert.assertEquals("q", i.result.body.rows[0].cells[0].type.get().value)
  }

  @Test fun testInlineTableSummaryTypeBodyRowCellTypeContent() {
    val pp = newParserForString("[table [summary s] [type t] [body [row [cell [type q] z]]]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertEquals("t", i.result.type.get().value)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(1, i.result.body.rows.size)
    Assert.assertEquals(1, i.result.body.rows[0].cells.size)
    Assert.assertEquals(1, i.result.body.rows[0].cells[0].content.size)
    Assert.assertEquals("q", i.result.body.rows[0].cells[0].type.get().value)
  }

  @Test fun testInlineTableSummaryTypeHeadBody() {
    val pp = newParserForString("[table [summary s] [type t] [head] [body]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertEquals("t", i.result.type.get().value)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(0, i.result.body.rows.size)
    Assert.assertEquals(0, i.result.head.get().column_names.size)
  }

  @Test fun testInlineTableSummaryTypeHeadNamesBody() {
    val pp = newParserForString("[table [summary s] [type t] [head [name x]] [body]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertEquals("t", i.result.type.get().value)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(0, i.result.body.rows.size)
    val head = i.result.head.get()
    Assert.assertEquals(1, head.column_names.size)
    Assert.assertFalse(head.type.isPresent)
    Assert.assertEquals("x", head.column_names[0].content[0].text)
  }

  @Test fun testInlineTableSummaryTypeHeadTypeNamesBody() {
    val pp = newParserForString("[table [summary s] [type t] [head [type q] [name x]] [body]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertEquals("t", i.result.type.get().value)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(0, i.result.body.rows.size)
    val head = i.result.head.get()
    Assert.assertEquals(1, head.column_names.size)
    Assert.assertTrue(head.type.isPresent)
    Assert.assertEquals("q", head.type.get().value)
    Assert.assertEquals("x", head.column_names[0].content[0].text)
  }

  @Test fun testInlineTableSummaryTypeHeadNamesTypeBody() {
    val pp = newParserForString("[table [summary s] [type t] [head [name [type q] x]] [body]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    Assert.assertEquals("t", i.result.type.get().value)
    Assert.assertEquals("s", i.result.summary.content[0].text)
    Assert.assertEquals(0, i.result.body.rows.size)
    val head = i.result.head.get()
    Assert.assertEquals(1, head.column_names.size)
    Assert.assertFalse(head.type.isPresent)
    Assert.assertEquals("x", head.column_names[0].content[0].text)
    Assert.assertEquals("q", head.column_names[0].type.get().value)
  }

  @Test fun testInlineTableTypeAll() {
    val pp = newParserForString("""
[table
  [summary x y z]
  [head
    [type a]
    [name t]
    [name [type b] u]
    [name v]]
  [body
    [row
      [type d]
      [cell x]
      [cell [type c] y]
      [cell z]]
    [row
      [cell x]
      [cell y]
      [cell z]]]]
""")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineTable<*>, KSParseError>
    val head = i.result.head.get();
    Assert.assertEquals("a", head.type.get().value)
    Assert.assertEquals("b", head.column_names[1].type.get().value)
    Assert.assertEquals("c", i.result.body.rows[0].cells[1].type.get().value)
  }

  @Test fun testInlineFootnoteReference() {
    val pp = newParserForString("[footnote-ref \"x\"]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSSuccess<KSInlineFootnoteReference<*>, KSParseError>
    val l = i.result as KSInlineFootnoteReference<Unit>

    Assert.assertEquals(KSID.create(Optional.empty(), "x", Unit), l.target)
  }

  @Test fun testInlineFootnoteReferenceError() {
    val pp = newParserForString("[footnote-ref]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())

    i as KSFailure
  }

  @Test fun testInlineInclude() {
    val pp = newParserForString("[include \"other.txt\"]")

    val file = filesystem!!.getPath("other.txt").toAbsolutePath()
    Files.newOutputStream(file).use { f ->
      IOUtils.write("hello", f, StandardCharsets.UTF_8)
      f.flush()
    }

    val c = KSParseContext.empty(rootDirectory())
    val i = pp.p.parse(c, pp.s(), defaultFile())

    i as KSSuccess<KSInlineText<KSParse>, KSParseError>
    Assert.assertEquals("hello", i.result.text)
    Assert.assertTrue(c.includesByTexts.containsKey(i.result))
    Assert.assertEquals("other.txt", c.includesByTexts[i.result]!!.file.text)
    Assert.assertTrue(c.includes.containsKey(file))
    Assert.assertEquals("hello", c.includes[file])
  }

  @Test fun testInlineIncludeOutside() {
    val base = filesystem!!.getPath("/base").toAbsolutePath()
    Files.createDirectories(base)

    val other_path = filesystem!!.getPath("/other/file.txt").toAbsolutePath()
    Files.createDirectories(other_path.parent)
    Files.write(other_path, "Hello\n".toByteArray(StandardCharsets.UTF_8))

    val c = KSParseContext.empty(base)
    val pp = newParserForStringAndContext(c, "[include \"/other/file.txt\"]")
    val i = pp.p.parse(c, pp.s(), defaultFile())

    i as KSFailure
  }

  @Test fun testInlineIncludeOutsideRelative() {
    val base = filesystem!!.getPath("/base").toAbsolutePath()
    Files.createDirectories(base)

    val other_path = filesystem!!.getPath("/other/file.txt").toAbsolutePath()
    Files.createDirectories(other_path.parent)
    Files.write(other_path, "Hello\n".toByteArray(StandardCharsets.UTF_8))

    val c = KSParseContext.empty(base)
    val pp = newParserForStringAndContext(c, "[include \"../other/file.txt\"]")
    val i = pp.p.parse(c, pp.s(), defaultFile())

    i as KSFailure
  }

  @Test fun testInlineIncludeNonexistent() {
    val pp = newParserForString("[include \"nonexistent.txt\"]")

    val c = KSParseContext.empty(rootDirectory())
    val i = pp.p.parse(c, pp.s(), defaultFile())

    i as KSFailure
  }

  @Test fun testInlineIncludeError() {
    val pp = newParserForString("[include [x]]")
    val i = pp.p.parse(KSParseContext.empty(rootDirectory()), pp.s(), defaultFile())
    i as KSFailure
  }
}
