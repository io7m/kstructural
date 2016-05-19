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

package com.io7m.kstructural.pretty.canon

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

class KSCanonPrettyPrinter private constructor(
  private val out : Writer,
  private val width : Int,
  private val indent : Int,
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
  private val layout = Layouter<IOException>(backend, indent)

  private fun bracketOpen(square : Boolean) : String =
    if (square) "[" else "("

  private fun bracketClose(square : Boolean) : String =
    if (square) "]" else ")"

  override fun pretty(e : KSElement<KSEvaluation>) : Unit =
    when (e) {
      is KSBlock               -> prettyBlock(e)
      is KSInline              -> prettyInline(e)
      is KSListItem            -> prettyListItem(e)
      is KSTableHeadColumnName -> prettyInlineTableHeadColumnName(e)
      is KSTableHead           -> prettyInlineTableHead(e)
      is KSTableBodyCell       -> prettyInlineTableBodyCell(e)
      is KSTableBodyRow        -> prettyInlineTableBodyRow(e)
      is KSTableBody           -> prettyInlineTableBody(e)
      is KSTableSummary        -> prettyInlineTableSummary(e)
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

  private fun prettyInline(e : KSInline<KSEvaluation>) : Unit =
    when (e) {
      is KSInlineLink              -> prettyInlineLink(e)
      is KSInlineText              -> prettyInlineText(e)
      is KSInlineVerbatim          -> prettyInlineVerbatim(e)
      is KSInlineTerm              -> prettyInlineTerm(e)
      is KSInlineFootnoteReference -> prettyInlineFootnoteReference(e)
      is KSInlineImage             -> prettyInlineImage(e)
      is KSInlineListOrdered       -> prettyInlineOrdered(e)
      is KSInlineListUnordered     -> prettyInlineUnordered(e)
      is KSInlineTable             -> prettyInlineTable(e)
      is KSInlineInclude           -> prettyInlineInclude(e)
    }

  private fun prettyImport(e : KSBlockImport<KSEvaluation>) : Unit {
    outStartMinor("import", e.square)
    layout.print(String.format("\"%s\"", e.file.text))
    outEnd(e.square)
  }

  private fun prettyInlineVerbatim(e : KSInlineVerbatim<KSEvaluation>) : Unit {
    outStartMinor("verbatim", e.square)
    outType(e.type)
    layout.print(String.format("\"%s\"", e.text))
    outEnd(e.square)
  }

  private fun prettyInlineImage(e : KSInlineImage<KSEvaluation>) : Unit {
    outStartMinor("image", e.square)
    outStartMinor("target", e.square)
    layout.print(String.format("\"%s\"", e.target))
    outEnd(e.square)
    layout.brk(1, 0)
    outType(e.type)
    e.size.ifPresent { size ->
      outStartMinor("size", e.square)
      layout.print(size.width.toString())
      layout.brk(1, 0)
      layout.print(size.height.toString())
      outEnd(e.square)
      layout.brk(1, 0)
    }
    prettyContentMapMinor(e.content, { c -> prettyInline(c) })
    outEnd(e.square)
  }

  private fun prettyInlineOrdered(e : KSInlineListOrdered<KSEvaluation>) : Unit {
    outStartMajor("list-ordered", e.square)
    prettyContentMapMajor(e.content, { c -> prettyListItem(c) })
    outEnd(e.square)
  }

  private fun prettyInlineUnordered(e : KSInlineListUnordered<KSEvaluation>) : Unit {
    outStartMajor("list-unordered", e.square)
    prettyContentMapMajor(e.content, { c -> prettyListItem(c) })
    outEnd(e.square)
  }

  private fun prettyListItem(e : KSListItem<KSEvaluation>) : Unit {
    outStartMinor("item", e.square)
    prettyContentMapMinor(e.content, { c -> prettyInline(c) })
    outEnd(e.square)
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
    outEnd(e.square)
  }

  private fun prettyInlineTableBody(e : KSTableBody<KSEvaluation>) : Unit {
    outStartMinor("body", e.square)
    e.rows.forEachIndexed { row_index, row ->
      prettyInlineTableBodyRow(row)
      if (row_index + 1 < e.rows.size) {
        layout.nl()
      }
    }
    outEnd(e.square)
  }

  private fun prettyInlineTableBodyRow(e : KSTableBodyRow<KSEvaluation>) : Unit {
    outStartMajor("row", e.square)
    e.cells.forEachIndexed { cell_index, cell ->
      prettyInlineTableBodyCell(cell)
      if (cell_index + 1 < e.cells.size) {
        layout.brk(1, 0)
      }
    }
    outEnd(e.square)
  }

  private fun prettyInlineTableBodyCell(e : KSTableBodyCell<KSEvaluation>) : Unit {
    outStartMinor("cell", e.square)
    prettyContentMapMinor(e.content, { c -> prettyInline(c) })
    outEnd(e.square)
  }

  private fun prettyInlineTableSummary(
    e : KSTableSummary<KSEvaluation>) : Unit {
    outStartMinor("summary", e.square)
    e.content.forEachIndexed { i, text ->
      layout.print(text.text)
      if (i + 1 < e.content.size) {
        layout.print(" ")
      }
    }
    outEnd(e.square)
  }

  private fun prettyInlineTableHead(e : KSTableHead<KSEvaluation>) : Unit {
    outStartMajor("head", e.square)
    e.column_names.forEachIndexed { i, name ->
      prettyInlineTableHeadColumnName(name)
      if (i + 1 < e.column_names.size) {
        layout.brk(1, 0)
      }
    }
    outEnd(e.square)
  }

  private fun prettyInlineTableHeadColumnName(
    e : KSTableHeadColumnName<KSEvaluation>) : Unit {
    outStartMinor("name", e.square)
    prettyContentMapMinor(e.content, { c -> prettyInline(c) })
    outEnd(e.square)
  }

  private fun prettyInlineLink(e : KSInlineLink<KSEvaluation>) : Unit =
    when (e.actual) {
      is KSLink.KSLinkExternal -> {
        val ee = e.actual as KSLink.KSLinkExternal
        outStartMinor("link-ext", e.square)
        outStartMinor("target", e.square)
        layout.print(String.format("\"%s\"", ee.target))
        outEnd(e.square)
        layout.brk(1, 0)
        prettyContentMapMinor(ee.content, { c -> prettyLinkContent(c) })
        outEnd(e.square)
      }
      is KSLink.KSLinkInternal -> {
        val ee = e.actual as KSLink.KSLinkInternal
        outStartMinor("link", e.square)
        outStartMinor("target", e.square)
        layout.print(ee.target.value)
        outEnd(e.square)
        layout.brk(1, 0)
        prettyContentMapMinor(ee.content, { c -> prettyLinkContent(c) })
        outEnd(e.square)
      }
    }

  private fun prettyLinkContent(c : KSLinkContent<KSEvaluation>) : Unit =
    when (c) {
      is KSLinkContent.KSLinkText  -> prettyInline(c.actual)
      is KSLinkContent.KSLinkImage -> prettyInline(c.actual)
    }

  private fun prettyInlineFootnoteReference(
    e : KSInlineFootnoteReference<KSEvaluation>) : Unit {
    outStartMinor("footnote-ref", e.square)
    layout.print(e.target.value)
    outEnd(e.square)
  }

  private fun prettyInlineInclude(e : KSInlineInclude<KSEvaluation>) : Unit {
    outStartMinor("include", e.square)
    layout.print(String.format("\"%s\"", e.file.text))
    outEnd(e.square)
  }

  private fun prettyInlineTerm(e : KSInlineTerm<KSEvaluation>) : Unit {
    outStartMinor("term", e.square)
    outType(e.type)
    prettyContentMapMinor(e.content, { c -> prettyInline(c) })
    outEnd(e.square)
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

  private fun prettyFormalItem(e : KSBlockFormalItem<KSEvaluation>) : Unit {
    outStartMajor("formal-item", e.square)
    outTitle(e.title)
    outId(e.id)
    outType(e.type)

    /**
     * The actual content of a formal item should be rendered as an inconsistent
     * block, because otherwise every word ends up on its own line.
     */

    layout.begin(
      Layouter.BreakConsistency.INCONSISTENT,
      Layouter.IndentationBase.FROM_IND,
      0)
    prettyContentMapMinor(e.content, { c -> pretty(c) })
    layout.end()

    outEnd(e.square)
  }

  private fun prettyFootnote(e : KSBlockFootnote<KSEvaluation>) : Unit {
    outStartMajor("footnote", e.square)
    outId(e.id)
    outType(e.type)

    /**
     * The actual content of a footnote should be rendered as an inconsistent
     * block, because otherwise every word ends up on its own line.
     */

    layout.begin(
      Layouter.BreakConsistency.INCONSISTENT,
      Layouter.IndentationBase.FROM_IND,
      0)
    prettyContentMapMinor(e.content, { c -> pretty(c) })
    layout.end()

    outEnd(e.square)
  }

  private fun prettyParagraph(e : KSBlockParagraph<KSEvaluation>) : Unit {
    outStartMajor("paragraph", e.square)
    outId(e.id)
    outType(e.type)

    /**
     * The actual content of a paragraph should be rendered as an inconsistent
     * block, because otherwise every word ends up on its own line.
     */

    layout.begin(
      Layouter.BreakConsistency.INCONSISTENT,
      Layouter.IndentationBase.FROM_IND,
      0)
    prettyContentMapMinor(e.content, { c -> pretty(c) })
    layout.end()

    outEnd(e.square)
  }

  private fun prettySubsection(e : KSBlockSubsection<KSEvaluation>) : Unit {
    outStartMajor("subsection", e.square)
    outTitle(e.title)
    outId(e.id)
    outType(e.type)

    prettyContentMapMajor(e.content, { c -> prettySubsectionContent(c) })
    outEnd(e.square)
  }

  private fun prettySection(e : KSBlockSection<KSEvaluation>) : Unit {
    outStartMajor("section", e.square)
    outTitle(e.title)
    outId(e.id)
    outType(e.type)

    when (e) {
      is KSBlockSectionWithSubsections ->
        prettyContentMapMajor(e.content, { c -> pretty(c) })
      is KSBlockSectionWithContent     ->
        prettyContentMapMajor(e.content, { c -> prettySubsectionContent(c) })
    }

    outEnd(e.square)
  }

  private fun prettySubsectionContent(
    c : KSSubsectionContent<KSEvaluation>) : Unit =
    when (c) {
      is KSSubsectionContent.KSSubsectionParagraph  -> pretty(c.paragraph)
      is KSSubsectionContent.KSSubsectionFormalItem -> pretty(c.formal)
      is KSSubsectionContent.KSSubsectionFootnote   -> pretty(c.footnote)
    }

  private fun <T> prettyContentMapMinor(
    xs : List<T>,
    f : (T) -> Unit) : Unit {

    xs.forEachIndexed { i, t ->
      f(t)
      if (i + 1 < xs.size) {
        layout.brk(1, 0)
      }
    }
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

  private fun prettyPart(e : KSBlockPart<KSEvaluation>) : Unit {
    outStartMajor("part", e.square)
    outTitle(e.title)
    outId(e.id)
    outType(e.type)

    prettyContentMapMajor(e.content, { c -> pretty(c) })
    outEnd(e.square)
  }

  private fun prettyDocument(e : KSBlockDocument<KSEvaluation>) : Unit {
    outStartMajor("document", e.square)
    outTitle(e.title)
    outId(e.id)
    outType(e.type)

    when (e) {
      is KSBlockDocumentWithParts    -> {
        prettyContentMapMajor(e.content, { c -> prettyPart(c) })
      }
      is KSBlockDocumentWithSections -> {
        prettyContentMapMajor(e.content, { c -> prettySection(c) })
      }
    }

    outEnd(e.square)
  }

  private fun outType(type : Optional<String>) {
    type.ifPresent { type ->
      outStartMinor("type", true)
      layout.print(type)
      outEnd(true)
      layout.brk(1, 0)
    }
  }

  private fun outId(id : Optional<KSID<KSEvaluation>>) {
    id.ifPresent { id ->
      outStartMinor("id", true)
      layout.print(id.value)
      outEnd(true)
      layout.brk(1, 0)
    }
  }

  private fun outTitle(
    title : List<KSInlineText<KSEvaluation>>) {
    outStartMinor("title", true)
    title.forEachIndexed { i, text ->
      layout.print(text.text)
      if (i + 1 < title.size) {
        layout.print(" ")
      }
    }
    outEnd(true)
    layout.brk(1, 0)
  }

  private fun outStartMinor(s : String, square : Boolean) {
    layout.begin(
      Layouter.BreakConsistency.INCONSISTENT,
      Layouter.IndentationBase.FROM_IND,
      2)
    layout.print(bracketOpen(square))
    layout.print(s)
    layout.brk(1, 0)
  }

  private fun outEnd(square : Boolean) {
    layout.end()
    layout.print(bracketClose(square))
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

  override fun finish() {
    layout.flush()
    layout.finish()
  }

  companion object {
    fun create(
      out : Writer,
      width : Int,
      indent : Int,
      imports : Boolean) : KSPrettyPrinterType {
      return KSCanonPrettyPrinter(out, width, indent, imports)
    }
  }

}