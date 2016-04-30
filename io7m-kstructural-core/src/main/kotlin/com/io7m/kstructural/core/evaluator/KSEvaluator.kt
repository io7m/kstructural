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

import com.io7m.kstructural.core.KSBlock
import com.io7m.kstructural.core.KSBlock.KSBlockDocument
import com.io7m.kstructural.core.KSBlock.KSBlockDocument.KSBlockDocumentWithParts
import com.io7m.kstructural.core.KSBlock.KSBlockDocument.KSBlockDocumentWithSections
import com.io7m.kstructural.core.KSBlock.KSBlockParagraph
import com.io7m.kstructural.core.KSBlock.KSBlockPart
import com.io7m.kstructural.core.KSBlock.KSBlockSection
import com.io7m.kstructural.core.KSBlock.KSBlockSection.KSBlockSectionWithContent
import com.io7m.kstructural.core.KSBlock.KSBlockSection.KSBlockSectionWithSubsections
import com.io7m.kstructural.core.KSBlock.KSBlockSubsection
import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSInline
import com.io7m.kstructural.core.KSInline.KSInlineImage
import com.io7m.kstructural.core.KSInline.KSInlineLink
import com.io7m.kstructural.core.KSInline.KSInlineTerm
import com.io7m.kstructural.core.KSInline.KSInlineText
import com.io7m.kstructural.core.KSInline.KSInlineVerbatim
import com.io7m.kstructural.core.KSLink.KSLinkExternal
import com.io7m.kstructural.core.KSLink.KSLinkInternal
import com.io7m.kstructural.core.KSLinkContent
import com.io7m.kstructural.core.KSLinkContent.KSLinkImage
import com.io7m.kstructural.core.KSLinkContent.KSLinkText
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.core.KSSubsectionContent
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionParagraph
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
import java.util.HashMap
import java.util.Optional
import java.util.OptionalLong

object KSEvaluator : KSEvaluatorType {

  private val LOG = LoggerFactory.getLogger(KSEvaluator::class.java)

  private data class Context(
    private var serial_pool : Long = 0L,
    private val blocks_by_id : MutableMap<String, KSBlock<KSEvaluation>> = HashMap(),
    private val blocks_by_number : MutableMap<KSNumber, KSBlock<KSEvaluation>> = HashMap(),
    private val id_references : MutableList<KSID<KSEvaluation>> = mutableListOf())
  : KSEvaluationContextType {

    private lateinit var document_actual : KSBlockDocument<KSEvaluation>

    override val document : KSBlockDocument<KSEvaluation>
      get() = document_actual

    override fun elementSegmentPrevious(
      b : KSBlock<KSEvaluation>) : Optional<KSBlock<KSEvaluation>> =
      when (b) {
        is KSBlock.KSBlockDocument -> Optional.empty()
        is KSBlock.KSBlockSection,
        is KSBlock.KSBlockSubsection,
        is KSBlock.KSBlockParagraph,
        is KSBlock.KSBlockPart     -> {
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
        is KSBlock.KSBlockDocument ->
          Optional.empty()
        is KSBlock.KSBlockSubsection,
        is KSBlock.KSBlockParagraph,
        is KSBlock.KSBlockPart,
        is KSBlock.KSBlockSection  -> {
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
        is KSBlock.KSBlockDocument -> {
          val bb : KSBlockDocument<KSEvaluation> = b
          when (bb) {
            is KSBlock.KSBlockDocument.KSBlockDocumentWithParts    ->
              Optional.of(bb.content[0] as KSBlock<KSEvaluation>)
            is KSBlock.KSBlockDocument.KSBlockDocumentWithSections ->
              Optional.of(bb.content[0] as KSBlock<KSEvaluation>)
          }
        }
        is KSBlock.KSBlockSection,
        is KSBlock.KSBlockSubsection,
        is KSBlock.KSBlockParagraph,
        is KSBlock.KSBlockPart     -> {
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
      return Optional.of(pp.content[0] as KSBlock<KSEvaluation>)
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

    fun freshSerial() : Long {
      val serial = this.serial_pool
      this.serial_pool = this.serial_pool + 1L
      return serial
    }

    fun <T : KSBlock<Unit>, U : KSBlock<KSEvaluation>> recordID(
      c : Context,
      b : T,
      f : (T, Optional<KSID<KSEvaluation>>) -> U)
      : KSResult<U, KSEvaluationError> {
      return if (b.id.isPresent) {
        val id = b.id.get()

        LOG.debug("declare {} {}", id, id.position)
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
          val id_eval = KSEvaluation(c, c.freshSerial(), Optional.empty())
          val ksid = KSID(id.position, id.value, id_eval)
          val r = f(b, Optional.of(ksid))
          c.blocks_by_id.put(id.value, r)
          KSResult.succeed<U, KSEvaluationError>(r)
        }
      } else {
        KSResult.succeed(f(b, Optional.empty()))
      }
    }

    fun referenceID(ksid : KSID<KSEvaluation>) {
      LOG.debug("reference {} {}", ksid, ksid.position)
      this.id_references.add(ksid)
    }

    fun checkIDs() : KSResult<Unit, KSEvaluationError> {
      return KSResult.listMap({ id ->
        if (this.blocks_by_id.containsKey(id.value)) {
          val b = this.blocks_by_id[id.value]
          LOG.debug("resolved {} -> {}", id, b)
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
      }, this.id_references) flatMap { x ->
        KSResult.succeed<Unit, KSEvaluationError>(Unit)
      }
    }

    fun <T : KSBlock<KSEvaluation>> recordBlock(b : T) : T {
      val number_opt = b.data.number
      number_opt.ifPresent { number ->
        Assertive.require(!this.blocks_by_number.containsKey(number))
        this.blocks_by_number.put(number, b)
      }
      return b
    }

    fun setDocumentResult(d : KSBlockDocument<KSEvaluation>) {
      this.document_actual = d
    }
  }

  override fun evaluate(
    d : KSBlockDocument<Unit>)
    : KSResult<KSBlockDocument<KSEvaluation>, KSEvaluationError> {
    val c = Context()
    return evaluateDocument(c, d) flatMap { d -> checkIDs(c, d) }
  }

  private fun checkIDs(
    c : KSEvaluator.Context,
    d : KSBlockDocument<KSEvaluation>)
    : KSResult<KSBlockDocument<KSEvaluation>, KSEvaluationError> {

    return c.checkIDs() flatMap { ignored ->
      c.setDocumentResult(d)
      KSResult.succeed<KSBlockDocument<KSEvaluation>, KSEvaluationError>(d)
    }
  }

  private fun evaluateDocument(
    c : Context,
    d : KSBlockDocument<Unit>)
    : KSResult<KSBlockDocument<KSEvaluation>, KSEvaluationError> =
    when (d) {
      is KSBlockDocument.KSBlockDocumentWithParts    ->
        evaluateDocumentWithParts(c, d)
      is KSBlockDocument.KSBlockDocumentWithSections ->
        evaluateDocumentWithSections(c, d)
    }

  private fun evaluateDocumentWithSections(
    c : Context,
    d : KSBlockDocumentWithSections<Unit>)
    : KSResult<KSBlockDocumentWithSections<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()
    val act_content = KSResult.listMapIndexed(
      { e, i -> evaluateSection(c, e, OptionalLong.empty(), i + 1L) }, d.content)
    val act_title = KSResult.listMap(
      { e -> evaluateInlineText (c, e) }, d.title)

    return act_content flatMap { content ->
      act_title flatMap { title ->
        val eval = KSEvaluation(c, serial, Optional.empty())
        c.recordID(c, d, { d, id ->
          KSBlockDocumentWithSections(d.position, eval, id, d.type, title, content)
        })
      }
    }
  }

  private fun evaluateInlineText(
    c : Context,
    e : KSInlineText<Unit>)
    : KSResult<KSInlineText<KSEvaluation>, KSEvaluationError> {
    val eval = KSEvaluation(c, c.freshSerial(), Optional.empty())
    return KSResult.succeed<KSInlineText<KSEvaluation>, KSEvaluationError>(
      KSInlineText(e.position, eval, e.text))
  }

  private fun evaluateInlineImage(
    c : Context,
    e : KSInlineImage<Unit>)
    : KSResult<KSInlineImage<KSEvaluation>, KSEvaluationError> {

    val act_content = KSResult.listMap(
      { ci -> evaluateInlineText(c, ci) }, e.content)

    return act_content flatMap { content ->
      val eval = KSEvaluation(c, c.freshSerial(), Optional.empty())
      KSResult.succeed<KSInlineImage<KSEvaluation>, KSEvaluationError>(
        KSInlineImage(e.position, eval, e.type, e.target, e.size, content))
    }
  }

  private fun evaluateInlineTerm(
    c : Context,
    e : KSInlineTerm<Unit>)
    : KSResult<KSInlineTerm<KSEvaluation>, KSEvaluationError> {

    val act_content = KSResult.listMap(
      { ci -> evaluateInlineText(c, ci) }, e.content)

    return act_content flatMap { content ->
      val eval = KSEvaluation(c, c.freshSerial(), Optional.empty())
      KSResult.succeed<KSInlineTerm<KSEvaluation>, KSEvaluationError>(
        KSInlineTerm(e.position, eval, e.type, content))
    }
  }

  private fun evaluateInlineVerbatim(
    c : Context,
    e : KSInlineVerbatim<Unit>)
    : KSResult<KSInlineVerbatim<KSEvaluation>, KSEvaluationError> {

    val eval = KSEvaluation(c, c.freshSerial(), Optional.empty())
    return KSResult.succeed<KSInlineVerbatim<KSEvaluation>, KSEvaluationError>(
      KSInlineVerbatim(e.position, eval, e.type, e.text))
  }

  private fun evaluateInline(
    c : Context,
    e : KSInline<Unit>)
    : KSResult<KSInline<KSEvaluation>, KSEvaluationError> =
    when (e) {
      is KSInlineLink     -> evaluateInlineLink(c, e)
      is KSInlineText     -> evaluateInlineText(c, e)
      is KSInlineVerbatim -> evaluateInlineVerbatim(c, e)
      is KSInlineTerm     -> evaluateInlineTerm(c, e)
      is KSInlineImage    -> evaluateInlineImage(c, e)
    }

  private fun evaluateInlineLink(
    c : Context,
    e : KSInlineLink<Unit>)
    : KSResult<KSInlineLink<KSEvaluation>, KSEvaluationError> {
    val act = e.actual
    return when (act) {
      is KSLinkExternal -> {
        val act_content = KSResult.listMap(
          { cc -> evaluateLinkContent(c, cc) }, act.content)

        act_content flatMap { content ->
          val link = KSLinkExternal(act.position, act.target, content)
          val eval = KSEvaluation(c, c.freshSerial(), Optional.empty())
          KSResult.succeed<KSInlineLink<KSEvaluation>, KSEvaluationError>(
            KSInlineLink(e.position, eval, link))
        }
      }

      is KSLinkInternal -> {
        val id_eval = KSEvaluation(c, c.freshSerial(), Optional.empty())
        val ksid = KSID(act.target.position, act.target.value, id_eval)
        c.referenceID(ksid)

        val act_content = KSResult.listMap(
          { cc -> evaluateLinkContent(c, cc) }, act.content)

        act_content flatMap { content ->
          val link = KSLinkInternal(act.position, ksid, content)
          val eval = KSEvaluation(c, c.freshSerial(), Optional.empty())
          KSResult.succeed<KSInlineLink<KSEvaluation>, KSEvaluationError>(
            KSInlineLink(e.position, eval, link))
        }
      }
    }
  }

  private fun evaluateLinkContent(
    c : KSEvaluator.Context,
    cc : KSLinkContent<Unit>)
    : KSResult<KSLinkContent<KSEvaluation>, KSEvaluationError> =
    when (cc) {
      is KSLinkContent.KSLinkText  ->
        evaluateInlineText(c, cc.actual) flatMap { t ->
          val ev = KSEvaluation(c, c.freshSerial(), Optional.empty())
          KSResult.succeed<KSLinkContent<KSEvaluation>, KSEvaluationError>(
            KSLinkText(cc.position, ev, t))
        }
      is KSLinkContent.KSLinkImage -> {
        evaluateInlineImage(c, cc.actual) flatMap { t ->
          val ev = KSEvaluation(c, c.freshSerial(), Optional.empty())
          KSResult.succeed<KSLinkContent<KSEvaluation>, KSEvaluationError>(
            KSLinkImage(cc.position, ev, t))
        }
      }
    }

  private fun evaluateSection(
    c : Context,
    e : KSBlockSection<Unit>,
    pn : OptionalLong,
    sn : Long)
    : KSResult<KSBlockSection<KSEvaluation>, KSEvaluationError> =
    when (e) {
      is KSBlockSectionWithSubsections ->
        evaluateSectionWithSubsections(c, e, pn, sn)
      is KSBlockSectionWithContent     ->
        evaluateSectionWithContent(c, e, pn, sn)
    }

  private fun evaluateSectionWithContent(
    c : Context,
    e : KSBlockSectionWithContent<Unit>,
    pn : OptionalLong,
    sn : Long)
    : KSResult<KSBlockSectionWithContent<KSEvaluation>, KSEvaluationError> {

    val act_content = KSResult.listMapIndexed({ cc, i ->
      evaluateSubsectionContent(c, cc, pn, sn, OptionalLong.empty(), i + 1L)
    }, e.content)
    val act_title = KSResult.listMap({ e ->
      evaluateInlineText (c, e)
    }, e.title)

    return act_content flatMap { content ->
      act_title flatMap { title ->

        val num : KSNumber = if (pn.isPresent) {
          KSNumberPartSection(pn.asLong, sn)
        } else {
          KSNumberSection(sn)
        }

        val ev = KSEvaluation(c, c.freshSerial(), Optional.of(num))
        c.recordID(c, e, { e, id ->
          c.recordBlock(KSBlockSectionWithContent(
            e.position, ev, e.type, id, title, content))
        })
      }
    }
  }

  private fun evaluateSubsectionContent(
    c : Context,
    cc : KSSubsectionContent<Unit>,
    pn : OptionalLong,
    sn : Long,
    ssn : OptionalLong,
    cn : Long)
    : KSResult<KSSubsectionContent<KSEvaluation>, KSEvaluationError> =
    when (cc) {
      is KSSubsectionContent.KSSubsectionParagraph ->
        evaluateParagraph(c, cc.paragraph, pn, sn, ssn, cn) map { x ->
          KSSubsectionParagraph(x)
        }
    }

  private fun evaluateParagraph(
    c : Context,
    p : KSBlockParagraph<Unit>,
    pn : OptionalLong,
    sn : Long,
    ssn : OptionalLong,
    cn : Long)
    : KSResult<KSBlockParagraph<KSEvaluation>, KSEvaluationError> {

    val act_content =
      KSResult.listMap({ i -> evaluateInline(c, i) }, p.content)

    return act_content flatMap { content ->

      val num : KSNumber = if (pn.isPresent) {
        if (ssn.isPresent) {
          KSNumberPartSectionSubsectionContent(pn.asLong, sn, ssn.asLong, cn)
        } else {
          KSNumberPartSectionContent(pn.asLong, sn, cn)
        }
      } else {
        if (ssn.isPresent) {
          KSNumberSectionSubsectionContent(sn, ssn.asLong, cn)
        } else {
          KSNumberSectionContent(sn, cn)
        }
      }

      val ev = KSEvaluation(c, c.freshSerial(), Optional.of(num))
      c.recordID(c, p, { p, id ->
        c.recordBlock(KSBlockParagraph(p.position, ev, p.type, id, content))
      })
    }
  }

  private fun evaluateSectionWithSubsections(
    c : Context,
    e : KSBlockSectionWithSubsections<Unit>,
    pn : OptionalLong,
    sn : Long)
    : KSResult<KSBlockSectionWithSubsections<KSEvaluation>, KSEvaluationError> {

    val act_content = KSResult.listMapIndexed({ cc, i ->
      evaluateSubsection(c, cc, pn, sn, i + 1L)
    }, e.content)
    val act_title = KSResult.listMap(
      { e -> evaluateInlineText (c, e) }, e.title)

    return act_content flatMap { content ->
      act_title flatMap { title ->

        val num : KSNumber = if (pn.isPresent) {
          KSNumberPartSection(pn.asLong, sn)
        } else {
          KSNumberSection(sn)
        }

        val ev = KSEvaluation(c, c.freshSerial(), Optional.of(num))
        c.recordID(c, e, { e, id ->
          c.recordBlock(KSBlockSectionWithSubsections(
            e.position, ev, e.type, id, title, content))
        })
      }
    }
  }

  private fun evaluateSubsection(
    c : Context,
    ss : KSBlockSubsection<Unit>,
    pn : OptionalLong,
    sn : Long,
    ssn : Long)
    : KSResult<KSBlockSubsection<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()
    val act_content =
      KSResult.listMapIndexed({ e, i ->
        evaluateSubsectionContent(c, e, pn, sn, OptionalLong.of(ssn), i + 1L)
      }, ss.content)
    val act_title = KSResult.listMap(
      { e -> evaluateInlineText (c, e) }, ss.title)

    return act_content flatMap { content ->
      act_title flatMap { title ->

        val num : KSNumber = if (pn.isPresent) {
          KSNumberPartSectionSubsection(pn.asLong, sn, ssn)
        } else {
          KSNumberSectionSubsection(sn, ssn)
        }

        val ev = KSEvaluation(c, serial, Optional.of(num))
        c.recordID(c, ss, { ss, id ->
          c.recordBlock(KSBlockSubsection(
            ss.position, ev, ss.type, id, title, content))
        })
      }
    }
  }

  private fun evaluateDocumentWithParts(
    c : Context,
    d : KSBlockDocumentWithParts<Unit>)
    : KSResult<KSBlockDocumentWithParts<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()
    val act_content = KSResult.listMapIndexed(
      { e, i -> evaluatePart(c, e, i + 1L) }, d.content)
    val act_title = KSResult.listMap(
      { e -> evaluateInlineText (c, e) }, d.title)

    return act_content flatMap { content ->
      act_title flatMap { title ->
        val ev = KSEvaluation(c, serial, Optional.empty())
        c.recordID(c, d, { ss, id ->
          c.recordBlock(KSBlockDocumentWithParts(
            d.position, ev, id, d.type, title, content))
        })
      }
    }
  }

  private fun evaluatePart(
    c : Context,
    e : KSBlockPart<Unit>,
    pn : Long) : KSResult<KSBlockPart<KSEvaluation>, KSEvaluationError> {

    val serial = c.freshSerial()
    val act_content = KSResult.listMapIndexed({ cc, i ->
      evaluateSection(c, cc, OptionalLong.of(pn), i + 1L)
    }, e.content)
    val act_title = KSResult.listMap(
      { e -> evaluateInlineText (c, e) }, e.title)

    return act_content flatMap { content ->
      act_title flatMap { title ->
        val ev = KSEvaluation(c, serial, Optional.of(KSNumberPart(pn)))
        c.recordID(c, e, { e, id ->
          c.recordBlock(
            KSBlockPart(e.position, ev, e.type, id, title, content))
        })
      }
    }
  }
}
