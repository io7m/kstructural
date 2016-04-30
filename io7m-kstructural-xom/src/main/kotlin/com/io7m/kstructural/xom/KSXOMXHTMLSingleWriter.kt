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

import com.io7m.kstructural.core.KSBlock.KSBlockDocument
import com.io7m.kstructural.core.KSBlock.KSBlockDocument.KSBlockDocumentWithParts
import com.io7m.kstructural.core.KSBlock.KSBlockDocument.KSBlockDocumentWithSections
import com.io7m.kstructural.core.KSBlock.KSBlockParagraph
import com.io7m.kstructural.core.KSBlock.KSBlockPart
import com.io7m.kstructural.core.KSBlock.KSBlockSection
import com.io7m.kstructural.core.KSBlock.KSBlockSubsection
import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSSubsectionContent
import com.io7m.kstructural.core.evaluator.KSEvaluation
import com.io7m.kstructural.core.evaluator.KSNumber
import nu.xom.Document
import nu.xom.Element

object KSXOMXHTMLSingleWriter : KSXOMXHTMLWriterType {

  override fun write(
    settings : KSXOMSettings,
    document : KSBlockDocument<KSEvaluation>) : Map<String, Document> {

    val prov = object: KSXOMLinkProviderType {
      override fun anchorForDocument() : String {
        return "index.xhtml"
      }

      override fun anchorForNumber(number : KSNumber) : String {
        return "#" + number.toAnchor()
      }

      override fun anchorForID(id : KSID<KSEvaluation>) : String {
        val e = document.data.context.elementForID(id)
        return "#" + e.id.get().value
      }
    }

    return when (document) {
      is KSBlockDocumentWithParts ->
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

    val (document, body) = KSXOM.newPage(settings, d)
    body.appendChild(KSXOM.documentIndexTitle(d))
    body.appendChild(KSXOM.documentContents(prov, d))

    d.content.forEach { p -> body.appendChild(writePart(settings, prov, d, p)) }
    return document
  }

  private fun writeDocumentWithSections(
    settings : KSXOMSettings,
    prov : KSXOMLinkProviderType,
    d : KSBlockDocumentWithSections<KSEvaluation>) : Document {

    val (document, body) = KSXOM.newPage(settings, d)
    body.appendChild(KSXOM.documentIndexTitle(d))
    body.appendChild(KSXOM.documentContents(prov, d))

    d.content.forEach { sc -> body.appendChild(writeSection(settings, prov, d, sc)) }
    return document
  }

  private fun writeSection(
    settings : KSXOMSettings,
    prov : KSXOMLinkProviderType,
    d : KSBlockDocument<KSEvaluation>,
    s : KSBlockSection<KSEvaluation>) : Element {

    val e = KSXOM.sectionContainer(s)
    if (settings.render_toc_sections) {
      e.appendChild(KSXOM.sectionContents(prov, s))
    }

    when (s) {
      is KSBlockSection.KSBlockSectionWithContent -> {
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

    val e = KSXOM.subsectionContainer(ss)
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
      is KSSubsectionContent.KSSubsectionParagraph ->
        writeParagraph(prov, d, sc.paragraph)
    }
  }

  private fun writeParagraph(
    prov : KSXOMLinkProviderType,
    d : KSBlockDocument<KSEvaluation>,
    p : KSBlockParagraph<KSEvaluation>) : Element {

    val (container, content) = KSXOM.paragraphContainer(p)
    KSXOM.inlinesAppend(content, p.content, { i -> KSXOM.inline(prov, i) })
    return container
  }

  private fun writePart(
    settings : KSXOMSettings,
    prov : KSXOMLinkProviderType,
    d : KSBlockDocumentWithParts<KSEvaluation>,
    p : KSBlockPart<KSEvaluation>) : Element {

    val e = KSXOM.partContainer(p)
    if (settings.render_toc_parts) {
      e.appendChild(KSXOM.partContents(prov, p))
    }

    p.content.forEach { s -> e.appendChild(writeSection(settings, prov, d, s)) }
    return e
  }
}
