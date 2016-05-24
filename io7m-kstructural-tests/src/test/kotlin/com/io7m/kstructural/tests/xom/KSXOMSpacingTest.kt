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

import com.io7m.kstructural.xom.KSXOMSpacing
import nu.xom.Document
import nu.xom.Element
import nu.xom.Serializer
import nu.xom.Text
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class KSXOMSpacingTest {

  fun <A> identity(x : A) : A = x

  @Test fun testTextText() {
    val e = Element("x")
    val k = listOf(Text("of"), Text("a"))

    KSXOMSpacing.appendWithSpace(e, k, { x -> identity(x) })
    val t = serialize(e)
    Assert.assertEquals("""<?xml version="1.0" encoding="UTF-8"?>
<x>of a</x>
""", t)
  }

  @Test fun testTextTerm() {
    val e = Element("x")
    val k = listOf(Text("of"), Element("else"))

    KSXOMSpacing.appendWithSpace(e, k, { x -> identity(x) })
    val t = serialize(e)
    Assert.assertEquals("""<?xml version="1.0" encoding="UTF-8"?>
<x>of <else/></x>
""", t)
  }

  @Test fun testTermText() {
    val e = Element("x")
    val k = listOf(Element("else"), Text("of"))

    KSXOMSpacing.appendWithSpace(e, k, { x -> identity(x) })
    val t = serialize(e)
    Assert.assertEquals("""<?xml version="1.0" encoding="UTF-8"?>
<x><else/> of</x>
""", t)
  }

  @Test fun testTermText1() {
    val e = Element("x")
    val k = listOf(Element("else"), Text("."))

    KSXOMSpacing.appendWithSpace(e, k, { x -> identity(x) })
    val t = serialize(e)
    Assert.assertEquals("""<?xml version="1.0" encoding="UTF-8"?>
<x><else/>.</x>
""", t)
  }

  @Test fun testTermTerm() {
    val e = Element("x")
    val k = listOf(Element("else"), Element("else"))

    KSXOMSpacing.appendWithSpace(e, k, { x -> identity(x) })
    val t = serialize(e)
    Assert.assertEquals("""<?xml version="1.0" encoding="UTF-8"?>
<x><else/> <else/></x>
""", t)
  }

  private fun serialize(e : Element) : String {
    val bao = ByteArrayOutputStream(1024)
    val s = Serializer(bao, "UTF-8")
    s.indent = 0
    s.maxLength = 9999
    s.lineSeparator = "\n"
    s.write(Document(e))
    s.flush()
    return String(bao.toByteArray(), StandardCharsets.UTF_8)
  }

}