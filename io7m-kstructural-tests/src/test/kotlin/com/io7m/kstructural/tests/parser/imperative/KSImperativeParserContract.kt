/*
 * Copyright © 2016 <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DIKSLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.kstructural.tests.parser.imperative

import com.io7m.kstructural.core.KSElement
import com.io7m.kstructural.core.KSElement.*
import com.io7m.kstructural.core.KSElement.KSInline.*
import com.io7m.kstructural.core.KSParseContext
import com.io7m.kstructural.core.KSParseError
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.core.KSResult.*
import com.io7m.kstructural.parser.KSBlockParserType
import com.io7m.kstructural.parser.KSExpression
import com.io7m.kstructural.parser.imperative.KSImperative
import com.io7m.kstructural.parser.imperative.KSImperative.*
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.*
import com.io7m.kstructural.parser.imperative.KSImperativeParserType
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.nio.file.FileSystem

abstract class KSImperativeParserContract {

  protected abstract fun newParserForString(text : String) : Parser

  protected abstract fun newFilesystem() : FileSystem

  protected var filesystem : FileSystem? = null

  @Before fun setupFilesystem() : Unit {
    this.filesystem = newFilesystem()
  }

  @After fun tearDownFilesystem() : Unit {
    this.filesystem!!.close()
  }

  protected fun defaultFile() = filesystem!!.getPath("file.txt")

  data class Parser(
    val p : KSImperativeParserType,
    val s : () -> KSExpression)

  @Test fun testInlineError() {
    val pp = newParserForString("[nonexistent]")
    val e = pp.p.parse(KSParseContext.empty(), pp.s.invoke(), defaultFile())

    e as KSFailure
  }

  @Test fun testInlineSymbol() {
    val pp = newParserForString("x")
    val e = pp.p.parse(KSParseContext.empty(), pp.s.invoke(), defaultFile())

    e as KSSuccess<KSImperativeInline, KSParseError>
    val er = e.result.value as KSInlineText
    Assert.assertEquals("x", er.text)
    Assert.assertFalse(er.quote)
  }

  @Test fun testInlineQuoted() {
    val pp = newParserForString("\"x\"")
    val e = pp.p.parse(KSParseContext.empty(), pp.s.invoke(), defaultFile())

    e as KSSuccess<KSImperativeInline, KSParseError>
    val er = e.result.value as KSInlineText
    Assert.assertEquals("x", er.text)
    Assert.assertTrue(er.quote)
  }

  @Test fun testPara() {
    val pp = newParserForString("[paragraph]")
    val e = pp.p.parse(KSParseContext.empty(), pp.s.invoke(), defaultFile())

    e as KSSuccess<KSImperativeParagraph, KSParseError>
    Assert.assertFalse(e.result.id.isPresent)
    Assert.assertFalse(e.result.type.isPresent)
  }

  @Test fun testParaError() {
    val pp = newParserForString("[paragraph x]")
    val e = pp.p.parse(KSParseContext.empty(), pp.s.invoke(), defaultFile())

    e as KSFailure
  }

  @Test fun testParaID() {
    val pp = newParserForString("[paragraph [id x]]")
    val e = pp.p.parse(KSParseContext.empty(), pp.s.invoke(), defaultFile())

    e as KSSuccess<KSImperativeParagraph, KSParseError>
    Assert.assertTrue(e.result.id.isPresent)
    Assert.assertEquals("x", e.result.id.get().value)
    Assert.assertFalse(e.result.type.isPresent)
  }

  @Test fun testParaIDType() {
    val pp = newParserForString("[paragraph [id x] [type t]]")
    val e = pp.p.parse(KSParseContext.empty(), pp.s.invoke(), defaultFile())

    e as KSSuccess<KSImperativeParagraph, KSParseError>
    Assert.assertTrue(e.result.id.isPresent)
    Assert.assertEquals("x", e.result.id.get().value)
    Assert.assertEquals("t", e.result.type.get())
  }

  @Test fun testParaTypeID() {
    val pp = newParserForString("[paragraph [type t] [id x]]")
    val e = pp.p.parse(KSParseContext.empty(), pp.s.invoke(), defaultFile())

    e as KSSuccess<KSImperativeParagraph, KSParseError>
    Assert.assertTrue(e.result.id.isPresent)
    Assert.assertEquals("x", e.result.id.get().value)
    Assert.assertEquals("t", e.result.type.get())
  }

  @Test fun testParaType() {
    val pp = newParserForString("[paragraph [type t]]")
    val e = pp.p.parse(KSParseContext.empty(), pp.s.invoke(), defaultFile())

    e as KSSuccess<KSImperativeParagraph, KSParseError>
    Assert.assertFalse(e.result.id.isPresent)
    Assert.assertEquals("t", e.result.type.get())
  }
}