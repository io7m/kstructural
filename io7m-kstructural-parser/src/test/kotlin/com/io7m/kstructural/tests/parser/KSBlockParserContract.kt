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

package com.io7m.kstructural.tests.parser

import com.io7m.kstructural.core.KSBlock
import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSInline
import com.io7m.kstructural.core.KSSubsectionContent
import com.io7m.kstructural.parser.KSBlockParserType
import com.io7m.kstructural.parser.KSExpression
import com.io7m.kstructural.parser.KSParseResult.KSParseFailure
import com.io7m.kstructural.parser.KSParseResult.KSParseSuccess
import org.junit.Assert
import org.junit.Test
import java.util.Optional


abstract class KSBlockParserContract {

  protected abstract fun newParserForString(text : String) : Parser

  data class Parser(
    val p : KSBlockParserType,
    val s : () -> KSExpression)

  @Test fun testParaError0()
  {
    val pp = newParserForString("[paragraph [link]]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSParseFailure<KSBlock.KSBlockParagraph<*>>
    Assert.assertTrue(e.partial.isPresent)
    Assert.assertEquals(1, e.errors.size)
  }

  @Test fun testParaSimple()
  {
    val pp = newParserForString("[paragraph Hello.]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSParseSuccess<KSBlock.KSBlockParagraph<Unit>>
    Assert.assertEquals(1, e.result.content.size)
    val t0 = e.result.content[0] as KSInline.KSInlineText<Unit>
    Assert.assertEquals("Hello.", t0.text)
  }

  @Test fun testParaID()
  {
    val pp = newParserForString("[paragraph [id x] Hello.]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSParseSuccess<KSBlock.KSBlockParagraph<Unit>>
    Assert.assertEquals("x", e.result.id.get().value)
    Assert.assertEquals(1, e.result.content.size)
    val t0 = e.result.content[0] as KSInline.KSInlineText<Unit>
    Assert.assertEquals("Hello.", t0.text)
  }

  @Test fun testParaType()
  {
    val pp = newParserForString("[paragraph [type x] Hello.]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSParseSuccess<KSBlock.KSBlockParagraph<Unit>>
    Assert.assertEquals("x", e.result.type.get())
    Assert.assertEquals(1, e.result.content.size)
    val t0 = e.result.content[0] as KSInline.KSInlineText<Unit>
    Assert.assertEquals("Hello.", t0.text)
  }

  @Test fun testParaTypeID()
  {
    val pp = newParserForString("[paragraph [type x] [id y] Hello.]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSParseSuccess<KSBlock.KSBlockParagraph<Unit>>
    Assert.assertEquals("x", e.result.type.get())
    Assert.assertEquals("y", e.result.id.get().value)
    Assert.assertEquals(1, e.result.content.size)
    val t0 = e.result.content[0] as KSInline.KSInlineText<Unit>
    Assert.assertEquals("Hello.", t0.text)
  }

  @Test fun testParaIDType()
  {
    val pp = newParserForString("[paragraph [id y] [type x] Hello.]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSParseSuccess<KSBlock.KSBlockParagraph<Unit>>
    Assert.assertEquals("x", e.result.type.get())
    Assert.assertEquals("y", e.result.id.get().value)
    Assert.assertEquals(1, e.result.content.size)
    val t0 = e.result.content[0] as KSInline.KSInlineText<Unit>
    Assert.assertEquals("Hello.", t0.text)
  }

  @Test fun testSubsectionErrorEmpty()
  {
    val pp = newParserForString("[subsection]")

    val e = pp.p.parse(pp.s.invoke())
    e as KSParseFailure<KSBlock.KSBlockSubsection<*>>

    Assert.assertFalse(e.partial.isPresent)
    Assert.assertEquals(1, e.errors.size)
  }

  @Test fun testSubsectionErrorWrongContent()
  {
    val pp = newParserForString("[subsection [title t] [subsection [title w]]]")

    val e = pp.p.parse(pp.s.invoke())
    e as KSParseFailure<KSBlock.KSBlockSubsection<*>>

    Assert.assertTrue(e.partial.isPresent)
    Assert.assertEquals(1, e.errors.size)
  }

  @Test fun testSubsectionErrorWrongTitle()
  {
    val pp = newParserForString("[subsection [title x [term q]]]")

    val e = pp.p.parse(pp.s.invoke())
    e as KSParseFailure<KSBlock.KSBlockSubsection<*>>

    Assert.assertFalse(e.partial.isPresent)
    Assert.assertEquals(1, e.errors.size)
  }

  @Test fun testSubsection()
  {
    val pp = newParserForString("[subsection [title t]]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSParseSuccess<KSBlock.KSBlockSubsection<Unit>>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.empty<KSID<Unit>>(), e.result.id)
    Assert.assertEquals(0, e.result.content.size)
  }

  @Test fun testSubsectionID()
  {
    val pp = newParserForString("[subsection [title t] [id x]]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSParseSuccess<KSBlock.KSBlockSubsection<Unit>>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals("x", e.result.id.get().value)
    Assert.assertEquals(0, e.result.content.size)
  }

  @Test fun testSubsectionIDType()
  {
    val pp = newParserForString("[subsection [title t] [id x] [type k]]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSParseSuccess<KSBlock.KSBlockSubsection<Unit>>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals("x", e.result.id.get().value)
    Assert.assertEquals("k", e.result.type.get())
    Assert.assertEquals(0, e.result.content.size)
  }

  @Test fun testSubsectionTypeID()
  {
    val pp = newParserForString("[subsection [title t] [type k] [id x]]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSParseSuccess<KSBlock.KSBlockSubsection<Unit>>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals("x", e.result.id.get().value)
    Assert.assertEquals("k", e.result.type.get())
    Assert.assertEquals(0, e.result.content.size)
  }

  @Test fun testSubsectionType()
  {
    val pp = newParserForString("[subsection [title t] [type k]]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSParseSuccess<KSBlock.KSBlockSubsection<Unit>>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.empty<KSID<Unit>>(), e.result.id)
    Assert.assertEquals("k", e.result.type.get())
    Assert.assertEquals(0, e.result.content.size)
  }

  @Test fun testSubsectionContent()
  {
    val pp = newParserForString("[subsection [title t] [paragraph Hello.]]")
    val e = pp.p.parse(pp.s.invoke())

    e as KSParseSuccess<KSBlock.KSBlockSubsection<Unit>>
    Assert.assertEquals("t", e.result.title[0].text)
    Assert.assertEquals(Optional.empty<KSID<Unit>>(), e.result.id)
    Assert.assertEquals(1, e.result.content.size)
    val p = e.result.content[0] as KSSubsectionContent.KSSubsectionParagraph<Unit>
    val t = p.paragraph.content[0] as KSInline.KSInlineText<Unit>
    Assert.assertEquals("Hello.", t.text)
  }
}
