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

import com.io7m.kstructural.core.KSBlock.KSBlockDocument
import com.io7m.kstructural.core.KSBlock.KSBlockDocument.KSBlockDocumentWithParts
import com.io7m.kstructural.core.KSBlock.KSBlockDocument.KSBlockDocumentWithSections
import com.io7m.kstructural.core.KSBlock.KSBlockSection.KSBlockSectionWithContent
import com.io7m.kstructural.core.KSBlock.KSBlockSection.KSBlockSectionWithSubsections
import com.io7m.kstructural.core.KSResult.KSFailure
import com.io7m.kstructural.core.KSResult.KSSuccess
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionParagraph
import com.io7m.kstructural.core.evaluator.KSEvaluation
import com.io7m.kstructural.core.evaluator.KSEvaluatorType
import org.junit.Assert
import org.junit.Test

abstract class KSEvaluatorContract {

  protected abstract fun newEvaluatorForString(
    text : String) : KSEvaluatorContract.Evaluator

  data class Evaluator(
    val e : KSEvaluatorType,
    val s : () -> KSBlockDocument<Unit>)

  @Test fun testDuplicateID()
  {
    val ee = newEvaluatorForString("""
[document (title dt) (id d0)
  (section [title st] [id d0]
    [paragraph p])]
""")

    val r = ee.e.evaluate(ee.s())
    r as KSFailure
    Assert.assertEquals(1, r.errors.size)
  }

  @Test fun testNonexistentID()
  {
    val ee = newEvaluatorForString("""
[document (title dt) (id d0)
  (section [title st]
    [paragraph (link [target d1] x)])]
""")

    val r = ee.e.evaluate(ee.s())
    r as KSFailure
    Assert.assertEquals(1, r.errors.size)
  }

  @Test fun testResolvedID()
  {
    val ee = newEvaluatorForString("""
[document (title dt) (id d0)
  (section [title st]
    [paragraph (link [target d0] x)])]
""")

    val i = ee.s()
    val r = ee.e.evaluate(i)
    r as KSSuccess<KSBlockDocumentWithSections<KSEvaluation>, *>
  }

  @Test fun testSectionNumber()
  {
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

    Assert.assertEquals("dt", r.result.title[0].text)

    run {
      val s = r.result.content[0] as KSBlockSectionWithContent
      Assert.assertEquals("s1", s.title[0].text)
      Assert.assertEquals(listOf(1L), s.data.number.get())
      Assert.assertEquals(3, s.content.size)
      val p1 = s.content[0] as KSSubsectionParagraph<KSEvaluation>
      Assert.assertEquals(listOf(1L, 1L), p1.paragraph.data.number.get())
      val p2 = s.content[1] as KSSubsectionParagraph<KSEvaluation>
      Assert.assertEquals(listOf(1L, 2L), p2.paragraph.data.number.get())
      val p3 = s.content[2] as KSSubsectionParagraph<KSEvaluation>
      Assert.assertEquals(listOf(1L, 3L), p3.paragraph.data.number.get())
    }

    run {
      val s = r.result.content[1] as KSBlockSectionWithContent
      Assert.assertEquals("s2", s.title[0].text)
      Assert.assertEquals(listOf(2L), s.data.number.get())
      Assert.assertEquals(3, s.content.size)
      val p1 = s.content[0] as KSSubsectionParagraph<KSEvaluation>
      Assert.assertEquals(listOf(2L, 1L), p1.paragraph.data.number.get())
      val p2 = s.content[1] as KSSubsectionParagraph<KSEvaluation>
      Assert.assertEquals(listOf(2L, 2L), p2.paragraph.data.number.get())
      val p3 = s.content[2] as KSSubsectionParagraph<KSEvaluation>
      Assert.assertEquals(listOf(2L, 3L), p3.paragraph.data.number.get())
    }

    run {
      val s = r.result.content[2] as KSBlockSectionWithContent
      Assert.assertEquals("s3", s.title[0].text)
      Assert.assertEquals(listOf(3L), s.data.number.get())
      Assert.assertEquals(3, s.content.size)
      val p1 = s.content[0] as KSSubsectionParagraph<KSEvaluation>
      Assert.assertEquals(listOf(3L, 1L), p1.paragraph.data.number.get())
      val p2 = s.content[1] as KSSubsectionParagraph<KSEvaluation>
      Assert.assertEquals(listOf(3L, 2L), p2.paragraph.data.number.get())
      val p3 = s.content[2] as KSSubsectionParagraph<KSEvaluation>
      Assert.assertEquals(listOf(3L, 3L), p3.paragraph.data.number.get())
    }
  }

  @Test fun testSectionSubsectionNumber()
  {
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

    Assert.assertEquals("dt", r.result.title[0].text)

    run {
      val s = r.result.content[0] as KSBlockSectionWithSubsections
      Assert.assertEquals("s1", s.title[0].text)
      Assert.assertEquals(listOf(1L), s.data.number.get())
      Assert.assertEquals(2, s.content.size)

      run {
        val ss = s.content[0]
        Assert.assertEquals("ss1", ss.title[0].text)
        Assert.assertEquals(listOf(1L, 1L), ss.data.number.get())
        val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(listOf(1L, 1L, 1L), p1.paragraph.data.number.get())
        val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(listOf(1L, 1L, 2L), p2.paragraph.data.number.get())
      }

      run {
        val ss = s.content[1]
        Assert.assertEquals("ss2", ss.title[0].text)
        Assert.assertEquals(listOf(1L, 2L), ss.data.number.get())
        val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(listOf(1L, 2L, 1L), p1.paragraph.data.number.get())
        val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(listOf(1L, 2L, 2L), p2.paragraph.data.number.get())
      }
    }

    run {
      val s = r.result.content[1] as KSBlockSectionWithSubsections
      Assert.assertEquals("s2", s.title[0].text)
      Assert.assertEquals(listOf(2L), s.data.number.get())
      Assert.assertEquals(2, s.content.size)

      run {
        val ss = s.content[0]
        Assert.assertEquals("ss1", ss.title[0].text)
        Assert.assertEquals(listOf(2L, 1L), ss.data.number.get())
        val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(listOf(2L, 1L, 1L), p1.paragraph.data.number.get())
        val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(listOf(2L, 1L, 2L), p2.paragraph.data.number.get())
      }

      run {
        val ss = s.content[1]
        Assert.assertEquals("ss2", ss.title[0].text)
        Assert.assertEquals(listOf(2L, 2L), ss.data.number.get())
        val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(listOf(2L, 2L, 1L), p1.paragraph.data.number.get())
        val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(listOf(2L, 2L, 2L), p2.paragraph.data.number.get())
      }
    }
  }

  @Test fun testPartSectionNumber()
  {
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

    Assert.assertEquals("dt", r.result.title[0].text)

    run {
      val p = r.result.content[0]
      Assert.assertEquals("p1", p.title[0].text)
      Assert.assertEquals(listOf(1L), p.data.number.get())
      Assert.assertEquals(2, p.content.size)

      run {
        val s = p.content[0] as KSBlockSectionWithContent<KSEvaluation>
        Assert.assertEquals("s1", s.title[0].text)
        Assert.assertEquals(listOf(1L, 1L), s.data.number.get())
        Assert.assertEquals(2, s.content.size)

        val p1 = s.content[0] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(listOf(1L, 1L, 1L), p1.paragraph.data.number.get())
        val p2 = s.content[1] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(listOf(1L, 1L, 2L), p2.paragraph.data.number.get())
      }

      run {
        val s = p.content[1] as KSBlockSectionWithContent<KSEvaluation>
        Assert.assertEquals("s2", s.title[0].text)
        Assert.assertEquals(listOf(1L, 2L), s.data.number.get())
        Assert.assertEquals(2, s.content.size)

        val p1 = s.content[0] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(listOf(1L, 2L, 1L), p1.paragraph.data.number.get())
        val p2 = s.content[1] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(listOf(1L, 2L, 2L), p2.paragraph.data.number.get())
      }
    }

    run {
      val p = r.result.content[1]
      Assert.assertEquals("p2", p.title[0].text)
      Assert.assertEquals(listOf(2L), p.data.number.get())
      Assert.assertEquals(2, p.content.size)

      run {
        val s = p.content[0] as KSBlockSectionWithContent<KSEvaluation>
        Assert.assertEquals("s1", s.title[0].text)
        Assert.assertEquals(listOf(2L, 1L), s.data.number.get())
        Assert.assertEquals(2, s.content.size)

        val p1 = s.content[0] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(listOf(2L, 1L, 1L), p1.paragraph.data.number.get())
        val p2 = s.content[1] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(listOf(2L, 1L, 2L), p2.paragraph.data.number.get())
      }

      run {
        val s = p.content[1] as KSBlockSectionWithContent<KSEvaluation>
        Assert.assertEquals("s2", s.title[0].text)
        Assert.assertEquals(listOf(2L, 2L), s.data.number.get())
        Assert.assertEquals(2, s.content.size)

        val p1 = s.content[0] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(listOf(2L, 2L, 1L), p1.paragraph.data.number.get())
        val p2 = s.content[1] as KSSubsectionParagraph<KSEvaluation>
        Assert.assertEquals(listOf(2L, 2L, 2L), p2.paragraph.data.number.get())
      }
    }
  }

  @Test fun testPartSectionSubsectionNumber()
  {
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

    Assert.assertEquals("dt", r.result.title[0].text)

    run {
      val p = r.result.content[0]
      Assert.assertEquals("p1", p.title[0].text)
      Assert.assertEquals(listOf(1L), p.data.number.get())
      Assert.assertEquals(2, p.content.size)

      run {
        val s = p.content[0] as KSBlockSectionWithSubsections<KSEvaluation>
        Assert.assertEquals("s1", s.title[0].text)
        Assert.assertEquals(listOf(1L, 1L), s.data.number.get())
        Assert.assertEquals(2, s.content.size)

        run {
          val ss = s.content[0]
          Assert.assertEquals("ss1", ss.title[0].text)
          val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(listOf(1L, 1L, 1L, 1L), p1.paragraph.data.number.get())
          val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(listOf(1L, 1L, 1L, 2L), p2.paragraph.data.number.get())
        }

        run {
          val ss = s.content[1]
          Assert.assertEquals("ss2", ss.title[0].text)
          val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(listOf(1L, 1L, 2L, 1L), p1.paragraph.data.number.get())
          val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(listOf(1L, 1L, 2L, 2L), p2.paragraph.data.number.get())
        }
      }

      run {
        val s = p.content[1] as KSBlockSectionWithSubsections<KSEvaluation>
        Assert.assertEquals("s2", s.title[0].text)
        Assert.assertEquals(listOf(1L, 2L), s.data.number.get())
        Assert.assertEquals(2, s.content.size)

        run {
          val ss = s.content[0]
          Assert.assertEquals("ss1", ss.title[0].text)
          val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(listOf(1L, 2L, 1L, 1L), p1.paragraph.data.number.get())
          val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(listOf(1L, 2L, 1L, 2L), p2.paragraph.data.number.get())
        }

        run {
          val ss = s.content[1]
          Assert.assertEquals("ss2", ss.title[0].text)
          val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(listOf(1L, 2L, 2L, 1L), p1.paragraph.data.number.get())
          val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(listOf(1L, 2L, 2L, 2L), p2.paragraph.data.number.get())
        }
      }
    }

    run {
      val p = r.result.content[1]
      Assert.assertEquals("p2", p.title[0].text)
      Assert.assertEquals(listOf(2L), p.data.number.get())
      Assert.assertEquals(2, p.content.size)

      run {
        val s = p.content[0] as KSBlockSectionWithSubsections<KSEvaluation>
        Assert.assertEquals("s1", s.title[0].text)
        Assert.assertEquals(listOf(2L, 1L), s.data.number.get())
        Assert.assertEquals(2, s.content.size)

        run {
          val ss = s.content[0]
          Assert.assertEquals("ss1", ss.title[0].text)
          val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(listOf(2L, 1L, 1L, 1L), p1.paragraph.data.number.get())
          val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(listOf(2L, 1L, 1L, 2L), p2.paragraph.data.number.get())
        }

        run {
          val ss = s.content[1]
          Assert.assertEquals("ss2", ss.title[0].text)
          val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(listOf(2L, 1L, 2L, 1L), p1.paragraph.data.number.get())
          val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(listOf(2L, 1L, 2L, 2L), p2.paragraph.data.number.get())
        }
      }

      run {
        val s = p.content[1] as KSBlockSectionWithSubsections<KSEvaluation>
        Assert.assertEquals("s2", s.title[0].text)
        Assert.assertEquals(listOf(2L, 2L), s.data.number.get())
        Assert.assertEquals(2, s.content.size)

        run {
          val ss = s.content[0]
          Assert.assertEquals("ss1", ss.title[0].text)
          val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(listOf(2L, 2L, 1L, 1L), p1.paragraph.data.number.get())
          val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(listOf(2L, 2L, 1L, 2L), p2.paragraph.data.number.get())
        }

        run {
          val ss = s.content[1]
          Assert.assertEquals("ss2", ss.title[0].text)
          val p1 = ss.content[0] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(listOf(2L, 2L, 2L, 1L), p1.paragraph.data.number.get())
          val p2 = ss.content[1] as KSSubsectionParagraph<KSEvaluation>
          Assert.assertEquals(listOf(2L, 2L, 2L, 2L), p2.paragraph.data.number.get())
        }
      }
    }
  }
}
