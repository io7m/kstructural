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

package com.io7m.kstructural.tests.xom

import com.io7m.kstructural.core.KSElement
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSParseContext
import com.io7m.kstructural.core.KSParseError
import com.io7m.kstructural.core.KSParserDriverType
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.tests.parser.imperative.KSImperativeParserDriverContract
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path

abstract class KSXOMParserDriverContract {

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
    filesystem!!.getPath("/other").toAbsolutePath().resolve("file.xml")

  protected fun defaultFile() =
    filesystem!!.getPath("file.xml")

  protected fun baseFile() =
    filesystem!!.getPath("/base").toAbsolutePath().resolve("file.xml")

  protected fun baseDirectory() =
    filesystem!!.getPath("/base").toAbsolutePath()

  protected fun rootDirectory() =
    filesystem!!.rootDirectories.first()!!

  private fun showErrors(r : KSResult.KSFailure<KSElement.KSBlock<KSParse>, KSParseError>) {
    r.errors.forEachIndexed { i, e ->
      LOG.error("[{}]: {}", i, e)
    }
  }

  private fun write(s : String, file : Path) {
    if (file.parent != null) Files.createDirectories(file.parent)
    Files.write(file.toAbsolutePath(), s.toByteArray(StandardCharsets.UTF_8))
  }

  @Test fun testErrorIncludeOutsideBase() {
    write("""<?xml version="1.0" encoding="UTF-8"?>
<s:paragraph xmlns:s="http://schemas.io7m.com/structural/3.0.0">
</s:paragraph>
""", otherFile())

    write("""<?xml version="1.0" encoding="UTF-8"?>
<s:subsection s:title="S" xmlns:s="http://schemas.io7m.com/structural/3.0.0">
  <xi:include xmlns:xi="http://www.w3.org/2001/XInclude" href="/other/file.xml"/>
</s:subsection>
""", baseFile())

    val d = newDriver()
    val c = KSParseContext.empty(baseDirectory())
    val r = d.parseBlock(c, baseFile())

    r as KSResult.KSFailure
    showErrors(r)
    Assert.assertEquals(1, r.errors.size)
  }

  @Test fun testErrorIncludeNetwork() {
    write("""<?xml version="1.0" encoding="UTF-8"?>
<s:subsection s:title="S" xmlns:s="http://schemas.io7m.com/structural/3.0.0">
  <xi:include xmlns:xi="http://www.w3.org/2001/XInclude" href="http://malware.io7m.com"/>
</s:subsection>
""", baseFile())

    val d = newDriver()
    val c = KSParseContext.empty(baseDirectory())
    val r = d.parseBlock(c, baseFile())

    r as KSResult.KSFailure
    showErrors(r)
    Assert.assertEquals(1, r.errors.size)
  }

  @Test fun testIncludeOK() {
    write("""<?xml version="1.0" encoding="UTF-8"?>
<s:paragraph xmlns:s="http://schemas.io7m.com/structural/3.0.0">
</s:paragraph>
""", otherFile())

    write("""<?xml version="1.0" encoding="UTF-8"?>
<s:subsection s:title="S" xmlns:s="http://schemas.io7m.com/structural/3.0.0">
  <xi:include xmlns:xi="http://www.w3.org/2001/XInclude" href="/other/file.xml"/>
</s:subsection>
""", baseFile())

    val d = newDriver()
    val c = KSParseContext.empty(rootDirectory())
    val r = d.parseBlock(c, baseFile())

    r as KSResult.KSSuccess<KSElement.KSBlock.KSBlockSubsection<KSParse>, KSParseError>
  }
}