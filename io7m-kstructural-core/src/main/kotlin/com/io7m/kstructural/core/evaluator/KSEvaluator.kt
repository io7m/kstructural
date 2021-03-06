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

package com.io7m.kstructural.core.evaluator

import com.io7m.jlexing.core.LexicalPositionType
import com.io7m.junreachable.UnreachableCodeException
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
import com.io7m.kstructural.core.KSImportPathEdge
import com.io7m.kstructural.core.KSLink.KSLinkExternal
import com.io7m.kstructural.core.KSLink.KSLinkInternal
import com.io7m.kstructural.core.KSLinkContent
import com.io7m.kstructural.core.KSLinkContent.KSLinkImage
import com.io7m.kstructural.core.KSLinkContent.KSLinkText
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.core.KSSubsectionContent
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionFootnote
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionFormalItem
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionParagraph
import com.io7m.kstructural.core.KSType
import com.io7m.kstructural.core.evaluator.KSNumber.KSNumberPart
import com.io7m.kstructural.core.evaluator.KSNumber.KSNumberPartSection
import com.io7m.kstructural.core.evaluator.KSNumber.KSNumberPartSectionContent
import com.io7m.kstructural.core.evaluator.KSNumber.KSNumberPartSectionSubsection
import com.io7m.kstructural.core.evaluator.KSNumber.KSNumberPartSectionSubsectionContent
import com.io7m.kstructural.core.evaluator.KSNumber.KSNumberSection
import com.io7m.kstructural.core.evaluator.KSNumber.KSNumberSectionContent
import com.io7m.kstructural.core.evaluator.KSNumber.KSNumberSectionSubsection
import com.io7m.kstructural.core.evaluator.KSNumber.KSNumberSectionSubsectionContent
import org.slf4j.LoggerFactory
import org.valid4j.Assertive
import java.nio.file.Path
import java.util.ArrayDeque
import java.util.Deque
import java.util.HashMap
import java.util.IdentityHashMap
import java.util.Optional
import java.util.OptionalLong

object KSEvaluator : KSEvaluatorType {

  private val LOG = LoggerFactory.getLogger(KSEvaluator::class.java)

  private data class Context private constructor(
    private var serial_pool : Long,
    private val all_by_serial : MutableMap<KSSerial, KSElement<KSEvaluation>>,
    private val blocks_by_id : MutableMap<String, KSBlock<KSEvaluation>>,
    private val blocks_by_number : MutableMap<KSNumber, KSBlock<KSEvaluation>>,
    private val id_references : MutableList<KSID<KSEvaluation>>,

    val file_stack : Deque<Path>,

    val includes_by_file : MutableMap<Path, String>,
    val includes_by_serial : MutableMap<KSSerial, String>,
    override val includesByText : MutableMap<KSInlineText<KSEvaluation>, KSInlineInclude<KSEvaluation>>,

    override val imports : MutableMap<KSBlock<KSEvaluation>, KSBlockImport<KSEvaluation>>,
    override val importsPaths : MutableMap<KSBlockImport<KSEvaluation>, KSImportPathEdge>,
    override val importedBlocks : MutableMap<KSBlockImport<KSEvaluation>, KSBlock<KSEvaluation>>,

    val count_by_class : MutableMap<Class<*>, Long>,

    var enclosing_table : Boolean = false,
    var enclosing_table_pos : Optional<LexicalPositionType<Path>> = Optional.empty(),

    override val footnotesAll : MutableMap<KSID<KSEvaluation>, KSBlockFootnote<KSEvaluation>>)
  : KSEvaluationContextType {

    companion object {
      fun create(
        f : Path)
        : Context {
        val c = Context(
          serial_pool = 0L,
          all_by_serial = HashMap(),
          blocks_by_id = HashMap(),
          blocks_by_number = HashMap(),
          id_references = mutableListOf(),
          file_stack = ArrayDeque(),
          includes_by_file = HashMap(),
          includes_by_serial = HashMap(),
          includesByText = IdentityHashMap(),
          imports = IdentityHashMap(),
          importsPaths = IdentityHashMap(),
          importedBlocks = IdentityHashMap(),
          count_by_class = HashMap(),
          enclosing_table = false,
          enclosing_table_pos = Optional.empty(),
          footnotesAll = mutableMapOf())
        c.file_stack.push(f)
        return c
      }
    }

    fun nextCount(c : Class<*>) : Long {
      return if (count_by_class.containsKey(c)) {
        val next = count_by_class[c]!! + 1L
        count_by_class[c] = next
        next
      } else {
        count_by_class[c] = 0L
        return 0L
      }
    }

    override fun elementForSerial(s : KSSerial) : Optional<KSElement<KSEvaluation>> {
      return if (this.all_by_serial.containsKey(s)) {
        Optional.of(this.all_by_serial.get(s)!!)
      } else {
        Optional.empty()
      }
    }

    override fun footnoteReferencesForFootnote(
      f : KSBlockFootnote<KSEvaluation>)
      : Map<Long, KSFootnoteReference<KSEvaluation>> {

      val id = f.id.get()
      return if (this.footnote_refs.containsKey(id)) {
        this.footnote_refs[id]!!
      } else {
        mapOf()
      }
    }

    override fun footnoteReferenceForInline(
      f : KSInlineFootnoteReference<KSEvaluation>)
      : KSFootnoteReference<KSEvaluation> {

      val refs = this.footnote_refs.get(f.target)!!
      for (e in refs) {
        if (e.value.ref.data.serial == f.data.serial) {
          return e.value
        }
      }

      throw UnreachableCodeException()
    }

    private val footnote_refs : MutableMap<
      KSID<KSEvaluation>,
      MutableMap<Long, KSFootnoteReference<KSEvaluation>>> =
      mutableMapOf()

    private val section_footnotes : MutableMap<
      KSNumber.HasSectionType,
      MutableMap<KSID<KSEvaluation>, KSBlockFootnote<KSEvaluation>>> =
      mutableMapOf()

    override fun footnotesForSection(
      n : KSNumber.HasSectionType)
      : Map<KSID<KSEvaluation>, KSBlockFootnote<KSEvaluation>> {
      return section_footnotes.getOrElse(n, {
        mapOf<KSID<KSEvaluation>, KSBlockFootnote<KSEvaluation>>()
      })
    }

    var part_number : OptionalLong = OptionalLong.empty()
      set(value) {
        LOG.trace("part: {}", value)
        field = value
      }

    var section_number : Long = 0L
      set(value) {
        LOG.trace("section: {}", value)
        field = value
      }

    var subsection_number : OptionalLong = OptionalLong.empty()
      set(value) {
        LOG.trace("subsection: {}", value)
        field = value
      }

    var content_number : Long = 0L
      set(value) {
        LOG.trace("content: {}", value)
        field = value
      }

    private lateinit var document_actual : KSBlockDocument<KSEvaluation>

    override val document : KSBlockDocument<KSEvaluation>
      get() = document_actual

    override fun elementSegmentPrevious(
      b : KSBlock<KSEvaluation>) : Optional<KSBlock<KSEvaluation>> =
      when (b) {
        is KSBlockDocument -> Optional.empty()
        is KSBlockSection,
        is KSBlockSubsection,
        is KSBlockParagraph,
        is KSBlockFormalItem,
        is KSBlockFootnote,
        is KSBlockImport,
        is KSBlockPart     -> {
          Assertive.require(b.data.number.isPresent)
          val n = b.data.number.get()
          when (n) {
            is KSNumber.KSNumberPart                         ->
              elementSegmentPreviousPart(n)

            is KSNumber.KSNumberPartSection                  ->
              elementSegmentPreviousPartSection(n)
            is KSNumber.KSNumberPartSectionContent           ->
              elementSegmentPreviousPartSection(n)
            is KSNumber.KSNumberPartSectionSubsection        ->
              elementSegmentPreviousPartSection(n)
            is KSNumber.KSNumberPartSectionSubsectionContent ->
              elementSegmentPreviousPartSection(n)

            is KSNumber.KSNumberSection                      ->
              elementSegmentPreviousSection(n)
            is KSNumber.KSNumberSectionContent               ->
              elementSegmentPreviousSection(n)
            is KSNumber.KSNumberSectionSubsection            ->
              elementSegmentPreviousSection(n)
            is KSNumber.KSNumberSectionSubsectionContent     ->
              elementSegmentPreviousSection(n)
          }
        }
      }

    private fun elementSegmentPreviousPart(n : KSNumberPart)
      : Optional<KSBlock<KSEvaluation>> {
      Assertive.require(document is KSBlockDocumentWithParts)
      return if (n.part == 1L) {
        Optional.of(document)
      } else {
        val dp = document as KSBlockDocumentWithParts<KSEvaluation>
        val pi = (n.part - 1) - 1
        Optional.of(dp.content[pi.toInt()] as KSBlock<KSEvaluation>)
      }
    }

    private fun <N> elementSegmentPreviousSection(n : N)
      : Optional<KSBlock<KSEvaluation>>
      where N : KSNumber.HasSectionType {
      Assertive.require(document is KSBlockDocumentWithSections)
      return if (n.section == 1L) {
        Optional.of(document)
      } else {
        val dp = document as KSBlockDocumentWithSections<KSEvaluation>
        val si = (n.section - 1) - 1
        val sb = dp.content[si.toInt()]
        Optional.of(sb as KSBlock<KSEvaluation>)
      }
    }

    private fun <N> elementSegmentPreviousPartSection(n : N)
      : Optional<KSBlock<KSEvaluation>>
      where N : KSNumber.HasPartType, N : KSNumber.HasSectionType {
      Assertive.require(document is KSBlockDocumentWithParts)
      return if (n.section == 1L) {
        val dp = document as KSBlockDocumentWithParts<KSEvaluation>
        val pi = (n.part - 1)
        val pp = dp.content[pi.toInt()]
        Optional.of(pp as KSBlock<KSEvaluation>)
      } else {
        val dp = document as KSBlockDocumentWithParts<KSEvaluation>
        val pi = (n.part - 1)
        val pp = dp.content[pi.toInt()]
        val si = (n.section - 1) - 1
        val sb = pp.content[si.toInt()]
        Optional.of(sb as KSBlock<KSEvaluation>)
      }
    }

    override fun elementSegmentUp(
      b : KSBlock<KSEvaluation>) : Optional<KSBlock<KSEvaluation>> =
      when (b) {
        is KSBlockDocument ->
          Optional.empty()
        is KSBlockSubsection,
        is KSBlockParagraph,
        is KSBlockFormalItem,
        is KSBlockFootnote,
        is KSBlockImport,
        is KSBlockPart,
        is KSBlockSection  -> {
          Assertive.require(b.data.number.isPresent)
          val n = b.data.number.get()
          when (n) {
            is KSNumber.KSNumberPart,
            is KSNumber.KSNumberSection,
            is KSNumber.KSNumberSectionContent,
            is KSNumber.KSNumberSectionSubsection,
            is KSNumber.KSNumberSectionSubsectionContent     -> {
              Optional.of(document as KSBlock<KSEvaluation>)
            }
            is KSNumber.KSNumberPartSectionContent           ->
              elementSegmentUpPartSection(n)
            is KSNumber.KSNumberPartSectionSubsection        ->
              elementSegmentUpPartSection(n)
            is KSNumber.KSNumberPartSectionSubsectionContent ->
              elementSegmentUpPartSection(n)
            is KSNumber.KSNumberPartSection                  ->
              elementSegmentUpPartSection(n)
          }
        }
      }

    private fun <N> elementSegmentUpPartSection(n : N)
      : Optional<KSBlock<KSEvaluation>>
      where N : KSNumber.HasPartType {
      Assertive.require(document is KSBlockDocumentWithParts)
      val dp = document as KSBlockDocumentWithParts<KSEvaluation>
      val p = dp.content[(n.part - 1).toInt()]
      return Optional.of(p as KSBlock<KSEvaluation>)
    }

    override fun elementSegmentNext(
      b : KSBlock<KSEvaluation>) : Optional<KSBlock<KSEvaluation>> =
      when (b) {
        is KSBlockDocument -> {
          val bb : KSBlockDocument<KSEvaluation> = b
          when (bb) {
            is KSBlockDocument.KSBlockDocumentWithParts    ->
              Optional.of(bb.content[0] as KSBlock<KSEvaluation>)
            is KSBlockDocument.KSBlockDocumentWithSections ->
              Optional.of(bb.content[0] as KSBlock<KSEvaluation>)
          }
        }
        is KSBlockSection,
        is KSBlockSubsection,
        is KSBlockParagraph,
        is KSBlockFormalItem,
        is KSBlockFootnote,
        is KSBlockImport,
        is KSBlockPart     -> {
          Assertive.require(b.data.number.isPresent)
          val n = b.data.number.get()
          when (n) {
            is KSNumber.KSNumberPart                         ->
              elementSegmentNextPart(n)

            is KSNumber.KSNumberPartSection                  ->
              elementSegmentNextPartSection(n)
            is KSNumber.KSNumberPartSectionContent           ->
              elementSegmentNextPartSection(n)
            is KSNumber.KSNumberPartSectionSubsection        ->
              elementSegmentNextPartSection(n)
            is KSNumber.KSNumberPartSectionSubsectionContent ->
              elementSegmentNextPartSection(n)

            is KSNumber.KSNumberSection                      ->
              elementSegmentNextSection(n)
            is KSNumber.KSNumberSectionContent               ->
              elementSegmentNextSection(n)
            is KSNumber.KSNumberSectionSubsection            ->
              elementSegmentNextSection(n)
            is KSNumber.KSNumberSectionSubsectionContent     ->
              elementSegmentNextSection(n)
          }
        }
      }

    private fun elementSegmentNextPart(
      n : KSNumberPart) : Optional<KSBlock<KSEvaluation>> {
      Assertive.require(document is KSBlockDocumentWithParts)
      val dp = document as KSBlockDocumentWithParts<KSEvaluation>
      val pi = n.part - 1
      val pp = dp.content[pi.toInt()]
      if (pp.content.size > 0) {
        return Optional.of(pp.content[0] as KSBlock<KSEvaluation>)
      }
      return Optional.empty()
    }

    private fun <N> elementSegmentNextSection(n : N)
      : Optional<KSBlock<KSEvaluation>>
      where N : KSNumber.HasSectionType {
      Assertive.require(document is KSBlockDocumentWithSections)
      val dp = document as KSBlockDocumentWithSections<KSEvaluation>
      val sn_next = n.section.toInt()
      return if (sn_next == dp.content.size) {
        Optional.empty<KSBlock<KSEvaluation>>()
      } else {
        Optional.of(dp.content[sn_next] as KSBlock<KSEvaluation>)
      }
    }

    private fun <N> elementSegmentNextPartSection(n : N)
      : Optional<KSBlock<KSEvaluation>>
      where N : KSNumber.HasPartType, N : KSNumber.HasSectionType {
      Assertive.require(document is KSBlockDocumentWithParts)
      val dp = document as KSBlockDocumentWithParts<KSEvaluation>
      val pn_current = (n.part - 1).toInt()
      val pn_next = n.part.toInt()
      val sn_next = n.section.toInt()
      val pp = dp.content[pn_current.toInt()]
      return if (sn_next == pp.content.size) {
        if (pn_next == dp.content.size) {
          Optional.empty<KSBlock<KSEvaluation>>()
        } else {
          Optional.of(dp.content[pn_next] as KSBlock<KSEvaluation>)
        }
      } else {
        Optional.of(pp.content[sn_next] as KSBlock<KSEvaluation>)
      }
    }

    override fun elementForNumber(n : KSNumber) : KSBlock<KSEvaluation> {
      Assertive.require(blocks_by_number.containsKey(n))
      return blocks_by_number[n]!!
    }

    override fun elementForID(id : KSID<KSEvaluation>) : KSBlock<KSEvaluation> {
      Assertive.require(blocks_by_id.containsKey(id.value))
      return blocks_by_id[id.value]!!
    }

    fun freshSerial() : KSSerial {
      this.serial_pool = this.serial_pool + 1L
      return KSSerial(this.serial_pool)
    }

    fun <T : KSBlock<KSParse>, U : KSBlock<KSEvaluation>> recordID(
      c : Context,
      b : T,
      parent : KSSerial,
      f : (T, Optional<KSID<KSEvaluation>>) -> U)
      : KSResult<U, KSEvaluationError> {
      return if (b.id.isPresent) {
        val id = b.id.get()

        LOG.trace("declare {} {}", id, id.position)
        if (c.blocks_by_id.containsKey(id.value)) {
          LOG.debug("duplicate id {}", id)

          val ob = c.blocks_by_id[id.value]!!
          Assertive.require(ob.id.isPresent)

          val sb = StringBuilder()
          sb.append("Duplicate ID.")
          sb.append(System.lineSeparator())

          sb.append("  Current:  ")
          sb.append(id.value)
          id.position.ifPresent { pos ->
            sb.append(" at ")
            sb.append(pos)
          }
          sb.append(System.lineSeparator())

          sb.append("  Original: ")
          sb.append(id.value)
          ob.id.get().position.ifPresent { pos ->
            sb.append(" at ")
            sb.append(pos)
          }
          sb.append(System.lineSeparator())
          KSResult.fail(KSEvaluationError(b.position, sb.toString()))

        } else {
          val id_eval = KSEvaluation(
            c, c.freshSerial(), parent, nextCount(KSID::class.java), Optional.empty())

          return createID(id.position, id.value, id_eval).flatMap { id ->
            val r = f(b, Optional.of(id))
            c.blocks_by_id.put(id.value, r)
            KSResult.succeed<U, KSEvaluationError>(r)
          }
        }
      } else {
        KSResult.succeed(f(b, Optional.empty()))
      }
    }

    fun createID(
      position : Optional<LexicalPositionType<Path>>,
      value : String,
      data : KSEvaluation)
      : KSResult<KSID<KSEvaluation>, KSEvaluationError> {
      return if (KSID.isValidID(value)) {
        KSResult.succeed(KSID.create(position, value, data))
      } else {
        KSResult.fail(KSEvaluationError(position, "Not a valid identifier"))
      }
    }

    fun referenceID(ksid : KSID<KSEvaluation>) : Unit {
      LOG.trace("reference {} {}", ksid, ksid.position)
      this.id_references.add(ksid)
    }

    fun checkIDs() : KSResult<Unit, KSEvaluationError> {
      LOG.trace("checking id references")

      return KSResult.listMap({ id ->
        if (this.blocks_by_id.containsKey(id.value)) {
          val b = this.blocks_by_id[id.value]
          LOG.trace("resolved {}", id, b)
          KSResult.succeed(Unit)
        } else {
          LOG.debug("nonexistent id {}", id)
          val sb = StringBuilder()
          sb.append("Reference to nonexistent ID.")
          sb.append(System.lineSeparator())

          sb.append("  Current: ")
          sb.append(id.value)
          id.position.ifPresent { pos ->
            sb.append(" at ")
            sb.append(pos)
          }
          sb.append(System.lineSeparator())

          KSResult.fail<Unit, KSEvaluationError>(
            KSEvaluationError(id.position, sb.toString()))
        }
      }, this.id_references).flatMap { x ->
        KSResult.succeed<Unit, KSEvaluationError>(Unit)
      }
    }

    fun checkFootnoteReferences() : KSResult<Unit, KSEvaluationError> {
      LOG.trace("checking footnote references")

      val refs = this.footnote_refs.toList()
      val act_refs = KSResult.listMap({ p ->
        val target = p.first

        LOG.trace("check footnote ref: {}", target)
        if (!this.blocks_by_id.containsKey(target.value)) {
          LOG.debug("nonexistent footnote {}", target.value)
          val sb = StringBuilder()
          sb.append("Reference to nonexistent footnote.")
          sb.append(System.lineSeparator())
          sb.append("  Current: ")
          sb.append(target.value)
          target.position.ifPresent { pos ->
            sb.append(" at ")
            sb.append(pos)
          }
          sb.append(System.lineSeparator())
          KSResult.fail<Unit, KSEvaluationError>(
            KSEvaluationError(target.position, sb.toString()))
        } else {
          val r = this.blocks_by_id.get(target.value)!!
          when (r) {
            is KSBlockFootnote -> {
              KSResult.succeed<Unit, KSEvaluationError>(Unit)
            }
            is KSBlockDocument,
            is KSBlockSection,
            is KSBlockSubsection,
            is KSBlockParagraph,
            is KSBlockImport,
            is KSBlockFormalItem,
            is KSBlockPart     -> {
              val sb = StringBuilder()
              sb.append("Footnote reference to non-footnote.")
              sb.append(System.lineSeparator())
              sb.append("  Reference: ")
              sb.append(target)
              sb.append(System.lineSeparator())
              sb.append("  Target:    ")
              sb.append(when (r) {
                is KSBlockDocument   -> "document"
                is KSBlockSection    -> "section"
                is KSBlockSubsection -> "subsection"
                is KSBlockParagraph  -> "paragraph"
                is KSBlockFormalItem -> "formal-item"
                is KSBlockFootnote   -> throw UnreachableCodeException()
                is KSBlockPart       -> "part"
                is KSBlockImport     -> "import"
              })
              target.position.ifPresent { pos ->
                sb.append(" at ")
                sb.append(pos)
              }
              sb.append(System.lineSeparator())
              KSResult.fail<Unit, KSEvaluationError>(
                KSEvaluationError(target.position, sb.toString()))
            }
          }
        }
      }, refs)

      return act_refs.flatMap {
        u ->
        KSResult.succeed<Unit, KSEvaluationError>(Unit)
      }
    }

    fun <T : KSBlock<KSParse>,
      U : KSBlock<KSEvaluation>>
      recordBlock(
      a : T,
      f : (Context, KSBlockImport<KSParse>, KSSerial) -> KSBlockImport<KSEvaluation>,
      b : U) : U {

      if (a.data.context.importsByElement.containsKey(a)) {
        val bi = a.data.context.importsByElement[a]!!
        val be = a.data.context.importPathsEdgesByElement[bi]!!
        val br = f.invoke(this, bi, b.data.parent)
        LOG.trace("record import {} for {}({})",
          bi, b.javaClass.simpleName, b.data.serial)
        this.imports[b] = br
        this.importsPaths[br] = be
        this.importedBlocks[br] = b
      }

      val number_opt = b.data.number
      number_opt.ifPresent { number ->
        Assertive.require(!this.blocks_by_number.containsKey(number))
        this.blocks_by_number.put(number, b)
      }

      addElement(b)
      return b
    }

    fun setDocumentResult(d : KSBlockDocument<KSEvaluation>) {
      this.document_actual = d
    }

    fun recordFootnote(b : KSBlockFootnote<KSEvaluation>) : Unit {
      val n : KSNumber.HasSectionType = if (this.part_number.isPresent) {
        KSNumberPartSection(this.part_number.asLong, this.section_number)
      } else {
        KSNumberSection(this.section_number)
      }

      val notes = if (this.section_footnotes.containsKey(n)) {
        this.section_footnotes[n]!!
      } else {
        mutableMapOf()
      }

      val i = b.id.get()
      LOG.trace("save footnote {} → {}", n, i)
      notes[i] = b
      section_footnotes[n] = notes

      Assertive.require(footnotesAll.containsKey(i) == false)
      footnotesAll[i] = b
    }

    fun referenceFootnote(
      eref : KSInlineFootnoteReference<KSEvaluation>,
      target : KSID<KSEvaluation>)
      : KSResult<KSFootnoteReference<KSEvaluation>, KSEvaluationError> {

      referenceID(target)

      val refs =
        if (this.footnote_refs.containsKey(target)) {
          this.footnote_refs[target]!!
        } else {
          mutableMapOf()
        }

      val count = refs.size.toLong()
      val ref = KSFootnoteReference(eref, target, target.data, count)
      Assertive.require(!refs.containsKey(count))
      refs[count] = ref
      this.footnote_refs[target] = refs
      return KSResult.succeed(ref)
    }

    fun addElement(e : KSElement<KSEvaluation>) {
      LOG.trace("record element {}", e.data.serial)
      this.all_by_serial.put(e.data.serial, e)
    }
  }

  override fun evaluate(
    document : KSBlockDocument<KSParse>,
    document_file : Path)
    : KSResult<KSBlockDocument<KSEvaluation>, KSEvaluationError> {
    val c = Context.create(document_file)
    val act_doc = evaluateDocument(c, document, KSSerial(0L))
    return act_doc.flatMap { d ->
      dumpCounts(c)
      checkIDs(c, d).flatMap { d ->
        c.checkFootnoteReferences().flatMap { k ->
          KSResult.succeed<KSBlockDocument<KSEvaluation>, KSEvaluationError>(d)
        }
      }
    }
  }

  private fun dumpCounts(c : Context) {
    if (LOG.isTraceEnabled) {
      val cc = c.count_by_class
      val it = cc.iterator()
      while (it.hasNext()) {
        val e = it.next()
        LOG.trace("count: {}: {}", e.key.simpleName, e.value)
      }
    }
  }

  private fun checkIDs(
    c : KSEvaluator.Context,
    d : KSBlockDocument<KSEvaluation>)
    : KSResult<KSBlockDocument<KSEvaluation>, KSEvaluationError> {

    return c.checkIDs().flatMap { ignored ->
      c.setDocumentResult(d)
      KSResult.succeed<KSBlockDocument<KSEvaluation>, KSEvaluationError>(d)
    }
  }

  private fun evaluateDocument(
    c : Context,
    d : KSBlockDocument<KSParse>,
    parent : KSSerial)
    : KSResult<KSBlockDocument<KSEvaluation>, KSEvaluationError> =
    when (d) {
      is KSBlockDocument.KSBlockDocumentWithParts    ->
        evaluateDocumentWithParts(c, d, parent)
      is KSBlockDocument.KSBlockDocumentWithSections ->
        evaluateDocumentWithSections(c, d, parent)
    }

  private fun evaluateType(
    c : Context,
    t : KSType<KSParse>,
    parent : KSSerial)
    : KSResult<KSType<KSEvaluation>, KSEvaluationError> {
    val serial = c.freshSerial()
    val eval = KSEvaluation(
      c, serial, parent, c.nextCount(KSType::class.java), Optional.empty())
    return KSResult.succeed<KSType<KSEvaluation>, KSEvaluationError>(
      KSType.create(t.position, t.value, eval))
  }

  private fun evaluateTypeOptional(
    c : Context,
    t : Optional<KSType<KSParse>>,
    parent : KSSerial)
    : KSResult<Optional<KSType<KSEvaluation>>, KSEvaluationError> {
    if (t.isPresent) {
      return evaluateType(c, t.get(), parent).flatMap { type ->
        KSResult.succeed<Optional<KSType<KSEvaluation>>, KSEvaluationError>(
          Optional.of(type))
      }
    }
    return KSResult.succeed(Optional.empty())
  }

  private fun evaluateDocumentWithSections(
    c : Context,
    d : KSBlockDocumentWithSections<KSParse>,
    parent : KSSerial)
    : KSResult<KSBlockDocumentWithSections<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()

    c.part_number = OptionalLong.empty()
    c.section_number = 0L
    c.subsection_number = OptionalLong.empty()
    c.content_number = 0L

    val act_content = KSResult.listMapIndexed({ e, i ->
      c.section_number = i + 1L
      evaluateSection(c, e, serial)
    }, d.content)

    val act_type = evaluateTypeOptional(c, d.type, serial)

    val act_title = KSResult.listMap(
      { e -> evaluateInlineText (c, e, serial) }, d.title)

    return act_content.flatMap { content ->
      act_title.flatMap { title ->
        act_type.flatMap { type ->
          val eval = KSEvaluation(
            c, serial, parent, c.nextCount(KSBlockDocumentWithSections::class.java), Optional.empty())
          c.recordID(c, d, serial, { d, id ->
            val d_eval = KSBlockDocumentWithSections(
              d.position, d.square, eval, id, type, title, content)
            c.recordBlock(d, { c, i, s -> translateImport(c, i, s) }, d_eval)
          })
        }
      }
    }
  }

  private fun translateImport(
    c : Context,
    i : KSBlockImport<KSParse>,
    parent : KSSerial)
    : KSBlockImport<KSEvaluation> {

    val es = c.freshSerial()
    val ie = KSEvaluation(
      c, es, parent, c.nextCount(KSInlineText::class.java), Optional.empty())
    val te = KSEvaluation(
      c, c.freshSerial(), es, c.nextCount(KSBlockImport::class.java), Optional.empty())
    val file = KSInlineText(i.file.position, false, te, true, i.file.text)
    return KSBlockImport(
      i.position, i.square, ie, Optional.empty(), Optional.empty(), file)
  }

  private fun evaluateDocumentWithParts(
    c : Context,
    d : KSBlockDocumentWithParts<KSParse>,
    parent : KSSerial)
    : KSResult<KSBlockDocumentWithParts<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()

    val act_content = KSResult.listMapIndexed({ e, i ->
      c.part_number = OptionalLong.of(i + 1L)
      evaluatePart(c, e, serial)
    }, d.content)
    val act_title = KSResult.listMap(
      { e -> evaluateInlineText (c, e, serial) }, d.title)
    val act_type = evaluateTypeOptional(c, d.type, serial)

    return act_content.flatMap { content ->
      act_title.flatMap { title ->
        act_type.flatMap { type ->
          val ev = KSEvaluation(
            c, serial, parent, c.nextCount(KSBlockDocumentWithParts::class.java), Optional.empty())
          c.recordID(c, d, serial, { ss, id ->
            val d_eval = KSBlockDocumentWithParts(
              d.position, d.square, ev, id, type, title, content)
            c.recordBlock(d, { c, i, s -> translateImport(c, i, s) }, d_eval)
          })
        }
      }
    }
  }

  private fun evaluateInlineText(
    c : Context,
    e : KSInlineText<KSParse>,
    parent : KSSerial)
    : KSResult<KSInlineText<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()
    return if (e.data.context.includesByTexts.containsKey(e)) {
      val ii = e.data.context.includesByTexts[e]!!
      evaluateInlineInclude(c, ii, parent).flatMap { inc ->
        val eval = KSEvaluation(
          c, serial, parent, c.nextCount(KSInlineText::class.java), Optional.empty())
        val re = KSInlineText(e.position, false, eval, e.quote, e.text)
        c.includesByText[re] = inc
        c.addElement(re)
        KSResult.succeed<KSInlineText<KSEvaluation>, KSEvaluationError>(re)
      }
    } else {
      val eval = KSEvaluation(
        c, serial, parent, c.nextCount(KSInlineText::class.java), Optional.empty())
      val re = KSInlineText(e.position, false, eval, e.quote, e.text)
      c.addElement(re)
      KSResult.succeed<KSInlineText<KSEvaluation>, KSEvaluationError>(re)
    }
  }

  private fun evaluateInlineImage(
    c : Context,
    e : KSInlineImage<KSParse>,
    parent : KSSerial)
    : KSResult<KSInlineImage<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()

    val act_type = evaluateTypeOptional(c, e.type, serial)
    val act_content = KSResult.listMap(
      { ci -> evaluateInlineText(c, ci, serial) }, e.content)

    return act_content.flatMap { content ->
      act_type.flatMap { type ->
        val eval = KSEvaluation(
          c, serial, parent, c.nextCount(KSInlineImage::class.java), Optional.empty())
        val re = KSInlineImage(
          e.position, e.square, eval, type, e.target, e.size, content)
        c.addElement(re)
        KSResult.succeed<KSInlineImage<KSEvaluation>, KSEvaluationError>(re)
      }
    }
  }

  private fun evaluateInlineTerm(
    c : Context,
    e : KSInlineTerm<KSParse>,
    parent : KSSerial)
    : KSResult<KSInlineTerm<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()

    val act_content = KSResult.listMap(
      { ci -> evaluateInlineText(c, ci, serial) }, e.content)
    val act_type = evaluateTypeOptional(c, e.type, serial)

    return act_content.flatMap { content ->
      act_type.flatMap { type ->
        val eval = KSEvaluation(
          c, serial, parent, c.nextCount(KSInlineTerm::class.java), Optional.empty())
        val re = KSInlineTerm(e.position, e.square, eval, type, content)
        c.addElement(re)
        KSResult.succeed<KSInlineTerm<KSEvaluation>, KSEvaluationError>(re)
      }
    }
  }

  private fun evaluateInlineVerbatim(
    c : Context,
    e : KSInlineVerbatim<KSParse>,
    parent : KSSerial)
    : KSResult<KSInlineVerbatim<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()
    val eval = KSEvaluation(
      c, serial, parent, c.nextCount(KSInlineVerbatim::class.java), Optional.empty())
    val act_content = evaluateInlineText(c, e.text, serial)
    val act_type = evaluateTypeOptional(c, e.type, serial)

    return act_content.flatMap { text ->
      act_type.flatMap { type ->
        val re = KSInlineVerbatim(e.position, e.square, eval, type, text)
        c.addElement(re)
        KSResult.succeed<KSInlineVerbatim<KSEvaluation>, KSEvaluationError>(re)
      }
    }
  }

  private fun evaluateInline(
    c : Context,
    e : KSInline<KSParse>,
    parent : KSSerial)
    : KSResult<KSInline<KSEvaluation>, KSEvaluationError> =
    when (e) {
      is KSInlineLink              -> evaluateInlineLink(c, e, parent)
      is KSInlineText              -> evaluateInlineText(c, e, parent)
      is KSInlineVerbatim          -> evaluateInlineVerbatim(c, e, parent)
      is KSInlineTerm              -> evaluateInlineTerm(c, e, parent)
      is KSInlineImage             -> evaluateInlineImage(c, e, parent)
      is KSInlineListOrdered       -> evaluateInlineListOrdered(c, e, parent)
      is KSInlineListUnordered     -> evaluateInlineListUnordered(c, e, parent)
      is KSInlineTable             -> evaluateInlineTable(c, e, parent)
      is KSInlineFootnoteReference -> evaluateInlineFootnoteReference(c, e, parent)
      is KSInlineInclude           -> evaluateInlineInclude(c, e, parent)
    }

  private fun evaluateInlineInclude(
    c : Context,
    e : KSInlineInclude<KSParse>,
    parent : KSSerial)
    : KSResult<KSInlineInclude<KSEvaluation>, KSEvaluationError> {

    Assertive.require(e.data.context.includePaths.containsKey(e))
    val p = e.data.context.includePaths[e]!!
    Assertive.require(e.data.context.includes.containsKey(p))
    val t = e.data.context.includes[p]!!

    val serial = c.freshSerial()
    val eval = KSEvaluation(
      c, serial, parent, c.nextCount(KSInlineInclude::class.java), Optional.empty())
    return evaluateInlineText(c, e.file, serial).flatMap { file ->
      val re = KSInlineInclude(e.position, e.square, eval, file)
      c.includes_by_file[p] = t
      c.includes_by_serial[serial] = t
      KSResult.succeed<KSInlineInclude<KSEvaluation>, KSEvaluationError>(re)
    }
  }

  private fun evaluateInlineFootnoteReference(
    c : Context,
    e : KSInlineFootnoteReference<KSParse>,
    parent : KSSerial)
    : KSResult<KSInlineFootnoteReference<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()
    val id_eval = KSEvaluation(
      c, c.freshSerial(), serial, c.nextCount(KSID::class.java), Optional.empty())
    val act_id = c.createID(e.target.position, e.target.value, id_eval)

    return act_id.flatMap { id ->
      val eval = KSEvaluation(
        c, serial, parent, c.nextCount(KSInlineFootnoteReference::class.java), Optional.empty())
      val re = KSInlineFootnoteReference(e.position, e.square, eval, id)
      c.addElement(re)
      c.referenceFootnote(re, id).flatMap { ref ->
        KSResult.succeed<KSInlineFootnoteReference<KSEvaluation>, KSEvaluationError>(re)
      }
    }
  }

  private fun evaluateInlineTable(
    c : Context,
    e : KSInlineTable<KSParse>,
    parent : KSSerial)
    : KSResult<KSInlineTable<KSEvaluation>, KSEvaluationError> {

    if (c.enclosing_table) {
      val sb = StringBuilder()
      sb.append("Tables cannot be nested.")
      sb.append(System.lineSeparator())
      if (c.enclosing_table_pos.isPresent) {
        sb.append("  Enclosing table at: ")
        sb.append(c.enclosing_table_pos.get())
      }
      sb.append(System.lineSeparator())
      return KSResult.fail(KSEvaluationError(e.position, sb.toString()))
    }

    val serial = c.freshSerial()

    c.enclosing_table = true
    c.enclosing_table_pos = e.position
    try {
      val act_head =
        evaluateInlineTableHeadOptional(c, e, serial)
      val act_summary =
        evaluateInlineTableSummary(c, e.summary, serial)
      val act_body =
        evaluateInlineTableBody(c, e.body, serial)
      val act_type =
        evaluateTypeOptional(c, e.type, serial)

      return act_body.flatMap { body ->
        act_summary.flatMap { summary ->
          act_head.flatMap { head ->
            act_type.flatMap { type ->
              val eval = KSEvaluation(
                c, serial, parent, c.nextCount(KSInlineTable::class.java), Optional.empty())
              val table = KSInlineTable(
                e.position, e.square, eval, type, summary, head, body)
              c.addElement(table)
              KSResult.succeed<KSInlineTable<KSEvaluation>, KSEvaluationError>(table)
            }
          }
        }
      }
    } finally {
      c.enclosing_table = false
      c.enclosing_table_pos = Optional.empty()
    }
  }

  private fun evaluateInlineTableHeadOptional(
    c : Context,
    e : KSInlineTable<KSParse>,
    parent : KSSerial)
    : KSResult<Optional<KSTableHead<KSEvaluation>>, KSEvaluationError> {

    val serial = c.freshSerial()
    return if (e.head.isPresent) {
      val eh = e.head.get()

      evaluateInlineTableCheckColumnCount(e).flatMap { x ->
        val act_names =
          KSResult.listMap({
            name ->
            evaluateInlineTableHeadColumnName(c, name, serial)
          }, eh.column_names)

        val act_type =
          evaluateTypeOptional(c, eh.type, serial)

        act_names.flatMap { names ->
          act_type.flatMap { t ->
            val eval = KSEvaluation(
              c, serial, parent, c.nextCount(KSTableHead::class.java), Optional.empty())
            val head = KSTableHead(eh.position, e.square, eval, names, t)
            c.addElement(head)
            KSResult.succeed<Optional<KSTableHead<KSEvaluation>>, KSEvaluationError>(
              Optional.of(head))
          }
        }
      }
    } else {
      KSResult.succeed(Optional.empty())
    }
  }

  private fun evaluateInlineTableHeadColumnName(
    c : Context,
    name : KSTableHeadColumnName<KSParse>,
    parent : KSSerial)
    : KSResult<KSTableHeadColumnName<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()

    val act_content = KSResult.listMap(
      { t -> evaluateInlineText(c, t, serial) }, name.content)
    val act_type =
      evaluateTypeOptional(c, name.type, serial)

    return act_content.flatMap { content ->
      act_type.flatMap { type ->
        val eval = KSEvaluation(
          c, serial, parent, c.nextCount(KSTableHeadColumnName::class.java), Optional.empty())
        val re = KSTableHeadColumnName(name.position, name.square, eval, content, type)
        c.addElement(re)
        KSResult.succeed<KSTableHeadColumnName<KSEvaluation>, KSEvaluationError>(re)
      }
    }
  }

  private fun evaluateInlineTableBody(
    c : Context,
    b : KSTableBody<KSParse>,
    parent : KSSerial)
    : KSResult<KSTableBody<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()
    val act_rows = KSResult.listMap(
      { row -> evaluateInlineTableRow(c, row, serial) }, b.rows)

    return act_rows.flatMap { rows ->
      val eval = KSEvaluation(
        c, serial, parent, c.nextCount(KSTableBody::class.java), Optional.empty())
      val re = KSTableBody(b.position, b.square, eval, rows)
      c.addElement(re)
      KSResult.succeed<KSTableBody<KSEvaluation>, KSEvaluationError>(re)
    }
  }

  private fun evaluateInlineTableRow(
    c : Context,
    row : KSTableBodyRow<KSParse>,
    parent : KSSerial)
    : KSResult<KSTableBodyRow<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()
    val act_cells =
      KSResult.listMap({ cell ->
        evaluateInlineTableCell(c, cell, serial)
      }, row.cells)
    val act_type =
      evaluateTypeOptional(c, row.type, serial)

    return act_cells.flatMap { cells ->
      act_type.flatMap { type ->
        val eval = KSEvaluation(
          c, serial, parent, c.nextCount(KSTableBodyRow::class.java), Optional.empty())
        val re = KSTableBodyRow(row.position, row.square, eval, cells, type)
        c.addElement(re)
        KSResult.succeed<KSTableBodyRow<KSEvaluation>, KSEvaluationError>(re)
      }
    }
  }

  private fun evaluateInlineTableCell(
    c : Context,
    cell : KSTableBodyCell<KSParse>,
    parent : KSSerial)
    : KSResult<KSTableBodyCell<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()
    val act_content =
      KSResult.listMap({ cc -> evaluateInline(c, cc, serial) }, cell.content)
    val act_type =
      evaluateTypeOptional(c, cell.type, serial)

    return act_content.flatMap { content ->
      act_type.flatMap { type ->
        val eval = KSEvaluation(
          c, serial, parent, c.nextCount(KSTableBodyCell::class.java), Optional.empty())
        val re = KSTableBodyCell(cell.position, cell.square, eval, content, type)
        c.addElement(re)
        KSResult.succeed<KSTableBodyCell<KSEvaluation>, KSEvaluationError>(re)
      }
    }
  }

  private fun evaluateInlineTableSummary(
    c : Context,
    s : KSTableSummary<KSParse>,
    parent : KSSerial)
    : KSResult<KSTableSummary<KSEvaluation>, KSEvaluationError> {
    val serial = c.freshSerial()
    val act_content =
      KSResult.listMap({ cc -> evaluateInlineText(c, cc, serial) }, s.content)
    return act_content.flatMap { content ->
      val eval = KSEvaluation(
        c, serial, parent, c.nextCount(KSTableSummary::class.java), Optional.empty())
      val re = KSTableSummary(s.position, s.square, eval, content)
      c.addElement(re)
      KSResult.succeed<KSTableSummary<KSEvaluation>, KSEvaluationError>(re)
    }
  }

  private fun evaluateInlineTableCheckColumnCount(
    e : KSInlineTable<KSParse>)
    : KSResult<List<Unit>, KSEvaluationError> {
    return if (e.head.isPresent) {
      val head = e.head.get()
      val col_count = head.column_names.size
      KSResult.listMapIndexed({ row, i ->
        if (row.cells.size != col_count) {
          val sb = StringBuilder()
          sb.append("Row cell count does not match the number of declared columns.")
          sb.append(System.lineSeparator())
          sb.append("  Row:      ")
          sb.append(i)
          sb.append(System.lineSeparator())
          sb.append("  Expected: ")
          sb.append(col_count)
          sb.append(" columns")
          sb.append(System.lineSeparator())
          sb.append("  Received: ")
          sb.append(row.cells.size)
          sb.append(System.lineSeparator())
          val m = sb.toString()
          KSResult.fail<Unit, KSEvaluationError>(
            KSEvaluationError(row.position, m))
        } else {
          KSResult.succeed(Unit)
        }
      }, e.body.rows)
    } else {
      KSResult.succeed(listOf())
    }
  }

  private fun evaluateInlineListUnordered(
    c : Context,
    e : KSInlineListUnordered<KSParse>,
    parent : KSSerial)
    : KSResult<KSInlineListUnordered<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()
    val act_items =
      KSResult.listMap({ item -> evaluateListItem(c, item, serial) }, e.content)

    return act_items.flatMap { items ->
      val eval = KSEvaluation(
        c, serial, parent, c.nextCount(KSInlineListUnordered::class.java), Optional.empty())
      val re = KSInlineListUnordered(e.position, e.square, eval, items)
      c.addElement(re)
      KSResult.succeed<KSInlineListUnordered<KSEvaluation>, KSEvaluationError>(re)
    }
  }

  private fun evaluateInlineListOrdered(
    c : Context,
    e : KSInlineListOrdered<KSParse>,
    parent : KSSerial)
    : KSResult<KSInlineListOrdered<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()
    val act_items = KSResult.listMap(
      { item -> evaluateListItem(c, item, serial) }, e.content)

    return act_items.flatMap { items ->
      val eval = KSEvaluation(
        c, serial, parent, c.nextCount(KSInlineListOrdered::class.java), Optional.empty())
      val re = KSInlineListOrdered(e.position, e.square, eval, items)
      c.addElement(re)
      KSResult.succeed<KSInlineListOrdered<KSEvaluation>, KSEvaluationError>(re)
    }
  }

  private fun evaluateListItem(
    c : Context,
    item : KSListItem<KSParse>,
    parent : KSSerial)
    : KSResult<KSListItem<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()
    val act_item_content = KSResult.listMap(
      { cc -> evaluateInline(c, cc, serial) }, item.content)

    return act_item_content.flatMap { content ->
      val eval = KSEvaluation(
        c, serial, parent, c.nextCount(KSListItem::class.java), Optional.empty())
      val re = KSListItem(item.position, item.square, eval, content)
      c.addElement(re)
      KSResult.succeed<KSListItem<KSEvaluation>, KSEvaluationError>(re)
    }
  }

  private fun evaluateInlineLink(
    c : Context,
    e : KSInlineLink<KSParse>,
    parent : KSSerial)
    : KSResult<KSInlineLink<KSEvaluation>, KSEvaluationError> {

    val act = e.actual
    val serial = c.freshSerial()
    return when (act) {
      is KSLinkExternal -> {
        val act_content = KSResult.listMap(
          { cc -> evaluateLinkContent(c, cc, serial) }, act.content)

        act_content.flatMap { content ->
          val link = KSLinkExternal(act.position, act.target, content)
          val eval = KSEvaluation(
            c, serial, parent, c.nextCount(KSInlineLink::class.java), Optional.empty())
          val re = KSInlineLink(e.position, e.square, eval, link)
          c.addElement(re)
          KSResult.succeed<KSInlineLink<KSEvaluation>, KSEvaluationError>(re)
        }
      }

      is KSLinkInternal -> {
        val act_content = KSResult.listMap(
          { cc -> evaluateLinkContent(c, cc, serial) }, act.content)

        val id_eval = KSEvaluation(
          c, serial, parent, c.nextCount(KSID::class.java), Optional.empty())
        val act_id = c.createID(act.target.position, act.target.value, id_eval)

        return act_id.flatMap { id ->
          c.referenceID(id)
          act_content.flatMap { content ->
            val link = KSLinkInternal(act.position, id, content)
            val eval = KSEvaluation(
              c, serial, parent, c.nextCount(KSInlineLink::class.java), Optional.empty())
            val re = KSInlineLink(e.position, e.square, eval, link)
            c.addElement(re)
            KSResult.succeed<KSInlineLink<KSEvaluation>, KSEvaluationError>(re)
          }
        }
      }
    }
  }

  private fun evaluateLinkContent(
    c : KSEvaluator.Context,
    cc : KSLinkContent<KSParse>,
    parent : KSSerial)
    : KSResult<KSLinkContent<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()
    return when (cc) {
      is KSLinkContent.KSLinkText  ->
        evaluateInlineText(c, cc.actual, serial).flatMap { t ->
          val ev = KSEvaluation(
            c, serial, parent, c.nextCount(KSLinkContent::class.java), Optional.empty())
          KSResult.succeed<KSLinkContent<KSEvaluation>, KSEvaluationError>(
            KSLinkText(cc.position, ev, t))
        }
      is KSLinkContent.KSLinkImage -> {
        evaluateInlineImage(c, cc.actual, serial).flatMap { t ->
          val ev = KSEvaluation(
            c, serial, parent, c.nextCount(KSLinkContent::class.java), Optional.empty())
          KSResult.succeed<KSLinkContent<KSEvaluation>, KSEvaluationError>(
            KSLinkImage(cc.position, ev, t))
        }
      }
    }
  }

  private fun evaluateSection(
    c : Context,
    e : KSBlockSection<KSParse>,
    parent : KSSerial)
    : KSResult<KSBlockSection<KSEvaluation>, KSEvaluationError> =
    when (e) {
      is KSBlockSectionWithSubsections ->
        evaluateSectionWithSubsections(c, e, parent)
      is KSBlockSectionWithContent     ->
        evaluateSectionWithContent(c, e, parent)
    }

  private fun evaluateSectionWithContent(
    c : Context,
    e : KSBlockSectionWithContent<KSParse>,
    parent : KSSerial)
    : KSResult<KSBlockSectionWithContent<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()

    c.content_number = 0L
    val act_content = KSResult.listMapIndexed({ cc, i ->
      c.content_number += 1L
      evaluateSubsectionContent(c, cc, serial)
    }, e.content)
    val act_title = KSResult.listMap({ e ->
      evaluateInlineText (c, e, serial)
    }, e.title)
    val act_type =
      evaluateTypeOptional(c, e.type, serial)

    return act_content.flatMap { content ->
      act_title.flatMap { title ->
        act_type.flatMap { type ->
          val num : KSNumber = if (c.part_number.isPresent) {
            KSNumberPartSection(c.part_number.asLong, c.section_number)
          } else {
            KSNumberSection(c.section_number)
          }

          val ev = KSEvaluation(
            c, serial, parent, c.nextCount(KSBlockSectionWithContent::class.java), Optional.of(num))
          c.recordID(c, e, serial, { e, id ->
            val e_eval = KSBlockSectionWithContent(
              e.position, e.square, ev, type, id, title, content)
            c.recordBlock(e, { c, i, s -> translateImport(c, i, s) }, e_eval)
          })
        }
      }
    }
  }

  private fun evaluateSubsectionContent(
    c : Context,
    cc : KSSubsectionContent<KSParse>,
    parent : KSSerial)
    : KSResult<KSSubsectionContent<KSEvaluation>, KSEvaluationError> =
    when (cc) {
      is KSSubsectionContent.KSSubsectionParagraph  ->
        evaluateParagraph(c, cc.paragraph, parent).map { x ->
          KSSubsectionParagraph(x)
        }
      is KSSubsectionContent.KSSubsectionFormalItem ->
        evaluateFormalItem(c, cc.formal, parent).map { x ->
          KSSubsectionFormalItem(x)
        }
      is KSSubsectionContent.KSSubsectionFootnote   ->
        evaluateFootnote(c, cc.footnote, parent).map { x ->
          KSSubsectionFootnote(x)
        }
    }

  private fun evaluateFootnote(
    c : Context,
    f : KSBlockFootnote<KSParse>,
    parent : KSSerial)
    : KSResult<KSBlockFootnote<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()

    LOG.trace("decrement content number for footnote")
    c.content_number = c.content_number - 1L

    val act_content =
      KSResult.listMap({ i -> evaluateInline(c, i, serial) }, f.content)
    val act_type =
      evaluateTypeOptional(c, f.type, serial)

    return act_content.flatMap { content ->
      act_type.flatMap { type ->
        val ev = KSEvaluation(
          c, serial, parent, c.nextCount(KSBlockFootnote::class.java), Optional.empty())
        c.recordID(c, f, serial, { f, id ->
          val f_eval = KSBlockFootnote(
            f.position,
            f.square,
            ev,
            id.get(),
            type,
            content)
          c.recordFootnote(f_eval)
          c.recordBlock(f, { c, i, s -> translateImport(c, i, s) }, f_eval)
        })
      }
    }
  }

  private fun evaluateFormalItem(
    c : Context,
    f : KSBlockFormalItem<KSParse>,
    parent : KSSerial)
    : KSResult<KSBlockFormalItem<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()

    val act_content =
      KSResult.listMap({ i -> evaluateInline(c, i, serial) }, f.content)
    val act_title =
      KSResult.listMap({ i -> evaluateInlineText(c, i, serial) }, f.title)
    val act_type =
      evaluateTypeOptional(c, f.type, serial)

    return act_content.flatMap { content ->
      act_title.flatMap { title ->
        act_type.flatMap { type ->

          val num : KSNumber = if (c.part_number.isPresent) {
            if (c.subsection_number.isPresent) {
              KSNumberPartSectionSubsectionContent(
                c.part_number.asLong,
                c.section_number,
                c.subsection_number.asLong,
                c.content_number)
            } else {
              KSNumberPartSectionContent(
                c.part_number.asLong,
                c.section_number,
                c.content_number)
            }
          } else {
            if (c.subsection_number.isPresent) {
              KSNumberSectionSubsectionContent(
                c.section_number,
                c.subsection_number.asLong,
                c.content_number)
            } else {
              KSNumberSectionContent(
                c.section_number,
                c.content_number)
            }
          }

          val ev = KSEvaluation(
            c, serial, parent, c.nextCount(KSBlockFormalItem::class.java), Optional.of(num))
          c.recordID(c, f, serial, { f, id ->
            val f_eval = KSBlockFormalItem(
              f.position, f.square, ev, type, id, title, content)
            c.recordBlock(f, { c, i, s -> translateImport(c, i, s) }, f_eval)
          })
        }
      }
    }
  }

  private fun evaluateParagraph(
    c : Context,
    p : KSBlockParagraph<KSParse>,
    parent : KSSerial)
    : KSResult<KSBlockParagraph<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()

    val act_content =
      KSResult.listMap({ i -> evaluateInline(c, i, serial) }, p.content)
    val act_type =
      evaluateTypeOptional(c, p.type, serial)

    return act_content.flatMap { content ->
      act_type.flatMap { type ->

        val num : KSNumber = if (c.part_number.isPresent) {
          if (c.subsection_number.isPresent) {
            KSNumberPartSectionSubsectionContent(
              c.part_number.asLong,
              c.section_number,
              c.subsection_number.asLong,
              c.content_number)
          } else {
            KSNumberPartSectionContent(
              c.part_number.asLong,
              c.section_number,
              c.content_number)
          }
        } else {
          if (c.subsection_number.isPresent) {
            KSNumberSectionSubsectionContent(
              c.section_number,
              c.subsection_number.asLong,
              c.content_number)
          } else {
            KSNumberSectionContent(
              c.section_number,
              c.content_number)
          }
        }

        val ev = KSEvaluation(
          c, serial, parent, c.nextCount(KSBlockParagraph::class.java), Optional.of(num))
        c.recordID(c, p, serial, { p, id ->
          val p_eval = KSBlockParagraph(
            p.position, p.square, ev, type, id, content)
          c.recordBlock(p, { c, i, s -> translateImport(c, i, s) }, p_eval)
        })
      }
    }
  }

  private fun evaluateSectionWithSubsections(
    c : Context,
    e : KSBlockSectionWithSubsections<KSParse>,
    parent : KSSerial)
    : KSResult<KSBlockSectionWithSubsections<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()

    val act_content = KSResult.listMapIndexed({ cc, i ->
      c.subsection_number = OptionalLong.of(i + 1L)
      evaluateSubsection(c, cc, serial)
    }, e.content)
    val act_title = KSResult.listMap(
      { e -> evaluateInlineText (c, e, serial) }, e.title)
    val act_type =
      evaluateTypeOptional(c, e.type, serial)

    return act_content.flatMap { content ->
      act_title.flatMap { title ->
        act_type.flatMap { type ->

          val num : KSNumber = if (c.part_number.isPresent) {
            KSNumberPartSection(c.part_number.asLong, c.section_number)
          } else {
            KSNumberSection(c.section_number)
          }

          val ev = KSEvaluation(
            c, serial, parent, c.nextCount(KSBlockSectionWithSubsections::class.java), Optional.of(num))
          c.recordID(c, e, serial, { e, id ->
            val e_eval = KSBlockSectionWithSubsections(
              e.position, e.square, ev, type, id, title, content)
            c.recordBlock(e, { c, i, s -> translateImport(c, i, s) }, e_eval)
          })
        }
      }
    }
  }

  private fun evaluateSubsection(
    c : Context,
    ss : KSBlockSubsection<KSParse>,
    parent : KSSerial)
    : KSResult<KSBlockSubsection<KSEvaluation>, KSEvaluationError> {

    Assertive.require(c.subsection_number.isPresent)

    val serial = c.freshSerial()

    c.content_number = 0L
    val act_content =
      KSResult.listMapIndexed({ e, i ->
        c.content_number += 1L
        evaluateSubsectionContent(c, e, serial)
      }, ss.content)
    val act_title = KSResult.listMap(
      { e -> evaluateInlineText (c, e, serial) }, ss.title)
    val act_type =
      evaluateTypeOptional(c, ss.type, serial)

    return act_content.flatMap { content ->
      act_title.flatMap { title ->
        act_type.flatMap { type ->

          val num : KSNumber = if (c.part_number.isPresent) {
            KSNumberPartSectionSubsection(
              c.part_number.asLong,
              c.section_number,
              c.subsection_number.asLong)
          } else {
            KSNumberSectionSubsection(
              c.section_number,
              c.subsection_number.asLong)
          }

          val ev = KSEvaluation(
            c, serial, parent, c.nextCount(KSBlockSubsection::class.java), Optional.of(num))
          c.recordID(c, ss, serial, { ss, id ->
            val ss_eval = KSBlockSubsection(
              ss.position, ss.square, ev, type, id, title, content)
            c.recordBlock(ss, { c, i, s -> translateImport(c, i, s) }, ss_eval)
          })
        }
      }
    }
  }

  private fun evaluatePart(
    c : Context,
    e : KSBlockPart<KSParse>,
    parent : KSSerial) : KSResult<KSBlockPart<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()

    c.section_number = 0L
    c.subsection_number = OptionalLong.empty()
    c.content_number = 0L

    val act_content = KSResult.listMapIndexed({ cc, i ->
      c.section_number = i + 1L
      evaluateSection(c, cc, serial)
    }, e.content)

    val act_title = KSResult.listMap(
      { e -> evaluateInlineText (c, e, serial) }, e.title)
    val act_type =
      evaluateTypeOptional(c, e.type, serial)

    return act_content.flatMap { content ->
      act_title.flatMap { title ->
        act_type.flatMap { type ->
          val ev = KSEvaluation(
            c, serial, parent, c.nextCount(KSBlockPart::class.java), Optional.of(KSNumberPart(c.part_number.asLong)))
          c.recordID(c, e, serial, { e, id ->
            val e_eval = KSBlockPart(
              e.position, e.square, ev, type, id, title, content)
            c.recordBlock(e, { c, i, s -> translateImport(c, i, s) }, e_eval)
          })
        }
      }
    }
  }
}
