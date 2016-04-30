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
import org.slf4j.LoggerFactory

object KSXOMXHTMLMultiWriter : KSXOMXHTMLWriterType {

  private val LOG = LoggerFactory.getLogger(KSXOMXHTMLMultiWriter::class.java)

  override fun write(
    d : KSBlockDocument<KSEvaluation>) : Map<String, Document> {

    val prov = object: KSXOMLinkProviderType {

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
          sb.append(".xhtml#")
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

      override fun anchorForNumber(number : KSNumber) : String =
        fileForNumber(number) + "#" + number.toAnchor()

      override fun anchorForDocument() : String {
        return "index-m.xhtml"
      }

      override fun anchorForID(id : KSID<KSEvaluation>) : String {
        val e = d.data.context.elementForID(id)
        val a = "#" + e.id.get().value
        return when (e) {
          is KSBlockDocument   -> "index-m.xhtml" + a
          is KSBlockSection    -> fileForNumber(e.data.number.get()) + a
          is KSBlockSubsection -> fileForNumber(e.data.number.get()) + a
          is KSBlockParagraph  -> fileForNumber(e.data.number.get()) + a
          is KSBlockPart       -> fileForNumber(e.data.number.get()) + a
        }
      }
    }

    val m : MutableMap<String, Document> = mutableMapOf()
    m.put("index-m.xhtml", writeDocumentIndexPage(prov, d))

    LOG.debug("create {}", "index-m.xhtml")

    return when (d) {
      is KSBlockDocumentWithParts    -> writeDocumentsWithParts(prov, d, m)
      is KSBlockDocumentWithSections -> writeDocumentsWithSections(prov, d, m)
    }
  }

  private fun writeDocumentsWithSections(
    prov : KSXOMLinkProviderType,
    d : KSBlockDocumentWithSections<KSEvaluation>,
    m : MutableMap<String, Document>) : Map<String, Document> {
    d.content.forEach { s -> writeDocumentSection(prov, d, s, m) }
    return m
  }

  private fun writeDocumentsWithParts(
    prov : KSXOMLinkProviderType,
    d : KSBlockDocumentWithParts<KSEvaluation>,
    m : MutableMap<String, Document>) : Map<String, Document> {
    d.content.forEach { p -> writeDocumentPart(prov, d, p, m) }
    return m
  }

  private fun writeDocumentSection(
    prov : KSXOMLinkProviderType,
    d : KSBlockDocument<KSEvaluation>,
    s : KSBlockSection<KSEvaluation>,
    m : MutableMap<String, Document>) : Unit {

    val (document, body) = KSXOM.newPage(d)
    body.appendChild(KSXOM.navigationBar(prov, s, KSXOM.NavigationBarPosition.Top))
    body.appendChild(writeSection(prov, d, s))
    body.appendChild(KSXOM.navigationBar(prov, s, KSXOM.NavigationBarPosition.Bottom))

    val file = s.data.number.get().toAnchor() + ".xhtml"
    LOG.debug("create {}", file)
    m.put(file, document)
  }

  private fun writeDocumentPart(
    prov : KSXOMLinkProviderType,
    d : KSBlockDocumentWithParts<KSEvaluation>,
    p : KSBlockPart<KSEvaluation>,
    m : MutableMap<String, Document>) : Unit {

    val (document, body) = KSXOM.newPage(d)
    body.appendChild(KSXOM.navigationBar(prov, p, KSXOM.NavigationBarPosition.Top))
    body.appendChild(KSXOM.partContainer(p))
    body.appendChild(KSXOM.navigationBar(prov, p, KSXOM.NavigationBarPosition.Bottom))

    val file = p.data.number.get().toAnchor() + ".xhtml"
    LOG.debug("create {}", file)
    m.put(file, document)
    p.content.forEach { s -> writeDocumentSection(prov, d, s, m) }
  }

  private fun writeDocumentIndexPage(
    prov : KSXOMLinkProviderType,
    d : KSBlockDocument<KSEvaluation>) : Document {
    val (document, body) = KSXOM.newPage(d)
    body.appendChild(KSXOM.navigationBar(prov, d, KSXOM.NavigationBarPosition.Top))
    body.appendChild(KSXOM.documentIndexTitle(d))
    body.appendChild(KSXOM.navigationBar(prov, d, KSXOM.NavigationBarPosition.Bottom))
    return document
  }

  private fun writeSection(
    prov : KSXOMLinkProviderType,
    d : KSBlockDocument<KSEvaluation>,
    s : KSBlockSection<KSEvaluation>) : Element {

    val e = KSXOM.sectionContainer(s)
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
}
