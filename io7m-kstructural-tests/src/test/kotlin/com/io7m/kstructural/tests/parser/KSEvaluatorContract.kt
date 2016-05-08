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

package com.io7m.kstructural.tests.parser

import com.io7m.kstructural.core.KSElement
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithParts
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithSections
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection.KSBlockSectionWithContent
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection.KSBlockSectionWithSubsections
import com.io7m.kstructural.core.KSResult.KSFailure
import com.io7m.kstructural.core.KSResult.KSSuccess
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionParagraph
import com.io7m.kstructural.core.evaluator.KSEvaluation
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
import org.junit.Assert
import org.junit.Test
import org.slf4j.LoggerFactory
import java.util.logging.Logger

abstract class KSEvaluatorContract {

  protected abstract fun newEvaluatorForString(
    text : String) : KSEvaluatorContract.Evaluator

  data class Evaluator(
    val e : KSEvaluatorType,
    val s : () -> KSBlockDocument<Unit>)

  @Test fun testDuplicateID() {
    val ee = newEvaluatorForString("""
[document (title dt) (id d0)
  (section [title st] [id d0]
    [paragraph p])]
""")

    val r = ee.e.evaluate(ee.s())
    r as KSFailure
    Assert.assertEquals(1, r.errors.size)
  }

  @Test fun testNonexistentID() {
    val ee = newEvaluatorForString("""
[document (title dt) (id d0)
  (section [title st]
    [paragraph (link [target d1] x)])]
""")

    val r = ee.e.evaluate(ee.s())
    r as KSFailure
    Assert.assertEquals(1, r.errors.size)
  }

  @Test fun testResolvedID() {
    val ee = newEvaluatorForString("""
[document (title dt) (id d0)
  (section [title st]
    [paragraph (link [target d0] x)])]
""")

    val i = ee.s()
    val r = ee.e.evaluate(i)
    r as KSSuccess<KSBlockDocumentWithSections<KSEvaluation>, *>
  }

  private val LOG = LoggerFactory.getLogger(KSEvaluatorContract::class.java)

  private fun checkSelf(e : KSElement<KSEvaluation>) : Unit {
    val c = e.data.context
    LOG.trace("checking self {} ({})", e.data.serial, e.javaClass.name)
    Assert.assertSame(e, c.elementForSerial(e.data.serial).get())
  }

  private fun checkParent(
    e : KSElement<KSEvaluation>,
    p : KSElement<KSEvaluation>) : Unit {
    val c = e.data.context
    LOG.trace("checking parent {} ({}) -> {} ({})",
      e.data.serial, e.javaClass.name, e.data.parent, p.javaClass.name)
    Assert.assertEquals(e.data.parent, p.data.serial)
    val k = c.elementForSerial(e.data.parent)
    Assert.assertTrue(k.isPresent)
    Assert.assertEquals(e.data.parent, k.get().data.serial)
    Assert.assertSame(p, k.get())
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

    val i = ee.s()
    val r = ee.e.evaluate(i)
    r as KSSuccess<KSBlockDocumentWithSections<KSEvaluation>, *>

    val ctx = r.result.data.context
    Assert.assertEquals(KSSerial(0L), r.result.data.parent)
    Assert.assertEquals("dt", r.result.title[0].text)
    Assert.assertFalse(ctx.elementSegmentUp(r.result).isPresent)
    Assert.assertFalse(ctx.elementSegmentPrevious(r.result).isPresent)
    checkSelf(r.result)

    run {
      val s = r.result.content[0] as KSBlockSectionWithContent
      checkSelf(s)
      checkParent(s, r.result)

      Assert.assertEquals("s1", s.title[0].text)
      Assert.assertEquals(KSNumberSection(1L), s.data.number.get())
      Assert.assertEquals(3, s.content.size)
      val p1 = s.content[0] as KSSubsectionParagraph<KSEvaluation>
      checkSelf(p1.paragraph)
      checkParent(p1.paragraph, s)
      Assert.assertEquals(KSNumberSectionContent(1L, 1L), p1.paragraph.data.number.get())
      val p2 = s.content[1] as KSSubsectionParagraph<KSEvaluation>
      checkSelf(p2.paragraph)
      checkParent(p2.paragraph, s)
      Assert.assertEquals(KSNumberSectionContent(1L, 2L), p2.paragraph.data.number.get())
      val p3 = s.content[2] as KSSubsectionParagraph<KSEvaluation>
      checkSelf(p3.paragraph)
      checkParent(p3.paragraph, s)
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
      checkSelf(s)
      checkParent(s, r.result)

      Assert.assertEquals("s2", s.title[0].text)
      Assert.assertEquals(KSNumberSection(2L), s.data.number.get())
      Assert.assertEquals(3, s.content.size)
      val p1 = s.content[0] as KSSubsectionParagraph<KSEvaluation>
      checkSelf(p1.paragraph)
      checkParent(p1.paragraph, s)
      Assert.assertEquals(KSNumberSectionContent(2L, 1L), p1.paragraph.data.number.get())
      val p2 = s.content[1] as KSSubsectionParagraph<KSEvaluation>
      checkSelf(p2.paragraph)
      checkParent(p2.paragraph, s)
      Assert.assertEquals(KSNumberSectionContent(2L, 2L), p2.paragraph.data.number.get())
      val p3 = s.content[2] as KSSubsectionParagraph<KSEvaluation>
      checkSelf(p3.paragraph)
      checkParent(p3.paragraph, s)
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
      checkSelf(s)
      checkParent(s, r.result)

      Assert.assertEquals("s3", s.title[0].text)
      Assert.assertEquals(KSNumberSection(3L), s.data.number.get())
      Assert.assertEquals(3, s.content.size)
      val p1 = s.content[0] as KSSubsectionParagraph<KSEvaluation>
      checkSelf(p1.paragraph)
      checkParent(p1.paragraph, s)
      Assert.assertEquals(KSNumberSectionContent(3L, 1L), p1.paragraph.data.number.get())
      val p2 = s.content[1] as KSSubsectionParagraph<KSEvaluation>
      checkSelf(p2.paragraph)
      checkParent(p2.paragraph, s)
      Assert.assertEquals(KSNumberSectionContent(3L, 2L), p2.paragraph.data.number.get())
      val p3 = s.content[2] as KSSubsectionParagraph<KSEvaluation>
      checkSelf(p3.paragraph)
      checkParent(p3.paragraph, s)
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

    val i = ee.s()
    val r = ee.e.evaluate(i)
    r as KSSuccess<KSBlockDocumentWithSections<KSEvaluation>, *>

    val ctx = r.result.data.context
    Assert.assertEquals("dt", r.result.title[0].text)
    Assert.assertFalse(ctx.elementSegmentUp(r.result).isPresent)
    Assert.assertFalse(ctx.elementSegmentPrevious(r.result).isPresent)
    Assert.assertEquals(r.result.content[0], ctx.elementSegmentNext(r.result).get())
    checkSelf(r.result)

    run {
      val s = r.result.content[0] as KSBlockSectionWithSubsections
      checkSelf(s)
      checkParent(s, r.result)

      Assert.assertEquals("s1", s.title[0].text)
      Assert.assertEquals(KSNumberSection(1L), s.data.number.get())
      Assert.assertEquals(2, s.content.size)
      Assert.assertSame(r.result, ctx.elementSegmentUp(s).get())
      Assert.assertSame(r.result, ctx.elementSegmentPrevious(s).get())
      Assert.assertSame(r.result.content[1], ctx.elementSegmentNext(s).get())

      run {
        val ss = s.content[0]
        checkSelf(ss)
        checkParent(ss, s)

        Assert.assertEquals("ss1", ss.title[0].text)
        Assert.assertEquals(KSNumberSectionSubsection(1L, 1L), ss.data.number.get())
        val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
        checkSelf(p1.paragraph)
        checkParent(p1.paragraph, ss)
        Assert.assertEquals(KSNumberSectionSubsectionContent(1L, 1L, 1L), p1.paragraph.data.number.get())
        val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
        checkSelf(p2.paragraph)
        checkParent(p2.paragraph, ss)
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
        checkSelf(ss)
        checkParent(ss, s)

        Assert.assertEquals("ss2", ss.title[0].text)
        Assert.assertEquals(KSNumberSectionSubsection(1L, 2L), ss.data.number.get())
        val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
        checkSelf(p1.paragraph)
        checkParent(p1.paragraph, ss)
        Assert.assertEquals(KSNumberSectionSubsectionContent(1L, 2L, 1L), p1.paragraph.data.number.get())
        val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
        checkSelf(p2.paragraph)
        checkParent(p2.paragraph, ss)
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
      checkSelf(s)
      checkParent(s, r.result)

      Assert.assertEquals("s2", s.title[0].text)
      Assert.assertEquals(KSNumberSection(2L), s.data.number.get())
      Assert.assertEquals(2, s.content.size)
      Assert.assertSame(r.result, ctx.elementSegmentUp(s).get())
      Assert.assertFalse(ctx.elementSegmentNext(s).isPresent)

      run {
        val ss = s.content[0]
        checkSelf(ss)
        checkParent(ss, s)

        Assert.assertEquals("ss1", ss.title[0].text)
        Assert.assertEquals(KSNumberSectionSubsection(2L, 1L), ss.data.number.get())
        val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
        checkSelf(p1.paragraph)
        checkParent(p1.paragraph, ss)
        Assert.assertEquals(KSNumberSectionSubsectionContent(2L, 1L, 1L), p1.paragraph.data.number.get())
        val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
        checkSelf(p2.paragraph)
        checkParent(p2.paragraph, ss)
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
        checkSelf(ss)
        checkParent(ss, s)

        Assert.assertEquals("ss2", ss.title[0].text)
        Assert.assertEquals(KSNumberSectionSubsection(2L, 2L), ss.data.number.get())
        val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
        checkSelf(p1.paragraph)
        checkParent(p1.paragraph, ss)
        Assert.assertEquals(KSNumberSectionSubsectionContent(2L, 2L, 1L), p1.paragraph.data.number.get())
        val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
        checkSelf(p2.paragraph)
        checkParent(p2.paragraph, ss)
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

    val i = ee.s()
    val r = ee.e.evaluate(i)
    r as KSSuccess<KSBlockDocumentWithParts<KSEvaluation>, *>

    val ctx = r.result.data.context
    Assert.assertEquals("dt", r.result.title[0].text)
    Assert.assertFalse(ctx.elementSegmentUp(r.result).isPresent)
    Assert.assertFalse(ctx.elementSegmentPrevious(r.result).isPresent)
    Assert.assertEquals(r.result.content[0], ctx.elementSegmentNext(r.result).get())
    checkSelf(r.result)

    run {
      val p = r.result.content[0]
      checkSelf(p)
      checkParent(p, r.result)

      Assert.assertEquals("p1", p.title[0].text)
      Assert.assertEquals(KSNumberPart(1L), p.data.number.get())
      Assert.assertEquals(2, p.content.size)
      Assert.assertSame(r.result, ctx.elementSegmentUp(p).get())
      Assert.assertSame(r.result, ctx.elementSegmentPrevious(p).get())
      Assert.assertSame(p, ctx.elementSegmentNext(r.result).get())

      run {
        val s = p.content[0] as KSBlockSectionWithContent<KSEvaluation>
        checkSelf(s)
        checkParent(s, p)

        Assert.assertEquals("s1", s.title[0].text)
        Assert.assertEquals(KSNumberPartSection(1L, 1L), s.data.number.get())
        Assert.assertEquals(2, s.content.size)

        val p1 = s.content[0] as KSSubsectionParagraph<KSEvaluation>
        checkSelf(p1.paragraph)
        checkParent(p1.paragraph, s)
        Assert.assertEquals(KSNumberPartSectionContent(1L, 1L, 1L), p1.paragraph.data.number.get())
        val p2 = s.content[1] as KSSubsectionParagraph<KSEvaluation>
        checkSelf(p2.paragraph)
        checkParent(p2.paragraph, s)
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
        checkSelf(s)
        checkParent(s, p)

        Assert.assertEquals("s2", s.title[0].text)
        Assert.assertEquals(KSNumberPartSection(1L, 2L), s.data.number.get())
        Assert.assertEquals(2, s.content.size)

        val p1 = s.content[0] as KSSubsectionParagraph<KSEvaluation>
        checkSelf(p1.paragraph)
        checkParent(p1.paragraph, s)
        Assert.assertEquals(KSNumberPartSectionContent(1L, 2L, 1L), p1.paragraph.data.number.get())
        val p2 = s.content[1] as KSSubsectionParagraph<KSEvaluation>
        checkSelf(p2.paragraph)
        checkParent(p2.paragraph, s)
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
      checkSelf(p)
      checkParent(p, r.result)

      Assert.assertEquals("p2", p.title[0].text)
      Assert.assertEquals(KSNumberPart(2L), p.data.number.get())
      Assert.assertEquals(2, p.content.size)
      Assert.assertSame(r.result, ctx.elementSegmentUp(p).get())
      Assert.assertSame(p.content[0], ctx.elementSegmentNext(p).get())

      run {
        val s = p.content[0] as KSBlockSectionWithContent<KSEvaluation>
        checkSelf(s)
        checkParent(s, p)

        Assert.assertEquals("s1", s.title[0].text)
        Assert.assertEquals(KSNumberPartSection(2L, 1L), s.data.number.get())
        Assert.assertEquals(2, s.content.size)

        val p1 = s.content[0] as KSSubsectionParagraph<KSEvaluation>
        checkSelf(p1.paragraph)
        checkParent(p1.paragraph, s)
        Assert.assertEquals(KSNumberPartSectionContent(2L, 1L, 1L), p1.paragraph.data.number.get())
        val p2 = s.content[1] as KSSubsectionParagraph<KSEvaluation>
        checkSelf(p2.paragraph)
        checkParent(p2.paragraph, s)
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
        checkSelf(s)
        checkParent(s, p)

        Assert.assertEquals("s2", s.title[0].text)
        Assert.assertEquals(KSNumberPartSection(2L, 2L), s.data.number.get())
        Assert.assertEquals(2, s.content.size)

        val p1 = s.content[0] as KSSubsectionParagraph<KSEvaluation>
        checkSelf(p1.paragraph)
        checkParent(p1.paragraph, s)
        Assert.assertEquals(KSNumberPartSectionContent(2L, 2L, 1L), p1.paragraph.data.number.get())
        val p2 = s.content[1] as KSSubsectionParagraph<KSEvaluation>
        checkSelf(p2.paragraph)
        checkParent(p2.paragraph, s)
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

    val i = ee.s()
    val r = ee.e.evaluate(i)
    r as KSSuccess<KSBlockDocumentWithParts<KSEvaluation>, *>

    val ctx = r.result.data.context
    Assert.assertEquals("dt", r.result.title[0].text)
    Assert.assertFalse(ctx.elementSegmentUp(r.result).isPresent)
    Assert.assertFalse(ctx.elementSegmentPrevious(r.result).isPresent)
    Assert.assertEquals(r.result.content[0], ctx.elementSegmentNext(r.result).get())
    checkSelf(r.result)

    run {
      val p = r.result.content[0]
      checkSelf(p)
      checkParent(p, r.result)

      Assert.assertEquals("p1", p.title[0].text)
      Assert.assertEquals(KSNumberPart(1L), p.data.number.get())
      Assert.assertEquals(2, p.content.size)
      Assert.assertSame(r.result, ctx.elementSegmentUp(p).get())
      Assert.assertSame(r.result, ctx.elementSegmentPrevious(p).get())
      Assert.assertSame(p, ctx.elementSegmentNext(r.result).get())

      run {
        val s = p.content[0] as KSBlockSectionWithSubsections<KSEvaluation>
        checkSelf(s)
        checkParent(s, p)

        Assert.assertEquals("s1", s.title[0].text)
        Assert.assertEquals(KSNumberPartSection(1L, 1L), s.data.number.get())
        Assert.assertEquals(2, s.content.size)
        Assert.assertSame(p, ctx.elementSegmentUp(s).get())
        Assert.assertSame(p, ctx.elementSegmentPrevious(s).get())
        Assert.assertSame(p.content[1], ctx.elementSegmentNext(s).get())

        run {
          val ss = s.content[0]
          checkSelf(ss)
          checkParent(ss, s)

          Assert.assertEquals("ss1", ss.title[0].text)
          val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
          checkSelf(p1.paragraph)
          checkParent(p1.paragraph, ss)
          Assert.assertEquals(KSNumberPartSectionSubsectionContent(1L, 1L, 1L, 1L), p1.paragraph.data.number.get())
          val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
          checkSelf(p2.paragraph)
          checkParent(p2.paragraph, ss)
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
          checkSelf(ss)
          checkParent(ss, s)

          Assert.assertEquals("ss2", ss.title[0].text)
          val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
          checkSelf(p1.paragraph)
          checkParent(p1.paragraph, ss)
          Assert.assertEquals(KSNumberPartSectionSubsectionContent(1L, 1L, 2L, 1L), p1.paragraph.data.number.get())
          val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
          checkSelf(p2.paragraph)
          checkParent(p2.paragraph, ss)
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
        checkSelf(s)
        checkParent(s, p)

        Assert.assertEquals("s2", s.title[0].text)
        Assert.assertEquals(KSNumberPartSection(1L, 2L), s.data.number.get())
        Assert.assertEquals(2, s.content.size)
        Assert.assertSame(p, ctx.elementSegmentUp(s).get())
        Assert.assertSame(r.result.content[1], ctx.elementSegmentNext(s).get())

        val s_prev = p.content[0]
        Assert.assertSame(s_prev, ctx.elementSegmentPrevious(s).get())

        run {
          val ss = s.content[0]
          checkSelf(ss)
          checkParent(ss, s)

          Assert.assertEquals("ss1", ss.title[0].text)
          val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
          checkSelf(p1.paragraph)
          checkParent(p1.paragraph, ss)
          Assert.assertEquals(KSNumberPartSectionSubsectionContent(1L, 2L, 1L, 1L), p1.paragraph.data.number.get())
          val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
          checkSelf(p2.paragraph)
          checkParent(p2.paragraph, ss)
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
          checkSelf(ss)
          checkParent(ss, s)

          Assert.assertEquals("ss2", ss.title[0].text)
          val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
          checkSelf(p1.paragraph)
          checkParent(p1.paragraph, ss)
          Assert.assertEquals(KSNumberPartSectionSubsectionContent(1L, 2L, 2L, 1L), p1.paragraph.data.number.get())
          val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
          checkSelf(p2.paragraph)
          checkParent(p2.paragraph, ss)
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
      checkSelf(p)
      checkParent(p, r.result)

      Assert.assertEquals("p2", p.title[0].text)
      Assert.assertEquals(KSNumberPart(2L), p.data.number.get())
      Assert.assertEquals(2, p.content.size)
      Assert.assertSame(p.content[0], ctx.elementSegmentNext(p).get())

      val p_prev = r.result.content[0]
      Assert.assertSame(p_prev, ctx.elementSegmentPrevious(p).get())

      run {
        val s = p.content[0] as KSBlockSectionWithSubsections<KSEvaluation>
        checkSelf(s)
        checkParent(s, p)

        Assert.assertEquals("s1", s.title[0].text)
        Assert.assertEquals(KSNumberPartSection(2L, 1L), s.data.number.get())
        Assert.assertEquals(2, s.content.size)
        Assert.assertSame(p, ctx.elementSegmentUp(s).get())
        Assert.assertSame(p, ctx.elementSegmentPrevious(s).get())
        Assert.assertSame(p.content[1], ctx.elementSegmentNext(s).get())

        run {
          val ss = s.content[0]
          checkSelf(ss)
          checkParent(ss, s)

          Assert.assertEquals("ss1", ss.title[0].text)
          val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
          checkSelf(p1.paragraph)
          checkParent(p1.paragraph, ss)
          Assert.assertEquals(KSNumberPartSectionSubsectionContent(2L, 1L, 1L, 1L), p1.paragraph.data.number.get())
          val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
          checkSelf(p2.paragraph)
          checkParent(p2.paragraph, ss)
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
          checkSelf(ss)
          checkParent(ss, s)

          Assert.assertEquals("ss2", ss.title[0].text)
          val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
          checkSelf(p1.paragraph)
          checkParent(p1.paragraph, ss)
          Assert.assertEquals(KSNumberPartSectionSubsectionContent(2L, 1L, 2L, 1L), p1.paragraph.data.number.get())
          val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
          checkSelf(p2.paragraph)
          checkParent(p2.paragraph, ss)
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
        checkSelf(s)
        checkParent(s, p)

        Assert.assertEquals("s2", s.title[0].text)
        Assert.assertEquals(KSNumberPartSection(2L, 2L), s.data.number.get())
        Assert.assertEquals(2, s.content.size)
        Assert.assertSame(p, ctx.elementSegmentUp(s).get())
        Assert.assertFalse(ctx.elementSegmentNext(s).isPresent)

        val s_prev = p.content[0]
        Assert.assertSame(s_prev, ctx.elementSegmentPrevious(s).get())

        run {
          val ss = s.content[0]
          checkSelf(ss)
          checkParent(ss, s)

          Assert.assertEquals("ss1", ss.title[0].text)
          val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
          checkSelf(p1.paragraph)
          checkParent(p1.paragraph, ss)
          Assert.assertEquals(KSNumberPartSectionSubsectionContent(2L, 2L, 1L, 1L), p1.paragraph.data.number.get())
          val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
          checkSelf(p2.paragraph)
          checkParent(p2.paragraph, ss)
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
          checkSelf(ss)
          checkParent(ss, s)

          Assert.assertEquals("ss2", ss.title[0].text)
          val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
          checkSelf(p1.paragraph)
          checkParent(p1.paragraph, ss)
          Assert.assertEquals(KSNumberPartSectionSubsectionContent(2L, 2L, 2L, 1L), p1.paragraph.data.number.get())
          val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
          checkSelf(p2.paragraph)
          checkParent(p2.paragraph, ss)
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

    val i = ee.s()
    val r = ee.e.evaluate(i)
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

    val i = ee.s()
    val r = ee.e.evaluate(i)
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

    val i = ee.s()
    val r = ee.e.evaluate(i)
    r as KSFailure
    Assert.assertEquals(1, r.errors.size)
  }
}
