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

import com.io7m.jlexing.core.LexicalPositionType
import com.io7m.kstructural.core.KSElement.KSBlock
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithParts
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithSections
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockFootnote
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockFormalItem
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockImport
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockParagraph
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockPart
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection.KSBlockSectionWithContent
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection.KSBlockSectionWithSubsections
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSubsection
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineText
import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSParseContext
import com.io7m.kstructural.core.KSParseError
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.core.KSResult.KSFailure
import com.io7m.kstructural.core.KSResult.KSSuccess
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionFootnote
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionFormalItem
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionParagraph
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.KSImperativeDocument
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.KSImperativeFootnote
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.KSImperativeFormalItem
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.KSImperativeImport
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.KSImperativeParagraph
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.KSImperativePart
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.KSImperativeSection
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.KSImperativeSubsection
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeEOF
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeInline
import com.io7m.kstructural.parser.imperative.KSImperativeBuilderType
import org.junit.Assert
import org.junit.Test
import java.nio.file.Path
import java.util.Optional

abstract class KSImperativeBuilderContract {

  protected abstract fun newBuilder() : KSImperativeBuilderType

  private fun successEmpty(
    r0 : KSResult<Optional<KSBlock<KSParse>>, KSParseError>) {
    r0 as KSSuccess
    Assert.assertFalse(r0.result.isPresent)
  }

  private val pos : Optional<LexicalPositionType<Path>> = Optional.empty()

  private val type : Optional<String> = Optional.empty()

  private val id : Optional<KSID<KSParse>> = Optional.empty()

  private val id_real : KSID<KSParse> = KSID(
    pos, "x", KSParse(KSParseContext.empty()))

  @Test fun testEmptyEOF() {
    val c = KSParseContext.empty()
    val b = newBuilder()
    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSFailure
  }

  @Test fun testEmptyInline() {
    val c = KSParseContext.empty()
    val b = newBuilder()
    val i = KSInlineText(pos, false, KSParse(c), false, "xyz")
    val r = b.add(c, KSImperativeInline(i))
    r as KSFailure
  }

  @Test fun testParagraph() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val pc = KSImperativeParagraph(pos, true, type, id)
    val i0 = KSInlineText(pos, false, kp, false, "xyz")
    val i1 = KSInlineText(pos, false, kp, false, "xyz")

    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, KSImperativeInline(i0)))
    successEmpty(b.add(c, KSImperativeInline(i1)))

    val r = b.add(c, KSImperativeEOF(pos))
    r as KSSuccess<Optional<KSBlockParagraph<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    Assert.assertEquals(2, r.result.get().content.size)
  }

  @Test fun testFormalItem() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativeFormalItem(pos, true, type, id, title)
    val i0 = KSInlineText(pos, false, kp, false, "xyz")
    val i1 = KSInlineText(pos, false, kp, false, "xyz")

    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, KSImperativeInline(i0)))
    successEmpty(b.add(c, KSImperativeInline(i1)))

    val r = b.add(c, KSImperativeEOF(pos))
    r as KSSuccess<Optional<KSBlockFormalItem<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    Assert.assertEquals(2, r.result.get().content.size)
  }

  @Test fun testFootnote() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val pc = KSImperativeFootnote(pos, true, type, id_real)
    val i0 = KSInlineText(pos, false, kp, false, "xyz")
    val i1 = KSInlineText(pos, false, kp, false, "xyz")

    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, KSImperativeInline(i0)))
    successEmpty(b.add(c, KSImperativeInline(i1)))

    val r = b.add(c, KSImperativeEOF(pos))
    r as KSSuccess<Optional<KSBlockFootnote<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    Assert.assertEquals(2, r.result.get().content.size)
  }

  @Test fun testPart() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativePart(pos, true, type, id, title)
    val sc = KSImperativeSection(pos, true, type, id, title)

    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, sc))

    val r = b.add(c, KSImperativeEOF(pos))
    r as KSSuccess<Optional<KSBlockPart<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    Assert.assertEquals(1, r.result.get().content.size)
  }

  @Test fun testPartErrorImportDocument() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val text = KSInlineText(pos, false, kp, false, "abc")
    val texts = mutableListOf(text)
    val i = KSBlockImport(pos, false, kp, type, id, text)
    val p = KSBlockDocumentWithSections(pos, false, kp, id, type, texts, mutableListOf())

    val ii = KSImperativeImport(pos, false, i, p)
    val pc = KSImperativePart(pos, false, type, id, texts)

    successEmpty(b.add(c, pc))

    val r = b.add(c, ii)
    r as KSFailure
  }

  @Test fun testPartErrorImportContent() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val text = KSInlineText(pos, false, kp, false, "abc")
    val texts = mutableListOf(text)
    val i = KSBlockImport(pos, false, kp, type, id, text)
    val p = KSBlockParagraph(pos, false, kp, type, id, mutableListOf())

    val ii = KSImperativeImport(pos, false, i, p)
    val pc = KSImperativePart(pos, false, type, id, texts)

    successEmpty(b.add(c, pc))

    val r = b.add(c, ii)
    r as KSFailure
  }

  @Test fun testPartSectionSubsection() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativePart(pos, true, type, id, title)
    val sc = KSImperativeSection(pos, true, type, id, title)
    val ssc = KSImperativeSubsection(pos, true, type, id, title)

    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, ssc))

    val r = b.add(c, KSImperativeEOF(pos))
    r as KSSuccess<Optional<KSBlockPart<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    Assert.assertEquals(1, r.result.get().content.size)
    val rs = r.result.get().content[0] as KSBlockSectionWithSubsections
    Assert.assertEquals(1, rs.content.size)
  }

  @Test fun testPartSectionParagraph() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativePart(pos, true, type, id, title)
    val sc = KSImperativeSection(pos, true, type, id, title)
    val prc = KSImperativeParagraph(pos, true, type, id)
    val i0 = KSInlineText(pos, false, kp, false, "xyz")

    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, prc))
    successEmpty(b.add(c, KSImperativeInline(i0)))

    val r = b.add(c, KSImperativeEOF(pos))
    r as KSSuccess<Optional<KSBlockPart<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    Assert.assertEquals(1, r.result.get().content.size)
    val rs = r.result.get().content[0] as KSBlockSectionWithContent
    Assert.assertEquals(1, rs.content.size)
    val rp = (rs.content[0] as KSSubsectionParagraph).paragraph
    Assert.assertEquals(i0, rp.content[0] as KSInlineText)
  }

  @Test fun testPartErrorPart() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativePart(pos, true, type, id, title)

    successEmpty(b.add(c, pc))

    val r = b.add(c, pc)
    r as KSFailure
  }

  @Test fun testPartErrorInline() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativePart(pos, true, type, id, title)
    val i0 = KSInlineText(pos, false, kp, false, "xyz")

    successEmpty(b.add(c, pc))

    val r = b.add(c, KSImperativeInline(i0))
    r as KSFailure
  }

  @Test fun testPartErrorSubsection() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativePart(pos, true, type, id, title)
    val ssc = KSImperativeSubsection(pos, true, type, id, title)

    successEmpty(b.add(c, pc))

    val r = b.add(c, ssc)
    r as KSFailure
  }

  @Test fun testPartErrorParagraph() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativePart(pos, true, type, id, title)
    val ssc = KSImperativeParagraph(pos, true, type, id)

    successEmpty(b.add(c, pc))

    val r = b.add(c, ssc)
    r as KSFailure
  }

  @Test fun testPartErrorFootnote() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativePart(pos, true, type, id, title)
    val ssc = KSImperativeFootnote(pos, true, type, id_real)

    successEmpty(b.add(c, pc))

    val r = b.add(c, ssc)
    r as KSFailure
  }

  @Test fun testPartErrorFormalItem() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativePart(pos, true, type, id, title)
    val ssc = KSImperativeFormalItem(pos, true, type, id, title)

    successEmpty(b.add(c, pc))

    val r = b.add(c, ssc)
    r as KSFailure
  }

  @Test fun testPartErrorDocument() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativePart(pos, true, type, id, title)
    val ssc = KSImperativeDocument(pos, true, type, id, title)

    successEmpty(b.add(c, pc))

    val r = b.add(c, ssc)
    r as KSFailure
  }

  @Test fun testSectionErrorPart() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativePart(pos, true, type, id, title)
    val sc = KSImperativeSection(pos, true, type, id, title)

    successEmpty(b.add(c, sc))
    val r = b.add(c, pc)
    r as KSFailure
  }

  @Test fun testSectionErrorDocument() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativeDocument(pos, true, type, id, title)
    val sc = KSImperativeSection(pos, true, type, id, title)

    successEmpty(b.add(c, sc))
    val r = b.add(c, pc)
    r as KSFailure
  }

  @Test fun testSectionErrorSection() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val sc = KSImperativeSection(pos, true, type, id, title)

    successEmpty(b.add(c, sc))
    val r = b.add(c, sc)
    r as KSFailure
  }

  @Test fun testSectionErrorSubsectionImportContent() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val text = KSInlineText(pos, false, kp, false, "abc")
    val title = mutableListOf(text)
    val sc = KSImperativeSection(pos, true, type, id, title)
    val ssc = KSBlockSubsection(pos, true, kp, type, id, title, mutableListOf())
    val pc = KSBlockParagraph(pos, true, kp, type, id, mutableListOf())

    val i = KSBlockImport(pos, true, kp, type, id, text)
    val ii0 = KSImperativeImport(pos, true, i, ssc)
    val ii1 = KSImperativeImport(pos, true, i, pc)

    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, ii0))

    val r = b.add(c, ii1)
    r as KSFailure
  }

  @Test fun testSectionErrorContentImportSubsection() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val text = KSInlineText(pos, false, kp, false, "abc")
    val title = mutableListOf(text)
    val sc = KSImperativeSection(pos, true, type, id, title)
    val ssc = KSBlockSubsection(pos, true, kp, type, id, title, mutableListOf())
    val pc = KSBlockParagraph(pos, true, kp, type, id, mutableListOf())

    val i = KSBlockImport(pos, true, kp, type, id, text)
    val ii0 = KSImperativeImport(pos, true, i, ssc)
    val ii1 = KSImperativeImport(pos, true, i, pc)

    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, ii1))

    val r = b.add(c, ii0)
    r as KSFailure
  }

  @Test fun testSectionErrorImportDocument() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val text = KSInlineText(pos, false, kp, false, "abc")
    val title = mutableListOf(text)
    val sc = KSImperativeSection(pos, true, type, id, title)
    val pc = KSBlockDocumentWithSections(pos, true, kp, id, type, title, mutableListOf())

    val i = KSBlockImport(pos, true, kp, type, id, text)
    val ii0 = KSImperativeImport(pos, true, i, pc)

    successEmpty(b.add(c, sc))

    val r = b.add(c, ii0)
    r as KSFailure
  }

  @Test fun testSectionParagraphs() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativeParagraph(pos, true, type, id)
    val sc = KSImperativeSection(pos, true, type, id, title)
    val i0 = KSInlineText(pos, false, kp, false, "xyz")
    val i1 = KSInlineText(pos, false, kp, false, "xyz")

    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, KSImperativeInline(i0)))
    successEmpty(b.add(c, KSImperativeInline(i1)))

    val r = b.add(c, KSImperativeEOF(pos))
    r as KSSuccess<Optional<KSBlockSectionWithContent<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    val rp = r.result.get().content[0] as KSSubsectionParagraph
    Assert.assertEquals(2, rp.paragraph.content.size)
  }

  @Test fun testSectionFootnotes() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativeFootnote(pos, true, type, id_real)
    val sc = KSImperativeSection(pos, true, type, id, title)
    val i0 = KSInlineText(pos, false, kp, false, "xyz")
    val i1 = KSInlineText(pos, false, kp, false, "xyz")

    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, KSImperativeInline(i0)))
    successEmpty(b.add(c, KSImperativeInline(i1)))

    val r = b.add(c, KSImperativeEOF(pos))
    r as KSSuccess<Optional<KSBlockSectionWithContent<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    val rp = r.result.get().content[0] as KSSubsectionFootnote
    Assert.assertEquals(2, rp.footnote.content.size)
  }

  @Test fun testSectionErrorParagraphsSubsection() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativeParagraph(pos, true, type, id)
    val ssc = KSImperativeSubsection(pos, true, type, id, title)
    val sc = KSImperativeSection(pos, true, type, id, title)

    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, pc))

    val r = b.add(c, ssc)
    r as KSFailure
  }

  @Test fun testSectionErrorFootnotesSubsection() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativeFootnote(pos, true, type, id_real)
    val ssc = KSImperativeSubsection(pos, true, type, id, title)
    val sc = KSImperativeSection(pos, true, type, id, title)

    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, pc))

    val r = b.add(c, ssc)
    r as KSFailure
  }

  @Test fun testSectionFormalItems() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativeFormalItem(pos, true, type, id, title)
    val sc = KSImperativeSection(pos, true, type, id, title)
    val i0 = KSInlineText(pos, false, kp, false, "xyz")
    val i1 = KSInlineText(pos, false, kp, false, "xyz")

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

  @Test fun testSectionErrorFormalItemsSubsection() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativeFormalItem(pos, true, type, id, title)
    val ssc = KSImperativeSubsection(pos, true, type, id, title)
    val sc = KSImperativeSection(pos, true, type, id, title)

    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, pc))

    val r = b.add(c, ssc)
    r as KSFailure
  }

  @Test fun testSectionSubsections() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val sc = KSImperativeSection(pos, true, type, id, title)
    val ssc = KSImperativeSubsection(pos, true, type, id, title)
    val pc = KSImperativeParagraph(pos, true, type, id)
    val i0 = KSInlineText(pos, false, kp, false, "xyz")
    val i1 = KSInlineText(pos, false, kp, false, "xyz")

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

  @Test fun testSectionErrorInline() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val sc = KSImperativeSection(pos, true, type, id, title)
    val i0 = KSInlineText(pos, false, kp, false, "xyz")

    successEmpty(b.add(c, sc))
    val r = b.add(c, KSImperativeInline(i0))
    r as KSFailure
  }

  @Test fun testSubsectionParagraph() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val ssc = KSImperativeSubsection(pos, true, type, id, title)
    val pc = KSImperativeParagraph(pos, true, type, id)
    val i0 = KSInlineText(pos, false, kp, false, "xyz")
    val i1 = KSInlineText(pos, false, kp, false, "xyz")

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

  @Test fun testSubsectionImportContent() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val ssc = KSImperativeSubsection(pos, true, type, id, title)
    val text = KSInlineText(pos, false, kp, false, "xyz")
    val c0 = KSBlockParagraph(pos, false, kp, type, id, mutableListOf())
    val c1 = KSBlockFootnote(pos, false, kp, id_real, type, mutableListOf())
    val c2 = KSBlockFormalItem(pos, false, kp, type, id, title, mutableListOf())

    val i = KSBlockImport(pos, true, kp, type, id, text)
    val ii0 = KSImperativeImport(pos, true, i, c0)
    val ii1 = KSImperativeImport(pos, true, i, c1)
    val ii2 = KSImperativeImport(pos, true, i, c2)

    successEmpty(b.add(c, ssc))
    successEmpty(b.add(c, ii0))
    successEmpty(b.add(c, ii1))
    successEmpty(b.add(c, ii2))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockSubsection<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    val rss = r.result.get()
    Assert.assertEquals(3, rss.content.size)
    rss.content[0] as KSSubsectionParagraph<KSParse>
    rss.content[1] as KSSubsectionFootnote<KSParse>
    rss.content[2] as KSSubsectionFormalItem<KSParse>
  }

  @Test fun testSubsectionFootnote() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val ssc = KSImperativeSubsection(pos, true, type, id, title)
    val pc = KSImperativeFootnote(pos, true, type, id_real)
    val i0 = KSInlineText(pos, false, kp, false, "xyz")
    val i1 = KSInlineText(pos, false, kp, false, "xyz")

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

  @Test fun testSubsectionFormalItem() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val ssc = KSImperativeSubsection(pos, true, type, id, title)
    val pc = KSImperativeFormalItem(pos, true, type, id, title)
    val i0 = KSInlineText(pos, false, kp, false, "xyz")
    val i1 = KSInlineText(pos, false, kp, false, "xyz")

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

  @Test fun testSubsectionErrorImportDocument() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val ssc = KSImperativeSubsection(pos, true, type, id, title)
    val text = KSInlineText(pos, false, kp, false, "xyz")
    val pc = KSBlockDocumentWithSections(pos, true, kp, id, type, title, mutableListOf())

    val i = KSBlockImport(pos, true, kp, type, id, text)
    val ii = KSImperativeImport(pos, true, i, pc)

    successEmpty(b.add(c, ssc))

    val r = b.add(c, ii)
    r as KSFailure
  }

  @Test fun testSubsectionErrorInline() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val ssc = KSImperativeSubsection(pos, true, type, id, title)
    val pc = KSImperativeParagraph(pos, true, type, id)
    val i0 = KSInlineText(pos, false, kp, false, "xyz")
    val i1 = KSInlineText(pos, false, kp, false, "xyz")

    successEmpty(b.add(c, ssc))
    val r = b.add(c, KSImperativeInline(i0))
    r as KSFailure
  }

  @Test fun testSubsectionErrorSubsection() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val ssc = KSImperativeSubsection(pos, true, type, id, title)

    successEmpty(b.add(c, ssc))
    val r = b.add(c, ssc)
    r as KSFailure
  }

  @Test fun testSubsectionErrorSection() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val ssc = KSImperativeSubsection(pos, true, type, id, title)
    val sc = KSImperativeSection(pos, true, type, id, title)

    successEmpty(b.add(c, ssc))
    val r = b.add(c, sc)
    r as KSFailure
  }

  @Test fun testSubsectionErrorPart() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val ssc = KSImperativeSubsection(pos, true, type, id, title)
    val sc = KSImperativePart(pos, true, type, id, title)

    successEmpty(b.add(c, ssc))
    val r = b.add(c, sc)
    r as KSFailure
  }

  @Test fun testSubsectionErrorDocument() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val ssc = KSImperativeSubsection(pos, true, type, id, title)
    val sc = KSImperativeDocument(pos, true, type, id, title)

    successEmpty(b.add(c, ssc))
    val r = b.add(c, sc)
    r as KSFailure
  }

  @Test fun testSectionSubsectionParagraph() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val sc = KSImperativeSection(pos, true, type, id, title)
    val ssc = KSImperativeSubsection(pos, true, type, id, title)
    val pc = KSImperativeParagraph(pos, true, type, id)
    val i0 = KSInlineText(pos, false, kp, false, "xyz")
    val i1 = KSInlineText(pos, false, kp, false, "xyz")

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

  @Test fun testSectionSubsectionFootnote() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val sc = KSImperativeSection(pos, true, type, id, title)
    val ssc = KSImperativeSubsection(pos, true, type, id, title)
    val pc = KSImperativeFootnote(pos, true, type, id_real)
    val i0 = KSInlineText(pos, false, kp, false, "xyz")
    val i1 = KSInlineText(pos, false, kp, false, "xyz")

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

  @Test fun testSectionSubsectionFormalItem() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val sc = KSImperativeSection(pos, true, type, id, title)
    val ssc = KSImperativeSubsection(pos, true, type, id, title)
    val pc = KSImperativeFormalItem(pos, true, type, id, title)
    val i0 = KSInlineText(pos, false, kp, false, "xyz")
    val i1 = KSInlineText(pos, false, kp, false, "xyz")

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

  @Test fun testDocument() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativeDocument(pos, true, type, id, title)

    successEmpty(b.add(c, pc))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockDocument<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
  }

  @Test fun testDocumentSection() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativeDocument(pos, true, type, id, title)
    val sc = KSImperativeSection(pos, true, type, id, title)

    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, sc))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockDocumentWithSections<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    Assert.assertEquals(1, r.result.get().content.size)
  }

  @Test fun testDocumentSectionImportContent() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val text = KSInlineText(pos, false, kp, false, "abc")
    val title = mutableListOf(text)
    val pc = KSImperativeDocument(pos, true, type, id, title)
    val sc = KSImperativeSection(pos, true, type, id, title)

    val p = KSBlockParagraph(pos, true, kp, type, id, mutableListOf())
    val i = KSBlockImport(pos, true, kp, type, id, text)
    val ii = KSImperativeImport(pos, true, i, p)

    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, ii))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockDocumentWithSections<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    Assert.assertEquals(1, r.result.get().content.size)
  }

  @Test fun testDocumentSectionContent() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val text = KSInlineText(pos, false, kp, false, "abc")
    val title = mutableListOf(text)
    val dc = KSImperativeDocument(pos, true, type, id, title)
    val sc = KSImperativeSection(pos, true, type, id, title)
    val p = KSImperativeParagraph(pos, true, type, id)

    successEmpty(b.add(c, dc))
    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, p))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockDocumentWithSections<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    val rd = r.result.get()
    Assert.assertEquals(1, rd.content.size)
    val rs = rd.content[0] as KSBlockSectionWithContent
    Assert.assertEquals(1, rs.content.size)
  }

  @Test fun testDocumentPartSectionContent() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val text = KSInlineText(pos, false, kp, false, "abc")
    val title = mutableListOf(text)
    val dc = KSImperativeDocument(pos, true, type, id, title)
    val pc = KSImperativePart(pos, true, type, id, title)
    val sc = KSImperativeSection(pos, true, type, id, title)
    val p = KSImperativeParagraph(pos, true, type, id)

    successEmpty(b.add(c, dc))
    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, p))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockDocumentWithParts<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    val rd = r.result.get()
    Assert.assertEquals(1, rd.content.size)
    val rp = rd.content[0]
    Assert.assertEquals(1, rp.content.size)
    val rs = rp.content[0] as KSBlockSectionWithContent
    Assert.assertEquals(1, rs.content.size)
  }

  @Test fun testDocumentPartImportContent() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val text = KSInlineText(pos, false, kp, false, "abc")
    val title = mutableListOf(text)
    val dc = KSImperativeDocument(pos, true, type, id, title)
    val pc = KSImperativePart(pos, true, type, id, title)
    val sc = KSImperativeSection(pos, true, type, id, title)

    val p = KSBlockParagraph(pos, true, kp, type, id, mutableListOf())
    val i = KSBlockImport(pos, true, kp, type, id, text)
    val ii = KSImperativeImport(pos, true, i, p)

    successEmpty(b.add(c, dc))
    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, sc))
    successEmpty(b.add(c, ii))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockDocumentWithParts<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    val rd = r.result.get()
    Assert.assertEquals(1, rd.content.size)
    val rs = rd.content[0]
    Assert.assertEquals(1, rs.content.size)
  }

  @Test fun testDocumentPart() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativeDocument(pos, true, type, id, title)
    val sc = KSImperativePart(pos, true, type, id, title)

    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, sc))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockDocumentWithParts<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    Assert.assertEquals(1, r.result.get().content.size)
  }

  @Test fun testDocumentSectionPart() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val dc = KSImperativeDocument(pos, true, type, id, title)
    val pc = KSImperativePart(pos, true, type, id, title)
    val sc = KSImperativeSection(pos, true, type, id, title)

    successEmpty(b.add(c, dc))
    successEmpty(b.add(c, sc))

    val r = b.add(c, pc)
    r as KSFailure
  }

  @Test fun testDocumentErrorInline() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativeDocument(pos, true, type, id, title)
    val i0 = KSInlineText(pos, false, kp, false, "xyz")

    successEmpty(b.add(c, pc))

    val r = b.add(c, KSImperativeInline(i0))
    r as KSFailure
  }

  @Test fun testDocumentErrorImportSubsection() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativeDocument(pos, true, type, id, title)
    val i0 = KSInlineText(pos, false, kp, false, "xyz")

    val i = KSBlockImport(pos, false, kp, type, id, i0)
    val p = KSBlockSubsection(pos, false, kp, type, id, title, mutableListOf())
    val ii = KSImperativeImport(pos, false, i, p)

    successEmpty(b.add(c, pc))

    val r = b.add(c, ii)
    r as KSFailure
  }

  @Test fun testDocumentErrorSubsection() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativeDocument(pos, true, type, id, title)
    val sc = KSImperativeSubsection(pos, true, type, id, title)

    successEmpty(b.add(c, pc))

    val r = b.add(c, sc)
    r as KSFailure
  }

  @Test fun testDocumentErrorSectionInline() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativeDocument(pos, true, type, id, title)
    val sc = KSImperativeSection(pos, true, type, id, title)
    val i0 = KSInlineText(pos, false, kp, false, "xyz")

    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, sc))

    val r = b.add(c, KSImperativeInline(i0))
    r as KSFailure
  }

  @Test fun testDocumentErrorPartInline() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val title = mutableListOf(KSInlineText(pos, false, kp, false, "abc"))
    val pc = KSImperativeDocument(pos, true, type, id, title)
    val sc = KSImperativePart(pos, true, type, id, title)
    val i0 = KSInlineText(pos, false, kp, false, "xyz")

    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, sc))

    val r = b.add(c, KSImperativeInline(i0))
    r as KSFailure
  }

  @Test fun testDocumentImport() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val text = KSInlineText(pos, false, kp, false, "abc")
    val texts = mutableListOf(text)
    val i = KSBlockImport(pos, false, kp, type, id, text)
    val p = KSBlockParagraph(pos, false, kp, type, id, texts)

    val pc = KSImperativeImport(pos, false, i, p)
    val i0 = KSInlineText(pos, false, kp, false, "xyz")

    val r = b.add(c, pc)
    r as KSSuccess<Optional<KSBlockParagraph<KSParse>>, KSParseError>
    Assert.assertSame(p, r.result.get())
  }

  @Test fun testDocumentErrorImportDocument() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val text = KSInlineText(pos, false, kp, false, "abc")
    val texts = mutableListOf(text)
    val i = KSBlockImport(pos, false, kp, type, id, text)
    val p = KSBlockDocumentWithSections(pos, false, kp, Optional.empty(), type, texts, mutableListOf())

    val pc = KSImperativeImport(pos, false, i, p)
    val i0 = KSInlineText(pos, false, kp, false, "xyz")
    val dc = KSImperativeDocument(Optional.empty(), false, type, id, texts)

    successEmpty(b.add(c, dc))

    val r = b.add(c, pc)
    r as KSFailure
  }

  @Test fun testDocumentImportSections() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val text = KSInlineText(pos, false, kp, false, "abc")
    val texts = mutableListOf(text)
    val i = KSBlockImport(pos, false, kp, type, id, text)
    val p = KSBlockSectionWithContent(pos, false, kp, type, id, texts, mutableListOf())

    val pc = KSImperativeImport(pos, false, i, p)
    val i0 = KSInlineText(pos, false, kp, false, "xyz")
    val dc = KSImperativeDocument(Optional.empty(), false, type, id, texts)

    successEmpty(b.add(c, dc))
    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, pc))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockDocumentWithSections<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    Assert.assertEquals(3, r.result.get().content.size)
  }

  @Test fun testDocumentErrorImportPartSectionsImport() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val text = KSInlineText(pos, false, kp, false, "abc")
    val texts = mutableListOf(text)
    val i = KSBlockImport(pos, false, kp, type, id, text)
    val s = KSBlockSectionWithContent(pos, false, kp, type, id, texts, mutableListOf())
    val p = KSBlockPart(pos, false, kp, type, id, texts, mutableListOf())

    val sc = KSImperativeImport(pos, false, i, s)
    val pc = KSImperativeImport(pos, false, i, p)
    val dc = KSImperativeDocument(Optional.empty(), false, type, id, texts)

    successEmpty(b.add(c, dc))
    successEmpty(b.add(c, pc))

    val r = b.add(c, sc)
    r as KSFailure
  }

  @Test fun testDocumentErrorImportPartSectionsImperative() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val text = KSInlineText(pos, false, kp, false, "abc")
    val texts = mutableListOf(text)
    val i = KSBlockImport(pos, false, kp, type, id, text)
    val s = KSBlockSectionWithContent(pos, false, kp, type, id, texts, mutableListOf())
    val p = KSBlockPart(pos, false, kp, type, id, texts, mutableListOf())

    val sc = KSImperativeSection(pos, false, type, id, texts)
    val pc = KSImperativeImport(pos, false, i, p)
    val dc = KSImperativeDocument(Optional.empty(), false, type, id, texts)

    successEmpty(b.add(c, dc))
    successEmpty(b.add(c, pc))

    val r = b.add(c, sc)
    r as KSFailure
  }

  @Test fun testDocumentErrorImportSectionParts() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val text = KSInlineText(pos, false, kp, false, "abc")
    val texts = mutableListOf(text)
    val i = KSBlockImport(pos, false, kp, type, id, text)
    val s = KSBlockSectionWithContent(pos, false, kp, type, id, texts, mutableListOf())
    val p = KSBlockPart(pos, false, kp, type, id, texts, mutableListOf())

    val sc = KSImperativeImport(pos, false, i, s)
    val pc = KSImperativeImport(pos, false, i, p)
    val dc = KSImperativeDocument(Optional.empty(), false, type, id, texts)

    successEmpty(b.add(c, dc))
    successEmpty(b.add(c, sc))

    val r = b.add(c, pc)
    r as KSFailure
  }

  @Test fun testDocumentPartImportSection() {
    val c = KSParseContext.empty()
    val b = newBuilder()

    val kp = KSParse(c)
    val text = KSInlineText(pos, false, kp, false, "abc")
    val texts = mutableListOf(text)
    val i = KSBlockImport(pos, false, kp, type, id, text)
    val s = KSBlockSectionWithContent(pos, false, kp, type, id, texts, mutableListOf())

    val sc = KSImperativeImport(pos, false, i, s)
    val pc = KSImperativePart(pos, false, type, id, texts)
    val dc = KSImperativeDocument(Optional.empty(), false, type, id, texts)

    successEmpty(b.add(c, dc))
    successEmpty(b.add(c, pc))
    successEmpty(b.add(c, sc))

    val r = b.add(c, KSImperativeEOF(Optional.empty()))
    r as KSSuccess<Optional<KSBlockDocumentWithParts<KSParse>>, KSParseError>
    Assert.assertTrue(r.result.isPresent)
    Assert.assertEquals(1, r.result.get().content.size)
  }
}