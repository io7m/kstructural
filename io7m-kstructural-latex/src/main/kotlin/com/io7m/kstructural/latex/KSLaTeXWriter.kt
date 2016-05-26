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

package com.io7m.kstructural.latex

import com.io7m.junreachable.UnimplementedCodeException
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithParts
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithSections
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockFootnote
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockFormalItem
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockParagraph
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockPart
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSubsection
import com.io7m.kstructural.core.KSElement.KSInline
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineFootnoteReference
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineImage
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineLink
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineListOrdered
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineListUnordered
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineTable
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineTerm
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineText
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineVerbatim
import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSLink
import com.io7m.kstructural.core.KSLinkContent
import com.io7m.kstructural.core.KSSubsectionContent
import com.io7m.kstructural.core.evaluator.KSEvaluation
import java.io.Writer
import java.util.Optional

object KSLaTeXWriter : KSLaTeXWriterType {

  private class Context {
    var formal_count = 0L
  }

  override fun write(
    settings : KSLaTeXSettings,
    document : KSBlockDocument<KSEvaluation>,
    output : Writer) : Unit {

    val c = Context()

    output.write("\\documentclass[twoside,11pt]{book}\n")
    output.write("\n")
    output.write("\\usepackage{graphicx}\n")
    output.write("\\usepackage[utf8]{inputenc}\n")
    output.write("\\usepackage[margin=1in]{geometry}\n")
    output.write("\\usepackage{hyperref}\n")
    output.write("\n")
    output.write("\\begin{document}\n")
    output.write("\\title{")
    output.write(text(document.title))
    output.write("}\n")
    output.write("\n")
    output.write("\\maketitle\n")
    output.write("\\tableofcontents\n")
    output.write("\n")

    when (document) {
      is KSBlockDocumentWithParts    -> {
        val d = document
        d.content.forEach { p ->
          writePart(settings, output, c, p)
        }
      }

      is KSBlockDocumentWithSections -> {
        val d = document
        d.content.forEach { s ->
          writeSection(settings, output, c, s)
        }
      }
    }

    if (c.formal_count > 0) {
      output.write("\\backmatter\n")
      output.write("\\listoffigures\n")
      output.write("\n")
    }

    output.write("\\end{document}\n")
    output.flush()
  }

  private fun writePart(
    settings : KSLaTeXSettings,
    output : Writer,
    context : Context,
    part : KSBlockPart<KSEvaluation>) {

    writeMarker(output, part.id)
    output.write("\\chapter{")
    output.write(text(part.title))
    output.write("}\n")
    output.write("\n")

    part.content.forEach { s ->
      writeSection(settings, output, context, s)
    }
  }

  private fun writeSection(
    settings : KSLaTeXSettings,
    output : Writer,
    context : Context,
    section : KSBlockSection<KSEvaluation>) {

    writeMarker(output, section.id)
    output.write("\\section{")
    output.write(text(section.title))
    output.write("}\n")
    output.write("\n")

    when (section) {
      is KSBlockSection.KSBlockSectionWithContent     -> {
        section.content.forEach { c ->
          writeSubsectionContent(settings, output, context, c)
        }
      }
      is KSBlockSection.KSBlockSectionWithSubsections -> {
        section.content.forEach { ss ->
          writeSubsection(settings, output, context, ss)
        }
      }
    }
  }

  private fun writeSubsection(
    settings : KSLaTeXSettings,
    output : Writer,
    context : Context,
    subsection : KSBlockSubsection<KSEvaluation>) {

    writeMarker(output, subsection.id)
    output.write("\\subsection{")
    output.write(text(subsection.title))
    output.write("}\n")
    output.write("\n")

    subsection.content.forEach { sc ->
      writeSubsectionContent(settings, output, context, sc)
    }
  }

  private fun writeMarker(
    output : Writer,
    id : Optional<KSID<KSEvaluation>>) {
    id.ifPresent { i ->
      output.write("\\label{")
      output.write(i.value)
      output.write("}\n")
    }
  }

  private fun writeSubsectionContent(
    settings : KSLaTeXSettings,
    output : Writer,
    context : Context,
    content : KSSubsectionContent<KSEvaluation>) : Unit =
    when (content) {
      is KSSubsectionContent.KSSubsectionParagraph  ->
        writeParagraph(settings, output, context, content.paragraph)
      is KSSubsectionContent.KSSubsectionFormalItem ->
        writeFormalItem(settings, output, context, content.formal)
      is KSSubsectionContent.KSSubsectionFootnote   ->
        writeFootnote(settings, output, context, content.footnote)
    }

  private fun writeFootnote(
    settings : KSLaTeXSettings,
    output : Writer,
    context : Context,
    footnote : KSBlockFootnote<KSEvaluation>) {

    output.write("\\footnotetext[")
    output.write(footnote.data.index.toString())
    output.write("]{")
    inlineContent(footnote.content, settings, output)
    output.write("}\n")
    output.write("\n")
    output.flush()
  }

  private fun writeFormalItem(
    settings : KSLaTeXSettings,
    output : Writer,
    context : Context,
    formal : KSBlockFormalItem<KSEvaluation>) {

    writeMarker(output, formal.id)
    output.write("\\begin{figure}\n")
    output.write("\\caption{")
    output.write(text(formal.title))
    output.write("}\n")
    inlineContent(formal.content, settings, output)
    output.write("\n")
    output.write("\\end{figure}\n")
    output.write("\n")
    output.flush()

    context.formal_count += 1L
  }

  private fun <T> spaceRequired(
    e_prev : KSInline<T>,
    e_curr : KSInline<T>) : Boolean {

    if (e_prev is KSInlineText && e_curr is KSInlineText) {
      return true
    }

    if (!(e_prev is KSInlineText) && e_curr is KSInlineText) {
      return e_curr.text.length > 1
    }

    return true
  }

  private fun inlineContent(
    content : List<KSInline<KSEvaluation>>,
    settings : KSLaTeXSettings,
    output : Writer) : Unit {

    val max = content.size - 1
    for (i in 0 .. max) {
      val e_prev = if (i > 0) content[i - 1] else null
      val e_curr = content[i]

      if (e_prev != null) {
        if (spaceRequired(e_prev, e_curr)) {
          output.write(" ")
        }
      }

      when (e_curr) {

        is KSInlineListOrdered       -> {
          output.write("\\begin{enumerate}\n")
          e_curr.content.forEach { item ->
            output.write("\\item ")
            inlineContent(item.content, settings, output)
            output.write("\n")
          }
          output.write("\\end{enumerate}\n")
        }

        is KSInlineListUnordered     -> {
          output.write("\\begin{itemize}\n")
          e_curr.content.forEach { item ->
            output.write("\\item ")
            inlineContent(item.content, settings, output)
            output.write("\n")
          }
          output.write("\\end{itemize}\n")
        }

        is KSInlineImage             -> {
          output.write("\\includegraphics{")
          output.write(e_curr.target.toString())
          output.write("}")
        }

        is KSInlineTable             -> {
          output.write("\\textbf{TABLE OMITTED}\n")
        }

        is KSInlineFootnoteReference -> {
          val fn = e_curr.data.context.elementForID(
            e_curr.target) as KSBlockFootnote<KSEvaluation>
          output.write("\\footnotemark[")
          output.write(fn.data.index.toString())
          output.write("]")
        }

        is KSInlineTerm              -> {
          if (e_curr.type.isPresent) {
            val type = e_curr.type.get()
            if (settings.typeEmphasisMap.containsKey(type.value)) {
              val emph = settings.typeEmphasisMap[type.value]!!
              when (emph) {
                is KSLaTeXEmphasis.Bold      -> {
                  output.write("\\textbf{")
                  inlineContent(e_curr.content, settings, output)
                  output.write("}")
                }
                is KSLaTeXEmphasis.Italic    -> {
                  output.write("\\emph{")
                  inlineContent(e_curr.content, settings, output)
                  output.write("}")
                }
                is KSLaTeXEmphasis.Monospace -> {
                  output.write("\\texttt{")
                  inlineContent(e_curr.content, settings, output)
                  output.write("}")
                }
              }
            } else {
              inlineContent(e_curr.content, settings, output)
            }
          }
        }

        is KSInlineVerbatim          -> {
          output.write("\\begin{verbatim}\n")
          output.write(e_curr.text.text)
          output.write("\n")
          output.write("\\end{verbatim}\n")
        }

        is KSInlineText              -> {
          output.write(e_curr.text)
        }

        is KSInlineLink              -> {
          when (e_curr.actual) {
            is KSLink.KSLinkInternal -> {
              val ee = e_curr.actual as KSLink.KSLinkInternal
              output.write("\\hyperref[")
              output.write(ee.target.value)
              output.write("]{")
              val max = ee.content.size - 1
              ee.content.forEachIndexed { i, lc ->
                when (lc) {
                  is KSLinkContent.KSLinkImage -> {

                  }
                  is KSLinkContent.KSLinkText  -> {
                    output.write(lc.actual.text)
                  }
                }
                if (i < max) {
                  output.write(" ")
                }
              }
              output.write("}")
            }
            is KSLink.KSLinkExternal -> {
              val ee = e_curr.actual as KSLink.KSLinkExternal
              output.write("\\href{")
              output.write(ee.target.toString())
              output.write("}{")
              val max = ee.content.size - 1
              ee.content.forEachIndexed { i, lc ->
                when (lc) {
                  is KSLinkContent.KSLinkImage -> {

                  }
                  is KSLinkContent.KSLinkText  -> {
                    output.write(lc.actual.text)
                  }
                }
                if (i < max) {
                  output.write(" ")
                }
              }
              output.write("}")
            }
          }
        }
      }
    }

    output.flush()
  }

  private fun writeParagraph(
    settings : KSLaTeXSettings,
    output : Writer,
    context : Context,
    paragraph : KSBlockParagraph<KSEvaluation>) : Unit {

    writeMarker(output, paragraph.id)
    inlineContent(paragraph.content, settings, output)
    output.write("\n")
    output.write("\n")
    output.flush()
  }

  private fun text(text : List<KSInlineText<KSEvaluation>>) : String {
    val sb = StringBuilder()
    val max = text.size - 1
    for (i in 0 .. max) {
      sb.append(text[i])
      if (i < max) {
        sb.append(" ")
      }
    }
    return sb.toString()
  }

}