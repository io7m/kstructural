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

package com.io7m.kstructural.core

object KSTextUtilities {

  fun <T> concatenateInto(sb : StringBuilder, xs : List<T>) : Unit {
    val max = xs.size - 1
    for (i in 0 .. max) {
      sb.append(xs[i])
      if (i < max) {
        sb.append(" ")
      }
    }
  }

  fun <T> concatenate(xs : List<T>) : String {
    val sb = StringBuilder()
    val max = xs.size - 1
    for (i in 0 .. max) {
      sb.append(xs[i])
      if (i < max) {
        sb.append(" ")
      }
    }
    return sb.toString()
  }

}
