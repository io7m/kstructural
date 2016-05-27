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
import com.io7m.kstructural.core.KSParserDriverType
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.core.KSResult.KSFailure
import com.io7m.kstructural.core.KSResult.KSSuccess
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionFootnote
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionFormalItem
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionParagraph
import com.io7m.kstructural.core.KSType
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
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Optional
import java.util.logging.Logger

abstract class KSImperativeParserDriverContract {

  private val LOG = LoggerFactory.getLogger(
    KSImperativeParserDriverContract::class.java)

  protected abstract fun newDriver() : KSParserDriverType

  protected abstract fun newFilesystem() : FileSystem

  protected var filesystem : FileSystem? = null

  @Before fun setupFilesystem() : Unit {
    this.filesystem = newFilesystem()
  }

  @After fun tearDownFilesystem() : Unit {
    this.filesystem!!.close()
  }

  protected fun otherFile() =
    filesystem!!.getPath("/other").toAbsolutePath().resolve("file.sd")

  protected fun defaultFile() =
    filesystem!!.getPath("file.txt")

  protected fun rootDirectory() =
    filesystem!!.rootDirectories.first()!!

  @Test fun testErrorUnexpectedEOF() {
    write("""""", defaultFile())

    val d = newDriver()
    val c = KSParseContext.empty(rootDirectory())
    val r = d.parseBlock(c, defaultFile())

    r as KSFailure
    showErrors(r)
    Assert.assertEquals(1, r.errors.size)
  }

  @Test fun testErrorSomewhere() {
    write("""
[section [title x]]
[paragraph]
[paragraph p]
[paragraph]
""", defaultFile())

    val d = newDriver()
    val c = KSParseContext.empty(rootDirectory())
    val r = d.parseBlock(c, defaultFile())

    r as KSFailure
    showErrors(r)
    Assert.assertEquals(1, r.errors.size)
  }

  @Test fun testErrorWrongContent() {
    write("""
[section [title x]]
[paragraph]
[subsection [title k]]
[paragraph]
""", defaultFile())

    val d = newDriver()
    val c = KSParseContext.empty(rootDirectory())
    val r = d.parseBlock(c, defaultFile())

    r as KSFailure
    showErrors(r)
    Assert.assertEquals(1, r.errors.size)
  }

  @Test fun testErrorNoInlineIntermediates() {
    write("""
[section [title x]]
[paragraph]
[subsection [title q]]
a b c d e f g h i j k l m n o p q r s t u v w x y z.
[paragraph]
""", defaultFile())

    val d = newDriver()
    val c = KSParseContext.empty(rootDirectory())
    val r = d.parseBlock(c, defaultFile())

    r as KSFailure
    showErrors(r)
    Assert.assertEquals(1, r.errors.size)
  }

  @Test fun testErrorTooMany() {
    write("""
[section [title x]
  [paragraph]]
""", otherFile())

    write("""
[import "/other/file.sd"]
[import "/other/file.sd"]
""", defaultFile())

    val d = newDriver()
    val c = KSParseContext.empty(filesystem!!.getPath("/base"))
    val r = d.parseBlock(c, defaultFile())

    r as KSFailure
    showErrors(r)
    Assert.assertEquals(1, r.errors.size)
  }

  @Test fun testErrorOutsideBase() {
    write("""
[section [title x]
  [paragraph]]
""", otherFile())

    val cfile = filesystem!!.getPath("/base/file.txt")
    write("""
[import "/other/file.sd"]
""", cfile)

    val d = newDriver()
    val c = KSParseContext.empty(filesystem!!.getPath("/base"))
    val r = d.parseBlock(c, cfile)

    r as KSFailure
    showErrors(r)
    Assert.assertEquals(2, r.errors.size)
  }

  @Test fun testErrorOutsideBaseInline() {
    write("""Hello.""", otherFile())

    val cfile = filesystem!!.getPath("/base/file.txt")
    write("""
[paragraph]
[include "/other/file.sd"]
""", cfile)

    val d = newDriver()
    val c = KSParseContext.empty(filesystem!!.getPath("/base"))
    val r = d.parseBlock(c, cfile)

    r as KSFailure
    showErrors(r)
    Assert.assertEquals(1, r.errors.size)
  }

  @Test fun testOK() {
    val cfile = filesystem!!.getPath("/base/file.txt")
    write("""
[paragraph]
A B C
""", cfile)

    val d = newDriver()
    val c = KSParseContext.empty(filesystem!!.getPath("/base"))
    val r = d.parseBlock(c, cfile)

    r as KSSuccess<KSBlockParagraph<KSParse>, KSParseError>
  }

  private fun showErrors(r : KSFailure<KSBlock<KSParse>, KSParseError>) {
    r.errors.forEachIndexed { i, e ->
      LOG.error("[{}]: {}", i, e)
    }
  }

  private fun write(s : String, file : Path) {
    if (file.parent != null) Files.createDirectories(file.parent)
    Files.write(file.toAbsolutePath(), s.toByteArray(StandardCharsets.UTF_8))
  }

}