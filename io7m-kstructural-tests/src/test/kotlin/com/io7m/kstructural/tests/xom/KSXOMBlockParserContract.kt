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

import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithParts
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithSections
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockFootnote
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockFormalItem
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockParagraph
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockPart
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection.KSBlockSectionWithContent
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection.KSBlockSectionWithSubsections
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSubsection
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineText
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSParseContext
import com.io7m.kstructural.core.KSParseError
import com.io7m.kstructural.core.KSResult.KSFailure
import com.io7m.kstructural.core.KSResult.KSSuccess
import com.io7m.kstructural.schema.KSSchemaNamespaces
import com.io7m.kstructural.xom.KSXOMBlockParserType
import nu.xom.Element
import org.junit.Assert
import org.junit.Test
import java.util.Optional

abstract class KSXOMBlockParserContract {

  abstract fun parseXML(text : String) : Element

  abstract fun parser() : KSXOMBlockParserType

  val NAMESPACE = KSSchemaNamespaces.NAMESPACE_URI_TEXT

  @Test
  fun testParagraphInvalidID() {
    val n = parseXML("""<s:paragraph xmlns:s="${NAMESPACE}" xml:id="-">x y z</s:paragraph>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSFailure
  }

  @Test
  fun testParagraph() {
    val n = parseXML("""<s:paragraph xmlns:s="${NAMESPACE}">x y z</s:paragraph>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSSuccess<KSBlockParagraph<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals(1, i.content.size)
    Assert.assertEquals("x y z", (i.content[0] as KSInlineText).text)
    Assert.assertFalse(i.type.isPresent)
    Assert.assertFalse(i.id.isPresent)
  }

  @Test
  fun testParagraphType() {
    val n = parseXML("""<s:paragraph s:type="t" xmlns:s="${NAMESPACE}">x y z</s:paragraph>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSSuccess<KSBlockParagraph<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals(1, i.content.size)
    Assert.assertEquals("x y z", (i.content[0] as KSInlineText).text)
    Assert.assertEquals(Optional.of("t"), i.type)
    Assert.assertFalse(i.id.isPresent)
  }

  @Test
  fun testParagraphTypeID() {
    val n = parseXML("""<s:paragraph xml:id="x" s:type="t" xmlns:s="${NAMESPACE}">x y z</s:paragraph>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSSuccess<KSBlockParagraph<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals(1, i.content.size)
    Assert.assertEquals("x y z", (i.content[0] as KSInlineText).text)
    Assert.assertEquals(Optional.of("t"), i.type)
    Assert.assertEquals("x", i.id.get().value)
  }

  @Test
  fun testFormalItem() {
    val n = parseXML("""<s:formal-item s:title="F" xmlns:s="${NAMESPACE}">x y z</s:formal-item>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSSuccess<KSBlockFormalItem<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals(1, i.content.size)
    Assert.assertEquals("x y z", (i.content[0] as KSInlineText).text)
    Assert.assertEquals("F", i.title[0].text)
    Assert.assertFalse(i.type.isPresent)
    Assert.assertFalse(i.id.isPresent)
  }

  @Test
  fun testFormalItemType() {
    val n = parseXML("""<s:formal-item s:title="F" s:type="t" xmlns:s="${NAMESPACE}">x y z</s:formal-item>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSSuccess<KSBlockFormalItem<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals(1, i.content.size)
    Assert.assertEquals("x y z", (i.content[0] as KSInlineText).text)
    Assert.assertEquals("F", i.title[0].text)
    Assert.assertEquals(Optional.of("t"), i.type)
    Assert.assertFalse(i.id.isPresent)
  }

  @Test
  fun testFormalItemTypeID() {
    val n = parseXML("""<s:formal-item s:title="F" xml:id="x" s:type="t" xmlns:s="${NAMESPACE}">x y z</s:formal-item>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSSuccess<KSBlockFormalItem<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals(1, i.content.size)
    Assert.assertEquals("x y z", (i.content[0] as KSInlineText).text)
    Assert.assertEquals("F", i.title[0].text)
    Assert.assertEquals(Optional.of("t"), i.type)
    Assert.assertEquals("x", i.id.get().value)
  }

  @Test
  fun testFootnote() {
    val n = parseXML("""<s:footnote xml:id="x" xmlns:s="${NAMESPACE}">x y z</s:footnote>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSSuccess<KSBlockFootnote<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals(1, i.content.size)
    Assert.assertEquals("x y z", (i.content[0] as KSInlineText).text)
    Assert.assertFalse(i.type.isPresent)
    Assert.assertEquals("x", i.id.get().value)
  }

  @Test
  fun testFootnoteInvalidId() {
    val n = parseXML("""<s:footnote xml:id="-" xmlns:s="${NAMESPACE}">x y z</s:footnote>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSFailure
  }

  @Test
  fun testFootnoteTypeID() {
    val n = parseXML("""<s:footnote xml:id="x" s:type="t" xmlns:s="${NAMESPACE}">x y z</s:footnote>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSSuccess<KSBlockFootnote<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals(1, i.content.size)
    Assert.assertEquals("x y z", (i.content[0] as KSInlineText).text)
    Assert.assertEquals(Optional.of("t"), i.type)
    Assert.assertEquals("x", i.id.get().value)
  }

  @Test
  fun testSection() {
    val n = parseXML("""<s:section s:title="S" xmlns:s="${NAMESPACE}"></s:section>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSSuccess<KSBlockSection<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("S", i.title[0].text)
    Assert.assertFalse(i.type.isPresent)
    Assert.assertFalse(i.id.isPresent)
  }

  @Test
  fun testSectionErrorContent() {
    val n = parseXML("""<s:section s:title="S" xmlns:s="${NAMESPACE}"><s:term/></s:section>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSFailure
  }

  @Test
  fun testSectionErrorContentMixed() {
    val n = parseXML("""<s:section s:title="S" xmlns:s="${NAMESPACE}"><s:subsection s:title="SS"/><s:paragraph/></s:section>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSFailure
  }

  @Test
  fun testSectionSubsections() {
    val n = parseXML("""<s:section s:title="S" xmlns:s="${NAMESPACE}"><s:subsection s:title="SS"/></s:section>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSSuccess<KSBlockSectionWithSubsections<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("S", i.title[0].text)
    Assert.assertEquals(1, i.content.size)
    Assert.assertFalse(i.type.isPresent)
    Assert.assertFalse(i.id.isPresent)
  }

  @Test
  fun testSectionContent() {
    val n = parseXML("""<s:section s:title="S" xmlns:s="${NAMESPACE}"><s:paragraph/></s:section>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSSuccess<KSBlockSectionWithContent<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("S", i.title[0].text)
    Assert.assertEquals(1, i.content.size)
    Assert.assertFalse(i.type.isPresent)
    Assert.assertFalse(i.id.isPresent)
  }

  @Test
  fun testSectionType() {
    val n = parseXML("""<s:section s:type="t" s:title="S" xmlns:s="${NAMESPACE}"></s:section>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSSuccess<KSBlockSection<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("S", i.title[0].text)
    Assert.assertEquals(Optional.of("t"), i.type)
    Assert.assertFalse(i.id.isPresent)
  }

  @Test
  fun testSectionTypeID() {
    val n = parseXML("""<s:section xml:id="x" s:type="t" s:title="S" xmlns:s="${NAMESPACE}"></s:section>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSSuccess<KSBlockSection<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("S", i.title[0].text)
    Assert.assertEquals(Optional.of("t"), i.type)
    Assert.assertEquals("x", i.id.get().value)
  }

  @Test
  fun testSubsection() {
    val n = parseXML("""<s:subsection s:title="S" xmlns:s="${NAMESPACE}"></s:subsection>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSSuccess<KSBlockSubsection<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("S", i.title[0].text)
    Assert.assertFalse(i.type.isPresent)
    Assert.assertFalse(i.id.isPresent)
  }

  @Test
  fun testSubsectionType() {
    val n = parseXML("""<s:subsection s:type="t" s:title="S" xmlns:s="${NAMESPACE}"></s:subsection>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSSuccess<KSBlockSubsection<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("S", i.title[0].text)
    Assert.assertEquals(Optional.of("t"), i.type)
    Assert.assertFalse(i.id.isPresent)
  }

  @Test
  fun testSubsectionTypeID() {
    val n = parseXML("""<s:subsection xml:id="x" s:type="t" s:title="S" xmlns:s="${NAMESPACE}"></s:subsection>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSSuccess<KSBlockSubsection<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("S", i.title[0].text)
    Assert.assertEquals(Optional.of("t"), i.type)
    Assert.assertEquals("x", i.id.get().value)
  }

  @Test
  fun testSubsectionErrorContent() {
    val n = parseXML("""<s:subsection s:title="S" xmlns:s="${NAMESPACE}"><s:section s:title="S"/></s:subsection>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSFailure
  }

  @Test
  fun testPart() {
    val n = parseXML("""<s:part s:title="S" xmlns:s="${NAMESPACE}"></s:part>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSSuccess<KSBlockPart<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("S", i.title[0].text)
    Assert.assertFalse(i.type.isPresent)
    Assert.assertFalse(i.id.isPresent)
  }

  @Test
  fun testPartErrorSubsection() {
    val n = parseXML("""<s:part s:title="S" xmlns:s="${NAMESPACE}"><s:subsection s:title="SS"/></s:part>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSFailure
  }

  @Test
  fun testDocument() {
    val n = parseXML("""<s:document s:title="S" xmlns:s="${NAMESPACE}"></s:document>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSSuccess<KSBlockDocumentWithParts<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("S", i.title[0].text)
    Assert.assertFalse(i.type.isPresent)
    Assert.assertFalse(i.id.isPresent)
  }

  @Test
  fun testDocumentParts() {
    val n = parseXML("""<s:document s:title="S" xmlns:s="${NAMESPACE}"><s:part s:title="P"/></s:document>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSSuccess<KSBlockDocumentWithParts<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("S", i.title[0].text)
    Assert.assertEquals(1, i.content.size)
    Assert.assertFalse(i.type.isPresent)
    Assert.assertFalse(i.id.isPresent)
  }

  @Test
  fun testDocumentSections() {
    val n = parseXML("""<s:document s:title="S" xmlns:s="${NAMESPACE}"><s:section s:title="P"/></s:document>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSSuccess<KSBlockDocumentWithSections<KSParse>, KSParseError>
    val i = r.result
    Assert.assertEquals("S", i.title[0].text)
    Assert.assertEquals(1, i.content.size)
    Assert.assertFalse(i.type.isPresent)
    Assert.assertFalse(i.id.isPresent)
  }

  @Test
  fun testDocumentErrorMixedPart() {
    val n = parseXML("""<s:document s:title="S" xmlns:s="${NAMESPACE}"><s:part s:title="P"/><s:section s:title="K"/></s:document>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSFailure
  }

  @Test
  fun testDocumentErrorMixedSection() {
    val n = parseXML("""<s:document s:title="S" xmlns:s="${NAMESPACE}"><s:section s:title="K"/><s:part s:title="P"/></s:document>""")
    val p = parser()
    val c = KSParseContext.empty()
    val r = p.parse(c, n)

    r as KSFailure
  }
}