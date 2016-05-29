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

import com.io7m.kstructural.xom.KSXOMTokenizer
import nu.xom.Element
import nu.xom.Text
import org.junit.Assert
import org.junit.Test

class KSXOMTokenizerTest {

  @Test
  fun testTokenizeText() : Unit {
    val r = KSXOMTokenizer.tokenizeText(Text(" x y  z "))
    Assert.assertEquals(3, r.size)
    Assert.assertEquals("x", r[0].value)
    Assert.assertEquals("y", r[1].value)
    Assert.assertEquals("z", r[2].value)
  }

  @Test
  fun testTokenizeNodes() : Unit {
    val r = KSXOMTokenizer.tokenizeNodes(listOf(
      Text(" x y  z "),
      Element("z"),
      Text(" a b  c ")))
    Assert.assertEquals(3 + 1 + 3, r.size)
    Assert.assertEquals("x", r[0].value)
    Assert.assertEquals("y", r[1].value)
    Assert.assertEquals("z", r[2].value)
    Assert.assertTrue(r[3] is Element)
    Assert.assertEquals("a", r[4].value)
    Assert.assertEquals("b", r[5].value)
    Assert.assertEquals("c", r[6].value)
  }

}