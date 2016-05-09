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

import com.io7m.kstructural.core.KSElement.KSBlock
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithParts
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithSections
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

object KSXOMXHTMLSingleWriter : KSXOMXHTMLWriterType {

  override fun write(
    settings : KSXOMSettings,
    document : KSBlockDocument<KSEvaluation>) : Map<String, Document> {

    val prov = object : KSXOMLinkProviderType {

      override fun numberLink(number : KSNumber) : String {
        return numberAnchor(number)
      }

      override fun idLink(id : KSID<KSEvaluation>) : String {
        throw UnsupportedOperationException()
      }

      override fun footnoteAnchor(
        f : KSBlock.KSBlockFootnote<KSEvaluation>, r : Long) : String {
        return KSXOM.prefixedName("f_" + f.data.serial + "_" + r)
      }

      override fun footnoteReferenceAnchor(
        r : KSFootnoteReference<KSEvaluation>) : String {
        return KSXOM.prefixedName("fr_" + r.data.serial)
      }

      override fun footnoteLink(
        f : KSBlock.KSBlockFootnote<KSEvaluation>, r : Long) : String {
        return "#" + footnoteAnchor(f, r)
      }

      override fun footnoteReferenceLink(
        r : KSFootnoteReference<KSEvaluation>) : String {
        return "#" + footnoteReferenceAnchor(r)
      }

      override fun documentAnchor() : String {
        return "index.xhtml"
      }

      override fun numberAnchorID(number : KSNumber) : String {
        return KSXOM.prefixedName(number.toAnchor())
      }

      override fun numberAnchor(number : KSNumber) : String {
        return "#" + numberAnchorID(number)
      }
    }

    return when (document) {
      is KSBlockDocumentWithParts    ->
        mapOf(Pair("index.xhtml",
          writeDocumentWithParts(settings, prov, document)))
      is KSBlockDocumentWithSections ->
        mapOf(Pair("index.xhtml",
          writeDocumentWithSections(settings, prov, document)))
    }
  }

  private fun writeDocumentWithParts(
    settings : KSXOMSettings,
    prov : KSXOMLinkProviderType,
    d : KSBlockDocumentWithParts<KSEvaluation>) : Document {

    val (document, body) = KSXOM.newPage(settings, d, d.data.number, d.title)
    body.appendChild(KSXOM.documentIndexTitle(d))
    body.appendChild(KSXOM.contentsForDocument(prov, d))

    d.content.forEach { p -> body.appendChild(writePart(settings, prov, d, p)) }
    return document
  }

  private fun writeDocumentWithSections(
    settings : KSXOMSettings,
    prov : KSXOMLinkProviderType,
    d : KSBlockDocumentWithSections<KSEvaluation>) : Document {

    val (document, body) = KSXOM.newPage(settings, d, d.data.number, d.title)
    body.appendChild(KSXOM.documentIndexTitle(d))
    body.appendChild(KSXOM.contentsForDocument(prov, d))

    d.content.forEach { sc -> body.appendChild(writeSection(settings, prov, d, sc)) }
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
    sc : KSSubsectionContent<KSEvaluation>) : Element {

    return when (sc) {
      is KSSubsectionContent.KSSubsectionParagraph  ->
        writeParagraph(prov, d, sc.paragraph)
      is KSSubsectionContent.KSSubsectionFormalItem ->
        writeFormalItem(prov, d, sc.formal)
      is KSSubsectionContent.KSSubsectionFootnote   -> TODO()
      is KSSubsectionContent.KSSubsectionImport     -> TODO()
    }
  }

  private fun writeFormalItem(
    prov : KSXOMLinkProviderType,
    d : KSBlockDocument<KSEvaluation>,
    f : KSBlock.KSBlockFormalItem<KSEvaluation>) : Element {

    val (container, content) = KSXOM.formalItemContainer(prov, f)
    KSXOM.inlinesAppend(content, f.content, { i -> KSXOM.inline(prov, i) })
    return container
  }

  private fun writeParagraph(
    prov : KSXOMLinkProviderType,
    d : KSBlockDocument<KSEvaluation>,
    p : KSBlockParagraph<KSEvaluation>) : Element {

    val (container, content) = KSXOM.paragraphContainer(prov, p)
    KSXOM.inlinesAppend(content, p.content, { i -> KSXOM.inline(prov, i) })
    return container
  }

  private fun writePart(
    settings : KSXOMSettings,
    prov : KSXOMLinkProviderType,
    d : KSBlockDocumentWithParts<KSEvaluation>,
    p : KSBlockPart<KSEvaluation>) : Element {

    val e = KSXOM.partContainer(prov, p)
    if (settings.render_toc_parts) {
      e.appendChild(KSXOM.contentsForPart(prov, p))
    }

    p.content.forEach { s -> e.appendChild(writeSection(settings, prov, d, s)) }
    return e
  }
}
