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

package com.io7m.kstructural.tests.core

import com.io7m.kstructural.core.KSElement
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithParts
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithSections
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection.KSBlockSectionWithContent
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection.KSBlockSectionWithSubsections
import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSLink
import com.io7m.kstructural.core.KSLinkContent
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSResult.KSFailure
import com.io7m.kstructural.core.KSResult.KSSuccess
import com.io7m.kstructural.core.KSSubsectionContent
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionParagraph
import com.io7m.kstructural.core.evaluator.KSEvaluation
import com.io7m.kstructural.core.evaluator.KSEvaluationError
import com.io7m.kstructural.core.evaluator.KSEvaluatorType
import com.io7m.kstructural.core.evaluator.KSNumber.KSNumberPart
import com.io7m.kstructural.core.evaluator.KSNumber.KSNumberPartSection
import com.io7m.kstructural.core.evaluator.KSNumber.KSNumberPartSectionContent
import com.io7m.kstructural.core.evaluator.KSNumber.KSNumberPartSectionSubsectionContent
import com.io7m.kstructural.core.evaluator.KSNumber.KSNumberSection
import com.io7m.kstructural.core.evaluator.KSNumber.KSNumberSectionContent
import com.io7m.kstructural.core.evaluator.KSNumber.KSNumberSectionSubsection
import com.io7m.kstructural.core.evaluator.KSNumber.KSNumberSectionSubsectionContent
import com.io7m.kstructural.core.evaluator.KSSerial
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional

abstract class KSEvaluatorContract {

  protected abstract fun newFilesystem() : FileSystem

  private var filesystem : FileSystem? = null

  @Before fun setupFilesystem() : Unit {
    this.filesystem = newFilesystem()
  }

  @After fun tearDownFilesystem() : Unit {
    this.filesystem!!.close()
  }

  private fun defaultFile() = filesystem!!.getPath("file.txt")

  protected abstract fun newEvaluatorForString(
    text : String) : KSEvaluatorContract.Evaluator

  protected abstract fun newEvaluatorForFile(
    file : String) : KSEvaluatorContract.Evaluator

  data class Evaluator(
    val e : KSEvaluatorType,
    val s : (Path) -> KSBlockDocument<KSParse>)

  @Test fun testDuplicateID() {
    val ee = newEvaluatorForString("""
[document (title dt) (id d0)
  (section [title st] [id d0]
    [paragraph p])]
""")

    val r = ee.e.evaluate(ee.s(defaultFile()), defaultFile())
    r as KSFailure
    Assert.assertEquals(1, r.errors.size)
  }

  @Test fun testNonexistentID() {
    val ee = newEvaluatorForString("""
[document (title dt) (id d0)
  (section [title st]
    [paragraph (link [target d1] x)])]
""")

    val r = ee.e.evaluate(ee.s(defaultFile()), defaultFile())
    r as KSFailure
    Assert.assertEquals(1, r.errors.size)
  }

  @Test fun testResolvedID() {
    val ee = newEvaluatorForString("""
[document (title dt) (id d0)
  (section [title st]
    [paragraph (link [target d0] x)])]
""")

    val i = ee.s(defaultFile())
    val r = ee.e.evaluate(i, defaultFile())
    r as KSSuccess<KSBlockDocumentWithSections<KSEvaluation>, *>
  }

  private val LOG = LoggerFactory.getLogger(KSEvaluatorContract::class.java)

  private fun checkSelf(e : KSElement<KSEvaluation>) : Unit {
    val c = e.data.context
    LOG.trace("checking self {} ({})", e.data.serial, e.javaClass.simpleName)
    Assert.assertSame(e, c.elementForSerial(e.data.serial).get())
  }

  private fun checkParent(
    e : KSElement<KSEvaluation>,
    p : KSElement<KSEvaluation>) : Unit {
    val c = e.data.context
    LOG.trace("checking parent {} ({}) -> {} ({})",
      e.data.serial,
      e.javaClass.simpleName,
      e.data.parent,
      p.javaClass.simpleName)
    Assert.assertEquals(e.data.parent, p.data.serial)
    val k = c.elementForSerial(e.data.parent)
    Assert.assertTrue(k.isPresent)
    Assert.assertEquals(e.data.parent, k.get().data.serial)
    Assert.assertSame(p, k.get())
  }

  private fun checkContentBlock(
    e : KSElement.KSBlock<KSEvaluation>,
    p : KSElement.KSBlock<KSEvaluation>
  ) : Unit {

    checkSelf(e)
    checkParent(e, p)
    return when (e) {

      is KSElement.KSBlock.KSBlockDocument   -> {
        checkSelf(e)
        when (e as KSBlockDocument) {
          is KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithParts    -> {
            e as KSBlockDocumentWithParts
            e.title.forEach { i -> checkAll(i, e) }
            e.content.forEach { c -> checkAll(c, e) }
          }
          is KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithSections -> {
            e as KSBlockDocumentWithSections
            e.title.forEach { i -> checkAll(i, e) }
            e.content.forEach { c -> checkAll(c, e) }
          }
        }
      }

      is KSElement.KSBlock.KSBlockSection    -> {
        e.title.forEach { i -> checkAll(i, e) }
        when (e as KSElement.KSBlock.KSBlockSection) {
          is KSElement.KSBlock.KSBlockSection.KSBlockSectionWithSubsections -> {
            val eb = e as KSElement.KSBlock.KSBlockSection.KSBlockSectionWithSubsections
            eb.content.forEach { c -> checkAll(c, e) }
          }
          is KSElement.KSBlock.KSBlockSection.KSBlockSectionWithContent     -> {
            val eb = e as KSElement.KSBlock.KSBlockSection.KSBlockSectionWithContent
            eb.content.forEach { c ->
              when (c) {
                is KSSubsectionContent.KSSubsectionParagraph  ->
                  checkAll(c.paragraph, e)
                is KSSubsectionContent.KSSubsectionFormalItem ->
                  checkAll(c.formal, e)
                is KSSubsectionContent.KSSubsectionFootnote   ->
                  checkAll(c.footnote, e)
              }
            }
          }
        }
      }

      is KSElement.KSBlock.KSBlockSubsection -> {
        e.title.forEach { i -> checkAll(i, e) }
        e.content.forEach { c ->
          when (c) {
            is KSSubsectionContent.KSSubsectionParagraph  ->
              checkAll(c.paragraph, e)
            is KSSubsectionContent.KSSubsectionFormalItem ->
              checkAll(c.formal, e)
            is KSSubsectionContent.KSSubsectionFootnote   ->
              checkAll(c.footnote, e)
          }
        }
      }

      is KSElement.KSBlock.KSBlockParagraph  -> {
        e.content.forEach { c -> checkAll(c, e) }
      }
      is KSElement.KSBlock.KSBlockFormalItem -> {
        e.content.forEach { c -> checkAll(c, e) }
      }
      is KSElement.KSBlock.KSBlockFootnote   -> {
        e.content.forEach { c -> checkAll(c, e) }
      }
      is KSElement.KSBlock.KSBlockPart       -> {
        e.content.forEach { c -> checkAll(c, e) }
      }
      is KSElement.KSBlock.KSBlockImport     -> {
        checkAll(e.file, e)
      }
    }
  }

  private fun checkContentInline(
    e : KSElement.KSInline<KSEvaluation>,
    p : KSElement<KSEvaluation>) {

    checkSelf(e)
    checkParent(e, p)

    return when (e) {
      is KSElement.KSInline.KSInlineLink              -> {
        checkContentLink(e, p)
      }
      is KSElement.KSInline.KSInlineText              -> {

      }
      is KSElement.KSInline.KSInlineVerbatim          -> {

      }
      is KSElement.KSInline.KSInlineTerm              -> {
        e.content.forEach { c -> checkAll(c, e) }
      }
      is KSElement.KSInline.KSInlineFootnoteReference -> {
      }
      is KSElement.KSInline.KSInlineImage             -> {
        e.content.forEach { c -> checkAll(c, e) }
      }
      is KSElement.KSInline.KSInlineListOrdered       -> {
        e.content.forEach { c -> checkAll(c, e) }
      }
      is KSElement.KSInline.KSInlineListUnordered     -> {
        e.content.forEach { c -> checkAll(c, e) }
      }
      is KSElement.KSInline.KSInlineTable             -> {
        checkAll(e.summary, e)
        if (e.head.isPresent) {
          checkAll(e.head.get(), e)
        }
        checkAll(e.body, e)
      }
      is KSElement.KSInline.KSInlineInclude           -> {

      }
    }
  }

  private fun checkContentLink(
    e : KSElement.KSInline.KSInlineLink<KSEvaluation>,
    p : KSElement<KSEvaluation>) {

    return when (e.actual) {
      is KSLink.KSLinkExternal -> {
        val actual = e.actual as KSLink.KSLinkExternal
        actual.content.forEach { c ->
          when (c) {
            is KSLinkContent.KSLinkText  -> {
            }
            is KSLinkContent.KSLinkImage ->
              checkAll(c.actual, e)
          }
        }
      }
      is KSLink.KSLinkInternal -> {
        val actual = e.actual as KSLink.KSLinkInternal
        actual.content.forEach { c ->
          when (c) {
            is KSLinkContent.KSLinkText  -> {
            }
            is KSLinkContent.KSLinkImage ->
              checkAll(c.actual, e)
          }
        }
      }
    }
  }

  private fun checkAll(
    e : KSElement<KSEvaluation>,
    p : KSElement<KSEvaluation>) : Unit {
    val c = e.data.context
    LOG.trace("checking all {} ({}) -> {} ({})",
      e.data.serial,
      e.javaClass.simpleName,
      e.data.parent,
      p.javaClass.simpleName)

    return when (e) {
      is KSElement.KSBlock                        ->
        checkContentBlock(e, p as KSElement.KSBlock<KSEvaluation>)
      is KSElement.KSInline                       ->
        checkContentInline(e, p)
      is KSElement.KSInline.KSListItem            -> {
        e.content.forEach { c -> checkAll(c, e) }
      }
      is KSElement.KSInline.KSTableHead           -> {
        e.column_names.forEach { cn -> checkAll(cn, e) }
      }
      is KSElement.KSInline.KSTableBodyCell       -> {
        e.content.forEach { c -> checkAll(c, e) }
      }
      is KSElement.KSInline.KSTableBodyRow        -> {
        e.cells.forEach { c -> checkAll(c, e) }
      }
      is KSElement.KSInline.KSTableBody           -> {
        e.rows.forEach { row -> checkAll(row, e) }
      }
      is KSElement.KSInline.KSTableSummary        -> {
        e.content.forEach { c -> checkAll(c, e) }
      }
      is KSElement.KSInline.KSTableHeadColumnName -> {
        e.content.forEach { c -> checkAll(c, e) }
      }
    }
  }

  private fun checkDocument(e : KSBlockDocument<KSEvaluation>) : Unit {
    checkSelf(e)
    when (e as KSBlockDocument) {
      is KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithParts    -> {
        e as KSBlockDocumentWithParts
        e.title.forEach { i -> checkAll(i, e) }
        e.content.forEach { c -> checkAll(c, e) }
      }
      is KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithSections -> {
        e as KSBlockDocumentWithSections
        e.title.forEach { i -> checkAll(i, e) }
        e.content.forEach { c -> checkAll(c, e) }
      }
    }
  }

  @Test fun testSections() {
    val ee = newEvaluatorForString("""
[document (title dt)
  (section [title s1]
    [paragraph p1]
    [paragraph p2]
    [paragraph p3])
  (section [title s2]
    [paragraph p1]
    [paragraph p2]
    [paragraph p3])
  (section [title s3]
    [paragraph p1]
    [paragraph p2]
    [paragraph p3])]
""")

    val i = ee.s(defaultFile())
    val r = ee.e.evaluate(i, defaultFile())
    r as KSSuccess<KSBlockDocumentWithSections<KSEvaluation>, *>

    val ctx = r.result.data.context
    Assert.assertEquals(KSSerial(0L), r.result.data.parent)
    Assert.assertEquals("dt", r.result.title[0].text)
    Assert.assertFalse(ctx.elementSegmentUp(r.result).isPresent)
    Assert.assertFalse(ctx.elementSegmentPrevious(r.result).isPresent)
    checkDocument(r.result)

    run {
      val s = r.result.content[0] as KSBlockSectionWithContent
      Assert.assertEquals("s1", s.title[0].text)
      Assert.assertEquals(KSNumberSection(1L), s.data.number.get())
      Assert.assertEquals(3, s.content.size)
      val p1 = s.content[0] as KSSubsectionParagraph<KSEvaluation>
      Assert.assertEquals(KSNumberSectionContent(1L, 1L), p1.paragraph.data.number.get())
      val p2 = s.content[1] as KSSubsectionParagraph<KSEvaluation>
      Assert.assertEquals(KSNumberSectionContent(1L, 2L), p2.paragraph.data.number.get())
      val p3 = s.content[2] as KSSubsectionParagraph<KSEvaluation>
      Assert.assertEquals(KSNumberSectionContent(1L, 3L), p3.paragraph.data.number.get())

      Assert.assertSame(r.result, ctx.elementSegmentUp(s).get())
      Assert.assertSame(r.result, ctx.elementSegmentUp(p1.paragraph).get())
      Assert.assertSame(r.result, ctx.elementSegmentUp(p2.paragraph).get())
      Assert.assertSame(r.result, ctx.elementSegmentUp(p3.paragraph).get())

      Assert.assertSame(r.result, ctx.elementSegmentPrevious(s).get())
      Assert.assertSame(r.result, ctx.elementSegmentPrevious(p1.paragraph).get())
      Assert.assertSame(r.result, ctx.elementSegmentPrevious(p2.paragraph).get())
      Assert.assertSame(r.result, ctx.elementSegmentPrevious(p3.paragraph).get())

      Assert.assertSame(s, ctx.elementSegmentNext(r.result).get())
      Assert.assertSame(r.result.content[1], ctx.elementSegmentNext(s).get())
      Assert.assertSame(r.result.content[1], ctx.elementSegmentNext(p1.paragraph).get())
      Assert.assertSame(r.result.content[1], ctx.elementSegmentNext(p2.paragraph).get())
      Assert.assertSame(r.result.content[1], ctx.elementSegmentNext(p3.paragraph).get())
    }

    run {
      val s = r.result.content[1] as KSBlockSectionWithContent
      Assert.assertEquals("s2", s.title[0].text)
      Assert.assertEquals(KSNumberSection(2L), s.data.number.get())
      Assert.assertEquals(3, s.content.size)
      val p1 = s.content[0] as KSSubsectionParagraph<KSEvaluation>
      Assert.assertEquals(KSNumberSectionContent(2L, 1L), p1.paragraph.data.number.get())
      val p2 = s.content[1] as KSSubsectionParagraph<KSEvaluation>
      Assert.assertEquals(KSNumberSectionContent(2L, 2L), p2.paragraph.data.number.get())
      val p3 = s.content[2] as KSSubsectionParagraph<KSEvaluation>
      Assert.assertEquals(KSNumberSectionContent(2L, 3L), p3.paragraph.data.number.get())

      Assert.assertSame(r.result, ctx.elementSegmentUp(s).get())
      Assert.assertSame(r.result, ctx.elementSegmentUp(p1.paragraph).get())
      Assert.assertSame(r.result, ctx.elementSegmentUp(p2.paragraph).get())
      Assert.assertSame(r.result, ctx.elementSegmentUp(p3.paragraph).get())

      val s_prev = r.result.content[0] as KSBlockSectionWithContent
      Assert.assertSame(s_prev, ctx.elementSegmentPrevious(s).get())
      Assert.assertSame(s_prev, ctx.elementSegmentPrevious(p1.paragraph).get())
      Assert.assertSame(s_prev, ctx.elementSegmentPrevious(p2.paragraph).get())
      Assert.assertSame(s_prev, ctx.elementSegmentPrevious(p3.paragraph).get())

      Assert.assertSame(r.result.content[2], ctx.elementSegmentNext(s).get())
      Assert.assertSame(r.result.content[2], ctx.elementSegmentNext(p1.paragraph).get())
      Assert.assertSame(r.result.content[2], ctx.elementSegmentNext(p2.paragraph).get())
      Assert.assertSame(r.result.content[2], ctx.elementSegmentNext(p3.paragraph).get())
    }

    run {
      val s = r.result.content[2] as KSBlockSectionWithContent
      Assert.assertEquals("s3", s.title[0].text)
      Assert.assertEquals(KSNumberSection(3L), s.data.number.get())
      Assert.assertEquals(3, s.content.size)
      val p1 = s.content[0] as KSSubsectionParagraph<KSEvaluation>
      Assert.assertEquals(KSNumberSectionContent(3L, 1L), p1.paragraph.data.number.get())
      val p2 = s.content[1] as KSSubsectionParagraph<KSEvaluation>
      Assert.assertEquals(KSNumberSectionContent(3L, 2L), p2.paragraph.data.number.get())
      val p3 = s.content[2] as KSSubsectionParagraph<KSEvaluation>
      Assert.assertEquals(KSNumberSectionContent(3L, 3L), p3.paragraph.data.number.get())

      Assert.assertSame(r.result, ctx.elementSegmentUp(s).get())
      Assert.assertSame(r.result, ctx.elementSegmentUp(p1.paragraph).get())
      Assert.assertSame(r.result, ctx.elementSegmentUp(p2.paragraph).get())
      Assert.assertSame(r.result, ctx.elementSegmentUp(p3.paragraph).get())

      val s_prev = r.result.content[1] as KSBlockSectionWithContent
      Assert.assertSame(s_prev, ctx.elementSegmentPrevious(s).get())
      Assert.assertSame(s_prev, ctx.elementSegmentPrevious(p1.paragraph).get())
      Assert.assertSame(s_prev, ctx.elementSegmentPrevious(p2.paragraph).get())
      Assert.assertSame(s_prev, ctx.elementSegmentPrevious(p3.paragraph).get())

      Assert.assertFalse(ctx.elementSegmentNext(s).isPresent)
      Assert.assertFalse(ctx.elementSegmentNext(p1.paragraph).isPresent)
      Assert.assertFalse(ctx.elementSegmentNext(p2.paragraph).isPresent)
      Assert.assertFalse(ctx.elementSegmentNext(p3.paragraph).isPresent)
    }
  }

  @Test fun testSectionSubsections() {
    val ee = newEvaluatorForString("""
[document (title dt)
  (section [title s1]
    [subsection (title ss1)
      (paragraph s1ss1p1)
      (paragraph s1ss1p2)]
    [subsection (title ss2)
      (paragraph s1ss2p1)
      (paragraph s1ss2p2)])
  (section [title s2]
    [subsection (title ss1)
      (paragraph s2ss1p1)
      (paragraph s2ss1p2)]
    [subsection (title ss2)
      (paragraph s2ss2p1)
      (paragraph s2ss2p2)])]
""")

    val i = ee.s(defaultFile())
    val r = ee.e.evaluate(i, defaultFile())
    r as KSSuccess<KSBlockDocumentWithSections<KSEvaluation>, *>

    val ctx = r.result.data.context
    Assert.assertEquals("dt", r.result.title[0].text)
    Assert.assertFalse(ctx.elementSegmentUp(r.result).isPresent)
    Assert.assertFalse(ctx.elementSegmentPrevious(r.result).isPresent)
    Assert.assertEquals(r.result.content[0], ctx.elementSegmentNext(r.result).get())
    checkDocument(r.result)

    run {
      val s = r.result.content[0] as KSBlockSectionWithSubsections
      Assert.assertEquals("s1", s.title[0].text)
      Assert.assertEquals(KSNumberSection(1L), s.data.number.get())
      Assert.assertEquals(2, s.content.size)
      Assert.assertSame(r.result, ctx.elementSegmentUp(s).get())
      Assert.assertSame(r.result, ctx.elementSegmentPrevious(s).get())
      Assert.assertSame(r.result.content[1], ctx.elementSegmentNext(s).get())

      run {
        val ss = s.content[0]
        Assert.assertEquals("ss1", ss.title[0].text)
        Assert.assertEquals(KSNumberSectionSubsection(1L, 1L), ss.data.number.get())
        val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(KSNumberSectionSubsectionContent(1L, 1L, 1L), p1.paragraph.data.number.get())
        val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(KSNumberSectionSubsectionContent(1L, 1L, 2L), p2.paragraph.data.number.get())

        Assert.assertSame(r.result, ctx.elementSegmentUp(ss).get())
        Assert.assertSame(r.result, ctx.elementSegmentUp(p1.paragraph).get())
        Assert.assertSame(r.result, ctx.elementSegmentUp(p2.paragraph).get())

        Assert.assertSame(r.result, ctx.elementSegmentPrevious(ss).get())
        Assert.assertSame(r.result, ctx.elementSegmentPrevious(p1.paragraph).get())
        Assert.assertSame(r.result, ctx.elementSegmentPrevious(p2.paragraph).get())

        Assert.assertSame(s, ctx.elementSegmentNext(r.result).get())
        Assert.assertSame(r.result.content[1], ctx.elementSegmentNext(ss).get())
        Assert.assertSame(r.result.content[1], ctx.elementSegmentNext(p1.paragraph).get())
        Assert.assertSame(r.result.content[1], ctx.elementSegmentNext(p2.paragraph).get())
      }

      run {
        val ss = s.content[1]
        Assert.assertEquals("ss2", ss.title[0].text)
        Assert.assertEquals(KSNumberSectionSubsection(1L, 2L), ss.data.number.get())
        val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(KSNumberSectionSubsectionContent(1L, 2L, 1L), p1.paragraph.data.number.get())
        val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(KSNumberSectionSubsectionContent(1L, 2L, 2L), p2.paragraph.data.number.get())

        Assert.assertSame(r.result, ctx.elementSegmentUp(ss).get())
        Assert.assertSame(r.result, ctx.elementSegmentUp(p1.paragraph).get())
        Assert.assertSame(r.result, ctx.elementSegmentUp(p2.paragraph).get())

        Assert.assertSame(r.result, ctx.elementSegmentPrevious(ss).get())
        Assert.assertSame(r.result, ctx.elementSegmentPrevious(p1.paragraph).get())
        Assert.assertSame(r.result, ctx.elementSegmentPrevious(p2.paragraph).get())

        Assert.assertSame(r.result.content[1], ctx.elementSegmentNext(ss).get())
        Assert.assertSame(r.result.content[1], ctx.elementSegmentNext(p1.paragraph).get())
        Assert.assertSame(r.result.content[1], ctx.elementSegmentNext(p2.paragraph).get())
      }
    }

    run {
      val s = r.result.content[1] as KSBlockSectionWithSubsections
      Assert.assertEquals("s2", s.title[0].text)
      Assert.assertEquals(KSNumberSection(2L), s.data.number.get())
      Assert.assertEquals(2, s.content.size)
      Assert.assertSame(r.result, ctx.elementSegmentUp(s).get())
      Assert.assertFalse(ctx.elementSegmentNext(s).isPresent)

      run {
        val ss = s.content[0]
        Assert.assertEquals("ss1", ss.title[0].text)
        Assert.assertEquals(KSNumberSectionSubsection(2L, 1L), ss.data.number.get())
        val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(KSNumberSectionSubsectionContent(2L, 1L, 1L), p1.paragraph.data.number.get())
        val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(KSNumberSectionSubsectionContent(2L, 1L, 2L), p2.paragraph.data.number.get())

        Assert.assertSame(r.result, ctx.elementSegmentUp(ss).get())
        Assert.assertSame(r.result, ctx.elementSegmentUp(p1.paragraph).get())
        Assert.assertSame(r.result, ctx.elementSegmentUp(p2.paragraph).get())

        val s_prev = r.result.content[0]
        Assert.assertSame(s_prev, ctx.elementSegmentPrevious(ss).get())
        Assert.assertSame(s_prev, ctx.elementSegmentPrevious(p1.paragraph).get())
        Assert.assertSame(s_prev, ctx.elementSegmentPrevious(p2.paragraph).get())

        Assert.assertFalse(ctx.elementSegmentNext(ss).isPresent)
        Assert.assertFalse(ctx.elementSegmentNext(p1.paragraph).isPresent)
        Assert.assertFalse(ctx.elementSegmentNext(p2.paragraph).isPresent)
      }

      run {
        val ss = s.content[1]
        Assert.assertEquals("ss2", ss.title[0].text)
        Assert.assertEquals(KSNumberSectionSubsection(2L, 2L), ss.data.number.get())
        val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(KSNumberSectionSubsectionContent(2L, 2L, 1L), p1.paragraph.data.number.get())
        val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(KSNumberSectionSubsectionContent(2L, 2L, 2L), p2.paragraph.data.number.get())

        Assert.assertSame(r.result, ctx.elementSegmentUp(ss).get())
        Assert.assertSame(r.result, ctx.elementSegmentUp(p1.paragraph).get())
        Assert.assertSame(r.result, ctx.elementSegmentUp(p2.paragraph).get())

        val s_prev = r.result.content[0]
        Assert.assertSame(s_prev, ctx.elementSegmentPrevious(ss).get())
        Assert.assertSame(s_prev, ctx.elementSegmentPrevious(p1.paragraph).get())
        Assert.assertSame(s_prev, ctx.elementSegmentPrevious(p2.paragraph).get())

        Assert.assertFalse(ctx.elementSegmentNext(ss).isPresent)
        Assert.assertFalse(ctx.elementSegmentNext(p1.paragraph).isPresent)
        Assert.assertFalse(ctx.elementSegmentNext(p2.paragraph).isPresent)
      }
    }
  }

  @Test fun testPartSections() {
    val ee = newEvaluatorForString("""
[document (title dt)
  (part [title p1]
    [section (title s1)
      (paragraph p1)
      (paragraph p2)]
    [section (title s2)
      (paragraph p1)
      (paragraph p2)])
  (part [title p2]
    [section (title s1)
      (paragraph p1)
      (paragraph p2)]
    [section (title s2)
      (paragraph p1)
      (paragraph p2)])]
""")

    val i = ee.s(defaultFile())
    val r = ee.e.evaluate(i, defaultFile())
    r as KSSuccess<KSBlockDocumentWithParts<KSEvaluation>, *>

    val ctx = r.result.data.context
    Assert.assertEquals("dt", r.result.title[0].text)
    Assert.assertFalse(ctx.elementSegmentUp(r.result).isPresent)
    Assert.assertFalse(ctx.elementSegmentPrevious(r.result).isPresent)
    Assert.assertEquals(r.result.content[0], ctx.elementSegmentNext(r.result).get())
    checkDocument(r.result)

    run {
      val p = r.result.content[0]
      Assert.assertEquals("p1", p.title[0].text)
      Assert.assertEquals(KSNumberPart(1L), p.data.number.get())
      Assert.assertEquals(2, p.content.size)
      Assert.assertSame(r.result, ctx.elementSegmentUp(p).get())
      Assert.assertSame(r.result, ctx.elementSegmentPrevious(p).get())
      Assert.assertSame(p, ctx.elementSegmentNext(r.result).get())

      run {
        val s = p.content[0] as KSBlockSectionWithContent<KSEvaluation>
        Assert.assertEquals("s1", s.title[0].text)
        Assert.assertEquals(KSNumberPartSection(1L, 1L), s.data.number.get())
        Assert.assertEquals(2, s.content.size)

        val p1 = s.content[0] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(KSNumberPartSectionContent(1L, 1L, 1L), p1.paragraph.data.number.get())
        val p2 = s.content[1] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(KSNumberPartSectionContent(1L, 1L, 2L), p2.paragraph.data.number.get())

        Assert.assertSame(p, ctx.elementSegmentUp(s).get())
        Assert.assertSame(p, ctx.elementSegmentUp(p1.paragraph).get())
        Assert.assertSame(p, ctx.elementSegmentUp(p2.paragraph).get())

        Assert.assertSame(p, ctx.elementSegmentPrevious(s).get())
        Assert.assertSame(p, ctx.elementSegmentPrevious(p1.paragraph).get())
        Assert.assertSame(p, ctx.elementSegmentPrevious(p2.paragraph).get())

        Assert.assertEquals(p.content[1], ctx.elementSegmentNext(s).get())
        Assert.assertEquals(p.content[1], ctx.elementSegmentNext(p1.paragraph).get())
        Assert.assertEquals(p.content[1], ctx.elementSegmentNext(p2.paragraph).get())
      }

      run {
        val s = p.content[1] as KSBlockSectionWithContent<KSEvaluation>
        Assert.assertEquals("s2", s.title[0].text)
        Assert.assertEquals(KSNumberPartSection(1L, 2L), s.data.number.get())
        Assert.assertEquals(2, s.content.size)

        val p1 = s.content[0] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(KSNumberPartSectionContent(1L, 2L, 1L), p1.paragraph.data.number.get())
        val p2 = s.content[1] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(KSNumberPartSectionContent(1L, 2L, 2L), p2.paragraph.data.number.get())

        Assert.assertSame(p, ctx.elementSegmentUp(s).get())
        Assert.assertSame(p, ctx.elementSegmentUp(p1.paragraph).get())
        Assert.assertSame(p, ctx.elementSegmentUp(p2.paragraph).get())

        val s_prev = p.content[0]
        Assert.assertSame(s_prev, ctx.elementSegmentPrevious(s).get())
        Assert.assertSame(s_prev, ctx.elementSegmentPrevious(p1.paragraph).get())
        Assert.assertSame(s_prev, ctx.elementSegmentPrevious(p2.paragraph).get())

        Assert.assertEquals(r.result.content[1], ctx.elementSegmentNext(s).get())
        Assert.assertEquals(r.result.content[1], ctx.elementSegmentNext(p1.paragraph).get())
        Assert.assertEquals(r.result.content[1], ctx.elementSegmentNext(p2.paragraph).get())
      }
    }

    run {
      val p = r.result.content[1]
      Assert.assertEquals("p2", p.title[0].text)
      Assert.assertEquals(KSNumberPart(2L), p.data.number.get())
      Assert.assertEquals(2, p.content.size)
      Assert.assertSame(r.result, ctx.elementSegmentUp(p).get())
      Assert.assertSame(p.content[0], ctx.elementSegmentNext(p).get())

      run {
        val s = p.content[0] as KSBlockSectionWithContent<KSEvaluation>
        Assert.assertEquals("s1", s.title[0].text)
        Assert.assertEquals(KSNumberPartSection(2L, 1L), s.data.number.get())
        Assert.assertEquals(2, s.content.size)

        val p1 = s.content[0] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(KSNumberPartSectionContent(2L, 1L, 1L), p1.paragraph.data.number.get())
        val p2 = s.content[1] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(KSNumberPartSectionContent(2L, 1L, 2L), p2.paragraph.data.number.get())

        Assert.assertSame(p, ctx.elementSegmentUp(s).get())
        Assert.assertSame(p, ctx.elementSegmentUp(p1.paragraph).get())
        Assert.assertSame(p, ctx.elementSegmentUp(p2.paragraph).get())

        Assert.assertSame(p, ctx.elementSegmentPrevious(s).get())
        Assert.assertSame(p, ctx.elementSegmentPrevious(p1.paragraph).get())
        Assert.assertSame(p, ctx.elementSegmentPrevious(p2.paragraph).get())

        Assert.assertEquals(p.content[1], ctx.elementSegmentNext(s).get())
        Assert.assertEquals(p.content[1], ctx.elementSegmentNext(p1.paragraph).get())
        Assert.assertEquals(p.content[1], ctx.elementSegmentNext(p2.paragraph).get())
      }

      run {
        val s = p.content[1] as KSBlockSectionWithContent<KSEvaluation>
        Assert.assertEquals("s2", s.title[0].text)
        Assert.assertEquals(KSNumberPartSection(2L, 2L), s.data.number.get())
        Assert.assertEquals(2, s.content.size)

        val p1 = s.content[0] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(KSNumberPartSectionContent(2L, 2L, 1L), p1.paragraph.data.number.get())
        val p2 = s.content[1] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(KSNumberPartSectionContent(2L, 2L, 2L), p2.paragraph.data.number.get())

        Assert.assertSame(p, ctx.elementSegmentUp(s).get())
        Assert.assertSame(p, ctx.elementSegmentUp(p1.paragraph).get())
        Assert.assertSame(p, ctx.elementSegmentUp(p2.paragraph).get())

        val s_prev = p.content[0]
        Assert.assertSame(s_prev, ctx.elementSegmentPrevious(s).get())
        Assert.assertSame(s_prev, ctx.elementSegmentPrevious(p1.paragraph).get())
        Assert.assertSame(s_prev, ctx.elementSegmentPrevious(p2.paragraph).get())

        Assert.assertFalse(ctx.elementSegmentNext(s).isPresent)
        Assert.assertFalse(ctx.elementSegmentNext(p1.paragraph).isPresent)
        Assert.assertFalse(ctx.elementSegmentNext(p2.paragraph).isPresent)
      }
    }
  }

  @Test fun testPartSectionSubsections() {
    val ee = newEvaluatorForString("""
[document (title dt)
  (part [title p1]
    [section (title s1)
      (subsection [title ss1]
        [paragraph p1]
        [paragraph p2])
      (subsection [title ss2]
        [paragraph p1]
        [paragraph p2])]
    [section (title s2)
      (subsection [title ss1]
        [paragraph p1]
        [paragraph p2])
      (subsection [title ss2]
        [paragraph p1]
        [paragraph p2])])
  (part [title p2]
    [section (title s1)
      (subsection [title ss1]
        [paragraph p1]
        [paragraph p2])
      (subsection [title ss2]
        [paragraph p1]
        [paragraph p2])]
    [section (title s2)
      (subsection [title ss1]
        [paragraph p1]
        [paragraph p2])
      (subsection [title ss2]
        [paragraph p1]
        [paragraph p2])])]
""")

    val i = ee.s(defaultFile())
    val r = ee.e.evaluate(i, defaultFile())
    r as KSSuccess<KSBlockDocumentWithParts<KSEvaluation>, *>

    val ctx = r.result.data.context
    Assert.assertEquals("dt", r.result.title[0].text)
    Assert.assertFalse(ctx.elementSegmentUp(r.result).isPresent)
    Assert.assertFalse(ctx.elementSegmentPrevious(r.result).isPresent)
    Assert.assertEquals(r.result.content[0], ctx.elementSegmentNext(r.result).get())
    checkDocument(r.result)

    run {
      val p = r.result.content[0]
      Assert.assertEquals("p1", p.title[0].text)
      Assert.assertEquals(KSNumberPart(1L), p.data.number.get())
      Assert.assertEquals(2, p.content.size)
      Assert.assertSame(r.result, ctx.elementSegmentUp(p).get())
      Assert.assertSame(r.result, ctx.elementSegmentPrevious(p).get())
      Assert.assertSame(p, ctx.elementSegmentNext(r.result).get())

      run {
        val s = p.content[0] as KSBlockSectionWithSubsections<KSEvaluation>
        Assert.assertEquals("s1", s.title[0].text)
        Assert.assertEquals(KSNumberPartSection(1L, 1L), s.data.number.get())
        Assert.assertEquals(2, s.content.size)
        Assert.assertSame(p, ctx.elementSegmentUp(s).get())
        Assert.assertSame(p, ctx.elementSegmentPrevious(s).get())
        Assert.assertSame(p.content[1], ctx.elementSegmentNext(s).get())

        run {
          val ss = s.content[0]
          Assert.assertEquals("ss1", ss.title[0].text)
          val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(KSNumberPartSectionSubsectionContent(1L, 1L, 1L, 1L), p1.paragraph.data.number.get())
          val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(KSNumberPartSectionSubsectionContent(1L, 1L, 1L, 2L), p2.paragraph.data.number.get())

          Assert.assertSame(p, ctx.elementSegmentUp(ss).get())
          Assert.assertSame(p, ctx.elementSegmentUp(p1.paragraph).get())
          Assert.assertSame(p, ctx.elementSegmentUp(p2.paragraph).get())

          Assert.assertSame(p, ctx.elementSegmentPrevious(ss).get())
          Assert.assertSame(p, ctx.elementSegmentPrevious(p1.paragraph).get())
          Assert.assertSame(p, ctx.elementSegmentPrevious(p2.paragraph).get())

          Assert.assertSame(p.content[1], ctx.elementSegmentNext(ss).get())
          Assert.assertSame(p.content[1], ctx.elementSegmentNext(p1.paragraph).get())
          Assert.assertSame(p.content[1], ctx.elementSegmentNext(p2.paragraph).get())
        }

        run {
          val ss = s.content[1]
          Assert.assertEquals("ss2", ss.title[0].text)
          val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(KSNumberPartSectionSubsectionContent(1L, 1L, 2L, 1L), p1.paragraph.data.number.get())
          val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(KSNumberPartSectionSubsectionContent(1L, 1L, 2L, 2L), p2.paragraph.data.number.get())

          Assert.assertSame(p, ctx.elementSegmentUp(ss).get())
          Assert.assertSame(p, ctx.elementSegmentUp(p1.paragraph).get())
          Assert.assertSame(p, ctx.elementSegmentUp(p2.paragraph).get())

          Assert.assertSame(p, ctx.elementSegmentPrevious(ss).get())
          Assert.assertSame(p, ctx.elementSegmentPrevious(p1.paragraph).get())
          Assert.assertSame(p, ctx.elementSegmentPrevious(p2.paragraph).get())

          Assert.assertSame(p.content[1], ctx.elementSegmentNext(ss).get())
          Assert.assertSame(p.content[1], ctx.elementSegmentNext(p1.paragraph).get())
          Assert.assertSame(p.content[1], ctx.elementSegmentNext(p2.paragraph).get())
        }
      }

      run {
        val s = p.content[1] as KSBlockSectionWithSubsections<KSEvaluation>
        Assert.assertEquals("s2", s.title[0].text)
        Assert.assertEquals(KSNumberPartSection(1L, 2L), s.data.number.get())
        Assert.assertEquals(2, s.content.size)
        Assert.assertSame(p, ctx.elementSegmentUp(s).get())
        Assert.assertSame(r.result.content[1], ctx.elementSegmentNext(s).get())

        val s_prev = p.content[0]
        Assert.assertSame(s_prev, ctx.elementSegmentPrevious(s).get())

        run {
          val ss = s.content[0]
          Assert.assertEquals("ss1", ss.title[0].text)
          val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(KSNumberPartSectionSubsectionContent(1L, 2L, 1L, 1L), p1.paragraph.data.number.get())
          val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(KSNumberPartSectionSubsectionContent(1L, 2L, 1L, 2L), p2.paragraph.data.number.get())

          Assert.assertSame(p, ctx.elementSegmentUp(ss).get())
          Assert.assertSame(p, ctx.elementSegmentUp(p1.paragraph).get())
          Assert.assertSame(p, ctx.elementSegmentUp(p2.paragraph).get())

          Assert.assertSame(s_prev, ctx.elementSegmentPrevious(ss).get())
          Assert.assertSame(s_prev, ctx.elementSegmentPrevious(p1.paragraph).get())
          Assert.assertSame(s_prev, ctx.elementSegmentPrevious(p2.paragraph).get())

          Assert.assertSame(r.result.content[1], ctx.elementSegmentNext(ss).get())
          Assert.assertSame(r.result.content[1], ctx.elementSegmentNext(p1.paragraph).get())
          Assert.assertSame(r.result.content[1], ctx.elementSegmentNext(p2.paragraph).get())
        }

        run {
          val ss = s.content[1]
          Assert.assertEquals("ss2", ss.title[0].text)
          val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(KSNumberPartSectionSubsectionContent(1L, 2L, 2L, 1L), p1.paragraph.data.number.get())
          val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(KSNumberPartSectionSubsectionContent(1L, 2L, 2L, 2L), p2.paragraph.data.number.get())

          Assert.assertSame(p, ctx.elementSegmentUp(ss).get())
          Assert.assertSame(p, ctx.elementSegmentUp(p1.paragraph).get())
          Assert.assertSame(p, ctx.elementSegmentUp(p2.paragraph).get())

          Assert.assertSame(s_prev, ctx.elementSegmentPrevious(ss).get())
          Assert.assertSame(s_prev, ctx.elementSegmentPrevious(p1.paragraph).get())
          Assert.assertSame(s_prev, ctx.elementSegmentPrevious(p2.paragraph).get())

          Assert.assertSame(r.result.content[1], ctx.elementSegmentNext(ss).get())
          Assert.assertSame(r.result.content[1], ctx.elementSegmentNext(p1.paragraph).get())
          Assert.assertSame(r.result.content[1], ctx.elementSegmentNext(p2.paragraph).get())
        }
      }
    }

    run {
      val p = r.result.content[1]
      Assert.assertEquals("p2", p.title[0].text)
      Assert.assertEquals(KSNumberPart(2L), p.data.number.get())
      Assert.assertEquals(2, p.content.size)
      Assert.assertSame(p.content[0], ctx.elementSegmentNext(p).get())

      val p_prev = r.result.content[0]
      Assert.assertSame(p_prev, ctx.elementSegmentPrevious(p).get())

      run {
        val s = p.content[0] as KSBlockSectionWithSubsections<KSEvaluation>
        Assert.assertEquals("s1", s.title[0].text)
        Assert.assertEquals(KSNumberPartSection(2L, 1L), s.data.number.get())
        Assert.assertEquals(2, s.content.size)
        Assert.assertSame(p, ctx.elementSegmentUp(s).get())
        Assert.assertSame(p, ctx.elementSegmentPrevious(s).get())
        Assert.assertSame(p.content[1], ctx.elementSegmentNext(s).get())

        run {
          val ss = s.content[0]
          Assert.assertEquals("ss1", ss.title[0].text)
          val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(KSNumberPartSectionSubsectionContent(2L, 1L, 1L, 1L), p1.paragraph.data.number.get())
          val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(KSNumberPartSectionSubsectionContent(2L, 1L, 1L, 2L), p2.paragraph.data.number.get())

          Assert.assertSame(p, ctx.elementSegmentUp(ss).get())
          Assert.assertSame(p, ctx.elementSegmentUp(p1.paragraph).get())
          Assert.assertSame(p, ctx.elementSegmentUp(p2.paragraph).get())

          Assert.assertSame(p, ctx.elementSegmentPrevious(ss).get())
          Assert.assertSame(p, ctx.elementSegmentPrevious(p1.paragraph).get())
          Assert.assertSame(p, ctx.elementSegmentPrevious(p2.paragraph).get())

          Assert.assertSame(p.content[1], ctx.elementSegmentNext(ss).get())
          Assert.assertSame(p.content[1], ctx.elementSegmentNext(p1.paragraph).get())
          Assert.assertSame(p.content[1], ctx.elementSegmentNext(p2.paragraph).get())
        }

        run {
          val ss = s.content[1]
          Assert.assertEquals("ss2", ss.title[0].text)
          val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(KSNumberPartSectionSubsectionContent(2L, 1L, 2L, 1L), p1.paragraph.data.number.get())
          val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(KSNumberPartSectionSubsectionContent(2L, 1L, 2L, 2L), p2.paragraph.data.number.get())

          Assert.assertSame(p, ctx.elementSegmentUp(ss).get())
          Assert.assertSame(p, ctx.elementSegmentUp(p1.paragraph).get())
          Assert.assertSame(p, ctx.elementSegmentUp(p2.paragraph).get())

          Assert.assertSame(p, ctx.elementSegmentPrevious(ss).get())
          Assert.assertSame(p, ctx.elementSegmentPrevious(p1.paragraph).get())
          Assert.assertSame(p, ctx.elementSegmentPrevious(p2.paragraph).get())

          Assert.assertSame(p.content[1], ctx.elementSegmentNext(ss).get())
          Assert.assertSame(p.content[1], ctx.elementSegmentNext(p1.paragraph).get())
          Assert.assertSame(p.content[1], ctx.elementSegmentNext(p2.paragraph).get())
        }
      }

      run {
        val s = p.content[1] as KSBlockSectionWithSubsections<KSEvaluation>
        Assert.assertEquals("s2", s.title[0].text)
        Assert.assertEquals(KSNumberPartSection(2L, 2L), s.data.number.get())
        Assert.assertEquals(2, s.content.size)
        Assert.assertSame(p, ctx.elementSegmentUp(s).get())
        Assert.assertFalse(ctx.elementSegmentNext(s).isPresent)

        val s_prev = p.content[0]
        Assert.assertSame(s_prev, ctx.elementSegmentPrevious(s).get())

        run {
          val ss = s.content[0]
          Assert.assertEquals("ss1", ss.title[0].text)
          val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(KSNumberPartSectionSubsectionContent(2L, 2L, 1L, 1L), p1.paragraph.data.number.get())
          val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(KSNumberPartSectionSubsectionContent(2L, 2L, 1L, 2L), p2.paragraph.data.number.get())

          Assert.assertSame(p, ctx.elementSegmentUp(ss).get())
          Assert.assertSame(p, ctx.elementSegmentUp(p1.paragraph).get())
          Assert.assertSame(p, ctx.elementSegmentUp(p2.paragraph).get())

          Assert.assertSame(s_prev, ctx.elementSegmentPrevious(ss).get())
          Assert.assertSame(s_prev, ctx.elementSegmentPrevious(p1.paragraph).get())
          Assert.assertSame(s_prev, ctx.elementSegmentPrevious(p2.paragraph).get())

          Assert.assertFalse(ctx.elementSegmentNext(ss).isPresent)
          Assert.assertFalse(ctx.elementSegmentNext(p1.paragraph).isPresent)
          Assert.assertFalse(ctx.elementSegmentNext(p2.paragraph).isPresent)
        }

        run {
          val ss = s.content[1]
          Assert.assertEquals("ss2", ss.title[0].text)
          val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(KSNumberPartSectionSubsectionContent(2L, 2L, 2L, 1L), p1.paragraph.data.number.get())
          val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(KSNumberPartSectionSubsectionContent(2L, 2L, 2L, 2L), p2.paragraph.data.number.get())

          Assert.assertSame(p, ctx.elementSegmentUp(ss).get())
          Assert.assertSame(p, ctx.elementSegmentUp(p1.paragraph).get())
          Assert.assertSame(p, ctx.elementSegmentUp(p2.paragraph).get())

          Assert.assertSame(s_prev, ctx.elementSegmentPrevious(ss).get())
          Assert.assertSame(s_prev, ctx.elementSegmentPrevious(p1.paragraph).get())
          Assert.assertSame(s_prev, ctx.elementSegmentPrevious(p2.paragraph).get())

          Assert.assertFalse(ctx.elementSegmentNext(ss).isPresent)
          Assert.assertFalse(ctx.elementSegmentNext(p1.paragraph).isPresent)
          Assert.assertFalse(ctx.elementSegmentNext(p2.paragraph).isPresent)
        }
      }
    }
  }

  @Test fun testTableColumnMismatchRow0() {
    val ee = newEvaluatorForString("""
[document (title dt) (id d0)
  (section [title st]
    [paragraph
      (table [summary s] [head (name a) (name b)] [body (row [cell x])])
    ]
  )
]
""")

    val i = ee.s(defaultFile())
    val r = ee.e.evaluate(i, defaultFile())
    r as KSFailure
    Assert.assertEquals(1, r.errors.size)
  }

  @Test fun testTableColumnMismatchRow1() {
    val ee = newEvaluatorForString("""
[document (title dt) (id d0)
  (section [title st]
    [paragraph
      (table
        [summary s]
        [head (name a) (name b)]
        [body
          (row [cell x] [cell y])
          (row [cell x])
          (row [cell x] [cell y])
        ])
    ]
  )
]
""")

    val i = ee.s(defaultFile())
    val r = ee.e.evaluate(i, defaultFile())
    r as KSFailure
    Assert.assertEquals(1, r.errors.size)
  }

  @Test fun testTableNested() {
    val ee = newEvaluatorForString("""
[document (title dt) (id d0)
  (section [title st]
    [paragraph
      (table
        [summary s]
        [body
          (row [cell
            (table [summary s] [body (row [cell x])])
          ])
        ])
    ]
  )
]
""")

    val i = ee.s(defaultFile())
    val r = ee.e.evaluate(i, defaultFile())
    r as KSFailure
    Assert.assertEquals(1, r.errors.size)
  }

  @Test fun testFootnoteReferenceDocument() {
    val ee = newEvaluatorForString("""
[document (title dt) (id d0)
  (section [title st]
    [paragraph (footnote-ref d0).])]
""")

    val i = ee.s(defaultFile())
    val r = ee.e.evaluate(i, defaultFile())
    r as KSFailure
    Assert.assertEquals(1, r.errors.size)
  }

  @Test fun testFootnoteReferenceSection() {
    val ee = newEvaluatorForString("""
[document (title dt)
  (section [title st] [id s0]
    [paragraph (footnote-ref s0).])]
""")

    val i = ee.s(defaultFile())
    val r = ee.e.evaluate(i, defaultFile())
    r as KSFailure
    Assert.assertEquals(1, r.errors.size)
  }

  @Test fun testFootnoteReferenceSubsection() {
    val ee = newEvaluatorForString("""
[document (title dt)
  (section [title st]
    (subsection [title ss] [id s0]
      [paragraph (footnote-ref s0).]))]
""")

    val i = ee.s(defaultFile())
    val r = ee.e.evaluate(i, defaultFile())
    r as KSFailure
    Assert.assertEquals(1, r.errors.size)
  }

  @Test fun testFootnoteReferenceParagraph() {
    val ee = newEvaluatorForString("""
[document (title dt)
  (section [title st]
    (subsection [title ss]
      [paragraph [id s0] (footnote-ref s0).]))]
""")

    val i = ee.s(defaultFile())
    val r = ee.e.evaluate(i, defaultFile())
    r as KSFailure
    Assert.assertEquals(1, r.errors.size)
  }

  @Test fun testFootnoteReferencePart() {
    val ee = newEvaluatorForString("""
[document (title dt)
  (part [title p] [id x]
    (section [title s]
      (paragraph [footnote-ref x])))]
""")

    val i = ee.s(defaultFile())
    val r = ee.e.evaluate(i, defaultFile())
    r as KSFailure
    Assert.assertEquals(1, r.errors.size)
  }

  @Test fun testFootnoteReferenceFormal() {
    val ee = newEvaluatorForString("""
[document (title dt)
  (section [title st]
    (formal-item [title z] [id x] [footnote-ref x]))]
""")

    val i = ee.s(defaultFile())
    val r = ee.e.evaluate(i, defaultFile())
    r as KSFailure
    Assert.assertEquals(1, r.errors.size)
  }

  @Test fun testFootnotesSections() {
    val ee = newEvaluatorForString("""
[document (title dt) (id d0)
  (section [title st]
    [footnote (id f0)])
  (section [title st]
    [footnote (id f1)])
  (section [title st]
    [footnote (id f2)])]
""")

    val i = ee.s(defaultFile())
    val r = ee.e.evaluate(i, defaultFile())
    r as KSSuccess<KSBlockDocumentWithSections<KSEvaluation>, KSEvaluationError>

    val rr = r.result
    Assert.assertEquals(3, rr.data.context.footnotesAll.size)

    val fn0 = rr.data.context.footnotesForSection(KSNumberSection(1L))
    Assert.assertEquals(1, fn0.size)
    Assert.assertTrue(fn0.containsKey(KSID.create(Optional.empty(), "f0", rr.data)))

    val fn1 = rr.data.context.footnotesForSection(KSNumberSection(2L))
    Assert.assertEquals(1, fn1.size)
    Assert.assertTrue(fn1.containsKey(KSID.create(Optional.empty(), "f1", rr.data)))

    val fn2 = rr.data.context.footnotesForSection(KSNumberSection(3L))
    Assert.assertEquals(1, fn2.size)
    Assert.assertTrue(fn2.containsKey(KSID.create(Optional.empty(), "f2", rr.data)))
  }

  @Test fun testFootnotesPartSections() {
    val ee = newEvaluatorForString("""
[document (title dt) (id d0)
  [part [title p]
    (section [title st]
      [footnote (id f0)])
    (section [title st]
      [footnote (id f1)])
    (section [title st]
      [footnote (id f2)])]
  [part [title p]
    (section [title st]
      [footnote (id f3)])
    (section [title st]
      [footnote (id f4)])
    (section [title st]
      [footnote (id f5)])]]
""")

    val i = ee.s(defaultFile())
    val r = ee.e.evaluate(i, defaultFile())
    r as KSSuccess<KSBlockDocumentWithParts<KSEvaluation>, KSEvaluationError>

    val rr = r.result
    Assert.assertEquals(6, rr.data.context.footnotesAll.size)

    val fn0 = rr.data.context.footnotesForSection(KSNumberPartSection(1L, 1L))
    Assert.assertEquals(1, fn0.size)
    Assert.assertTrue(fn0.containsKey(KSID.create(Optional.empty(), "f0", rr.data)))

    val fn1 = rr.data.context.footnotesForSection(KSNumberPartSection(1L, 2L))
    Assert.assertEquals(1, fn1.size)
    Assert.assertTrue(fn1.containsKey(KSID.create(Optional.empty(), "f1", rr.data)))

    val fn2 = rr.data.context.footnotesForSection(KSNumberPartSection(1L, 3L))
    Assert.assertEquals(1, fn2.size)
    Assert.assertTrue(fn2.containsKey(KSID.create(Optional.empty(), "f2", rr.data)))

    val fn3 = rr.data.context.footnotesForSection(KSNumberPartSection(2L, 1L))
    Assert.assertEquals(1, fn3.size)
    Assert.assertTrue(fn3.containsKey(KSID.create(Optional.empty(), "f3", rr.data)))

    val fn4 = rr.data.context.footnotesForSection(KSNumberPartSection(2L, 2L))
    Assert.assertEquals(1, fn4.size)
    Assert.assertTrue(fn4.containsKey(KSID.create(Optional.empty(), "f4", rr.data)))

    val fn5 = rr.data.context.footnotesForSection(KSNumberPartSection(2L, 3L))
    Assert.assertEquals(1, fn5.size)
    Assert.assertTrue(fn5.containsKey(KSID.create(Optional.empty(), "f5", rr.data)))
  }

  @Test fun testSimpleDocument() {
    val ee = newEvaluatorForFile("/com/io7m/kstructural/tests/simple.sd")
    val i = ee.s(defaultFile())
    val r = ee.e.evaluate(i, defaultFile())
    r as KSSuccess
    checkDocument(r.result)
  }

  @Test fun testImport() {
    val other_path = filesystem!!.getPath("other.txt").toAbsolutePath()
    Files.write(other_path,
      "[section [title s] [paragraph p]]".toByteArray(StandardCharsets.UTF_8))

    val ee = newEvaluatorForString("""
[document (title dt) (import "other.txt")]
""")

    val i = ee.s(defaultFile())
    val r = ee.e.evaluate(i, defaultFile())
    r as KSSuccess<KSBlockDocumentWithSections<KSEvaluation>, *>
    Assert.assertEquals(1, r.result.data.context.imports.size)
  }

  @Test fun testFormalItemContentFootnoteRefBug44() {
    val ee = newEvaluatorForString("""
[document (title dt)
  (section [title s]
    [formal-item (title q)
      (footnote-ref nonexistent)])]
""")

    val i = ee.s(defaultFile())
    val r = ee.e.evaluate(i, defaultFile())
    r as KSFailure
  }
}
