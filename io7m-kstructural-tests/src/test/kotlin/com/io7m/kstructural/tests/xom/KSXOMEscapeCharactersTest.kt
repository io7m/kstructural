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

import com.io7m.kstructural.xom.KSXOMEscapeCharacters
import nu.xom.Element
import org.junit.Assert
import org.junit.Test

class KSXOMEscapeCharactersTest {

  @Test fun testAllCharacters()
  {
    val sb = StringBuilder(1)
    val e = Element("e")
    for (i in 0 .. 0x10FFFF) {
      sb.setLength(0)
      sb.appendCodePoint(i)
      e.appendChild(KSXOMEscapeCharacters.filterXML10(sb.toString()))
    }

    Assert.assertEquals(0x10FFFF + 1, e.childCount)
  }

}