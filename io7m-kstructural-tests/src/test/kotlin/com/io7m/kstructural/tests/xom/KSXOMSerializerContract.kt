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

package com.io7m.kstructural.tests.xom

import com.io7m.kstructural.core.KSElement
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.schema.KSXMLNamespace
import com.io7m.kstructural.xom.KSXOMInlineParser
import nu.xom.Node
import org.junit.Assert
import org.junit.Test
import org.slf4j.LoggerFactory

abstract class KSXOMSerializerContract {

  val LOG = LoggerFactory.getLogger(KSXOMSerializerContract::class.java)

  val NAMESPACE = KSXMLNamespace.NAMESPACE_URI_TEXT

  abstract fun parseInlineXML(text : String) : KSElement<KSParse>

  abstract fun parseBlockXML(text : String) : KSElement<KSParse>

  abstract fun serializeXML(e : KSElement<KSParse>) : String

  fun roundTripInline(text : String) {
    val tt = text.trim()
    LOG.trace("text : {}", tt)
    val e0 = parseInlineXML(tt)
    val r0 = serializeXML(e0).trim()
    LOG.trace("r0   : {}", r0)
    val e1 = parseInlineXML(r0)
    val r1 = serializeXML(e1).trim()
    LOG.trace("r1   : {}", r1)

    Assert.assertEquals(tt, r0)
    Assert.assertEquals(r0, r1)
  }

  fun roundTripBlock(text : String) {
    val tt = text.trim()
    LOG.trace("text : {}", tt)
    val e0 = parseBlockXML(tt)
    val r0 = serializeXML(e0).trim()
    LOG.trace("r0   : {}", r0)
    val e1 = parseBlockXML(r0)
    val r1 = serializeXML(e1).trim()
    LOG.trace("r1   : {}", r1)

    Assert.assertEquals(tt, r0)
    Assert.assertEquals(r0, r1)
  }

  @Test fun testVerbatim()
  {
    roundTripInline("""
<?xml version="1.0" encoding="UTF-8"?>
<s:verbatim xmlns:s="${NAMESPACE}">x y z</s:verbatim>""")
  }

  @Test fun testVerbatimType()
  {
    roundTripInline("""
<?xml version="1.0" encoding="UTF-8"?>
<s:verbatim s:type="t" xmlns:s="${NAMESPACE}">x y z</s:verbatim>""")
  }

  @Test fun testTerm()
  {
    roundTripInline("""
<?xml version="1.0" encoding="UTF-8"?>
<s:term xmlns:s="${NAMESPACE}">x y z</s:term>""")
  }

  @Test fun testTermType()
  {
    roundTripInline("""
<?xml version="1.0" encoding="UTF-8"?>
<s:term s:type="t" xmlns:s="${NAMESPACE}">x y z</s:term>""")
  }

  @Test fun testImage()
  {
    roundTripInline("""
<?xml version="1.0" encoding="UTF-8"?>
<s:image s:target="http://example.com" xmlns:s="${NAMESPACE}">x y z</s:image>""")
  }

  @Test fun testImageType()
  {
    roundTripInline("""
<?xml version="1.0" encoding="UTF-8"?>
<s:image s:target="http://example.com" s:type="t" xmlns:s="${NAMESPACE}">x y z</s:image>""")
  }

  @Test fun testImageSize()
  {
    roundTripInline("""
<?xml version="1.0" encoding="UTF-8"?>
<s:image s:target="http://example.com" s:width="640" s:height="480" xmlns:s="${NAMESPACE}">x y z</s:image>""")
  }

  @Test fun testLink()
  {
    roundTripInline("""
<?xml version="1.0" encoding="UTF-8"?>
<s:link s:target="x" xmlns:s="${NAMESPACE}">x y z</s:link>""")
  }

  @Test fun testLinkExternal()
  {
    roundTripInline("""
<?xml version="1.0" encoding="UTF-8"?>
<s:link-external s:target="http://example.com" xmlns:s="${NAMESPACE}">x y z</s:link-external>""")
  }

  @Test fun testParagraph()
  {
    roundTripBlock("""
<?xml version="1.0" encoding="UTF-8"?>
<s:paragraph xmlns:s="${NAMESPACE}">x y z</s:paragraph>""")
  }

  @Test fun testParagraphType()
  {
    roundTripBlock("""
<?xml version="1.0" encoding="UTF-8"?>
<s:paragraph s:type="t" xmlns:s="${NAMESPACE}">x y z</s:paragraph>""")
  }

  @Test fun testParagraphTypeID()
  {
    roundTripBlock("""
<?xml version="1.0" encoding="UTF-8"?>
<s:paragraph s:type="t" xml:id="x" xmlns:s="${NAMESPACE}">x y z</s:paragraph>""")
  }

  @Test fun testSection()
  {
    roundTripBlock("""
<?xml version="1.0" encoding="UTF-8"?>
<s:section s:title="A B C" xmlns:s="${NAMESPACE}"/>""")
  }

  @Test fun testSectionContent()
  {
    roundTripBlock("""
<?xml version="1.0" encoding="UTF-8"?>
<s:section s:title="A B C" xmlns:s="${NAMESPACE}"><s:paragraph/></s:section>""")
  }

  @Test fun testSectionType()
  {
    roundTripBlock("""
<?xml version="1.0" encoding="UTF-8"?>
<s:section s:type="t" s:title="A B C" xmlns:s="${NAMESPACE}"/>""")
  }

  @Test fun testSectionTypeID()
  {
    roundTripBlock("""
<?xml version="1.0" encoding="UTF-8"?>
<s:section s:type="t" xml:id="x" s:title="A B C" xmlns:s="${NAMESPACE}"/>""")
  }

  @Test fun testSubsection()
  {
    roundTripBlock("""
<?xml version="1.0" encoding="UTF-8"?>
<s:subsection s:title="A B C" xmlns:s="${NAMESPACE}"/>""")
  }

  @Test fun testSubsectionType()
  {
    roundTripBlock("""
<?xml version="1.0" encoding="UTF-8"?>
<s:subsection s:type="t" s:title="A B C" xmlns:s="${NAMESPACE}"/>""")
  }

  @Test fun testSubsectionTypeID()
  {
    roundTripBlock("""
<?xml version="1.0" encoding="UTF-8"?>
<s:subsection s:type="t" xml:id="x" s:title="A B C" xmlns:s="${NAMESPACE}"/>""")
  }

  @Test fun testPart()
  {
    roundTripBlock("""
<?xml version="1.0" encoding="UTF-8"?>
<s:part s:title="A B C" xmlns:s="${NAMESPACE}"/>""")
  }

  @Test fun testPartType()
  {
    roundTripBlock("""
<?xml version="1.0" encoding="UTF-8"?>
<s:part s:type="t" s:title="A B C" xmlns:s="${NAMESPACE}"/>""")
  }

  @Test fun testPartTypeID()
  {
    roundTripBlock("""
<?xml version="1.0" encoding="UTF-8"?>
<s:part s:type="t" xml:id="x" s:title="A B C" xmlns:s="${NAMESPACE}"/>""")
  }

  @Test fun testDocument()
  {
    roundTripBlock("""
<?xml version="1.0" encoding="UTF-8"?>
<s:document s:title="A B C" xmlns:s="${NAMESPACE}"/>""")
  }

  @Test fun testDocumentSection()
  {
    roundTripBlock("""
<?xml version="1.0" encoding="UTF-8"?>
<s:document s:title="A B C" xmlns:s="${NAMESPACE}"><s:section s:title="A B C"/></s:document>""")
  }

  @Test fun testDocumentPart()
  {
    roundTripBlock("""
<?xml version="1.0" encoding="UTF-8"?>
<s:document s:title="A B C" xmlns:s="${NAMESPACE}"><s:part s:title="A B C"/></s:document>""")
  }

  @Test fun testFootnote()
  {
    roundTripBlock("""
<?xml version="1.0" encoding="UTF-8"?>
<s:footnote xml:id="x" xmlns:s="${NAMESPACE}">x y z</s:footnote>""")
  }

  @Test fun testFootnoteTypeID()
  {
    roundTripBlock("""
<?xml version="1.0" encoding="UTF-8"?>
<s:footnote s:type="t" xml:id="x" xmlns:s="${NAMESPACE}">x y z</s:footnote>""")
  }

  @Test fun testFormalItem()
  {
    roundTripBlock("""
<?xml version="1.0" encoding="UTF-8"?>
<s:formal-item s:title="A B C" xmlns:s="${NAMESPACE}">x y z</s:formal-item>""")
  }

  @Test fun testFormalItemType()
  {
    roundTripBlock("""
<?xml version="1.0" encoding="UTF-8"?>
<s:formal-item s:type="t" s:title="A B C" xmlns:s="${NAMESPACE}">x y z</s:formal-item>""")
  }

  @Test fun testFormalItemTypeID()
  {
    roundTripBlock("""
<?xml version="1.0" encoding="UTF-8"?>
<s:formal-item s:type="t" xml:id="x" s:title="A B C" xmlns:s="${NAMESPACE}">x y z</s:formal-item>""")
  }
}