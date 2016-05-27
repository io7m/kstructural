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

package com.io7m.kstructural.tests.xom

import com.io7m.kstructural.core.KSElement.KSInline.KSInlineFootnoteReference
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineImage
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineLink
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineListOrdered
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineListUnordered
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineTable
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineTerm
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineVerbatim
import com.io7m.kstructural.core.KSElement.KSInline.KSSize
import com.io7m.kstructural.core.KSLink
import com.io7m.kstructural.core.KSLinkContent.KSLinkText
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSParseContext
import com.io7m.kstructural.core.KSParseError
import com.io7m.kstructural.core.KSResult.KSFailure
import com.io7m.kstructural.core.KSResult.KSSuccess
import com.io7m.kstructural.schema.KSSchemaNamespaces
import com.io7m.kstructural.xom.KSXOMInlineParserType
import nu.xom.Node
import org.junit.Assert
import org.junit.Test
import java.math.BigInteger
import java.net.URI
import java.nio.file.Paths
import java.util.Optional

abstract class KSXOMInlineParserContract {

  abstract fun parseXML(text : String) : Node

  abstract fun parser() : KSXOMInlineParserType

  val NAMESPACE = KSSchemaNamespaces.NAMESPACE_URI_TEXT

  private fun defaultContext() =
    KSParseContext.empty(Paths.get(""))
  
  @Test
  fun testTerm() {
    val n = parseXML("""<s:term xmlns:s="${NAMESPACE}">xyz</s:term>""")
    val p = parser()
    val c = defaultContext()
    val r = p.parse(c, n)

    r as KSSuccess<KSInlineTerm<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("xyz", i.content[0].text)
    Assert.assertFalse(i.type.isPresent)
  }

  @Test
  fun testTermType() {
    val n = parseXML("""<s:term xmlns:s="${NAMESPACE}" s:type="t">xyz</s:term>""")
    val p = parser()
    val c = defaultContext()
    val r = p.parse(c, n)

    r as KSSuccess<KSInlineTerm<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("xyz", i.content[0].text)
    Assert.assertEquals("t", i.type.get().value)
  }

  @Test
  fun testTermTypeInvalid() {
    val n = parseXML("""<s:term xmlns:s="${NAMESPACE}" s:type="-">xyz</s:term>""")
    val p = parser()
    val c = defaultContext()
    val r = p.parse(c, n)

    r as KSFailure
  }

  @Test
  fun testImage() {
    val n = parseXML("""<s:image xmlns:s="${NAMESPACE}" s:target="http://example.com">xyz</s:image>""")
    val p = parser()
    val c = defaultContext()
    val r = p.parse(c, n)

    r as KSSuccess<KSInlineImage<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("xyz", i.content[0].text)
    Assert.assertFalse(i.size.isPresent)
    Assert.assertEquals(URI.create("http://example.com"), i.target)
    Assert.assertFalse(i.type.isPresent)
  }

  @Test
  fun testImageErrorTarget() {
    val n = parseXML("""<s:image xmlns:s="${NAMESPACE}" s:target="x x x">xyz</s:image>""")
    val p = parser()
    val c = defaultContext()
    val r = p.parse(c, n)

    r as KSFailure
  }

  @Test
  fun testImageSize() {
    val n = parseXML("""<s:image xmlns:s="${NAMESPACE}" s:width="640" s:height="480" s:target="http://example.com">xyz</s:image>""")
    val p = parser()
    val c = defaultContext()
    val r = p.parse(c, n)

    r as KSSuccess<KSInlineImage<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("xyz", i.content[0].text)
    val size = KSSize(BigInteger.valueOf(640), BigInteger.valueOf(480))
    Assert.assertEquals(Optional.of(size), i.size)
    Assert.assertEquals(URI.create("http://example.com"), i.target)
    Assert.assertFalse(i.type.isPresent)
  }

  @Test
  fun testImageErrorSize() {
    val n = parseXML("""<s:image xmlns:s="${NAMESPACE}" s:width="z" s:height="z" s:target="http://example.com">xyz</s:image>""")
    val p = parser()
    val c = defaultContext()
    val r = p.parse(c, n)

    r as KSFailure
  }

  @Test
  fun testImageType() {
    val n = parseXML("""<s:image xmlns:s="${NAMESPACE}" s:type="t" s:target="http://example.com">xyz</s:image>""")
    val p = parser()
    val c = defaultContext()
    val r = p.parse(c, n)

    r as KSSuccess<KSInlineImage<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("xyz", i.content[0].text)
    Assert.assertFalse(i.size.isPresent)
    Assert.assertEquals(URI.create("http://example.com"), i.target)
    Assert.assertEquals("t", i.type.get().value)
  }

  @Test
  fun testLink() {
    val n = parseXML("""<s:link xmlns:s="${NAMESPACE}" s:target="q">xyz</s:link>""")
    val p = parser()
    val c = defaultContext()
    val r = p.parse(c, n)

    r as KSSuccess<KSInlineLink<KSParse>, KSParseError>
    val i = r.result.actual as KSLink.KSLinkInternal
    Assert.assertEquals("xyz", (i.content[0] as KSLinkText<*>).actual.text)
    Assert.assertEquals("q", i.target.value)
  }

  @Test
  fun testLinkInvalidID() {
    val n = parseXML("""<s:link xmlns:s="${NAMESPACE}" s:target="-">xyz</s:link>""")
    val p = parser()
    val c = defaultContext()
    val r = p.parse(c, n)

    r as KSFailure
  }

  @Test
  fun testLinkErrorTerm() {
    val n = parseXML("""<s:link xmlns:s="${NAMESPACE}" s:target="q"><s:term>q</s:term></s:link>""")
    val p = parser()
    val c = defaultContext()
    val r = p.parse(c, n)

    r as KSFailure
  }

  @Test
  fun testLinkExternal() {
    val n = parseXML("""<s:link-external xmlns:s="${NAMESPACE}" s:target="q">xyz</s:link-external>""")
    val p = parser()
    val c = defaultContext()
    val r = p.parse(c, n)

    r as KSSuccess<KSInlineLink<KSParse>, KSParseError>
    val i = r.result.actual as KSLink.KSLinkExternal
    Assert.assertEquals("xyz", (i.content[0] as KSLinkText<*>).actual.text)
    Assert.assertEquals(URI.create("q"), i.target)
  }

  @Test
  fun testLinkExternalErrorTerm() {
    val n = parseXML("""<s:link-external xmlns:s="${NAMESPACE}" s:target="q"><s:term>q</s:term></s:link-external>""")
    val p = parser()
    val c = defaultContext()
    val r = p.parse(c, n)

    r as KSFailure
  }

  @Test
  fun testVerbatim() {
    val n = parseXML("""<s:verbatim xmlns:s="${NAMESPACE}">xyz</s:verbatim>""")
    val p = parser()
    val c = defaultContext()
    val r = p.parse(c, n)

    r as KSSuccess<KSInlineVerbatim<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("xyz", i.text.text)
    Assert.assertFalse(i.type.isPresent)
  }

  @Test
  fun testVerbatimType() {
    val n = parseXML("""<s:verbatim s:type="t" xmlns:s="${NAMESPACE}">xyz</s:verbatim>""")
    val p = parser()
    val c = defaultContext()
    val r = p.parse(c, n)

    r as KSSuccess<KSInlineVerbatim<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("xyz", i.text.text)
    Assert.assertEquals("t", i.type.get().value)
  }

  @Test
  fun testListOrdered() {
    val n = parseXML("""<s:list-ordered xmlns:s="${NAMESPACE}"><s:item>x</s:item><s:item>y</s:item></s:list-ordered>""")
    val p = parser()
    val c = defaultContext()
    val r = p.parse(c, n)

    r as KSSuccess<KSInlineListOrdered<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals(2, i.content.size)
  }

  @Test
  fun testListOrderedError() {
    val n = parseXML("""<s:list-ordered xmlns:s="${NAMESPACE}">x</s:list-ordered>""")
    val p = parser()
    val c = defaultContext()
    val r = p.parse(c, n)

    r as KSFailure
  }

  @Test
  fun testListUnordered() {
    val n = parseXML("""<s:list-unordered xmlns:s="${NAMESPACE}"><s:item>x</s:item><s:item>y</s:item></s:list-unordered>""")
    val p = parser()
    val c = defaultContext()
    val r = p.parse(c, n)

    r as KSSuccess<KSInlineListUnordered<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals(2, i.content.size)
  }

  @Test
  fun testListUnorderedError() {
    val n = parseXML("""<s:list-unordered xmlns:s="${NAMESPACE}">x</s:list-unordered>""")
    val p = parser()
    val c = defaultContext()
    val r = p.parse(c, n)

    r as KSFailure
  }

  @Test
  fun testTable() {
    val n = parseXML("""
<s:table s:summary="A B C" xmlns:s="${NAMESPACE}">
  <s:body>
    <s:row>
      <s:cell>x</s:cell>
      <s:cell>y</s:cell>
    </s:row>
    <s:row>
      <s:cell>x</s:cell>
      <s:cell>y</s:cell>
    </s:row>
  </s:body>
</s:table>""")
    val p = parser()
    val c = defaultContext()
    val r = p.parse(c, n)

    r as KSSuccess<KSInlineTable<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("A B C", i.summary.content[0].text)
    Assert.assertFalse(i.head.isPresent)
    Assert.assertEquals(2, i.body.rows.size)
    Assert.assertEquals(2, i.body.rows[0].cells.size)
    Assert.assertEquals(2, i.body.rows[1].cells.size)
  }

  @Test
  fun testTableHead() {
    val n = parseXML("""
<s:table s:summary="A B C" xmlns:s="${NAMESPACE}">
  <s:head>
    <s:name>A</s:name>
    <s:name>B</s:name>
    <s:name>C</s:name>
  </s:head>
  <s:body>
    <s:row>
      <s:cell>x</s:cell>
      <s:cell>y</s:cell>
    </s:row>
    <s:row>
      <s:cell>x</s:cell>
      <s:cell>y</s:cell>
    </s:row>
  </s:body>
</s:table>""")
    val p = parser()
    val c = defaultContext()
    val r = p.parse(c, n)

    r as KSSuccess<KSInlineTable<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("A B C", i.summary.content[0].text)
    val ih = i.head.get()
    Assert.assertEquals(3, ih.column_names.size)
    Assert.assertEquals(2, i.body.rows.size)
    Assert.assertEquals(2, i.body.rows[0].cells.size)
    Assert.assertEquals(2, i.body.rows[1].cells.size)
  }

  @Test
  fun testFootnoteReference() {
    val n = parseXML("""<s:footnote-ref s:target="x" xmlns:s="${NAMESPACE}"/>""")
    val p = parser()
    val c = defaultContext()
    val r = p.parse(c, n)

    r as KSSuccess<KSInlineFootnoteReference<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("x", i.target.value)
  }
}