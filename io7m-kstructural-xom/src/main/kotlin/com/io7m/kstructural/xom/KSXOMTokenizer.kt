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

package com.io7m.kstructural.xom

import nu.xom.Node
import nu.xom.Text
import java.util.regex.Pattern

object KSXOMTokenizer {

  val WHITESPACE = Pattern.compile("\\s+")

  fun tokenizeText(n : Text) : List<Text> {
    val st = n.value.trim()
    val ns = st.split(WHITESPACE)
    return ns.filter { s -> !s.isEmpty() }.map { s -> Text(s) }
  }

  fun tokenizeNodes(n : List<Node>) : List<Node> {
    val xs = mutableListOf<Node>()
    for (i in 0 .. n.size - 1) {
      val cn = n[i]
      if (cn is Text) {
        xs.addAll(tokenizeText(cn))
      } else {
        xs.add(cn)
      }
    }
    return xs
  }

}