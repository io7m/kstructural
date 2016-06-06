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

package com.io7m.kstructural.tests.core

import com.io7m.kstructural.core.KSType
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import java.util.Optional

class KSTypeTest {

  @JvmField
  @Rule val expected = ExpectedException.none()

  @Test fun testValid0()
  {
    same("x")
  }

  @Test fun testValid1()
  {
    same("abcdefghijklmnopqrstuvwxyz0123456789_")
  }

  private fun same(name : String) {
    val x = KSType.create(Optional.empty(), name, Unit)
    val y = KSType.create(Optional.empty(), name, Unit)
    Assert.assertEquals(name, x.value)
    Assert.assertEquals(x, y)
  }

  @Test fun testInvalid0()
  {
    expected.expect(IllegalArgumentException::class.java)
    KSType.create(Optional.empty(), " ", Unit)
  }

  @Test fun testInvalid1()
  {
    expected.expect(IllegalArgumentException::class.java)
    KSType.create(Optional.empty(), "-", Unit)
  }
}