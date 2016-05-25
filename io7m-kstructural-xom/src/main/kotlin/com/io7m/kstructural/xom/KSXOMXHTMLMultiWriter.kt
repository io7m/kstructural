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

import com.io7m.kstructural.core.KSElement
import com.io7m.kstructural.core.KSElement.KSBlock
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithParts
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithSections
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockFootnote
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockFormalItem
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockParagraph
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockPart
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSubsection
import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSSubsectionContent
import com.io7m.kstructural.core.evaluator.KSEvaluation
import com.io7m.kstructural.core.evaluator.KSFootnoteReference
import com.io7m.kstructural.core.evaluator.KSNumber
import nu.xom.Document
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.slf4j.LoggerFactory
import org.valid4j.Assertive
import java.util.Optional

object KSXOMXHTMLMultiWriter : KSXOMXHTMLWriterType {

  private val LOG = LoggerFactory.getLogger(KSXOMXHTMLMultiWriter::class.java)

  override fun write(
    settings : KSXOMSettings,
    document : KSBlockDocument<KSEvaluation>) : Map<String, Document> {

    val prov = object : KSXOMLinkProviderType {

      override fun footnoteReferenceAnchor(
        r : KSFootnoteReference<KSEvaluation>) : String {
        return KSXOM.prefixedName("fr_" + r.data.serial)
      }

      override fun footnoteReferenceLink(
        r : KSFootnoteReference<KSEvaluation>) : String {

        val s_opt = containingSegment(r.ref)
        Assertive.require(s_opt.isPresent)
        val s = s_opt.get()
        Assertive.require(s.data.number.isPresent)

        val n = s.data.number.get()
        return fileForNumber(n) + "#" + footnoteReferenceAnchor(r)
      }

      override fun footnoteAnchor(
        f : KSBlockFootnote<KSEvaluation>, r : Long) : String {
        return KSXOM.prefixedName("f_" + f.data.serial + "_" + r)
      }

      override fun footnoteLink(
        f : KSBlockFootnote<KSEvaluation>, r : Long) : String {

        val s_opt = containingSegment(f)
        Assertive.require(s_opt.isPresent)
        val s = s_opt.get()
        Assertive.require(s.data.number.isPresent)

        val n = s.data.number.get()
        return fileForNumber(n) + "#" + footnoteAnchor(f, r)
      }

      private fun containingSegment(
        b : KSElement<KSEvaluation>) : Optional<KSBlock<KSEvaluation>> {
        val c = b.data.context
        val e = c.elementForSerial(b.data.parent)
        return if (e.isPresent) {
          val r = e.get()
          when (r) {
            is KSElement.KSBlock                        ->
              when (r as KSBlock) {
                is KSBlockDocument,
                is KSBlockSection,
                is KSBlockPart                     -> Optional.of(r)
                is KSBlockSubsection,
                is KSBlockParagraph,
                is KSBlockFormalItem,
                is KSBlockFootnote                 -> containingSegment(r)
                is KSElement.KSBlock.KSBlockImport -> TODO()
              }
            is KSElement.KSInline                       -> containingSegment(r)
            is KSElement.KSInline.KSListItem            -> containingSegment(r)
            is KSElement.KSInline.KSTableHeadColumnName -> containingSegment(r)
            is KSElement.KSInline.KSTableHead           -> containingSegment(r)
            is KSElement.KSInline.KSTableBodyCell       -> containingSegment(r)
            is KSElement.KSInline.KSTableBodyRow        -> containingSegment(r)
            is KSElement.KSInline.KSTableBody           -> containingSegment(r)
            is KSElement.KSInline.KSTableSummary        -> containingSegment(r)
          }
        } else {
          Optional.empty<KSBlock<KSEvaluation>>()
        }
      }

      private fun fileForNumber(number : KSNumber) : String =
        when (number) {
          is KSNumber.KSNumberPart                         -> {
            val sb = StringBuilder()
            sb.append("p")
            sb.append(number.part)
            sb.append(".xhtml")
            sb.toString()
          }
          is KSNumber.KSNumberPartSection                  -> {
            val sb = StringBuilder()
            sb.append("p")
            sb.append(number.part)
            sb.append("s")
            sb.append(number.section)
            sb.append(".xhtml")
            sb.toString()
          }
          is KSNumber.KSNumberPartSectionContent           -> {
            val sb = StringBuilder()
            sb.append("p")
            sb.append(number.part)
            sb.append("s")
            sb.append(number.section)
            sb.append(".xhtml")
            sb.toString()
          }
          is KSNumber.KSNumberPartSectionSubsection        -> {
            val sb = StringBuilder()
            sb.append("p")
            sb.append(number.part)
            sb.append("s")
            sb.append(number.section)
            sb.append(".xhtml")
            sb.toString()
          }
          is KSNumber.KSNumberPartSectionSubsectionContent -> {
            val sb = StringBuilder()
            sb.append("p")
            sb.append(number.part)
            sb.append("s")
            sb.append(number.section)
            sb.append(".xhtml")
            sb.toString()
          }
          is KSNumber.KSNumberSection                      -> {
            val sb = StringBuilder()
            sb.append("s")
            sb.append(number.section)
            sb.append(".xhtml")
            sb.toString()
          }
          is KSNumber.KSNumberSectionContent               -> {
            val sb = StringBuilder()
            sb.append("s")
            sb.append(number.section)
            sb.append(".xhtml")
            sb.toString()
          }
          is KSNumber.KSNumberSectionSubsection            -> {
            val sb = StringBuilder()
            sb.append("s")
            sb.append(number.section)
            sb.append(".xhtml")
            sb.toString()
          }
          is KSNumber.KSNumberSectionSubsectionContent     -> {
            val sb = StringBuilder()
            sb.append("s")
            sb.append(number.section)
            sb.append(".xhtml")
            sb.toString()
          }
        }

      override fun numberAnchorID(number : KSNumber) : String =
        KSXOM.prefixedName(number.toAnchor())

      override fun numberAnchor(number : KSNumber) : String =
        "#" + numberAnchorID(number)

      override fun numberLink(number : KSNumber) : String =
        fileForNumber(number) + numberAnchor(number)

      override fun documentAnchor() : String {
        return "index-m.xhtml"
      }

      override fun idLink(id : KSID<KSEvaluation>) : String {
        val e = document.data.context.elementForID(id)
        val a = e.id.get()
        return when (e) {
          is KSBlockDocument                 -> "index-m.xhtml#" + a
          is KSBlockSection                  -> fileForNumber(e.data.number.get()) + "#" + a
          is KSBlockSubsection               -> fileForNumber(e.data.number.get()) + "#" + a
          is KSBlockParagraph                -> fileForNumber(e.data.number.get()) + "#" + a
          is KSBlockPart                     -> fileForNumber(e.data.number.get()) + "#" + a
          is KSBlockFormalItem               -> fileForNumber(e.data.number.get()) + "#" + a
          is KSBlockFootnote                 -> throw UnsupportedOperationException("Cannot resolve a footnote directly!")
          is KSElement.KSBlock.KSBlockImport -> TODO()
        }
      }
    }

    val m : MutableMap<String, Document> = mutableMapOf()
    m.put("index-m.xhtml", writeDocumentIndexPage(settings, prov, document))

    LOG.debug("create {}", "index-m.xhtml")

    return when (document) {
      is KSBlockDocumentWithParts    ->
        writeDocumentsWithParts(settings, prov, document, m)
      is KSBlockDocumentWithSections ->
        writeDocumentsWithSections(settings, prov, document, m)
    }
  }

  private fun writeDocumentsWithSections(
    settings : KSXOMSettings,
    prov : KSXOMLinkProviderType,
    d : KSBlockDocumentWithSections<KSEvaluation>,
    m : MutableMap<String, Document>) : Map<String, Document> {
    d.content.forEach { s -> writeDocumentSection(settings, prov, d, s, m) }
    return m
  }

  private fun writeDocumentsWithParts(
    settings : KSXOMSettings,
    prov : KSXOMLinkProviderType,
    d : KSBlockDocumentWithParts<KSEvaluation>,
    m : MutableMap<String, Document>) : Map<String, Document> {
    d.content.forEach { p -> writeDocumentPart(settings, prov, d, p, m) }
    return m
  }

  private fun writeDocumentSection(
    settings : KSXOMSettings,
    prov : KSXOMLinkProviderType,
    d : KSBlockDocument<KSEvaluation>,
    s : KSBlockSection<KSEvaluation>,
    m : MutableMap<String, Document>) : Unit {

    val (document, body) = KSXOM.newPage(settings, d, s.data.number, s.title)
    settings.on_body_start.call(body)
    body.appendChild(KSXOM.navigationBar(prov, s, KSXOM.NavigationBarPosition.Top))
    body.appendChild(writeSection(settings, prov, d, s))
    body.appendChild(KSXOM.navigationBar(prov, s, KSXOM.NavigationBarPosition.Bottom))
    settings.on_body_end.call(body)

    val file = s.data.number.get().toAnchor() + ".xhtml"
    LOG.debug("create {}", file)
    m.put(file, document)
  }

  private fun writeDocumentPart(
    settings : KSXOMSettings,
    prov : KSXOMLinkProviderType,
    d : KSBlockDocumentWithParts<KSEvaluation>,
    p : KSBlockPart<KSEvaluation>,
    m : MutableMap<String, Document>) : Unit {

    val (document, body) = KSXOM.newPage(settings, d, p.data.number, p.title)
    settings.on_body_start.call(body)
    body.appendChild(KSXOM.navigationBar(prov, p, KSXOM.NavigationBarPosition.Top))

    val part_container = KSXOM.partContainer(prov, p)
    if (settings.render_toc_parts) {
      part_container.appendChild(KSXOM.contentsForPart(prov, p))
    }
    body.appendChild(part_container)
    body.appendChild(KSXOM.navigationBar(prov, p, KSXOM.NavigationBarPosition.Bottom))

    val file = p.data.number.get().toAnchor() + ".xhtml"
    LOG.debug("create {}", file)
    m.put(file, document)
    p.content.forEach { s -> writeDocumentSection(settings, prov, d, s, m) }
    settings.on_body_end.call(body)
  }

  private fun writeDocumentIndexPage(
    settings : KSXOMSettings,
    prov : KSXOMLinkProviderType,
    d : KSBlockDocument<KSEvaluation>) : Document {
    val (document, body) = KSXOM.newPage(settings, d, d.data.number, d.title)
    settings.on_body_start.call(body)
    body.appendChild(KSXOM.navigationBar(prov, d, KSXOM.NavigationBarPosition.Top))
    body.appendChild(KSXOM.documentIndexTitle(d))
    if (settings.render_toc_document) {
      body.appendChild(KSXOM.contentsForDocument(prov, d))
    }
    body.appendChild(KSXOM.navigationBar(prov, d, KSXOM.NavigationBarPosition.Bottom))
    settings.on_body_end.call(body)
    return document
  }

  private fun writeSection(
    settings : KSXOMSettings,
    prov : KSXOMLinkProviderType,
    d : KSBlockDocument<KSEvaluation>,
    s : KSBlockSection<KSEvaluation>) : Element {

    val e = KSXOM.sectionContainer(prov, s)
    if (settings.render_toc_sections) {
      e.appendChild(KSXOM.contentsForSection(prov, s))
    }

    when (s) {
      is KSBlockSection.KSBlockSectionWithContent     -> {
        s.content.forEach { sc ->
          e.appendChild(writeSubsectionContent(prov, d, sc))
        }
      }
      is KSBlockSection.KSBlockSectionWithSubsections -> {
        s.content.forEach { ss ->
          e.appendChild(writeSubsection(prov, d, ss))
        }
      }
    }

    val n = s.data.number.get() as KSNumber.HasSectionType
    val footnotes = s.data.context.footnotesForSection(n)
    if (footnotes.isNotEmpty()) {
      if (LOG.isTraceEnabled) {
        val iter = footnotes.iterator()
        while (iter.hasNext()) {
          val fn_e = iter.next()
          LOG.trace("footnote {} for section {}", fn_e.key, n)
        }
      }

      e.appendChild(KSXOM.footnotes(prov, footnotes))
    }

    return e
  }

  private fun writeSubsection(
    prov : KSXOMLinkProviderType,
    d : KSBlockDocument<KSEvaluation>,
    ss : KSBlockSubsection<KSEvaluation>) : Element {

    val e = KSXOM.subsectionContainer(prov, ss)
    ss.content.forEach { sc ->
      e.appendChild(writeSubsectionContent(prov, d, sc))
    }
    return e
  }

  private fun writeSubsectionContent(
    prov : KSXOMLinkProviderType,
    d : KSBlockDocument<KSEvaluation>,
    sc : KSSubsectionContent<KSEvaluation>) : Node {

    return when (sc) {
      is KSSubsectionContent.KSSubsectionParagraph  ->
        writeParagraph(prov, d, sc.paragraph)
      is KSSubsectionContent.KSSubsectionFormalItem ->
        writeFormalItem(prov, d, sc.formal)
      is KSSubsectionContent.KSSubsectionFootnote   ->
        Text("")
    }
  }

  private fun writeFormalItem(
    prov : KSXOMLinkProviderType,
    d : KSBlockDocument<KSEvaluation>,
    f : KSBlockFormalItem<KSEvaluation>) : Element {

    val (container, content) = KSXOM.formalItemContainer(prov, f)
    KSXOMSpacing.appendWithSpace(content, f.content, { i -> KSXOM.inline(prov, i) })
    return container
  }

  private fun writeParagraph(
    prov : KSXOMLinkProviderType,
    d : KSBlockDocument<KSEvaluation>,
    p : KSBlockParagraph<KSEvaluation>) : Element {

    val (container, content) = KSXOM.paragraphContainer(prov, p)
    KSXOMSpacing.appendWithSpace(content, p.content, { i -> KSXOM.inline(prov, i) })
    return container
  }
}
