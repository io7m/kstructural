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

package com.io7m.kstructural.pretty.imperative

import com.io7m.kstructural.core.KSElement
import com.io7m.kstructural.core.KSElement.KSBlock
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithParts
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithSections
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockFootnote
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockFormalItem
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockImport
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockParagraph
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockPart
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection.KSBlockSectionWithContent
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection.KSBlockSectionWithSubsections
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSubsection
import com.io7m.kstructural.core.KSElement.KSInline
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
import com.io7m.kstructural.core.KSElement.KSInline.KSListItem
import com.io7m.kstructural.core.KSElement.KSInline.KSTableBody
import com.io7m.kstructural.core.KSElement.KSInline.KSTableBodyCell
import com.io7m.kstructural.core.KSElement.KSInline.KSTableBodyRow
import com.io7m.kstructural.core.KSElement.KSInline.KSTableHead
import com.io7m.kstructural.core.KSElement.KSInline.KSTableHeadColumnName
import com.io7m.kstructural.core.KSElement.KSInline.KSTableSummary
import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSLink
import com.io7m.kstructural.core.KSLinkContent
import com.io7m.kstructural.core.KSSubsectionContent
import com.io7m.kstructural.core.evaluator.KSEvaluation
import com.io7m.kstructural.pretty.KSPrettyPrinterType
import de.uka.ilkd.pp.Layouter
import de.uka.ilkd.pp.WriterBackend
import java.io.IOException
import java.io.Writer
import java.util.Optional

class KSImperativePrettyPrinter private constructor(
  private val out : Writer,
  private val width : Int,
  private val imports : Boolean)
: KSPrettyPrinterType {

  override fun close() {
    if (!this.layout.isFinished) {
      this.finish()
    }
    this.out.flush()
    this.out.close()
  }

  private val backend = WriterBackend(out, width)
  private val layout = Layouter<IOException>(backend, 0)

  private fun bracketOpen(square : Boolean) : String =
    if (square) "[" else "("

  private fun bracketClose(square : Boolean) : String =
    if (square) "]" else ")"

  override fun pretty(e : KSElement<KSEvaluation>) : Unit =
    when (e) {
      is KSBlock               -> prettyBlock(e)
      is KSInline              -> prettyInline(e)
      is KSListItem            -> prettyInlineListItem(e)
      is KSTableHeadColumnName -> prettyInlineTableHeadColumnName(e)
      is KSTableHead           -> prettyInlineTableHead(e)
      is KSTableBodyCell       -> prettyInlineTableBodyCell(e)
      is KSTableBodyRow        -> prettyInlineTableBodyRow(e)
      is KSTableBody           -> prettyInlineTableBody(e)
      is KSTableSummary        -> prettyInlineTableSummary(e)
    }

  private fun prettyInline(e : KSInline<KSEvaluation>) =
    when (e) {
      is KSInlineLink              -> prettyInlineLink(e)
      is KSInlineText              -> prettyInlineText(e)
      is KSInlineVerbatim          -> prettyInlineVerbatim(e)
      is KSInlineTerm              -> prettyInlineTerm(e)
      is KSInlineFootnoteReference -> prettyInlineFootnoteReference(e)
      is KSInlineImage             -> prettyInlineImage(e)
      is KSInlineListOrdered       -> prettyInlineListOrdered(e)
      is KSInlineListUnordered     -> prettyInlineListUnordered(e)
      is KSInlineTable             -> prettyInlineTable(e)
      is KSInlineInclude           -> prettyInlineInclude(e)
    }

  private fun prettyInlineTable(e : KSInlineTable<KSEvaluation>) : Unit {
    outStartMajor("table", e.square)
    outType(e.type)

    prettyInlineTableSummary(e.summary)
    layout.brk(1, 0)

    e.head.ifPresent { head ->
      prettyInlineTableHead(head)
      layout.brk(1, 0)
    }

    prettyInlineTableBody(e.body)
    outEnd(e.square, newline = false)
  }

  private fun prettyInlineTableBody(e : KSTableBody<KSEvaluation>) : Unit {
    outStart("body", e.square, space = true)
    e.rows.forEachIndexed { row_index, row ->
      prettyInlineTableBodyRow(row)
      if (row_index + 1 < e.rows.size) {
        layout.nl()
      }
    }
    outEnd(e.square, newline = false)
  }

  private fun prettyInlineTableBodyRow(e : KSTableBodyRow<KSEvaluation>) : Unit {
    outStartMajor("row", e.square)
    e.cells.forEachIndexed { cell_index, cell ->
      prettyInlineTableBodyCell(cell)
      if (cell_index + 1 < e.cells.size) {
        layout.brk(1, 0)
      }
    }
    outEnd(e.square, newline = false)
  }

  private fun prettyInlineTableBodyCell(e : KSTableBodyCell<KSEvaluation>) : Unit {
    outStart("cell", e.square, space = e.content.isNotEmpty())
    prettyContentMapInline(e.content, { c -> prettyInline(c) })
    outEnd(e.square, newline = false)
  }

  private fun prettyInlineTableSummary(
    e : KSTableSummary<KSEvaluation>) : Unit {
    outStart("summary", e.square, space = e.content.isNotEmpty())
    e.content.forEachIndexed { i, text ->
      layout.print(text.text)
      if (i + 1 < e.content.size) {
        layout.print(" ")
      }
    }
    outEnd(e.square, newline = false)
  }

  private fun prettyInlineTableHead(e : KSTableHead<KSEvaluation>) : Unit {
    outStartMajor("head", e.square)
    e.column_names.forEachIndexed { i, name ->
      prettyInlineTableHeadColumnName(name)
      if (i + 1 < e.column_names.size) {
        layout.brk(1, 0)
      }
    }
    outEnd(e.square, newline = false)
  }

  private fun prettyInlineTableHeadColumnName(
    e : KSTableHeadColumnName<KSEvaluation>) : Unit {
    outStart("name", e.square, space = e.content.isNotEmpty())
    prettyContentMapInline(e.content, { c -> prettyInline(c) })
    outEnd(e.square, newline = false)
  }

  private fun prettyInlineListItem(e : KSListItem<KSEvaluation>) : Unit {
    outStart("item", e.square, space = e.content.isNotEmpty())
    prettyContentMapInline(e.content, { c -> prettyInline(c) })
    outEnd(e.square, newline = false)
  }

  private fun outStartMajor(s : String, square : Boolean) {
    layout.begin(
      Layouter.BreakConsistency.CONSISTENT,
      Layouter.IndentationBase.FROM_IND,
      2)
    layout.print(bracketOpen(square))
    layout.print(s)
    layout.nl()
  }

  private fun <T> prettyContentMapMajor(
    xs : List<T>,
    f : (T) -> Unit) : Unit {
    xs.forEachIndexed { i, t ->
      f(t)
      if (i + 1 < xs.size) {
        layout.nl()
      }
    }
  }

  private fun prettyInlineListOrdered(
    e : KSInlineListOrdered<KSEvaluation>) : Unit {
    outStartMajor("list-ordered", e.square)
    prettyContentMapMajor(e.content, { c -> prettyInlineListItem(c) })
    outEnd(e.square, newline = false)
  }

  private fun prettyInlineListUnordered(
    e : KSInlineListUnordered<KSEvaluation>) : Unit {
    outStartMajor("list-unordered", e.square)
    prettyContentMapMajor(e.content, { c -> prettyInlineListItem(c) })
    outEnd(e.square, newline = false)
  }

  private fun prettyInlineImage(e : KSInlineImage<KSEvaluation>) {
    outStart("image", e.square, space = true)
    outStart("target", e.square, space = true)
    layout.print(String.format("\"%s\"", e.target))
    outEnd(e.square, newline = false)
    layout.brk(1, 0)
    outType(e.type)
    if (e.type.isPresent) layout.brk(1, 0)
    e.size.ifPresent { size ->
      outStart("size", e.square, space = true)
      layout.print(size.width.toString())
      layout.brk(1, 0)
      layout.print(size.height.toString())
      outEnd(e.square, newline = false)
      layout.brk(1, 0)
    }
    prettyContentMapInline(e.content, { c -> prettyInline(c) })
    outEnd(e.square, newline = false)
  }

  private fun prettyInlineFootnoteReference(
    e : KSInlineFootnoteReference<KSEvaluation>) : Unit {
    outStart("footnote-ref", e.square, space = true)
    layout.print(e.target.value)
    outEnd(e.square, newline = false)
  }

  private fun prettyInlineVerbatim(e : KSInlineVerbatim<KSEvaluation>) : Unit {
    outStart("verbatim", e.square, space = e.type.isPresent)
    outType(e.type)
    layout.brk(1, 0)
    layout.print(String.format("\"%s\"", e.text))
    outEnd(e.square, newline = false)
  }

  private fun prettyInlineLink(e : KSInlineLink<KSEvaluation>) : Unit =
    when (e.actual) {
      is KSLink.KSLinkExternal -> {
        val ee = e.actual as KSLink.KSLinkExternal
        outStart("link-ext", e.square, space = true)
        outStart("target", e.square, space = true)
        layout.print(String.format("\"%s\"", ee.target))
        outEnd(e.square, newline = false)
        layout.brk(1, 0)
        prettyContentMapInline(ee.content, { c -> prettyLinkContent(c) })
        outEnd(e.square, newline = false)
      }
      is KSLink.KSLinkInternal -> {
        val ee = e.actual as KSLink.KSLinkInternal
        outStart("link", e.square, space = true)
        outStart("target", e.square, space = true)
        layout.print(ee.target.value)
        outEnd(e.square, newline = false)
        layout.brk(1, 0)
        prettyContentMapInline(ee.content, { c -> prettyLinkContent(c) })
        outEnd(e.square, newline = false)
      }
    }

  private fun prettyLinkContent(c : KSLinkContent<KSEvaluation>) : Unit =
    when (c) {
      is KSLinkContent.KSLinkText  -> prettyInline(c.actual)
      is KSLinkContent.KSLinkImage -> prettyInline(c.actual)
    }

  private fun prettyInlineText(e : KSInlineText<KSEvaluation>) : Unit {
    if (imports && e.data.include.isPresent) {
      prettyInlineInclude(e.data.include.get())
    } else {
      if (e.quote) {
        layout.print(String.format("\"%s\"", e.text))
      } else {
        layout.print(e.text)
      }
    }
  }

  private fun prettyInlineTerm(e : KSInlineTerm<KSEvaluation>) : Unit {
    outStart("term", e.square, space = e.content.isNotEmpty() || e.type.isPresent)
    outType(e.type)
    e.type.ifPresent { layout.brk(1, 0) }
    prettyContentMapInline(e.content, { c -> prettyInline(c) })
    outEnd(e.square, newline = false)
  }

  private fun prettyInlineInclude(e : KSInlineInclude<KSEvaluation>) : Unit {
    outStart("include", e.square, space = true)
    layout.print(String.format("\"%s\"", e.file.text))
    outEnd(e.square, newline = false)
  }

  tailrec private fun prettyBlock(e : KSBlock<KSEvaluation>) : Unit {
    if (imports) {
      val by_block = e.data.context.imports
      if (by_block.containsKey(e)) {
        return prettyBlock(by_block[e]!!)
      }
    }

    when (e) {
      is KSBlockDocument   -> prettyDocument(e)
      is KSBlockSection    -> prettySection(e)
      is KSBlockSubsection -> prettySubsection(e)
      is KSBlockParagraph  -> prettyParagraph(e)
      is KSBlockFormalItem -> prettyFormalItem(e)
      is KSBlockFootnote   -> prettyFootnote(e)
      is KSBlockPart       -> prettyPart(e)
      is KSBlockImport     -> prettyImport(e)
    }
  }

  private fun prettySubsectionContent(c : KSSubsectionContent<KSEvaluation>) =
    when (c) {
      is KSSubsectionContent.KSSubsectionParagraph  -> pretty(c.paragraph)
      is KSSubsectionContent.KSSubsectionFormalItem -> pretty(c.formal)
      is KSSubsectionContent.KSSubsectionFootnote   -> pretty(c.footnote)
    }

  private fun <T> prettyContentMapInline(
    xs : List<T>,
    f : (T) -> Unit) : Unit {

    xs.forEachIndexed { i, t ->
      f(t)
      if (i + 1 < xs.size) {
        layout.brk(1, 0)
      }
    }
  }

  private fun prettyPart(e : KSBlockPart<KSEvaluation>) {
    outStart("part", e.square, space = true)
    prettyTitleIdType(e.id, e.title, e.type)
    outEnd(e.square, newline = true)

    if (e.content.isNotEmpty()) {
      outStartAnonymous()
      e.content.map { c -> pretty(c) }
      outEndAnonymous(brk = false)
    }
  }

  private fun prettyParagraph(e : KSBlockParagraph<KSEvaluation>) {
    outStart("paragraph", e.square, space = e.id.isPresent || e.type.isPresent)
    prettyIdType(e.id, e.type)
    outEnd(e.square, newline = true)

    if (e.content.isNotEmpty()) {
      outStartAnonymous()
      prettyContentMapInline(e.content, { c -> prettyInline(c) })
      layout.nl()
      outEndAnonymous(brk = true)
    }
  }

  private fun prettyFootnote(e : KSBlockFootnote<KSEvaluation>) {
    outStart("footnote", e.square, space = e.id.isPresent || e.type.isPresent)
    prettyIdType(e.id, e.type)
    outEnd(e.square, newline = true)

    if (e.content.isNotEmpty()) {
      outStartAnonymous()
      prettyContentMapInline(e.content, { c -> prettyInline(c) })
      layout.nl()
      outEndAnonymous(brk = true)
    }
  }

  private fun prettyFormalItem(e : KSBlockFormalItem<KSEvaluation>) {
    outStart("formal-item", e.square, space = true)
    prettyTitleIdType(e.id, e.title, e.type)
    outEnd(e.square, newline = true)

    if (e.content.isNotEmpty()) {
      outStartAnonymous()
      prettyContentMapInline(e.content, { c -> prettyInline(c) })
      layout.nl()
      outEndAnonymous(brk = true)
    }
  }

  private fun prettyImport(e : KSBlockImport<KSEvaluation>) : Unit {
    outStart("import", e.square, space = true)
    layout.print(String.format("\"%s\"", e.file.text))
    outEnd(e.square, newline = true)
  }

  private fun prettySection(e : KSBlockSection<KSEvaluation>) {
    outStart("section", e.square, space = true)
    prettyTitleIdType(e.id, e.title, e.type)
    outEnd(e.square, newline = true)

    when (e) {
      is KSBlockSectionWithContent     -> {
        if (e.content.isNotEmpty()) {
          outStartAnonymous()
          e.content.map { c -> prettySubsectionContent(c) }
          outEndAnonymous(brk = false)
        }
      }
      is KSBlockSectionWithSubsections -> {
        if (e.content.isNotEmpty()) {
          outStartAnonymous()
          e.content.map { c -> pretty(c) }
          outEndAnonymous(brk = false)
        }
      }
    }
  }

  private fun prettySubsection(e : KSBlockSubsection<KSEvaluation>) {
    outStart("subsection", e.square, space = true)
    prettyTitleIdType(e.id, e.title, e.type)
    outEnd(e.square, newline = true)

    if (e.content.isNotEmpty()) {
      outStartAnonymous()
      e.content.map { c -> prettySubsectionContent(c) }
      outEndAnonymous(brk = false)
    }
  }

  private fun prettyDocument(e : KSBlockDocument<KSEvaluation>) {
    outStart("document", e.square, space = true)
    prettyTitleIdType(e.id, e.title, e.type)
    outEnd(e.square, newline = true)

    when (e) {
      is KSBlockDocumentWithSections -> {
        if (e.content.isNotEmpty()) {
          outStartAnonymous()
          e.content.map { c -> pretty(c) }
          outEndAnonymous(brk = false)
        }
      }
      is KSBlockDocumentWithParts    -> {
        if (e.content.isNotEmpty()) {
          outStartAnonymous()
          e.content.map { c -> pretty(c) }
          outEndAnonymous(brk = false)
        }
      }
    }
  }

  private fun prettyTitleIdType(
    id : Optional<KSID<KSEvaluation>>,
    title : List<KSInlineText<KSEvaluation>>,
    type : Optional<String>) {
    outTitle(title)
    id.ifPresent { layout.brk(1, 0) }
    outId(id)
    type.ifPresent { layout.brk(1, 0) }
    outType(type)
  }

  private fun prettyIdType(
    id : Optional<KSID<KSEvaluation>>,
    type : Optional<String>) {
    outId(id)
    type.ifPresent { layout.brk(1, 0) }
    outType(type)
  }

  private fun outType(type : Optional<String>) {
    type.ifPresent { type ->
      outStart("type", true, space = true)
      layout.print(type)
      outEnd(true, newline = false)
    }
  }

  private fun outId(id : Optional<KSID<KSEvaluation>>) {
    id.ifPresent { id ->
      outStart("id", true, space = true)
      layout.print(id.value)
      outEnd(true, newline = false)
    }
  }

  private fun outTitle(
    title : List<KSInlineText<KSEvaluation>>) {
    outStart("title", true, space = title.isNotEmpty())
    title.forEachIndexed { i, text ->
      layout.print(text.text)
      if (i + 1 < title.size) {
        layout.print(" ")
      }
    }
    outEnd(true, newline = false)
  }

  private fun outStartAnonymous() {
    layout.begin(
      Layouter.BreakConsistency.INCONSISTENT,
      Layouter.IndentationBase.FROM_IND,
      0)
  }

  private fun outEndAnonymous(brk : Boolean) {
    if (brk) layout.nl()
    layout.end()
  }

  private fun outStart(s : String, square : Boolean, space : Boolean) {
    layout.begin(
      Layouter.BreakConsistency.INCONSISTENT,
      Layouter.IndentationBase.FROM_IND,
      0)
    layout.print(bracketOpen(square))
    layout.print(s)
    if (space) layout.brk(1, 0)
  }

  private fun outEnd(square : Boolean, newline : Boolean) {
    layout.print(bracketClose(square))
    if (newline) layout.nl()
    layout.end()
  }

  override fun finish() {
    layout.flush()
    layout.finish()
  }

  companion object {
    fun create(
      out : Writer,
      width : Int,
      imports : Boolean) : KSPrettyPrinterType {
      return KSImperativePrettyPrinter(out, width, imports)
    }
  }

}