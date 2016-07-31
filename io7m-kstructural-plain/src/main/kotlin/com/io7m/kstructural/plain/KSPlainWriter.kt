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

import com.io7m.jptbox.core.JPTextImages
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithParts
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockFormalItem
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockParagraph
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockPart
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection.KSBlockSectionWithContent
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection.KSBlockSectionWithSubsections
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSubsection
import com.io7m.kstructural.core.KSElement.KSInline
import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSSubsectionContent
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionFootnote
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionFormalItem
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionParagraph
import com.io7m.kstructural.core.evaluator.KSEvaluation
import com.io7m.kstructural.core.evaluator.KSNumber
import java.io.Writer
import java.util.Optional

object KSPlainWriter : KSPlainWriterType {

  override fun write(
    settings : KSPlainSettings,
    document : KSBlockDocument<KSEvaluation>,
    output : Writer) {

    output.write(text(document.title))
    output.write("\n")
    KSInlineRenderer.dividerHeavy(output, settings.page_width - 1)
    output.write("\n")
    output.write("\n")

    when (document) {
      is KSBlockDocumentWithParts                    -> {
        if (settings.render_toc_document) {
          tocDocument(settings, output, document)
        }
        document.content.forEach { part ->
          writePart(settings, output, part)
        }
      }
      is KSBlockDocument.KSBlockDocumentWithSections -> {
        document.content.forEach { section ->
          writeSection(settings, output, section)
        }
      }
    }

    output.write("\n")
    output.write("Footnotes\n")
    KSInlineRenderer.dividerHeavy(output, settings.page_width - 1)
    output.write("\n")
    output.write("\n")

    document.data.context.footnotesAll.forEach { entry ->
      val layout = KSPlainLayout.layoutFootnote(
        settings.page_width, entry.value)
      val image = KSPlainRasterizer.rasterize(layout)
      output.write(JPTextImages.show(image))
      output.write("\n")
    }
  }

  private fun tocDocument(
    settings : KSPlainSettings,
    output : Writer,
    document : KSBlockDocumentWithParts<KSEvaluation>) {

    output.write("  ")
    output.write("Contents\n")
    output.write("  ")
    KSInlineRenderer.dividerLight(output, settings.page_width - 3)
    output.write("\n")

    document.content.forEach { part ->
      writeTOCItem(
        settings,
        output,
        2,
        part.id,
        part.data.number,
        part.title)

      part.content.forEach { section ->
        writeTOCItem(
          settings,
          output,
          4,
          section.id,
          section.data.number,
          section.title)
      }
    }

    output.write("\n")
    output.write("\n")
  }

  private fun writeTOCItem(
    settings : KSPlainSettings,
    output : Writer,
    indent : Int,
    id : Optional<KSID<KSEvaluation>>,
    number : Optional<KSNumber>,
    title : List<KSInline.KSInlineText<KSEvaluation>>) {

    val title_buffer = StringBuilder(40)
    title_buffer.setLength(0)
    title_buffer.append(" ".repeat(indent))
    title_buffer.append(number.get().toString())
    title_buffer.append(" ")
    title_buffer.append(text(title))

    val id_buffer = StringBuilder(40)
    id_buffer.setLength(0)
    id.ifPresent { id -> id_buffer.append(id.value) }

    val line_buffer = StringBuilder(settings.page_width)
    line_buffer.append(title_buffer.toString())

    val title_len = title_buffer.length + 1
    val id_len = id_buffer.length + 1
    val max_len = title_len + id_len
    if (max_len < settings.page_width) {
      line_buffer.append(" ")

      for (i in 0 .. (settings.page_width - max_len) - 2) {
        line_buffer.append(".")
      }
      if (!id.isPresent) {
        line_buffer.append(".")
      }
    }

    line_buffer.append(" ")
    line_buffer.append(id_buffer.toString())

    output.write(KSInlineRenderer.trimTrailing(line_buffer.toString()))
    output.write("\n")
  }

  private fun writeSection(
    settings : KSPlainSettings,
    output : Writer,
    section : KSBlockSection<KSEvaluation>) {

    output.write(section.data.number.get().toString())
    output.write(" ")
    output.write(text(section.title))
    outID(section.id, output)
    output.write("\n")
    output.write("\n")

    when (section) {
      is KSBlockSectionWithSubsections -> {
        section.content.forEach { subsection ->
          writeSubsection(settings, output, subsection)
        }
      }
      is KSBlockSectionWithContent     -> {
        section.content.forEach { content ->
          writeSubsectionContent(settings, output, content)
        }
      }
    }
  }

  private fun writeSubsectionContent(
    settings : KSPlainSettings,
    output : Writer,
    content : KSSubsectionContent<KSEvaluation>) {

    return when (content) {
      is KSSubsectionParagraph  -> {
        writeParagraph(settings, output, content.paragraph)
      }
      is KSSubsectionFormalItem -> {
        writeFormalItem(settings, output, content.formal)
      }
      is KSSubsectionFootnote   -> {

      }
    }
  }

  private fun writeFormalItem(
    settings : KSPlainSettings,
    output : Writer,
    formal : KSBlockFormalItem<KSEvaluation>) {

    val layout = KSPlainLayout.layoutFormal(settings.page_width, formal)
    val image = KSPlainRasterizer.rasterize(layout)
    output.write(JPTextImages.show(image))
    output.write("\n")
  }

  private fun writeParagraph(
    settings : KSPlainSettings,
    output : Writer,
    paragraph : KSBlockParagraph<KSEvaluation>) {

    val layout = KSPlainLayout.layoutParagraph(settings.page_width, paragraph)
    val image = KSPlainRasterizer.rasterize(layout)
    output.write(JPTextImages.show(image))
    output.write("\n")
  }

  private fun <T> spaceRequired(
    e_prev : KSInline<T>,
    e_curr : KSInline<T>) : Boolean {

    if (e_prev is KSInline.KSInlineText && e_curr is KSInline.KSInlineText) {
      return true
    }

    if (!(e_prev is KSInline.KSInlineText) && e_curr is KSInline.KSInlineText) {
      return e_curr.text.length > 1
    }

    return true
  }

  private fun writeSubsection(
    settings : KSPlainSettings,
    output : Writer,
    subsection : KSBlockSubsection<KSEvaluation>) {

    output.write(subsection.data.number.get().toString())
    output.write(" ")
    output.write(text(subsection.title))
    outID(subsection.id, output)
    output.write("\n")
    output.write("\n")

    subsection.content.forEach { content ->
      writeSubsectionContent(settings, output, content)
    }
  }

  private fun writePart(
    settings : KSPlainSettings,
    output : Writer,
    part : KSBlockPart<KSEvaluation>) {

    output.write(part.data.number.get().toString())
    output.write(" ")
    output.write(text(part.title))
    outID(part.id, output)
    output.write("\n")
    KSInlineRenderer.dividerHeavy(output, settings.page_width - 1)
    output.write("\n")
    output.write("\n")

    if (settings.render_toc_parts) {
      tocPart(settings, output, part)
    }

    part.content.forEach { section -> writeSection(settings, output, section) }
  }

  private fun tocPart(
    settings : KSPlainSettings,
    output : Writer,
    part : KSBlockPart<KSEvaluation>) {

    output.write("  ")
    output.write("Contents\n")
    output.write("  ")
    KSInlineRenderer.dividerLight(output, settings.page_width - 3)
    output.write("\n")

    part.content.forEach { section ->
      writeTOCItem(
        settings,
        output,
        2,
        section.id,
        section.data.number,
        section.title)

      when (section) {
        is KSBlockSectionWithSubsections -> {
          section.content.forEach { subsection ->
            writeTOCItem(
              settings,
              output,
              4,
              subsection.id,
              subsection.data.number,
              subsection.title)
          }
        }
        is KSBlockSectionWithContent     -> {

        }
      }
    }

    output.write("\n")
    output.write("\n")
  }

  private fun outID(iid : Optional<KSID<KSEvaluation>>, output : Writer) {
    iid.ifPresent { id ->
      output.write(" [id: ")
      output.write(id.value)
      output.write("]")
    }
  }

  private fun text(text : List<KSInline.KSInlineText<KSEvaluation>>) : String {
    val sb = StringBuilder()
    val max = text.size - 1
    for (i in 0 .. max) {
      sb.append(text[i].text)
      if (i < max) {
        sb.append(" ")
      }
    }
    return sb.toString()
  }

}