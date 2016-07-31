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

package com.io7m.kstructural.tests.plain

import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithSections
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection.KSBlockSectionWithContent
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionParagraph
import com.io7m.kstructural.core.evaluator.KSEvaluation
import com.io7m.kstructural.plain.KSPlainLayoutType
import com.io7m.kstructural.tests.core.KSEvaluatorContract
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import java.nio.file.FileSystem

abstract class KSPlainContract {

  private val LOG = LoggerFactory.getLogger(KSPlainContract::class.java)

  protected abstract fun newFilesystem() : FileSystem

  private var filesystem : FileSystem? = null

  @Before fun setupFilesystem() : Unit {
    this.filesystem = newFilesystem()
  }

  @After fun tearDownFilesystem() : Unit {
    this.filesystem!!.close()
  }

  protected abstract fun layout() : KSPlainLayoutType

  private fun defaultFile() = filesystem!!.getPath("file.txt")

  protected abstract fun newEvaluatorForString(
    text : String) : KSEvaluatorContract.Evaluator

  @Test fun testParagraph() {
    val la = layout()
    val ee = newEvaluatorForString("""
[document (title dt) (id d0)
  (section [title st] [id s0]
    [paragraph p])]
""")

    val r = ee.e.evaluate(ee.s(defaultFile()), defaultFile())
    r as KSResult.KSSuccess

    val d = r.result as KSBlockDocumentWithSections<KSEvaluation>
    val s = d.content[0] as KSBlockSectionWithContent<KSEvaluation>
    val p = s.content[0] as KSSubsectionParagraph<KSEvaluation>
    val x = la.layoutParagraph(80, p.paragraph)

    System.out.println(x)
  }
}