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

package com.io7m.kstructural.plain

import com.io7m.jorchard.core.JOTreeNodeType
import com.io7m.jptbox.core.JPTextBoxDrawing
import com.io7m.jptbox.core.JPTextImage
import com.io7m.jptbox.core.JPTextImageType
import java.util.LinkedList

object KSPlainRasterizer {

  private class Node(
    val node : JOTreeNodeType<KSPlainLayoutBox>,
    val x : Int,
    val y : Int)

  fun rasterize(
    layout : JOTreeNodeType<KSPlainLayoutBox>) : JPTextImageType {

    val box = layout.value().box
    val image = JPTextImage.create(box.width(), box.height())
    val draw = JPTextBoxDrawing.get()

    val stack = LinkedList<Node>()
    stack.push(Node(layout, 0, 0))

    while (!stack.isEmpty()) {
      val next = stack.pop()
      val layout = next.node.value()
      val box = layout.box
      val lines = layout.lines
      val lbox = layout.border

      for (y in 0 .. lines.size - 1) {
        val line = lines[y]
        for (x in 0 .. line.length - 1) {
          val image_x = Math.addExact(next.x, x)
          val image_y = Math.addExact(next.y, y)
          image.put(image_x, image_y, line.codePointAt(x))
        }
      }

      when (lbox) {
        KSPlainBorder.None  -> {

        }

        KSPlainBorder.Light -> {
          draw.drawBox(image, next.x, next.y, box.width(), box.height())
        }
      }

      val next_children = next.node.children()
      for (child in next_children) {
        val x = Math.addExact(child.value().box.minimumX(), next.x)
        val y = Math.addExact(child.value().box.minimumY(), next.y)
        stack.push(Node(child, x, y))
      }
    }

    return image
  }

}