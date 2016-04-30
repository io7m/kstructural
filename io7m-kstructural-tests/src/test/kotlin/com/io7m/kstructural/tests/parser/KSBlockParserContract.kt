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

import com.io7m.kstructural.core.KSBlock.KSBlockDocument
import com.io7m.kstructural.core.KSBlock.KSBlockDocument.KSBlockDocumentWithParts
import com.io7m.kstructural.core.KSBlock.KSBlockDocument.KSBlockDocumentWithSections
import com.io7m.kstructural.core.KSBlock.KSBlockParagraph
import com.io7m.kstructural.core.KSBlock.KSBlockPart
import com.io7m.kstructural.core.KSBlock.KSBlockSection.KSBlockSectionWithContent
import com.io7m.kstructural.core.KSBlock.KSBlockSection.KSBlockSectionWithSubsections
import com.io7m.kstructural.core.KSBlock.KSBlockSubsection
import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSInline
import com.io7m.kstructural.core.KSInline.KSInlineText
import com.io7m.kstructural.core.KSResult.KSFailure
import com.io7m.kstructural.core.KSResult.KSSuccess
import com.io7m.kstructural.core.KSSubsectionContent
import com.io7m.kstructural.parser.KSBlockParserType
import com.io7m.kstructural.parser.KSExpression
import com.io7m.kstructural.parser.KSParseError
import org.junit.Assert
import org.junit.Test
import java.util.Optional


abstract class KSBlockParserContract {

  protected abstract fun newParserForString(text : String) : Parser

  data class Parser(
    val p : KSBlockParserType,
    val s : () -> KSExpression)

  @Test fun testParaError0()
  {
    val pp = newParserForString("[paragraph [link]]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSFailure<KSBlockParagraph<*>, KSParseError>
    Assert.assertTrue(e.partial.isPresent)
    Assert.assertEquals(1, e.errors.size)
  }

  @Test fun testParaSimple()
  {
    val pp = newParserForString("[paragraph Hello.]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockParagraph<Unit>, KSParseError>
    Assert.assertEquals(1, e.result.content.size)
    val t0 = e.result.content[0] as KSInline.KSInlineText<Unit>
    Assert.assertEquals("Hello.", t0.text)
  }

  @Test fun testParaID()
  {
    val pp = newParserForString("[paragraph [id x] Hello.]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockParagraph<Unit>, KSParseError>
    Assert.assertEquals("x", e.result.id.get().value)
    Assert.assertEquals(1, e.result.content.size)
    val t0 = e.result.content[0] as KSInline.KSInlineText<Unit>
    Assert.assertEquals("Hello.", t0.text)
  }

  @Test fun testParaType()
  {
    val pp = newParserForString("[paragraph [type x] Hello.]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockParagraph<Unit>, KSParseError>
    Assert.assertEquals("x", e.result.type.get())
    Assert.assertEquals(1, e.result.content.size)
    val t0 = e.result.content[0] as KSInline.KSInlineText<Unit>
    Assert.assertEquals("Hello.", t0.text)
  }

  @Test fun testParaTypeID()
  {
    val pp = newParserForString("[paragraph [type x] [id y] Hello.]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockParagraph<Unit>, KSParseError>
    Assert.assertEquals("x", e.result.type.get())
    Assert.assertEquals("y", e.result.id.get().value)
    Assert.assertEquals(1, e.result.content.size)
    val t0 = e.result.content[0] as KSInline.KSInlineText<Unit>
    Assert.assertEquals("Hello.", t0.text)
  }

  @Test fun testParaIDType()
  {
    val pp = newParserForString("[paragraph [id y] [type x] Hello.]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockParagraph<Unit>, KSParseError>
    Assert.assertEquals("x", e.result.type.get())
    Assert.assertEquals("y", e.result.id.get().value)
    Assert.assertEquals(1, e.result.content.size)
    val t0 = e.result.content[0] as KSInline.KSInlineText<Unit>
    Assert.assertEquals("Hello.", t0.text)
  }

  @Test fun testSubsectionErrorEmpty()
  {
    val pp = newParserForString("[subsection]")

    val e = pp.p.parse(pp.s.invoke())
    e as KSFailure<KSBlockSubsection<*>, KSParseError>

    Assert.assertFalse(e.partial.isPresent)
    Assert.assertEquals(1, e.errors.size)
  }

  @Test fun testSubsectionErrorWrongContent()
  {
    val pp = newParserForString("[subsection [title t] [subsection [title w]]]")

    val e = pp.p.parse(pp.s.invoke())
    e as KSFailure<KSBlockSubsection<*>, KSParseError>

    Assert.assertTrue(e.partial.isPresent)
    Assert.assertEquals(1, e.errors.size)
  }

  @Test fun testSubsectionErrorWrongTitle()
  {
    val pp = newParserForString("[subsection [title x [term q]]]")

    val e = pp.p.parse(pp.s.invoke())
    e as KSFailure<KSBlockSubsection<*>, KSParseError>

    Assert.assertTrue(e.partial.isPresent)
    Assert.assertEquals(1, e.errors.size)
  }

  @Test fun testSubsection()
  {
    val pp = newParserForString("[subsection [title t]]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockSubsection<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.empty<KSID<Unit>>(), e.result.id)
    Assert.assertEquals(0, e.result.content.size)
  }

  @Test fun testSubsectionID()
  {
    val pp = newParserForString("[subsection [title t] [id x]]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockSubsection<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals("x", e.result.id.get().value)
    Assert.assertEquals(0, e.result.content.size)
  }

  @Test fun testSubsectionIDType()
  {
    val pp = newParserForString("[subsection [title t] [id x] [type k]]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockSubsection<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals("x", e.result.id.get().value)
    Assert.assertEquals("k", e.result.type.get())
    Assert.assertEquals(0, e.result.content.size)
  }

  @Test fun testSubsectionTypeID()
  {
    val pp = newParserForString("[subsection [title t] [type k] [id x]]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockSubsection<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals("x", e.result.id.get().value)
    Assert.assertEquals("k", e.result.type.get())
    Assert.assertEquals(0, e.result.content.size)
  }

  @Test fun testSubsectionType()
  {
    val pp = newParserForString("[subsection [title t] [type k]]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockSubsection<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.empty<KSID<Unit>>(), e.result.id)
    Assert.assertEquals("k", e.result.type.get())
    Assert.assertEquals(0, e.result.content.size)
  }

  @Test fun testSubsectionContent()
  {
    val pp = newParserForString("[subsection [title t] [paragraph Hello.]]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockSubsection<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.empty<KSID<Unit>>(), e.result.id)
    Assert.assertEquals(1, e.result.content.size)
    val p = e.result.content[0] as KSSubsectionContent.KSSubsectionParagraph<Unit>
    val t = p.paragraph.content[0] as KSInline.KSInlineText<Unit>
    Assert.assertEquals("Hello.", t.text)
  }

  @Test fun testSection()
  {
    val pp = newParserForString("[section [title t] [paragraph p]]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockSectionWithContent<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.empty<KSID<Unit>>(), e.result.id)
    Assert.assertEquals(Optional.empty<String>(), e.result.type)
    Assert.assertEquals(1, e.result.content.size)
    val sp = e.result.content[0] as KSSubsectionContent.KSSubsectionParagraph
    val pc = sp.paragraph.content[0] as KSInlineText<Unit>
    Assert.assertEquals("p", pc.text)
  }

  @Test fun testSectionID()
  {
    val pp = newParserForString("[section [title t] [id x] [paragraph p]]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockSectionWithContent<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.of(KSID(Optional.empty(), "x", Unit)), e.result.id)
    Assert.assertEquals(Optional.empty<String>(), e.result.type)
    Assert.assertEquals(1, e.result.content.size)
    val sp = e.result.content[0] as KSSubsectionContent.KSSubsectionParagraph
    val pc = sp.paragraph.content[0] as KSInlineText<Unit>
    Assert.assertEquals("p", pc.text)
  }

  @Test fun testSectionIDType()
  {
    val pp = newParserForString("[section [title t] [id x] [type t] [paragraph p]]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockSectionWithContent<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.of(KSID(Optional.empty(), "x", Unit)), e.result.id)
    Assert.assertEquals(Optional.of("t"), e.result.type)
    Assert.assertEquals(1, e.result.content.size)
    val sp = e.result.content[0] as KSSubsectionContent.KSSubsectionParagraph
    val pc = sp.paragraph.content[0] as KSInlineText<Unit>
    Assert.assertEquals("p", pc.text)
  }

  @Test fun testSectionTypeID()
  {
    val pp = newParserForString("[section [title t] [type t] [id x] [paragraph p]]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockSectionWithContent<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.of(KSID(Optional.empty(), "x", Unit)), e.result.id)
    Assert.assertEquals(Optional.of("t"), e.result.type)
    Assert.assertEquals(1, e.result.content.size)
    val sp = e.result.content[0] as KSSubsectionContent.KSSubsectionParagraph
    val pc = sp.paragraph.content[0] as KSInlineText<Unit>
    Assert.assertEquals("p", pc.text)
  }

  @Test fun testSectionType()
  {
    val pp = newParserForString("[section [title t] [type t] [paragraph p]]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockSectionWithContent<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.empty<KSID<Unit>>(), e.result.id)
    Assert.assertEquals(Optional.of("t"), e.result.type)
    Assert.assertEquals(1, e.result.content.size)
    val sp = e.result.content[0] as KSSubsectionContent.KSSubsectionParagraph
    val pc = sp.paragraph.content[0] as KSInlineText<Unit>
    Assert.assertEquals("p", pc.text)
  }

  @Test fun testSectionSubsectionTypeID()
  {
    val pp = newParserForString("""
[section [title t] [type t] [id x]
  [subsection [title ss0] [paragraph p]]]""")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockSectionWithSubsections<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.of(KSID(Optional.empty(), "x", Unit)), e.result.id)
    Assert.assertEquals(Optional.of("t"), e.result.type)
    Assert.assertEquals(1, e.result.content.size)
    val ss = e.result.content[0]
    val sp = ss.content[0] as KSSubsectionContent.KSSubsectionParagraph<Unit>
    val pc = sp.paragraph.content[0] as KSInlineText<Unit>
    Assert.assertEquals("p", pc.text)
  }

  @Test fun testSectionSubsectionIDType()
  {
    val pp = newParserForString("""
[section [title t] [id x] [type t]
  [subsection [title ss0] [paragraph p]]]""")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockSectionWithSubsections<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.of(KSID(Optional.empty(), "x", Unit)), e.result.id)
    Assert.assertEquals(Optional.of("t"), e.result.type)
    Assert.assertEquals(1, e.result.content.size)
    val ss = e.result.content[0]
    val sp = ss.content[0] as KSSubsectionContent.KSSubsectionParagraph<Unit>
    val pc = sp.paragraph.content[0] as KSInlineText<Unit>
    Assert.assertEquals("p", pc.text)
  }

  @Test fun testSectionSubsectionID()
  {
    val pp = newParserForString("""
[section [title t] [id x]
  [subsection [title ss0] [paragraph p]]]""")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockSectionWithSubsections<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.of(KSID(Optional.empty(), "x", Unit)), e.result.id)
    Assert.assertEquals(Optional.empty<String>(), e.result.type)
    Assert.assertEquals(1, e.result.content.size)
    val ss = e.result.content[0]
    val sp = ss.content[0] as KSSubsectionContent.KSSubsectionParagraph<Unit>
    val pc = sp.paragraph.content[0] as KSInlineText<Unit>
    Assert.assertEquals("p", pc.text)
  }

  @Test fun testSectionSubsectionType()
  {
    val pp = newParserForString("""
[section [title t] [type t]
  [subsection [title ss0] [paragraph p]]]""")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockSectionWithSubsections<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.empty<KSID<Unit>>(), e.result.id)
    Assert.assertEquals(Optional.of("t"), e.result.type)
    Assert.assertEquals(1, e.result.content.size)
    val ss = e.result.content[0]
    val sp = ss.content[0] as KSSubsectionContent.KSSubsectionParagraph<Unit>
    val pc = sp.paragraph.content[0] as KSInlineText<Unit>
    Assert.assertEquals("p", pc.text)
  }

  @Test fun testSectionSubsection()
  {
    val pp = newParserForString("""
[section [title t]
  [subsection [title ss0] [paragraph p]]]""")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockSectionWithSubsections<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.empty<KSID<Unit>>(), e.result.id)
    Assert.assertEquals(Optional.empty<String>(), e.result.type)
    Assert.assertEquals(1, e.result.content.size)
    val ss = e.result.content[0]
    val sp = ss.content[0] as KSSubsectionContent.KSSubsectionParagraph<Unit>
    val pc = sp.paragraph.content[0] as KSInlineText<Unit>
    Assert.assertEquals("p", pc.text)
  }

  @Test fun testSectionMixedParagraphSubsection()
  {
    val pp = newParserForString("""
[section [title t]
  (subsection [title ss0] [paragraph p])
  (paragraph q)]""")
    val e = pp.p.parse(pp.s.invoke())

    e as KSFailure
    Assert.assertEquals(1, e.errors.size)
  }

  @Test fun testSectionMixedSubsectionParagraph()
  {
    val pp = newParserForString("""
[section [title t]
  (paragraph q)
  (subsection [title ss0] [paragraph p])]""")
    val e = pp.p.parse(pp.s.invoke())

    e as KSFailure
    Assert.assertEquals(1, e.errors.size)
  }

  @Test fun testSectionNonsense()
  {
    val pp = newParserForString("""
[section [title t]
  (section [title s] [paragraph p])]""")
    val e = pp.p.parse(pp.s.invoke())

    e as KSFailure
    Assert.assertEquals(2, e.errors.size)
  }

  @Test fun testSectionEmpty()
  {
    val pp = newParserForString("[section [title t]]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSFailure
    Assert.assertEquals(1, e.errors.size)
  }

  @Test fun testPartErrorEmpty()
  {
    val pp = newParserForString("[part]")

    val e = pp.p.parse(pp.s.invoke())
    e as KSFailure<KSBlockPart<*>, KSParseError>

    Assert.assertFalse(e.partial.isPresent)
    Assert.assertEquals(1, e.errors.size)
  }

  @Test fun testPartErrorWrongContent()
  {
    val pp = newParserForString("[part [title t] [part [title w]]]")

    val e = pp.p.parse(pp.s.invoke())
    e as KSFailure<KSBlockPart<*>, KSParseError>

    Assert.assertTrue(e.partial.isPresent)
    Assert.assertEquals(1, e.errors.size)
  }

  @Test fun testPartErrorWrongTitle()
  {
    val pp = newParserForString("[part [title x [term q]]]")

    val e = pp.p.parse(pp.s.invoke())
    e as KSFailure<KSBlockPart<*>, KSParseError>

    Assert.assertTrue(e.partial.isPresent)
    Assert.assertEquals(1, e.errors.size)
  }

  @Test fun testPart()
  {
    val pp = newParserForString("[part [title t] (section [title k] [paragraph p])]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockPart<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.empty<KSID<Unit>>(), e.result.id)
    Assert.assertEquals(1, e.result.content.size)

    val s = e.result.content[0] as KSBlockSectionWithContent
    val t = s.title[0]
    Assert.assertEquals("k", t.text)
  }

  @Test fun testPartID()
  {
    val pp = newParserForString("[part [title t] [id x] (section [title k] [paragraph p])]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockPart<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals("x", e.result.id.get().value)
    Assert.assertEquals(1, e.result.content.size)

    val s = e.result.content[0] as KSBlockSectionWithContent
    val t = s.title[0]
    Assert.assertEquals("k", t.text)
  }

  @Test fun testPartIDType()
  {
    val pp = newParserForString("[part [title t] [id x] [type k] (section [title k] [paragraph p])]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockPart<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals("x", e.result.id.get().value)
    Assert.assertEquals("k", e.result.type.get())
    Assert.assertEquals(1, e.result.content.size)

    val s = e.result.content[0] as KSBlockSectionWithContent
    val t = s.title[0]
    Assert.assertEquals("k", t.text)
  }

  @Test fun testPartTypeID()
  {
    val pp = newParserForString("[part [title t] [type k] [id x] (section [title k] [paragraph p])]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockPart<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals("x", e.result.id.get().value)
    Assert.assertEquals("k", e.result.type.get())
    Assert.assertEquals(1, e.result.content.size)

    val s = e.result.content[0] as KSBlockSectionWithContent
    val t = s.title[0]
    Assert.assertEquals("k", t.text)
  }

  @Test fun testPartType()
  {
    val pp = newParserForString("[part [title t] [type k] (section [title k] [paragraph p])]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockPart<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.empty<KSID<Unit>>(), e.result.id)
    Assert.assertEquals("k", e.result.type.get())
    Assert.assertEquals(1, e.result.content.size)

    val s = e.result.content[0] as KSBlockSectionWithContent
    val t = s.title[0]
    Assert.assertEquals("k", t.text)
  }

  @Test fun testPartContent()
  {
    val pp = newParserForString(
      "[part (title t) (section [title k] [paragraph p])]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockPart<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.empty<KSID<Unit>>(), e.result.id)
    Assert.assertEquals(1, e.result.content.size)
    val s = e.result.content[0] as KSBlockSectionWithContent
    val t = s.title[0]
    Assert.assertEquals("k", t.text)
  }

  @Test fun testDocumentErrorEmpty()
  {
    val pp = newParserForString("[document]")

    val e = pp.p.parse(pp.s.invoke())
    e as KSFailure<KSBlockDocument<*>, KSParseError>

    Assert.assertFalse(e.partial.isPresent)
    Assert.assertEquals(1, e.errors.size)
  }

  @Test fun testDocumentErrorWrongContent()
  {
    val pp = newParserForString("[document [title t] [paragraph q]]")

    val e = pp.p.parse(pp.s.invoke())
    e as KSFailure<KSBlockDocument<*>, KSParseError>

    Assert.assertEquals(2, e.errors.size)
  }

  @Test fun testDocumentErrorWrongTitle()
  {
    val pp = newParserForString("[document [title x [term q]]]")

    val e = pp.p.parse(pp.s.invoke())
    e as KSFailure<KSBlockDocument<*>, KSParseError>

    Assert.assertEquals(2, e.errors.size)
  }

  @Test fun testDocumentSection()
  {
    val pp = newParserForString("""
[document [title t] (section [title k] [paragraph p])]
    """)
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockDocumentWithSections<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.empty<KSID<Unit>>(), e.result.id)
    Assert.assertEquals(1, e.result.content.size)

    val s = e.result.content[0] as KSBlockSectionWithContent
    val t = s.title[0]
    Assert.assertEquals("k", t.text)
  }

  @Test fun testDocumentSectionID()
  {
    val pp = newParserForString(
      "[document [title t] [id x] (section [title k] [paragraph p])]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockDocumentWithSections<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals("x", e.result.id.get().value)
    Assert.assertEquals(1, e.result.content.size)

    val s = e.result.content[0] as KSBlockSectionWithContent
    val t = s.title[0]
    Assert.assertEquals("k", t.text)
  }

  @Test fun testDocumentSectionIDType()
  {
    val pp = newParserForString("" +
      "[document [title t] [id x] [type k] (section [title k] [paragraph p])]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockDocumentWithSections<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals("x", e.result.id.get().value)
    Assert.assertEquals("k", e.result.type.get())
    Assert.assertEquals(1, e.result.content.size)

    val s = e.result.content[0] as KSBlockSectionWithContent
    val t = s.title[0]
    Assert.assertEquals("k", t.text)
  }

  @Test fun testDocumentSectionTypeID()
  {
    val pp = newParserForString(
      "[document [title t] [type k] [id x] (section [title k] [paragraph p])]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockDocumentWithSections<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals("x", e.result.id.get().value)
    Assert.assertEquals("k", e.result.type.get())
    Assert.assertEquals(1, e.result.content.size)

    val s = e.result.content[0] as KSBlockSectionWithContent
    val t = s.title[0]
    Assert.assertEquals("k", t.text)
  }

  @Test fun testDocumentSectionType()
  {
    val pp = newParserForString(
      "[document [title t] [type k] (section [title k] [paragraph p])]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockDocumentWithSections<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.empty<KSID<Unit>>(), e.result.id)
    Assert.assertEquals("k", e.result.type.get())
    Assert.assertEquals(1, e.result.content.size)

    val s = e.result.content[0] as KSBlockSectionWithContent
    val t = s.title[0]
    Assert.assertEquals("k", t.text)
  }

  @Test fun testDocumentSectionContent()
  {
    val pp = newParserForString(
      "[document (title t) (section [title k] [paragraph p])]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockDocumentWithSections<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.empty<KSID<Unit>>(), e.result.id)
    Assert.assertEquals(1, e.result.content.size)
    val s = e.result.content[0] as KSBlockSectionWithContent
    val t = s.title[0]
    Assert.assertEquals("k", t.text)
  }

  @Test fun testDocumentEmpty()
  {
    val pp = newParserForString(
      "[document (title t)]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSFailure<KSBlockDocumentWithSections<Unit>, KSParseError>
    Assert.assertEquals(1, e.errors.size)
  }

  @Test fun testDocumentPart()
  {
    val pp = newParserForString("""
[document [title t] (part [title q] [section (title k) (paragraph p)])]
    """)
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockDocumentWithParts<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.empty<KSID<Unit>>(), e.result.id)
    Assert.assertEquals(1, e.result.content.size)

    val s = e.result.content[0]
    val t = s.title[0]
    Assert.assertEquals("q", t.text)
  }

  @Test fun testDocumentPartID()
  {
    val pp = newParserForString(
      "[document [title t] [id x] (part [title q] [section (title k) (paragraph p)])]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockDocumentWithParts<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals("x", e.result.id.get().value)
    Assert.assertEquals(1, e.result.content.size)

    val s = e.result.content[0]
    val t = s.title[0]
    Assert.assertEquals("q", t.text)
  }

  @Test fun testDocumentPartIDType()
  {
    val pp = newParserForString("" +
      "[document [title t] [id x] [type k] (part [title q] [section (title k) (paragraph p)])]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockDocumentWithParts<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals("x", e.result.id.get().value)
    Assert.assertEquals("k", e.result.type.get())
    Assert.assertEquals(1, e.result.content.size)

    val s = e.result.content[0]
    val t = s.title[0]
    Assert.assertEquals("q", t.text)
  }

  @Test fun testDocumentPartTypeID()
  {
    val pp = newParserForString(
      "[document [title t] [type k] [id x] (part [title q] [section (title k) (paragraph p)])]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockDocumentWithParts<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals("x", e.result.id.get().value)
    Assert.assertEquals("k", e.result.type.get())
    Assert.assertEquals(1, e.result.content.size)

    val s = e.result.content[0]
    val t = s.title[0]
    Assert.assertEquals("q", t.text)
  }

  @Test fun testDocumentPartType()
  {
    val pp = newParserForString(
      "[document [title t] [type k] (part [title q] [section (title k) (paragraph p)])]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockDocumentWithParts<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.empty<KSID<Unit>>(), e.result.id)
    Assert.assertEquals("k", e.result.type.get())
    Assert.assertEquals(1, e.result.content.size)

    val s = e.result.content[0]
    val t = s.title[0]
    Assert.assertEquals("q", t.text)
  }

  @Test fun testDocumentPartContent()
  {
    val pp = newParserForString(
      "[document (title t) (part [title q] [section (title k) (paragraph p)])]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSSuccess<KSBlockDocumentWithParts<Unit>, KSParseError>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.empty<KSID<Unit>>(), e.result.id)
    Assert.assertEquals(1, e.result.content.size)
    val s = e.result.content[0]
    val t = s.title[0]
    Assert.assertEquals("q", t.text)
  }

  @Test fun testDocumentPartSection()
  {
    val pp = newParserForString("""
[document
  (title t)
  (part [title q] [section (title k) (paragraph p)])
  (section [title s] [paragraph z])]
""")
    val e = pp.p.parse(pp.s.invoke())
    e as KSFailure<KSBlockDocument<*>, KSParseError>

    Assert.assertTrue(e.partial.isPresent)
    Assert.assertEquals(1, e.errors.size)
  }

  @Test fun testDocumentSectionPart()
  {
    val pp = newParserForString("""
[document
  (title t)
  (section [title s] [paragraph z])
  (part [title q] [section (title k) (paragraph p)])]
""")
    val e = pp.p.parse(pp.s.invoke())
    e as KSFailure<KSBlockDocument<*>, KSParseError>

    Assert.assertTrue(e.partial.isPresent)
    Assert.assertEquals(1, e.errors.size)
  }
}
