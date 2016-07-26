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

import com.io7m.jpita.core.JPAlignerBasic
import com.io7m.jpita.core.JPAlignerType
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
import com.io7m.kstructural.core.KSLink
import com.io7m.kstructural.core.KSLinkContent
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
    dividerHeavy(output, 72)
    output.write("\n")
    output.write("\n")

    when (document) {
      is KSBlockDocumentWithParts                    -> {
        if (settings.render_toc_document) {
          tocDocument(output, document)
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
    dividerHeavy(output, 72)
    output.write("\n")
    output.write("\n")

    document.data.context.footnotesAll.forEach { entry ->
      val id = entry.key
      val fn = entry.value

      val jb = JPAlignerBasic.create(72 - 8)
      inlineContent(fn.content, jb)

      val lines = jb.finish()
      for (i in 0 .. lines.size - 1) {
        if (i == 0) {
          val ns = fn.data.index.toString()
          val fs = "[" + ns + "]"
          output.write(fs)
          output.write(" ".repeat(8 - fs.length))
        } else {
          output.write("        ")
        }
        output.write(lines[i])
        output.write("\n")
      }
      output.write("\n")
    }

    output.flush()
  }

  private fun tocDocument(
    output : Writer,
    document : KSBlockDocumentWithParts<KSEvaluation>) {

    val title_buffer = StringBuilder()
    val id_buffer = StringBuilder()

    output.write("  ")
    output.write("Contents\n")
    output.write("  ")
    dividerLight(output, 70)
    output.write("\n")

    document.content.forEach { part ->
      writeTOCItem(
        output,
        title_buffer,
        id_buffer,
        2,
        part.id,
        part.data.number,
        part.title)

      part.content.forEach { section ->
        writeTOCItem(
          output,
          title_buffer,
          id_buffer,
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
    output : Writer,
    title_buffer : StringBuilder,
    id_buffer : StringBuilder,
    indent : Int,
    id : Optional<KSID<KSEvaluation>>,
    number : Optional<KSNumber>,
    title : List<KSInline.KSInlineText<KSEvaluation>>) {

    title_buffer.setLength(0)
    title_buffer.append(" ".repeat(indent))
    title_buffer.append(number.get().toString())
    title_buffer.append(" ")
    title_buffer.append(text(title))

    id_buffer.setLength(0)
    id.ifPresent { id -> id_buffer.append(id.value) }

    output.write(title_buffer.toString())
    val title_len = title_buffer.length + 1
    val id_len = id_buffer.length + 1
    val max_len = title_len + id_len
    if (max_len < 72) {
      output.write(" ")
      for (i in 0 .. (72 - max_len) - 1) {
        output.write(".")
      }
      if (!id.isPresent) {
        output.write(".")
      }
    }
    output.write(" ")
    output.write(id_buffer.toString())
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
          writeSubsection(output, subsection)
        }
      }
      is KSBlockSectionWithContent     -> {
        section.content.forEach { content ->
          writeSubsectionContent(output, content)
        }
      }
    }
  }

  private fun writeSubsectionContent(
    output : Writer,
    content : KSSubsectionContent<KSEvaluation>) {

    return when (content) {
      is KSSubsectionParagraph  -> {
        writeParagraph(output, content.paragraph)
      }
      is KSSubsectionFormalItem -> {
        writeFormalItem(output, content.formal)
      }
      is KSSubsectionFootnote   -> {

      }
    }
  }

  private fun writeFormalItem(
    output : Writer,
    formal : KSBlockFormalItem<KSEvaluation>) {

    output.write("    ")
    dividerLight(output, 68)
    output.write("\n")
    output.write("    ")
    output.write(formal.data.number.get().toString())
    output.write(" ")
    output.write(text(formal.title))
    outID(formal.id, output)
    output.write("\n")
    output.write("    ")
    dividerLight(output, 68)
    output.write("\n")

    val jb = JPAlignerBasic.create(68)
    inlineContent(formal.content, jb)

    val lines = jb.finish()
    lines.forEach { line ->
      output.write(line)
      output.write("\n")
    }

    output.write("\n")
    output.write("    ")
    dividerLight(output, 68)
    output.write("\n")
    output.write("\n")
  }

  private fun dividerLight(output : Writer, length : Int) {
    divider('─', output, length)
  }

  private fun dividerHeavy(output : Writer, length : Int) {
    divider('━', output, length)
  }

  private fun divider(c : Char, output : Writer, length : Int) {
    for (i in 0 .. length - 1) output.write("" + c)
  }

  private fun writeParagraph(
    output : Writer,
    paragraph : KSBlockParagraph<KSEvaluation>) {

    val jb = JPAlignerBasic.create(68)
    inlineContent(paragraph.content, jb)

    val lines = jb.finish()
    for (i in 0 .. lines.size - 1) {
      if (i == 0) {
        val ns = paragraph.data.number.get().least.toString()
        if (ns.length >= 4) {
          output.write(ns.substring(0, 3))
          output.write("…")
        } else {
          output.write(ns)
          output.write(" ".repeat(4 - ns.length))
        }

      } else {
        output.write("    ")
      }

      output.write(lines[i])
      output.write("\n")
    }

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

  private fun inlineContent(
    content : List<KSInline<KSEvaluation>>,
    output : JPAlignerType) : Unit {

    val word_buffer = StringBuilder()
    inlineContentBuffered(content, output, word_buffer)
    if (!word_buffer.isEmpty()) {
      output.addWord(word_buffer.toString())
    }
  }

  private fun inlineContentBuffered(
    content : List<KSInline<KSEvaluation>>,
    output : JPAlignerType,
    buffer : StringBuilder) {

    fun finishWord()
    {
      if (buffer.length > 0) {
        output.addWord(buffer.toString())
        buffer.setLength(0)
      }
    }

    val max = content.size - 1
    for (i in 0 .. max) {
      val e_prev = if (i > 0) content[i - 1] else null
      val e_curr = content[i]

      if (e_prev != null) {
        if (spaceRequired(e_prev, e_curr)) {
          finishWord()
        }
      }

      when (e_curr) {
        is KSInline.KSInlineListOrdered       -> {
          buffer.append("[list-ordered: Not yet supported]")
        }

        is KSInline.KSInlineListUnordered     -> {
          buffer.append("[list-unordered: Not yet supported]")
        }

        is KSInline.KSInlineImage             -> {
          finishWord()

          buffer.append("[image: ")
          buffer.append(e_curr.target)
          buffer.append("]")
          finishWord()

          buffer.append("(")
          buffer.append(text(e_curr.content))
          buffer.append(")")
          finishWord()
        }

        is KSInline.KSInlineTable             -> {
          buffer.append("[table: Not yet supported]")
        }

        is KSInline.KSInlineFootnoteReference -> {
          finishWord()

          buffer.append("[")
          buffer.append(e_curr.data.index)
          buffer.append("]")
        }

        is KSInline.KSInlineTerm              -> {
          inlineContentBuffered(e_curr.content, output, buffer)
        }

        is KSInline.KSInlineVerbatim          -> {
          finishWord()
          buffer.append(e_curr.text.text.trim())
        }

        is KSInline.KSInlineText              -> {
          buffer.append(e_curr.text)
        }

        is KSInline.KSInlineLink              -> {
          when (e_curr.actual) {
            is KSLink.KSLinkInternal -> {
              val ee = e_curr.actual as KSLink.KSLinkInternal
              inlineContentBuffered(ee.content.map { c ->
                when (c) {
                  is KSLinkContent.KSLinkText  -> c.actual
                  is KSLinkContent.KSLinkImage -> c.actual
                }
              }, output, buffer)

              finishWord()

              buffer.append("[ref: ")
              buffer.append(ee.target.value)
              buffer.append("]")
            }
            is KSLink.KSLinkExternal -> {
              val ee = e_curr.actual as KSLink.KSLinkExternal
              inlineContentBuffered(ee.content.map { c ->
                when (c) {
                  is KSLinkContent.KSLinkText  -> c.actual
                  is KSLinkContent.KSLinkImage -> c.actual
                }
              }, output, buffer)

              finishWord()

              buffer.append("[url: ")
              buffer.append(ee.target)
              buffer.append("]")
            }
          }
        }
      }
    }
  }

  private fun writeSubsection(
    output : Writer,
    subsection : KSBlockSubsection<KSEvaluation>) {

    output.write(subsection.data.number.get().toString())
    output.write(" ")
    output.write(text(subsection.title))
    outID(subsection.id, output)
    output.write("\n")
    output.write("\n")

    subsection.content.forEach { content ->
      writeSubsectionContent(output, content)
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
    dividerHeavy(output, 72)
    output.write("\n")
    output.write("\n")

    if (settings.render_toc_parts) {
      tocPart(output, part)
    }

    part.content.forEach { section -> writeSection(settings, output, section) }
  }

  private fun tocPart(output : Writer, part : KSBlockPart<KSEvaluation>) {

    val title_buffer = StringBuilder()
    val id_buffer = StringBuilder()

    output.write("  ")
    output.write("Contents\n")
    output.write("  ")
    dividerLight(output, 70)
    output.write("\n")

    part.content.forEach { section ->
      writeTOCItem(
        output,
        title_buffer,
        id_buffer,
        2,
        section.id,
        section.data.number,
        section.title)

      when (section) {
        is KSBlockSectionWithSubsections -> {
          section.content.forEach { subsection ->
            writeTOCItem(
              output,
              title_buffer,
              id_buffer,
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