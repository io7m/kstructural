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

package com.io7m.kstructural.tests.parser.imperative

import com.io7m.kstructural.core.KSElement
import com.io7m.kstructural.core.KSElement.KSBlock
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection.*
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument.*
import com.io7m.kstructural.core.KSElement.KSBlock.*
import com.io7m.kstructural.core.KSElement.KSInline.*
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSParseContext
import com.io7m.kstructural.core.KSParseError
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.core.KSResult.*
import com.io7m.kstructural.core.KSSubsectionContent
import com.io7m.kstructural.core.KSSubsectionContent.*
import com.io7m.kstructural.parser.imperative.KSImperative
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.*
import com.io7m.kstructural.parser.imperative.KSImperative.*
import com.io7m.kstructural.parser.imperative.KSImperativeBuilderType
import org.junit.Assert
import org.junit.Test
import java.util.Optional

abstract class KSImperativeBuilderContract {

  protected abstract fun newBuilder() : KSImperativeBuilderType

  private fun successEmpty(
    r0 : KSResult<Optional<KSBlock<KSParse>>, KSParseError>) {
    r0 as KSSuccess
    Assert.assertFalse(r0.result.isPresent)
  }

  @Test fun testEmptyEOF()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()
    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSFailure
  }

  @Test fun testEmptyInline()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()
    val i = KSInlineText(
      Optional.empty(), false, KSParse(c, Optional.empty()), false, "xyz")
    val r = b.add(c, KSImperativeInline(i))
    r as KSFailure
  }

  @Test fun testParagraph()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val pc = KSImperativeParagraph(
      Optional.empty(), true, Optional.empty(), Optional.empty())
    val i0 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")
    val i1 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")

    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, KSImperativeInline(i0)))
    successEmpty(b.add(c, KSImperativeInline(i1)))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockParagraph<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    Assert.assertEquals(2, r.result.get().content.size)
  }

  @Test fun testFormalItem()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val pc = KSImperativeFormalItem(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val i0 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")
    val i1 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")

    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, KSImperativeInline(i0)))
    successEmpty(b.add(c, KSImperativeInline(i1)))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockFormalItem<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    Assert.assertEquals(2, r.result.get().content.size)
  }

  @Test fun testFootnote()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val pc = KSImperativeFootnote(
      Optional.empty(), true, Optional.empty(), Optional.empty())
    val i0 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")
    val i1 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")

    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, KSImperativeInline(i0)))
    successEmpty(b.add(c, KSImperativeInline(i1)))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockFootnote<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    Assert.assertEquals(2, r.result.get().content.size)
  }

  @Test fun testPart()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val pc = KSImperativePart(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val sc = KSImperativeSection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)

    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, sc))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockPart<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    Assert.assertEquals(1, r.result.get().content.size)
  }

  @Test fun testPartSectionSubsection()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val pc = KSImperativePart(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val sc = KSImperativeSection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val ssc = KSImperativeSubsection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)

    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, ssc))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockPart<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    Assert.assertEquals(1, r.result.get().content.size)
    val rs = r.result.get().content[0] as KSBlockSectionWithSubsections
    Assert.assertEquals(1, rs.content.size)
  }

  @Test fun testPartSectionParagraph()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val pc = KSImperativePart(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val sc = KSImperativeSection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val prc = KSImperativeParagraph(
      Optional.empty(), true, Optional.empty(), Optional.empty())
    val i0 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")

    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, prc))
    successEmpty(b.add(c, KSImperativeInline(i0)))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockPart<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    Assert.assertEquals(1, r.result.get().content.size)
    val rs = r.result.get().content[0] as KSBlockSectionWithContent
    Assert.assertEquals(1, rs.content.size)
    val rp = (rs.content[0] as KSSubsectionParagraph).paragraph
    Assert.assertEquals(i0, rp.content[0] as KSInlineText)
  }

  @Test fun testPartErrorPart()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val pc = KSImperativePart(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)

    successEmpty(b.add(c, pc))

    val r = b.add(c, pc)
    r as KSFailure
  }

  @Test fun testPartErrorInline()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val pc = KSImperativePart(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val i0 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")

    successEmpty(b.add(c, pc))

    val r = b.add(c, KSImperativeInline(i0))
    r as KSFailure
  }

  @Test fun testPartErrorSubsection()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val pc = KSImperativePart(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val ssc = KSImperativeSubsection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)

    successEmpty(b.add(c, pc))

    val r = b.add(c, ssc)
    r as KSFailure
  }

  @Test fun testPartErrorParagraph()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val pc = KSImperativePart(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val ssc = KSImperativeParagraph(
      Optional.empty(), true, Optional.empty(), Optional.empty())

    successEmpty(b.add(c, pc))

    val r = b.add(c, ssc)
    r as KSFailure
  }

  @Test fun testPartErrorFootnote()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val pc = KSImperativePart(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val ssc = KSImperativeFootnote(
      Optional.empty(), true, Optional.empty(), Optional.empty())

    successEmpty(b.add(c, pc))

    val r = b.add(c, ssc)
    r as KSFailure
  }

  @Test fun testPartErrorFormalItem()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val pc = KSImperativePart(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val ssc = KSImperativeFormalItem(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)

    successEmpty(b.add(c, pc))

    val r = b.add(c, ssc)
    r as KSFailure
  }

  @Test fun testPartErrorDocument()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val pc = KSImperativePart(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val ssc = KSImperativeDocument(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)

    successEmpty(b.add(c, pc))

    val r = b.add(c, ssc)
    r as KSFailure
  }

  @Test fun testSectionErrorPart()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val pc = KSImperativePart(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val sc = KSImperativeSection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)

    successEmpty(b.add(c, sc))
    val r = b.add(c, pc)
    r as KSFailure
  }

  @Test fun testSectionErrorDocument()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val pc = KSImperativeDocument(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val sc = KSImperativeSection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)

    successEmpty(b.add(c, sc))
    val r = b.add(c, pc)
    r as KSFailure
  }

  @Test fun testSectionErrorSection()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val sc = KSImperativeSection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)

    successEmpty(b.add(c, sc))
    val r = b.add(c, sc)
    r as KSFailure
  }

  @Test fun testSectionParagraphs()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val pc = KSImperativeParagraph(
      Optional.empty(), true, Optional.empty(), Optional.empty())
    val sc = KSImperativeSection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val i0 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")
    val i1 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")

    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, KSImperativeInline(i0)))
    successEmpty(b.add(c, KSImperativeInline(i1)))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockSectionWithContent<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    val rp = r.result.get().content[0] as KSSubsectionParagraph
    Assert.assertEquals(2, rp.paragraph.content.size)
  }

  @Test fun testSectionFootnotes()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val pc = KSImperativeFootnote(
      Optional.empty(), true, Optional.empty(), Optional.empty())
    val sc = KSImperativeSection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val i0 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")
    val i1 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")

    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, KSImperativeInline(i0)))
    successEmpty(b.add(c, KSImperativeInline(i1)))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockSectionWithContent<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    val rp = r.result.get().content[0] as KSSubsectionFootnote
    Assert.assertEquals(2, rp.footnote.content.size)
  }

  @Test fun testSectionErrorParagraphsSubsection()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val pc = KSImperativeParagraph(
      Optional.empty(), true, Optional.empty(), Optional.empty())
    val ssc = KSImperativeSubsection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val sc = KSImperativeSection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)

    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, pc))

    val r = b.add(c, ssc)
    r as KSFailure
  }

  @Test fun testSectionErrorFootnotesSubsection()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val pc = KSImperativeFootnote(
      Optional.empty(), true, Optional.empty(), Optional.empty())
    val ssc = KSImperativeSubsection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val sc = KSImperativeSection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)

    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, pc))

    val r = b.add(c, ssc)
    r as KSFailure
  }

  @Test fun testSectionFormalItems()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val pc = KSImperativeFormalItem(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val sc = KSImperativeSection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val i0 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")
    val i1 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")

    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, KSImperativeInline(i0)))
    successEmpty(b.add(c, KSImperativeInline(i1)))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockSectionWithContent<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    val rp = r.result.get().content[0] as KSSubsectionFormalItem
    Assert.assertEquals(2, rp.formal.content.size)
  }

  @Test fun testSectionErrorFormalItemsSubsection()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val pc = KSImperativeFormalItem(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val ssc = KSImperativeSubsection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val sc = KSImperativeSection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)

    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, pc))

    val r = b.add(c, ssc)
    r as KSFailure
  }

  @Test fun testSectionSubsections()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val sc = KSImperativeSection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val ssc = KSImperativeSubsection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val pc = KSImperativeParagraph(
      Optional.empty(), true, Optional.empty(), Optional.empty())
    val i0 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")
    val i1 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")

    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, ssc))
    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, KSImperativeInline(i0)))
    successEmpty(b.add(c, KSImperativeInline(i1)))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockSectionWithSubsections<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    val rp = r.result.get().content[0]
    Assert.assertEquals(1, rp.content.size)
    Assert.assertEquals(2, (rp.content[0] as KSSubsectionParagraph<KSParse>).paragraph.content.size)
  }

  @Test fun testSectionErrorInline()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val sc = KSImperativeSection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val i0 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")

    successEmpty(b.add(c, sc))
    val r = b.add(c, KSImperativeInline(i0))
    r as KSFailure
  }

  @Test fun testSubsectionParagraph()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val ssc = KSImperativeSubsection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val pc = KSImperativeParagraph(
      Optional.empty(), true, Optional.empty(), Optional.empty())
    val i0 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")
    val i1 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")

    successEmpty(b.add(c, ssc))
    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, KSImperativeInline(i0)))
    successEmpty(b.add(c, KSImperativeInline(i1)))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockSubsection<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    val rp = r.result.get().content[0] as KSSubsectionParagraph<KSParse>
    Assert.assertEquals(2, rp.paragraph.content.size)
  }

  @Test fun testSubsectionFootnote()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val ssc = KSImperativeSubsection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val pc = KSImperativeFootnote(
      Optional.empty(), true, Optional.empty(), Optional.empty())
    val i0 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")
    val i1 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")

    successEmpty(b.add(c, ssc))
    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, KSImperativeInline(i0)))
    successEmpty(b.add(c, KSImperativeInline(i1)))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockSubsection<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    val rp = r.result.get().content[0] as KSSubsectionFootnote<KSParse>
    Assert.assertEquals(2, rp.footnote.content.size)
  }

  @Test fun testSubsectionFormalItem()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val ssc = KSImperativeSubsection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val pc = KSImperativeFormalItem(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val i0 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")
    val i1 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")

    successEmpty(b.add(c, ssc))
    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, KSImperativeInline(i0)))
    successEmpty(b.add(c, KSImperativeInline(i1)))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockSubsection<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    val rp = r.result.get().content[0] as KSSubsectionFormalItem<KSParse>
    Assert.assertEquals(2, rp.formal.content.size)
  }

  @Test fun testSubsectionErrorInline()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val ssc = KSImperativeSubsection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val pc = KSImperativeParagraph(
      Optional.empty(), true, Optional.empty(), Optional.empty())
    val i0 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")
    val i1 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")

    successEmpty(b.add(c, ssc))
    val r = b.add(c, KSImperativeInline(i0))
    r as KSFailure
  }

  @Test fun testSubsectionErrorSubsection()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val ssc = KSImperativeSubsection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)

    successEmpty(b.add(c, ssc))
    val r = b.add(c, ssc)
    r as KSFailure
  }

  @Test fun testSubsectionErrorSection()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val ssc = KSImperativeSubsection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val sc = KSImperativeSection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)

    successEmpty(b.add(c, ssc))
    val r = b.add(c, sc)
    r as KSFailure
  }

  @Test fun testSubsectionErrorPart()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val ssc = KSImperativeSubsection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val sc = KSImperativePart(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)

    successEmpty(b.add(c, ssc))
    val r = b.add(c, sc)
    r as KSFailure
  }

  @Test fun testSubsectionErrorDocument()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val ssc = KSImperativeSubsection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val sc = KSImperativeDocument(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)

    successEmpty(b.add(c, ssc))
    val r = b.add(c, sc)
    r as KSFailure
  }

  @Test fun testSectionSubsectionParagraph()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val sc = KSImperativeSection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val ssc = KSImperativeSubsection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val pc = KSImperativeParagraph(
      Optional.empty(), true, Optional.empty(), Optional.empty())
    val i0 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")
    val i1 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")

    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, ssc))
    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, KSImperativeInline(i0)))
    successEmpty(b.add(c, KSImperativeInline(i1)))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockSectionWithSubsections<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    val rss = r.result.get().content[0]
    val rp = rss.content[0] as KSSubsectionParagraph
    Assert.assertEquals(2, rp.paragraph.content.size)
  }

  @Test fun testSectionSubsectionFootnote()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val sc = KSImperativeSection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val ssc = KSImperativeSubsection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val pc = KSImperativeFootnote(
      Optional.empty(), true, Optional.empty(), Optional.empty())
    val i0 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")
    val i1 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")

    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, ssc))
    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, KSImperativeInline(i0)))
    successEmpty(b.add(c, KSImperativeInline(i1)))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockSectionWithSubsections<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    val rss = r.result.get().content[0]
    val rp = rss.content[0] as KSSubsectionFootnote
    Assert.assertEquals(2, rp.footnote.content.size)
  }

  @Test fun testSectionSubsectionFormalItem()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val sc = KSImperativeSection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val ssc = KSImperativeSubsection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val pc = KSImperativeFormalItem(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val i0 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")
    val i1 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")

    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, ssc))
    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, KSImperativeInline(i0)))
    successEmpty(b.add(c, KSImperativeInline(i1)))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockSectionWithSubsections<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    val rss = r.result.get().content[0]
    val rp = rss.content[0] as KSSubsectionFormalItem
    Assert.assertEquals(2, rp.formal.content.size)
  }

  @Test fun testDocument()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val pc = KSImperativeDocument(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)

    successEmpty(b.add(c, pc))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockDocument<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
  }

  @Test fun testDocumentSection()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val pc = KSImperativeDocument(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val sc = KSImperativeSection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)

    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, sc))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockDocumentWithSections<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    Assert.assertEquals(1, r.result.get().content.size)
  }

  @Test fun testDocumentPart()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val pc = KSImperativeDocument(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val sc = KSImperativePart(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)

    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, sc))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockDocumentWithParts<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    Assert.assertEquals(1, r.result.get().content.size)
  }

  @Test fun testDocumentSectionPart()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val dc = KSImperativeDocument(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val pc = KSImperativePart(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val sc = KSImperativeSection(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)

    successEmpty(b.add(c, dc))
    successEmpty(b.add(c, sc))

    val r = b.add(c, pc)
    r as KSFailure
  }
  
  @Test fun testDocumentErrorInline()
  {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c, Optional.empty())
    val title =
      mutableListOf(KSInlineText(Optional.empty(), false, kp, false, "abc"))
    val pc = KSImperativeDocument(
      Optional.empty(), true, Optional.empty(), Optional.empty(), title)
    val i0 = KSInlineText(
      Optional.empty(), false, kp, false, "xyz")

    successEmpty(b.add(c, pc))

    val r = b.add(c, KSImperativeInline(i0))
    r as KSFailure
  }
}