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

import nu.xom.Element
import nu.xom.Node
import nu.xom.Text

object KSXOMSpacing {

  private fun inlinesAppendConditionalSpace(
    node_current : Node,
    node_previous : Node)
    : Boolean
  {
    if (node_previous is Text && node_current is Text) {
      return true
    }

    if (!(node_previous is Text) && node_current is Text) {
      return (node_current.value.length > 1)
    }

    return true
  }

  fun <T> appendWithSpace(
    e : Element,
    q : List<T>,
    f : (T) -> Node) : Element {

    val es = q.map { k -> f(k) }
    val max = q.size - 1
    for (i in 0 .. max) {
      val e_now = es[i]

      if (i > 0) {
        if (inlinesAppendConditionalSpace(
          node_current = e_now,
          node_previous = es[i - 1])) {
          e.appendChild(" ")
        }
      }

      e.appendChild(e_now)
    }

    return e
  }

}