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

package com.io7m.kstructural.tests.pretty.canon

import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineText
import com.io7m.kstructural.core.evaluator.KSEvaluation
import com.io7m.kstructural.tests.pretty.imperative.KSImperativePrettyPrinterContract
import org.apache.commons.io.IOUtils
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

abstract class KSCanonPrettyPrinterContract {

  private val LOG = LoggerFactory.getLogger(KSCanonPrettyPrinterContract::class.java)

  protected abstract fun newFilesystem() : FileSystem

  protected var filesystem : FileSystem? = null

  @Before fun setupFilesystem() : Unit {
    this.filesystem = newFilesystem()
  }

  @After fun tearDownFilesystem() : Unit {
    this.filesystem!!.close()
  }

  protected fun defaultFile() = filesystem!!.getPath("/file.txt")

  fun roundTrip(
    text : String,
    imports : Boolean) : Unit =
    roundTripExpanded(text, text, imports)

  fun roundTripExpanded(
    text_start : String,
    text_expect : String,
    imports : Boolean) : Unit {

    val path = defaultFile()
    Files.newOutputStream(path,
      StandardOpenOption.TRUNCATE_EXISTING,
      StandardOpenOption.CREATE).use { os ->
      IOUtils.write(text_start, os, StandardCharsets.UTF_8)
    }

    val before = parse(path)
    val serial0 = serialize(before, imports)
    val after = parse(path)
    val serial1 = serialize(after, imports)

    LOG.trace("text        : {}", text_start)
    LOG.trace("text_expect : {}", text_expect)
    LOG.trace("before      : {}", before)
    LOG.trace("serial0     : {}", serial0)
    LOG.trace("after       : {}", after)
    LOG.trace("serial1     : {}", serial1)

    Assert.assertEquals(text_expect, serial0)
    Assert.assertEquals(serial0, serial1)
  }

  abstract fun serialize(
    text : KSBlockDocument<KSEvaluation>,
    imports : Boolean) : String

  abstract fun parse(file : Path) : KSBlockDocument<KSEvaluation>

  @Test fun testTitles() {
    roundTrip("""
[document
  [title d0 d1 d2]
  [part
    [title p0 p1 p2]
    [section
      [title s0 s1 s2]
      [paragraph
        p]]]
  [part
    [title p0 p1 p2]
    [section
      [title s0 s1 s2]
      [subsection
        [title ss0 ss1 ss2]
        [paragraph
          p]]]]]
""".trim(), imports = false)
  }

  @Test fun testTitlesIds() {
    roundTrip("""
[document
  [title d0 d1 d2]
  [id d0]
  [part
    [title p0 p1 p2]
    [id p0]
    [section
      [title s0 s1 s2]
      [id p0s0]
      [paragraph
        p]]]
  [part
    [title p0 p1 p2]
    [id p1]
    [section
      [title s0 s1 s2]
      [id p1s0]
      [subsection
        [title ss0 ss1 ss2]
        [id p1s0ss0]
        [paragraph
          p]]]]]
""".trim(), imports = false)
  }

  @Test fun testText() {
    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [paragraph
      p0 p1 p2]]]
""".trim(), imports = false)
  }

  @Test fun testTextSpacing() {
    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [paragraph
      p0 "  p1  " p2]]]
""".trim(), imports = false)
  }

  @Test fun testTerm() {
    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [paragraph
      [term t]]]]
""".trim(), imports = false)
  }

  @Test fun testTermType() {
    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [paragraph
      [term [type q] t]]]]
""".trim(), imports = false)
  }

  @Test fun testInclude() {
    Files.newOutputStream(filesystem!!.getPath("other.txt")).use { os ->
      os.write("Hello".toByteArray(StandardCharsets.UTF_8))
    }

    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [paragraph
      [include "other.txt"]]]]
""".trim(), imports = true)
  }

  @Test fun testIncludeExpanded() {
    Files.newOutputStream(filesystem!!.getPath("other.txt")).use { os ->
      os.write("Hello A B C D".toByteArray(StandardCharsets.UTF_8))
    }

    roundTripExpanded("""
[document
  [title d]
  [section
    [title s]
    [paragraph
      [include "other.txt"]]]]
""".trim(), """
[document
  [title d]
  [section
    [title s]
    [paragraph
      "Hello A B C D"]]]
""".trim(), imports = false)
  }

  @Test fun testImport() {
    Files.newOutputStream(filesystem!!.getPath("other.txt")).use { os ->
      os.write("[paragraph p]".toByteArray(StandardCharsets.UTF_8))
    }

    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [import "other.txt"]]]
""".trim(), imports = true)
  }

  @Test fun testImportExpanded() {
    Files.newOutputStream(filesystem!!.getPath("other.txt")).use { os ->
      os.write("[paragraph p]".toByteArray(StandardCharsets.UTF_8))
    }

    roundTripExpanded("""
[document
  [title d]
  [section
    [title s]
    [import "other.txt"]]]
""".trim(), """
[document
  [title d]
  [section
    [title s]
    [paragraph
      p]]]
""".trim(), imports = false)
  }

  @Test fun testImage() {
    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [paragraph
      [image [target "x"] x]]]]
""".trim(), imports = false)
  }

  @Test fun testImageType() {
    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [paragraph
      [image [target "x"] [type q] x]]]]
""".trim(), imports = false)
  }

  @Test fun testImageSize() {
    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [paragraph
      [image [target "x"] [size 23 24] x]]]]
""".trim(), imports = false)
  }

  @Test fun testImageTypeSize() {
    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [paragraph
      [image [target "x"] [type z] [size 23 24] x]]]]
""".trim(), imports = false)
  }

  @Test fun testVerbatim() {
    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [paragraph
      [verbatim "v"]]]]
""".trim(), imports = false)
  }

  @Test fun testVerbatimType() {
    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [paragraph
      [verbatim [type t] "v"]]]]
""".trim(), imports = false)
  }

  @Test fun testLinkInternal() {
    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [id z]
    [paragraph
      [link [target z] x]]]]
""".trim(), imports = false)
  }

  @Test fun testLinkInternalImage() {
    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [id z]
    [paragraph
      [link [target z] [image [target "z"] x]]]]]
""".trim(), imports = false)
  }

  @Test fun testLinkInternalInclude() {
    Files.newOutputStream(filesystem!!.getPath("other.txt")).use { os ->
      os.write("Hello".toByteArray(StandardCharsets.UTF_8))
    }

    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [id z]
    [paragraph
      [link [target z] [include "other.txt"]]]]]
""".trim(), imports = true)
  }

  @Test fun testLinkExternal() {
    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [id z]
    [paragraph
      [link-ext [target "z"] x]]]]
""".trim(), imports = false)
  }

  @Test fun testLinkExternalImage() {
    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [id z]
    [paragraph
      [link-ext [target "z"] [image [target "z"] x]]]]]
""".trim(), imports = false)
  }

  @Test fun testLinkExternalInclude() {
    Files.newOutputStream(filesystem!!.getPath("other.txt")).use { os ->
      os.write("Hello".toByteArray(StandardCharsets.UTF_8))
    }

    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [id z]
    [paragraph
      [link-ext [target "z"] [include "other.txt"]]]]]
""".trim(), imports = true)
  }

  @Test fun testListOrdered() {
    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [paragraph
      [list-ordered
        [item x0 x1 x2]
        [item y0 y1 y2]
        [item z0 z1 z2]]]]]
""".trim(), imports = false)
  }

  @Test fun testListUnordered() {
    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [paragraph
      [list-unordered
        [item x0 x1 x2]
        [item y0 y1 y2]
        [item z0 z1 z2]]]]]
""".trim(), imports = false)
  }

  @Test fun testFootnote() {
    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [footnote
      [id f0]
      x y z]]]
""".trim(), imports = false)
  }

  @Test fun testFootnoteRef() {
    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [footnote
      [id f0]
      [footnote-ref f0]]]]
""".trim(), imports = false)
  }

  @Test fun testFootnoteType() {
    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [footnote
      [id f0]
      [type t]
      x y z]]]
""".trim(), imports = false)
  }

  @Test fun testFormalItemType() {
    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [formal-item
      [title f0]
      [type t]
      x y z]]]
""".trim(), imports = false)
  }

  @Test fun testFormalItemID() {
    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [formal-item
      [title f0]
      [id f0]
      x y z]]]
""".trim(), imports = false)
  }

  @Test fun testFormalItemIDType() {
    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [formal-item
      [title f0]
      [id f0]
      [type t]
      x y z]]]
""".trim(), imports = false)
  }

  @Test fun testTable() {
    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [paragraph
      [table
        [summary x y z]
        [body
          [row
            [cell x]
            [cell y]
            [cell z]]
          [row
            [cell x]
            [cell y]
            [cell z]]]]]]]
""".trim(), imports = false)
  }

  @Test fun testTableHead() {
    roundTrip("""
[document
  [title d]
  [section
    [title s]
    [paragraph
      [table
        [summary x y z]
        [head
          [type a]
          [name t]
          [name [type b] u]
          [name v]]
        [body
          [row
            [type d]
            [cell x]
            [cell [type c] y]
            [cell z]]
          [row
            [cell x]
            [cell y]
            [cell z]]]]]]]
""".trim(), imports = false)
  }

  @Test fun testSquareRound() {
    roundTrip("""
(document
  [title d0 d1 d2]
  [id d0]
  [part
    [title p0 p1 p2]
    [id p0]
    (section
      [title s0 s1 s2]
      [id p0s0]
      [paragraph
        p])]
  [part
    [title p0 p1 p2]
    [id p1]
    (section
      [title s0 s1 s2]
      [id p1s0]
      [subsection
        [title ss0 ss1 ss2]
        [id p1s0ss0]
        (paragraph
          p)])])
""".trim(), imports = false)
  }

  @Test fun testQuote() {
    roundTripExpanded("""
[document
  [title d]
  [section
    [title s]
    [paragraph
      [term "\"t\""]]]]
""".trim(),
      """
[document
  [title d]
  [section
    [title s]
    [paragraph
      [term "\"t\""]]]]
""".trim(),
      imports = false)
  }

  @Test fun testEscape0_Bug51() {

    val text_start = """
[document [title "(x"]
  [section [title s]
    [paragraph p]]]
"""

    val path = defaultFile()
    Files.newOutputStream(path).use { os ->
      IOUtils.write(text_start, os, StandardCharsets.UTF_8)
    }

    val d : KSBlockDocument.KSBlockDocumentWithSections<KSEvaluation> =
      parse(path) as KSBlockDocument.KSBlockDocumentWithSections<KSEvaluation>

    val title = d.title.map { t ->
      KSInlineText(t.position, t.square, t.data, false, t.text)
    }

    val dx = KSBlockDocument.KSBlockDocumentWithSections(
      d.position, d.square, d.data, d.id, d.type, title, d.content)

    val s = serialize(dx, false)
    Files.newOutputStream(path).use { os ->
      IOUtils.write(s, os, StandardCharsets.UTF_8)
    }

    parse(defaultFile())
  }

  @Test fun testEscape1_Bug51() {

    val text_start = """
[document [title "[x"]
  [section [title s]
    [paragraph p]]]
"""

    val path = defaultFile()
    Files.newOutputStream(path).use { os ->
      IOUtils.write(text_start, os, StandardCharsets.UTF_8)
    }

    val d : KSBlockDocument.KSBlockDocumentWithSections<KSEvaluation> =
      parse(path) as KSBlockDocument.KSBlockDocumentWithSections<KSEvaluation>

    val title = d.title.map { t ->
      KSInlineText(t.position, t.square, t.data, false, t.text)
    }

    val dx = KSBlockDocument.KSBlockDocumentWithSections(
      d.position, d.square, d.data, d.id, d.type, title, d.content)

    val s = serialize(dx, false)
    Files.newOutputStream(path).use { os ->
      IOUtils.write(s, os, StandardCharsets.UTF_8)
    }

    parse(defaultFile())
  }

  @Test fun testEscape2_Bug51() {

    val text_start = """
[document [title "x)"]
  [section [title s]
    [paragraph p]]]
"""

    val path = defaultFile()
    Files.newOutputStream(path).use { os ->
      IOUtils.write(text_start, os, StandardCharsets.UTF_8)
    }

    val d : KSBlockDocument.KSBlockDocumentWithSections<KSEvaluation> =
      parse(path) as KSBlockDocument.KSBlockDocumentWithSections<KSEvaluation>

    val title = d.title.map { t ->
      KSInlineText(t.position, t.square, t.data, false, t.text)
    }

    val dx = KSBlockDocument.KSBlockDocumentWithSections(
      d.position, d.square, d.data, d.id, d.type, title, d.content)

    val s = serialize(dx, false)
    Files.newOutputStream(path).use { os ->
      IOUtils.write(s, os, StandardCharsets.UTF_8)
    }

    parse(defaultFile())
  }

  @Test fun testEscape3_Bug51() {

    val text_start = """
[document [title "x]"]
  [section [title s]
    [paragraph p]]]
"""

    val path = defaultFile()
    Files.newOutputStream(path).use { os ->
      IOUtils.write(text_start, os, StandardCharsets.UTF_8)
    }

    val d : KSBlockDocument.KSBlockDocumentWithSections<KSEvaluation> =
      parse(path) as KSBlockDocument.KSBlockDocumentWithSections<KSEvaluation>

    val title = d.title.map { t ->
      KSInlineText(t.position, t.square, t.data, false, t.text)
    }

    val dx = KSBlockDocument.KSBlockDocumentWithSections(
      d.position, d.square, d.data, d.id, d.type, title, d.content)

    val s = serialize(dx, false)
    Files.newOutputStream(path).use { os ->
      IOUtils.write(s, os, StandardCharsets.UTF_8)
    }

    parse(defaultFile())
  }
}