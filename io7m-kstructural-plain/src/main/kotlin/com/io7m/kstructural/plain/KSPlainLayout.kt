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

import com.io7m.jboxes.core.Box
import com.io7m.jboxes.core.BoxMutable
import com.io7m.jboxes.core.BoxType
import com.io7m.jboxes.core.Boxes
import com.io7m.jorchard.core.JOTreeNode
import com.io7m.jorchard.core.JOTreeNodeReadableType
import com.io7m.jorchard.core.JOTreeNodeType
import com.io7m.jpita.core.JPAlignerBasic
import com.io7m.kstructural.core.KSElement
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockFootnote
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockFormalItem
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockParagraph
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineFootnoteReference
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineImage
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineInclude
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineLink
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineListOrdered
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineListUnordered
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineTable
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineTerm
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineText
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineVerbatim
import com.io7m.kstructural.core.KSElement.KSInline.KSTableHeadColumnName
import com.io7m.kstructural.core.evaluator.KSEvaluation
import org.slf4j.LoggerFactory
import org.valid4j.Assertive
import java.lang.Math.addExact
import java.lang.Math.max

object KSPlainLayout : KSPlainLayoutType {

  private val LOG = LoggerFactory.getLogger(KSPlainLayout::class.java)

  private fun <T> measureTableCountColumns(
    table : KSInlineTable<T>) : Int {
    return table.head.map { head ->
      head.column_names.size
    }.orElse(table.body.rows.fold(0, { count, row ->
      max(count, row.cells.size)
    }))
  }

  private fun createNode(
    width : Int,
    border : KSPlainBorder = KSPlainBorder.None) : JOTreeNodeType<KSPlainLayoutBox> {
    val box = Boxes.create<Any>(0, 0, width, 1)
    val lbox = layoutBoxFrom(box, border)
    return JOTreeNode.create(lbox)
  }

  private fun layoutBoxFrom(
    box : BoxType<Any>,
    border : KSPlainBorder = KSPlainBorder.None) =
    KSPlainLayoutBox(mutableBoxFrom(box), border, mutableListOf())

  private fun mutableBoxFrom(box : BoxType<Any>) =
    BoxMutable.create<Any>().from(box)

  override fun layoutFormal(
    page_width : Int,
    paragraph : KSBlockFormalItem<KSEvaluation>) : JOTreeNodeType<KSPlainLayoutBox> {

    val contents = paragraph.content

    /**
     * Split the original layout into a fixed width margin and a body
     * column.
     */

    val margin_width = 6
    val layout = createNode(page_width)
    val initial_split = Boxes.splitAlongVertical(layout.value().box, margin_width)
    val initial_margin = initial_split.left()
    val initial_body = initial_split.right()

    /**
     * Add nodes for the margin and body text.
     */

    val margin_node = JOTreeNode.create(layoutBoxFrom(initial_margin))
    layout.childAdd(margin_node)
    val body_node = JOTreeNode.create(layoutBoxFrom(initial_body))
    layout.childAdd(body_node)

    /**
     * Process the inline content.
     */

    inlines(body_node, contents)

    /**
     * Resize the height of the margin to match the body.
     */

    val margin_mbox = margin_node.value().box
    margin_mbox.from(Boxes.create(
      margin_mbox.minimumX(),
      margin_mbox.minimumY(),
      margin_mbox.width(),
      body_node.value().box.height()))

    /**
     * Make the whole layout contain the margin and body.
     */

    val layout_mbox = layout.value().box
    layout_mbox.from(Boxes.containing(margin_mbox, body_node.value().box))
    return layout
  }

  override fun layoutParagraph(
    page_width : Int,
    paragraph : KSBlockParagraph<KSEvaluation>) : JOTreeNodeType<KSPlainLayoutBox> {

    val contents = paragraph.content

    /**
     * Split the original layout into a fixed width margin and a body
     * column.
     */

    val margin_width = 6
    val layout = createNode(page_width)
    val initial_split = Boxes.splitAlongVertical(layout.value().box, margin_width)
    val initial_margin = initial_split.left()
    val initial_body = initial_split.right()

    /**
     * Add nodes for the margin and body text.
     */

    val margin_node = JOTreeNode.create(layoutBoxFrom(initial_margin))
    layout.childAdd(margin_node)
    val body_node = JOTreeNode.create(layoutBoxFrom(initial_body))
    layout.childAdd(body_node)

    /**
     * Put the paragraph number into the margin.
     */

    margin_node.value().lines.add(paragraphNumber(paragraph, margin_width))

    /**
     * Process the inline content.
     */

    inlines(body_node, contents)

    /**
     * Resize the height of the margin to match the body.
     */

    val margin_mbox = margin_node.value().box
    margin_mbox.from(Boxes.create(
      margin_mbox.minimumX(),
      margin_mbox.minimumY(),
      margin_mbox.width(),
      body_node.value().box.height()))

    /**
     * Make the whole layout contain the margin and body.
     */

    val layout_mbox = layout.value().box
    layout_mbox.from(Boxes.containing(margin_mbox, body_node.value().box))
    return layout
  }

  override fun layoutFootnote(
    page_width : Int,
    footnote : KSBlockFootnote<KSEvaluation>)
    : JOTreeNodeType<KSPlainLayoutBox> {

    val contents = footnote.content

    /**
     * Split the original layout into a fixed width margin and a body
     * column.
     */

    val margin_width = 6
    val layout = createNode(page_width)
    val initial_split = Boxes.splitAlongVertical(layout.value().box, margin_width)
    val initial_margin = initial_split.left()
    val initial_body = initial_split.right()

    /**
     * Add nodes for the margin and body text.
     */

    val margin_node = JOTreeNode.create(layoutBoxFrom(initial_margin))
    layout.childAdd(margin_node)
    val body_node = JOTreeNode.create(layoutBoxFrom(initial_body))
    layout.childAdd(body_node)

    /**
     * Put the footnote number into the margin.
     */

    margin_node.value().lines.add("[" + footnote.data.index + "]")

    /**
     * Process the inline content.
     */

    inlines(body_node, contents)

    /**
     * Resize the height of the margin to match the body.
     */

    val margin_mbox = margin_node.value().box
    margin_mbox.from(Boxes.create(
      margin_mbox.minimumX(),
      margin_mbox.minimumY(),
      margin_mbox.width(),
      body_node.value().box.height()))

    /**
     * Make the whole layout contain the margin and body.
     */

    val layout_mbox = layout.value().box
    layout_mbox.from(Boxes.containing(margin_mbox, body_node.value().box))
    return layout
  }

  private fun inlines(
    container_node : JOTreeNodeType<KSPlainLayoutBox>,
    contents : List<KSElement.KSInline<KSEvaluation>>) : Unit {

    /**
     * Construct boxes for sets of inline elements. A box is completed
     * when encountering an inline element that needs to appear in its
     * own box (such as verbatim text, tables, etc). Generally, anything
     * that would be rendered in its own block by a browser in HTML.
     */

    val subnodes = mutableListOf<JOTreeNodeType<KSPlainLayoutBox>>()
    val aligned = JPAlignerBasic.create(container_node.value().box.width())

    fun finishLines() {
      val lines = aligned.finish()
      val width = lines.fold(0, { width, line -> max(width, line.length) })
      val ibox = Boxes.create<Any>(0, 0, width, lines.size)
      subnodes.add(JOTreeNode.create(
        KSPlainLayoutBox(mutableBoxFrom(ibox), KSPlainBorder.None, lines)))
    }

    contents.forEach { content ->
      when (content) {
        is KSInlineLink              ->
          KSInlineRenderer.link(content).forEach { word ->
            aligned.addWord(word)
          }

        is KSInlineText              ->
          KSInlineRenderer.text(content).forEach { word ->
            aligned.addWord(word)
          }

        is KSInlineTerm              ->
          content.content.forEach { text ->
            KSInlineRenderer.text(text).forEach { word ->
              aligned.addWord(word)
            }
          }

        is KSInlineFootnoteReference ->
          KSInlineRenderer.footnoteReference(content).forEach { word ->
            aligned.addWord(word)
          }

        is KSInlineImage             ->
          KSInlineRenderer.image(content).forEach { word ->
            aligned.addWord(word)
          }

        is KSInlineVerbatim          -> {
          finishLines()
          subnodes.add(verbatim(content))
        }

        is KSInlineListOrdered       -> {
          finishLines()
          subnodes.add(listOrdered(container_node, content))
        }

        is KSInlineListUnordered     -> {
          finishLines()
          subnodes.add(listUnordered(container_node, content))
        }

        is KSInlineTable             -> {
          finishLines()
          subnodes.add(table(container_node, content))
        }

        is KSInlineInclude           -> {

        }
      }
    }

    finishLines()

    /**
     * Sum the heights of all of the boxes in the body, and move each box
     * below the previous one.
     */

    val container_mbox = container_node.value().box
    var accum_max_x = container_mbox.maximumX()
    var accum_max_y = 0

    subnodes.forEach { node ->
      val node_mbox = node.value().box
      accum_max_y = Math.addExact(node_mbox.minimumY(), accum_max_y)
      val aligned = Boxes.moveAbsolute(node_mbox, 0, accum_max_y)
      accum_max_y = Math.addExact(node_mbox.height(), accum_max_y)
      node_mbox.from(aligned)
      accum_max_x = max(accum_max_x, addExact(container_mbox.minimumX(), aligned.width()))
      container_node.childAdd(node)
    }

    /**
     * Resize the body such that it tightly contains all of the boxes.
     */

    container_mbox.from(Box.of(
      container_mbox.minimumX(),
      accum_max_x,
      container_mbox.minimumY(),
      accum_max_y))
  }

  private class TableDimensions(
    val column_widths : List<Int>,
    val head_height : Int,
    val row_heights : List<Int>)

  private fun tableColumnDimensions(
    container_node : JOTreeNodeReadableType<KSPlainLayoutBox>,
    table : KSInlineTable<KSEvaluation>) : TableDimensions {

    val container_box = container_node.value().box
    val container_width = container_box.width()
    val column_widths = mutableListOf<Int>()
    val column_count = measureTableCountColumns(table)
    val column_width_average = container_width / column_count
    val row_count = table.body.rows.size
    val row_heights = mutableListOf<Int>()
    var head_height = 1

    for (row_index in 0 .. row_count - 1) {
      row_heights.add(row_index, 1)
    }

    for (column_index in 0 .. column_count - 1) {
      column_widths.add(column_index, column_width_average)

      table.head.map { head ->
        if (column_index < head.column_names.size) {
          val name = head.column_names[column_index]
          val name_node = tableColumnName(container_node, name)
          val name_width = name_node.value().box.width() + 2
          column_widths.set(column_index, name_width)
          head_height = name_node.value().box.height()
        }
      }

      for (row_index in 0 .. row_count - 1) {
        val row = table.body.rows[row_index]
        if (column_index < row.cells.size) {
          val cell = row.cells[column_index]
          val cell_node = createNode(column_widths[column_index])
          inlines(cell_node, cell.content)

          val cell_box = cell_node.value().box
          val cell_height = cell_box.height()
          val cell_width = cell_box.width() + 3

          row_heights[row_index] =
            max(row_heights[row_index], cell_height)
          column_widths[column_index] =
            max(column_widths[column_index], cell_width)
        }
      }
    }

    return TableDimensions(
      column_widths = column_widths,
      head_height = head_height,
      row_heights = row_heights)
  }

  private fun table(
    container_node : JOTreeNodeReadableType<KSPlainLayoutBox>,
    table : KSInlineTable<KSEvaluation>) : JOTreeNodeType<KSPlainLayoutBox> {

    val container_box = container_node.value().box
    val inner_container = createNode(container_box.width())
    val inner_box = inner_container.value().box

    val dimensions = tableColumnDimensions(inner_container, table)
    if (LOG.isTraceEnabled) {
      dimensions.column_widths.forEachIndexed { index, width ->
        LOG.trace("table column {} width {}", index, width)
      }
      LOG.trace("table head height {}", dimensions.head_height)
      dimensions.row_heights.forEachIndexed { index, height ->
        LOG.trace("table row {} height {}", index, height)
      }
    }

    var height_max = 0
    var width_max = 0
    var next_y = 0

    table.head.map { head ->
      var next_x = 0
      var accum_width = 0

      head.column_names.forEachIndexed { column_index, name ->
        val width = dimensions.column_widths[column_index]
        val height = dimensions.head_height

        val name_node = tableColumnName(inner_container, name)
        val name_box = name_node.value().box
        name_box.from(Boxes.moveAbsolute(name_box, 2, 1))

        val cell_node = createNode(dimensions.column_widths[column_index], KSPlainBorder.Light)
        val cell_box = cell_node.value().box

        cell_box.from(Boxes.moveAbsolute(cell_box, next_x, 0))
        cell_box.from(Boxes.setSizeFromBottomRight(cell_box, width + 1, height + 1))

        cell_node.childAdd(name_node)
        inner_container.childAdd(cell_node)

        next_x += cell_box.width() - 1
        accum_width += cell_box.width()
      }

      width_max = max(width_max, accum_width)
      next_y = dimensions.head_height
      height_max = dimensions.head_height
    }

    table.body.rows.forEachIndexed { row_index, row ->
      var next_x = 0
      var accum_width = 0
      var largest_height = 0
      val height = dimensions.row_heights[row_index]

      row.cells.forEachIndexed { column_index, cell ->
        val width = dimensions.column_widths[column_index]

        val content_node = createNode(width)
        val content_box = content_node.value().box
        content_box.from(Boxes.moveAbsolute(content_box, 2, 1))
        inlines(content_node, cell.content)

        val cell_node = createNode(dimensions.column_widths[column_index], KSPlainBorder.Light)
        val cell_box = cell_node.value().box

        cell_box.from(Boxes.moveAbsolute(cell_box, next_x, next_y))
        cell_box.from(Boxes.setSizeFromBottomRight(
          cell_box,
          content_box.width() + 1,
          height + 2))

        cell_node.childAdd(content_node)
        inner_container.childAdd(cell_node)

        next_x += cell_box.width() - 1
        accum_width += cell_box.width()
        largest_height = max(largest_height, cell_box.height())
      }

      height_max += largest_height
      next_y += largest_height - 1
      width_max = max(width_max, accum_width)
    }

    inner_box.from(Boxes.setSizeFromBottomRight(inner_box, width_max, height_max - 1))
    return inner_container
  }

  private fun tableColumnName(
    container : JOTreeNodeReadableType<KSPlainLayoutBox>,
    name : KSTableHeadColumnName<KSEvaluation>) : JOTreeNodeType<KSPlainLayoutBox> {

    val aligned = JPAlignerBasic.create(container.value().box.width())
    name.content.forEach { text ->
      KSInlineRenderer.text(text).forEach { word ->
        aligned.addWord(word)
      }
    }

    val lines = aligned.finish()
    val width = lines.fold(0, { width, line -> max(width, line.length) })
    val ibox = Boxes.create<Any>(0, 0, width + 3, lines.size + 1)

    return JOTreeNode.create(
      KSPlainLayoutBox(mutableBoxFrom(ibox), KSPlainBorder.None, lines))
  }

  private fun listOrdered(
    container : JOTreeNodeReadableType<KSPlainLayoutBox>,
    list : KSInlineListOrdered<KSEvaluation>) : JOTreeNodeType<KSPlainLayoutBox> {
    return list(container, list.content, number = true)
  }

  private fun listUnordered(
    container : JOTreeNodeReadableType<KSPlainLayoutBox>,
    list : KSInlineListUnordered<KSEvaluation>) : JOTreeNodeType<KSPlainLayoutBox> {
    return list(container, list.content, number = false)
  }

  private fun list(
    container : JOTreeNodeReadableType<KSPlainLayoutBox>,
    items : List<KSElement.KSInline.KSListItem<KSEvaluation>>,
    number : Boolean) : JOTreeNodeType<KSPlainLayoutBox> {

    val container_box = container.value().box
    val inner_container = createNode(container_box.width())

    /**
     * Split the original layout into a fixed width margin and a body
     * column.
     */

    val margin_width = 4
    val inner_box = inner_container.value().box
    val initial_split = Boxes.splitAlongVertical(inner_box, margin_width)
    val initial_margin = initial_split.left()
    val initial_body = initial_split.right()

    /**
     * Add nodes for the margin and body text.
     */

    val margin_node = JOTreeNode.create(layoutBoxFrom(initial_margin))
    val margin_box = margin_node.value().box
    inner_container.childAdd(margin_node)
    val body_node = JOTreeNode.create(layoutBoxFrom(initial_body))
    val body_box = body_node.value().box
    inner_container.childAdd(body_node)

    var current_y = 0
    items.forEachIndexed { index, item ->
      val bullet_node = createNode(margin_box.width())
      val content_node = createNode(body_box.width())

      if (number) {
        bullet_node.value().lines.add(index.toString() + ".")
      } else {
        bullet_node.value().lines.add("•")
      }

      inlines(content_node, item.content)

      val content_box = content_node.value().box
      content_box.from(Boxes.moveAbsolute(content_box, 0, current_y))

      val bullet_box = bullet_node.value().box
      bullet_box.from(Boxes.moveAbsolute(bullet_box, 0, current_y))

      margin_node.childAdd(bullet_node)
      body_node.childAdd(content_node)

      val height = max(bullet_box.height(), content_box.height())
      current_y += height
    }

    /**
     * Resize the margin and body such that they contain all of the boxes,
     * and resize the container such that it contains the margin and body.
     */

    current_y += 1

    margin_box.from(Boxes.create(
      margin_box.minimumX(),
      margin_box.minimumY(),
      margin_box.width(),
      current_y))

    body_box.from(Boxes.create(
      body_box.minimumX(),
      body_box.minimumY(),
      body_box.width(),
      current_y))

    inner_box.from(Boxes.create(
      inner_box.minimumX(),
      inner_box.minimumY(),
      inner_box.width(),
      current_y))

    Assertive.ensure(
      inner_box.width() == container_box.width(),
      "Inner container width (%d) must match the container width (%d)",
      inner_box.width(),
      container_box.width())
    Assertive.ensure(
      inner_box.height() == margin_box.height(),
      "Inner container height (%d) must match margin height (%d)",
      inner_box.height(),
      margin_box.height())
    Assertive.ensure(
      inner_box.height() == body_box.height(),
      "Inner container height (%d) must match body height (%d)",
      inner_box.height(),
      body_box.height())

    return inner_container
  }

  private fun verbatim(
    content : KSInlineVerbatim<KSEvaluation>)
    : JOTreeNodeType<KSPlainLayoutBox> {

    val lines = content.text.text.lines().toMutableList()
    val width = lines.fold(0, { width, line -> max(width, line.length) })
    val ibox = Boxes.create<Any>(0, 1, width, lines.size)
    return JOTreeNode.create(
      KSPlainLayoutBox(mutableBoxFrom(ibox), KSPlainBorder.None, lines))
  }

  private fun paragraphNumber(
    paragraph : KSBlockParagraph<KSEvaluation>,
    max_width : Int) : String {
    val ns = paragraph.data.number.get().least.toString()
    return if (ns.length > max_width) {
      ns.substring(0, max_width) + "…"
    } else {
      ns
    }
  }
}