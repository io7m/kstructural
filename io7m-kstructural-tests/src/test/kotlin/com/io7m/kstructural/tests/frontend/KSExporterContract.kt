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

package com.io7m.kstructural.tests.frontend

import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument
import com.io7m.kstructural.core.evaluator.KSEvaluation
import com.io7m.kstructural.frontend.KSExporterType
import com.io7m.kstructural.frontend.KSInputFormat
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path

abstract class KSExporterContract {

  private val LOG = LoggerFactory.getLogger(KSExporterContract::class.java)

  abstract fun newFilesystem() : FileSystem

  var filesystem : FileSystem? = null
  var in_dir : Path? = null
  var out_dir : Path? = null

  abstract fun newExporter(f : KSInputFormat) : KSExporterType

  abstract fun parse(file : Path) : KSBlockDocument<KSEvaluation>

  @Before fun setupFilesystem() : Unit {
    val fs = newFilesystem()
    val id = fs.getPath("/input")
    val od = fs.getPath("/output")
    Files.createDirectories(id)
    Files.createDirectories(od);
    this.filesystem = fs
    this.in_dir = id
    this.out_dir = od
  }

  @After fun tearDownFilesystem() : Unit {
    this.filesystem!!.close()
  }

  @Test fun testImportsCanonical() {
    val main = this.in_dir!!.resolve("main.sd")
    val other = this.in_dir!!.resolve("other.sd")

    write(other, """[paragraph p]""")
    write(main, """
[document [title d0]
  [section [title t0]
    [import "other.sd"]]]
    """)

    val doc = parse(main)
    val e = newExporter(KSInputFormat.KS_INPUT_CANONICAL);
    e.export(in_dir, doc, out_dir, true)

    val out_main = out_dir!!.resolve("main.sd")
    val out_other = out_dir!!.resolve("other.sd")
    Assert.assertTrue(Files.exists(out_main))
    Assert.assertTrue(Files.exists(out_other))

    assertFileContents(out_main, """
[document
  [title d0]
  [section
    [title t0]
    [import "other.sd"]]]
""".trim())

    assertFileContents(out_other, """
[paragraph
  p]
""".trim())
  }

  private fun assertFileContents(p : Path, text : String) {
    val t = String(Files.readAllBytes(p), StandardCharsets.UTF_8).trim()
    Assert.assertEquals(text, t)
  }

  @Test fun testImportsImperative() {
    val main = this.in_dir!!.resolve("main.sd")
    val other = this.in_dir!!.resolve("other.sd")

    write(other, """[paragraph p]""")
    write(main, """
[document [title d0]
  [section [title t0]
    [import "other.sd"]]]
    """)

    val doc = parse(main)
    val e = newExporter(KSInputFormat.KS_INPUT_IMPERATIVE);
    e.export(in_dir, doc, out_dir, true)

    val out_main = out_dir!!.resolve("main.sdi")
    val out_other = out_dir!!.resolve("other.sdi")
    Assert.assertTrue(Files.exists(out_main))
    Assert.assertTrue(Files.exists(out_other))

    assertFileContents(out_main, """
[document [title d0]]
[section [title t0]]
[import "other.sdi"]
""".trim())

    assertFileContents(out_other, """
[paragraph]
p
""".trim())
  }

  @Test fun testNoImportsCanonical() {
    val main = this.in_dir!!.resolve("main.sd")
    val other = this.in_dir!!.resolve("other.sd")

    write(other, """[paragraph p]""")
    write(main, """
[document [title d0]
  [section [title t0]
    [import "other.sd"]]]
    """)

    val doc = parse(main)
    val e = newExporter(KSInputFormat.KS_INPUT_CANONICAL);
    e.export(in_dir, doc, out_dir, false)

    val out_main = out_dir!!.resolve("main.sd")
    val out_other = out_dir!!.resolve("other.sd")
    Assert.assertTrue(Files.exists(out_main))
    Assert.assertFalse(Files.exists(out_other))

    assertFileContents(out_main, """
[document
  [title d0]
  [section
    [title t0]
    [paragraph
      p]]]
""".trim())
  }

  @Test fun testNoImportsImperative() {
    val main = this.in_dir!!.resolve("main.sd")
    val other = this.in_dir!!.resolve("other.sd")

    write(other, """[paragraph p]""")
    write(main, """
[document [title d0]
  [section [title t0]
    [import "other.sd"]]]
    """)

    val doc = parse(main)
    val e = newExporter(KSInputFormat.KS_INPUT_IMPERATIVE);
    e.export(in_dir, doc, out_dir, false)

    val out_main = out_dir!!.resolve("main.sdi")
    val out_other = out_dir!!.resolve("other.sdi")
    Assert.assertTrue(Files.exists(out_main))
    Assert.assertFalse(Files.exists(out_other))

    assertFileContents(out_main, """
[document [title d0]]
[section [title t0]]
[paragraph]
p
""".trim())
  }

  private fun write(p : Path, s : String) {
    Files.write(p, s.toByteArray(StandardCharsets.UTF_8))
  }
}