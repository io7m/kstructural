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

import com.io7m.kstructural.core.KSElement
import com.io7m.kstructural.core.KSElement.*
import com.io7m.kstructural.core.KSElement.KSInline.*
import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSLink
import com.io7m.kstructural.core.KSLinkContent
import com.io7m.kstructural.core.KSLinkContent.*
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSParseContext
import com.io7m.kstructural.core.KSParseError
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.core.KSResult.*
import com.io7m.kstructural.schema.KSXMLNamespace
import com.io7m.kstructural.xom.KSXOMBlockParserType
import com.io7m.kstructural.xom.KSXOMInlineParser
import com.io7m.kstructural.xom.KSXOMInlineParserType
import nu.xom.Node
import org.junit.Assert
import org.junit.Test
import java.math.BigInteger
import java.net.URI
import java.util.Optional

abstract class KSXOMInlineParserContract {

  abstract fun parseXML(text : String) : Node

  abstract fun parser() : KSXOMInlineParserType

  val NAMESPACE = KSXMLNamespace.NAMESPACE_URI_TEXT

  @Test
  fun testTerm() {
    val n = parseXML("""<s:term xmlns:s="${NAMESPACE}">xyz</s:term>""")
    val p = parser()
    val c = KSParseContext.empty()
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
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSSuccess<KSInlineTerm<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("xyz", i.content[0].text)
    Assert.assertEquals(Optional.of("t"), i.type)
  }

  @Test
  fun testImage() {
    val n = parseXML("""<s:image xmlns:s="${NAMESPACE}" s:target="http://example.com">xyz</s:image>""")
    val p = parser()
    val c = KSParseContext.empty()
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
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSFailure
  }

  @Test
  fun testImageSize() {
    val n = parseXML("""<s:image xmlns:s="${NAMESPACE}" s:width="640" s:height="480" s:target="http://example.com">xyz</s:image>""")
    val p = parser()
    val c = KSParseContext.empty()
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
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSFailure
  }

  @Test
  fun testImageType() {
    val n = parseXML("""<s:image xmlns:s="${NAMESPACE}" s:type="t" s:target="http://example.com">xyz</s:image>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSSuccess<KSInlineImage<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("xyz", i.content[0].text)
    Assert.assertFalse(i.size.isPresent)
    Assert.assertEquals(URI.create("http://example.com"), i.target)
    Assert.assertEquals(Optional.of("t"), i.type)
  }

  @Test
  fun testLink() {
    val n = parseXML("""<s:link xmlns:s="${NAMESPACE}" s:target="q">xyz</s:link>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSSuccess<KSInlineLink<KSParse>, KSParseError>
    val i = r.result.actual as KSLink.KSLinkInternal
    Assert.assertEquals("xyz", (i.content[0] as KSLinkText<*>).actual.text)
    Assert.assertEquals("q", i.target.value)
  }

  @Test
  fun testLinkErrorTerm() {
    val n = parseXML("""<s:link xmlns:s="${NAMESPACE}" s:target="q"><s:term>q</s:term></s:link>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSFailure
  }

  @Test
  fun testLinkExternal() {
    val n = parseXML("""<s:link-external xmlns:s="${NAMESPACE}" s:target="q">xyz</s:link-external>""")
    val p = parser()
    val c = KSParseContext.empty()
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
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSFailure
  }

  @Test
  fun testVerbatim() {
    val n = parseXML("""<s:verbatim xmlns:s="${NAMESPACE}">xyz</s:verbatim>""")
    val p = parser()
    val c = KSParseContext.empty()
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
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSSuccess<KSInlineVerbatim<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("xyz", i.text.text)
    Assert.assertEquals(Optional.of("t"), i.type)
  }
}