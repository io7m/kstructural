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

package com.io7m.kstructural.parser

import com.io7m.kstructural.core.KSDocumentContent
import com.io7m.kstructural.core.KSDocumentContent.KSDocumentPart
import com.io7m.kstructural.core.KSDocumentContent.KSDocumentSection
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
import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSLexicalType
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSParseContextType
import com.io7m.kstructural.core.KSParseError
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.core.KSSectionContent
import com.io7m.kstructural.core.KSSectionContent.KSSectionSubsection
import com.io7m.kstructural.core.KSSectionContent.KSSectionSubsectionContent
import com.io7m.kstructural.core.KSSubsectionContent
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionFootnote
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionFormalItem
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionParagraph
import com.io7m.kstructural.parser.KSExpression.KSExpressionList
import com.io7m.kstructural.parser.KSExpression.KSExpressionSymbol
import org.slf4j.LoggerFactory
import org.valid4j.Assertive
import java.nio.file.Path
import java.util.HashMap
import java.util.Optional

class KSBlockParser private constructor(
  private val inlines : (KSParseContextType, KSExpression, Path) -> KSResult<KSInline<KSParse>, KSParseError>,
  private val importer : (KSParseContextType, KSBlockParserType, Path) -> KSResult<KSBlock<KSParse>, KSParseError>)
: KSBlockParserType {

  private data class Context(
    val context : KSParseContextType,
    val file : Path)

  companion object {

    private val LOG = LoggerFactory.getLogger(KSBlockParser::class.java)

    fun get(
      inlines : (KSParseContextType, KSExpression, Path) -> KSResult<KSInline<KSParse>, KSParseError>,
      importer : (KSParseContextType, KSBlockParserType, Path) -> KSResult<KSBlock<KSParse>, KSParseError>)
      : KSBlockParserType =
      KSBlockParser(inlines, importer)

    private fun failedToMatch(
      e : KSExpressionList,
      m : List<KSExpressionMatch>) : KSParseError {

      val sb = StringBuilder()
      sb.append("Input did not match expected form.")
      sb.append(System.lineSeparator())
      sb.append("  Expected one of: ")
      sb.append(System.lineSeparator())

      for (i in 0 .. m.size - 1) {
        sb.append("    ")
        sb.append(m[i])
        sb.append(System.lineSeparator())
      }

      sb.append("  Received: ")
      sb.append(System.lineSeparator())
      sb.append("    ")
      sb.append(e)
      sb.append(System.lineSeparator())
      return KSParseError(e.position, sb.toString())
    }

    private fun <A : Any> failedToMatchResult(
      e : KSExpressionList,
      m : List<KSExpressionMatch>) : KSResult<A, KSParseError> =
      KSResult.fail(failedToMatch(e, m))

    private fun <A : Any> parseError(
      e : KSLexicalType, m : String) : KSResult<A, KSParseError> =
      KSResult.fail<A, KSParseError>(KSParseError(e.position, m))

    private fun parseAttributeType(e : KSExpressionList) : String {
      Assertive.require(e.elements.size == 2)
      Assertive.require(e.elements[0] is KSExpressionSymbol)
      Assertive.require(e.elements[1] is KSExpressionSymbol)
      return (e.elements[1] as KSExpressionSymbol).value
    }

    private fun parseAttributeID(
      e : KSExpressionList,
      c : Context)
      : KSID<KSParse> {
      Assertive.require(e.elements.size == 2)
      Assertive.require(e.elements[0] is KSExpressionSymbol)
      Assertive.require(e.elements[1] is KSExpressionSymbol)
      return KSID(e.position, (e.elements[1] as KSExpressionSymbol).value, KSParse(c.context))
    }
  }

  private object CommandMatchers {

    val symbol =
      KSExpressionMatch.anySymbol()
    val string =
      KSExpressionMatch.anyString()
    val symbol_or_string =
      KSExpressionMatch.oneOf(listOf(symbol, string))
    val any =
      KSExpressionMatch.anything()

    val id_name =
      KSExpressionMatch.exactSymbol("id")
    val id =
      KSExpressionMatch.allOfList(listOf(id_name, symbol))

    val import_name =
      KSExpressionMatch.exactSymbol("import")
    val import =
      KSExpressionMatch.allOfList(listOf(import_name, KSExpressionMatch.anyString()))

    val type_name =
      KSExpressionMatch.exactSymbol("type")
    val type =
      KSExpressionMatch.allOfList(listOf(type_name, symbol))

    val footnote_name =
      KSExpressionMatch.exactSymbol("footnote")
    val footnote =
      KSExpressionMatch.prefixOfList(listOf(footnote_name, id))
    val footnote_type =
      KSExpressionMatch.prefixOfList(listOf(footnote_name, id, type))

    val para_name =
      KSExpressionMatch.exactSymbol("paragraph")
    val para_any =
      KSExpressionMatch.prefixOfList(listOf(para_name))
    val para_with_id =
      KSExpressionMatch.prefixOfList(listOf(para_name, id))
    val para_with_id_type =
      KSExpressionMatch.prefixOfList(listOf(para_name, id, type))
    val para_with_type_id =
      KSExpressionMatch.prefixOfList(listOf(para_name, type, id))
    val para_with_type =
      KSExpressionMatch.prefixOfList(listOf(para_name, type))

    val title_name =
      KSExpressionMatch.exactSymbol("title")
    val title =
      KSExpressionMatch.prefixOfList(listOf(title_name, symbol_or_string))

    val formal_item_name =
      KSExpressionMatch.exactSymbol("formal-item")
    val formal_item_none =
      KSExpressionMatch.prefixOfList(listOf(formal_item_name, title))
    val formal_item_with_id =
      KSExpressionMatch.prefixOfList(listOf(formal_item_name, title, id))
    val formal_item_with_id_type =
      KSExpressionMatch.prefixOfList(listOf(formal_item_name, title, id, type))
    val formal_item_with_type_id =
      KSExpressionMatch.prefixOfList(listOf(formal_item_name, title, type, id))
    val formal_item_with_type =
      KSExpressionMatch.prefixOfList(listOf(formal_item_name, title, type))

    val subsection_name =
      KSExpressionMatch.exactSymbol("subsection")
    val subsection_none =
      KSExpressionMatch.prefixOfList(listOf(subsection_name, title))
    val subsection_with_id =
      KSExpressionMatch.prefixOfList(listOf(subsection_name, title, id))
    val subsection_with_id_type =
      KSExpressionMatch.prefixOfList(listOf(subsection_name, title, id, type))
    val subsection_with_type_id =
      KSExpressionMatch.prefixOfList(listOf(subsection_name, title, type, id))
    val subsection_with_type =
      KSExpressionMatch.prefixOfList(listOf(subsection_name, title, type))

    val section_name =
      KSExpressionMatch.exactSymbol("section")
    val section_none =
      KSExpressionMatch.prefixOfList(listOf(section_name, title))
    val section_with_id =
      KSExpressionMatch.prefixOfList(listOf(section_name, title, id))
    val section_with_id_type =
      KSExpressionMatch.prefixOfList(listOf(section_name, title, id, type))
    val section_with_type_id =
      KSExpressionMatch.prefixOfList(listOf(section_name, title, type, id))
    val section_with_type =
      KSExpressionMatch.prefixOfList(listOf(section_name, title, type))

    val part_name =
      KSExpressionMatch.exactSymbol("part")
    val part_none =
      KSExpressionMatch.prefixOfList(listOf(part_name, title))
    val part_with_id =
      KSExpressionMatch.prefixOfList(listOf(part_name, title, id))
    val part_with_id_type =
      KSExpressionMatch.prefixOfList(listOf(part_name, title, id, type))
    val part_with_type_id =
      KSExpressionMatch.prefixOfList(listOf(part_name, title, type, id))
    val part_with_type =
      KSExpressionMatch.prefixOfList(listOf(part_name, title, type))

    val document_name =
      KSExpressionMatch.exactSymbol("document")
    val document_none =
      KSExpressionMatch.prefixOfList(listOf(document_name, title))
    val document_with_id =
      KSExpressionMatch.prefixOfList(listOf(document_name, title, id))
    val document_with_id_type =
      KSExpressionMatch.prefixOfList(listOf(document_name, title, id, type))
    val document_with_type_id =
      KSExpressionMatch.prefixOfList(listOf(document_name, title, type, id))
    val document_with_type =
      KSExpressionMatch.prefixOfList(listOf(document_name, title, type))
  }

  private fun parseAttributeTitle(
    e : KSExpressionList,
    c : Context)
    : KSResult<List<KSInlineText<KSParse>>, KSParseError> {
    Assertive.require(e.elements.size >= 2)
    Assertive.require(e.elements[0] is KSExpressionSymbol)
    val texts = e.elements.subList(1, e.elements.size)
    return parseInlineTexts(texts, c)
  }

  private fun parseBlockFootnote(
    e : KSExpressionList,
    c : Context)
    : KSResult<KSBlockFootnote<KSParse>, KSParseError> {

    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpressionSymbol)

    when {
      KSExpressionMatch.matches(e, CommandMatchers.footnote_type) -> {
        Assertive.require(e.elements.size >= 3)
        val id = parseAttributeID(e.elements[1] as KSExpressionList, c)
        val type = parseAttributeType(e.elements[2] as KSExpressionList)
        val rest = e.elements.subList(3, e.elements.size)
        val act_content = parseInlines(rest, c)

        return act_content flatMap { content ->
          KSResult.succeed<KSBlockFootnote<KSParse>, KSParseError>(
            KSBlockFootnote(
              e.position,
              e.square,
              KSParse(c.context),
              id = Optional.of(id),
              type = Optional.of(type),
              content = content))
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.footnote)      -> {
        Assertive.require(e.elements.size >= 2)
        val id = parseAttributeID(e.elements[1] as KSExpressionList, c)
        val rest = e.elements.subList(2, e.elements.size)
        val act_content = parseInlines(rest, c)

        return act_content flatMap { content ->
          KSResult.succeed<KSBlockFootnote<KSParse>, KSParseError>(
            KSBlockFootnote(
              e.position,
              e.square,
              KSParse(c.context),
              id = Optional.of(id),
              type = Optional.empty(),
              content = content))
        }
      }
    }

    return failedToMatchResult(e, listOf(
      CommandMatchers.footnote,
      CommandMatchers.footnote_type))
  }

  private fun parseBlockFormalItem(
    e : KSExpressionList,
    c : Context)
    : KSResult<KSBlockFormalItem<KSParse>, KSParseError> {

    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpressionSymbol)

    when {
      KSExpressionMatch.matches(e, CommandMatchers.formal_item_with_type_id) -> {
        Assertive.require(e.elements.size >= 4)
        val act_title = parseAttributeTitle(e.elements[1] as KSExpressionList, c)
        val type = parseAttributeType(e.elements[2] as KSExpressionList)
        val id = parseAttributeID(e.elements[3] as KSExpressionList, c)
        val rest = e.elements.subList(4, e.elements.size)
        val act_content = parseInlines(rest, c)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSResult.succeed<KSBlockFormalItem<KSParse>, KSParseError>(
              KSBlockFormalItem(
                e.position,
                e.square,
                KSParse(c.context),
                title = title,
                id = Optional.of(id),
                type = Optional.of(type),
                content = content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.formal_item_with_id_type) -> {
        Assertive.require(e.elements.size >= 4)
        val act_title = parseAttributeTitle(e.elements[1] as KSExpressionList, c)
        val id = parseAttributeID(e.elements[2] as KSExpressionList, c)
        val type = parseAttributeType(e.elements[3] as KSExpressionList)
        val rest = e.elements.subList(4, e.elements.size)
        val act_content = parseInlines(rest, c)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSResult.succeed<KSBlockFormalItem<KSParse>, KSParseError>(
              KSBlockFormalItem(
                e.position,
                e.square,
                KSParse(c.context),
                title = title,
                id = Optional.of(id),
                type = Optional.of(type),
                content = content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.formal_item_with_type)    -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(e.elements[1] as KSExpressionList, c)
        val type = parseAttributeType(e.elements[2] as KSExpressionList)
        val rest = e.elements.subList(3, e.elements.size)
        val act_content = parseInlines(rest, c)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSResult.succeed<KSBlockFormalItem<KSParse>, KSParseError>(
              KSBlockFormalItem(
                e.position,
                e.square,
                KSParse(c.context),
                title = title,
                id = Optional.empty(),
                type = Optional.of(type),
                content = content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.formal_item_with_id)      -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(e.elements[1] as KSExpressionList, c)
        val id = parseAttributeID(e.elements[2] as KSExpressionList, c)
        val rest = e.elements.subList(3, e.elements.size)
        val act_content = parseInlines(rest, c)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSResult.succeed<KSBlockFormalItem<KSParse>, KSParseError>(
              KSBlockFormalItem(
                e.position,
                e.square,
                KSParse(c.context),
                title = title,
                id = Optional.of(id),
                type = Optional.empty(),
                content = content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.formal_item_none)         -> {
        Assertive.require(e.elements.size >= 2)
        val act_title = parseAttributeTitle(e.elements[1] as KSExpressionList, c)
        val rest = e.elements.subList(2, e.elements.size)
        val act_content = parseInlines(rest, c)

        return act_title flatMap { title ->
          act_content flatMap { content ->
            KSResult.succeed<KSBlockFormalItem<KSParse>, KSParseError>(
              KSBlockFormalItem(
                e.position,
                e.square,
                KSParse(c.context),
                title = title,
                id = Optional.empty(),
                type = Optional.empty(),
                content = content))
          }
        }
      }
    }

    return failedToMatchResult(e, listOf(
      CommandMatchers.formal_item_none,
      CommandMatchers.formal_item_with_id,
      CommandMatchers.formal_item_with_id_type,
      CommandMatchers.formal_item_with_type,
      CommandMatchers.formal_item_with_type_id))
  }

  private fun parseBlockImport(
    e : KSExpressionList,
    c : Context)
    : KSResult<KSBlockImport<KSParse>, KSParseError> {

    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpressionSymbol)

    when {
      KSExpressionMatch.matches(e, CommandMatchers.import) -> {
        Assertive.require(e.elements.size == 2)
        return parseInlineText(c, e.elements[1]) flatMap { file ->
          val re = KSBlockImport(
            e.position, e.square, KSParse(c.context), Optional.empty(), Optional.empty(), file)
          loadImport(re, c, file)
        }
      }
    }

    return failedToMatchResult(e, listOf(CommandMatchers.import))
  }

  private fun loadImport(
    e : KSBlockImport<KSParse>,
    c : Context,
    f : KSInlineText<KSParse>)
    : KSResult<KSBlockImport<KSParse>, KSParseError> {

    val base_abs = c.file.toAbsolutePath()
    val real = base_abs.resolveSibling(f.text)

    return c.context.checkImportCycle(
      importer = base_abs,
      import = e,
      imported_path = real) flatMap {

      val r : KSResult<KSBlock<KSParse>, KSParseError> =
        try {
          LOG.debug("import: {}", real)
          this.importer.invoke(c.context, this, real)
        } catch (x : Throwable) {
          val sb = StringBuilder()
          sb.append("Failed to import file.")
          sb.append(System.lineSeparator())
          sb.append("  File:  ")
          sb.append(real)
          sb.append(System.lineSeparator())
          sb.append("  Error: ")
          sb.append(x)
          sb.append(System.lineSeparator())
          KSResult.fail<KSBlock<KSParse>, KSParseError>(
            KSParseError(e.position, sb.toString()))
        }

      r flatMap { b ->
        c.context.addImport(
          importer = base_abs,
          import = e,
          imported_path = real,
          imported = b)
        KSResult.succeed<KSBlockImport<KSParse>, KSParseError>(e)
      }
    }
  }

  private fun parseBlockPara(
    e : KSExpressionList,
    c : Context)
    : KSResult<KSBlockParagraph<KSParse>, KSParseError> {
    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpressionSymbol)

    when {
      KSExpressionMatch.matches(e, CommandMatchers.para_with_id_type) -> {
        Assertive.require(e.elements.size >= 3)
        val id = parseAttributeID(
          e.elements[1] as KSExpressionList, c)
        val type = parseAttributeType(
          e.elements[2] as KSExpressionList)
        val rest = e.elements.subList(3, e.elements.size)
        val act_content = parseInlines(rest, c)

        return act_content flatMap { content ->
          KSResult.succeed<KSBlockParagraph<KSParse>, KSParseError>(
            newBlockParagraph(c, content, e, Optional.of(id), Optional.of(type)))
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.para_with_type_id) -> {
        Assertive.require(e.elements.size >= 3)
        val type = parseAttributeType(
          e.elements[1] as KSExpressionList)
        val id = parseAttributeID(
          e.elements[2] as KSExpressionList, c)
        val rest = e.elements.subList(3, e.elements.size)
        val act_content = parseInlines(rest, c)

        return act_content flatMap { content ->
          KSResult.succeed<KSBlockParagraph<KSParse>, KSParseError>(
            newBlockParagraph(c, content, e, Optional.of(id), Optional.of(type)))
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.para_with_id)      -> {
        Assertive.require(e.elements.size >= 2)
        val id = parseAttributeID(e.elements[1] as KSExpressionList, c)
        val rest = e.elements.subList(2, e.elements.size)
        val act_content = parseInlines(rest, c)

        return act_content flatMap { content ->
          KSResult.succeed<KSBlockParagraph<KSParse>, KSParseError>(
            newBlockParagraph(c, content, e, Optional.of(id), Optional.empty()))
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.para_with_type)    -> {
        Assertive.require(e.elements.size >= 2)
        val type = parseAttributeType(e.elements[1] as KSExpressionList)
        val rest = e.elements.subList(2, e.elements.size)
        val act_content = parseInlines(rest, c)

        return act_content flatMap { content ->
          KSResult.succeed<KSBlockParagraph<KSParse>, KSParseError>(
            newBlockParagraph(c, content, e, Optional.empty(), Optional.of(type)))
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.para_any)          -> {
        Assertive.require(e.elements.size >= 1)
        val rest = e.elements.subList(1, e.elements.size)
        val act_content = parseInlines(rest, c)

        return act_content flatMap { content ->
          KSResult.succeed<KSBlockParagraph<KSParse>, KSParseError>(
            newBlockParagraph(c, content, e, Optional.empty(), Optional.empty()))
        }
      }
    }

    return failedToMatchResult(e, listOf(
      CommandMatchers.para_any,
      CommandMatchers.para_with_id,
      CommandMatchers.para_with_id_type,
      CommandMatchers.para_with_type_id,
      CommandMatchers.para_with_type))
  }

  private fun newBlockParagraph(
    c : Context,
    content : List<KSInline<KSParse>>,
    e : KSExpressionList,
    id : Optional<KSID<KSParse>>,
    type : Optional<String>)
    : KSBlockParagraph<KSParse> {
    return KSBlockParagraph(
      e.position, e.square, KSParse(c.context), type, id, content)
  }

  private fun parseBlockSubsection(
    e : KSExpressionList,
    c : Context)
    : KSResult<KSBlockSubsection<KSParse>, KSParseError> {
    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpressionSymbol)

    when {
      KSExpressionMatch.matches(e, CommandMatchers.subsection_with_type_id) -> {
        Assertive.require(e.elements.size >= 4)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val type = parseAttributeType(
          e.elements[2] as KSExpressionList)
        val id = parseAttributeID(
          e.elements[3] as KSExpressionList, c)
        val rest = e.elements.subList(4, e.elements.size)
        val act_content = parseSubsectionContents(rest, c)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSResult.succeed<KSBlockSubsection<KSParse>, KSParseError>(
              KSBlockSubsection(
                e.position,
                e.square,
                KSParse(c.context),
                title = title,
                id = Optional.of(id),
                type = Optional.of(type),
                content = content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.subsection_with_id_type) -> {
        Assertive.require(e.elements.size >= 4)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val id = parseAttributeID(
          e.elements[2] as KSExpressionList, c)
        val type = parseAttributeType(
          e.elements[3] as KSExpressionList)
        val rest = e.elements.subList(4, e.elements.size)
        val act_content = parseSubsectionContents(rest, c)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSResult.succeed<KSBlockSubsection<KSParse>, KSParseError>(
              KSBlockSubsection(
                e.position,
                e.square,
                KSParse(c.context),
                title = title,
                id = Optional.of(id),
                type = Optional.of(type),
                content = content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.subsection_with_type)    -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val type = parseAttributeType(
          e.elements[2] as KSExpressionList)
        val rest = e.elements.subList(3, e.elements.size)
        val act_content = parseSubsectionContents(rest, c)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSResult.succeed<KSBlockSubsection<KSParse>, KSParseError>(
              KSBlockSubsection(
                e.position,
                e.square,
                KSParse(c.context),
                title = title,
                id = Optional.empty(),
                type = Optional.of(type),
                content = content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.subsection_with_id)      -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val id = parseAttributeID(
          e.elements[2] as KSExpressionList, c)
        val rest = e.elements.subList(3, e.elements.size)
        val act_content = parseSubsectionContents(rest, c)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSResult.succeed<KSBlockSubsection<KSParse>, KSParseError>(
              KSBlockSubsection(
                e.position,
                e.square,
                KSParse(c.context),
                title = title,
                id = Optional.of(id),
                type = Optional.empty(),
                content = content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.subsection_none)         -> {
        Assertive.require(e.elements.size >= 2)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val rest = e.elements.subList(2, e.elements.size)
        val act_content = parseSubsectionContents(rest, c)

        return act_title flatMap { title ->
          act_content flatMap { content ->
            KSResult.succeed<KSBlockSubsection<KSParse>, KSParseError>(
              KSBlockSubsection(
                e.position,
                e.square,
                KSParse(c.context),
                title = title,
                id = Optional.empty(),
                type = Optional.empty(),
                content = content))
          }
        }
      }
    }

    return failedToMatchResult(e, listOf(
      CommandMatchers.subsection_none,
      CommandMatchers.subsection_with_id,
      CommandMatchers.subsection_with_id_type,
      CommandMatchers.subsection_with_type,
      CommandMatchers.subsection_with_type_id))
  }

  private fun sectionContentToSubsectionContent(
    e : KSSectionContent<KSParse>,
    c : Context)
    : KSResult<KSSubsectionContent<KSParse>, KSParseError> =
    when (e) {
      is KSSectionContent.KSSectionSubsectionContent ->
        KSResult.succeed<KSSubsectionContent<KSParse>, KSParseError>(e.content)
      is KSSectionContent.KSSectionSubsection        -> {
        val sb = StringBuilder()
        sb.append("Expected subsection content.")
        sb.append(System.lineSeparator())
        sb.append("  Expected: Subsection content")
        sb.append(System.lineSeparator())
        sb.append("  Received: ")
        sb.append(e)
        sb.append(System.lineSeparator())
        parseError(e, sb.toString())
      }
    }

  private fun sectionContentToSubsection(
    e : KSSectionContent<KSParse>,
    c : Context)
    : KSResult<KSBlockSubsection<KSParse>, KSParseError> =
    when (e) {
      is KSSectionContent.KSSectionSubsectionContent -> {
        val sb = StringBuilder()
        sb.append("Expected a subsection.")
        sb.append(System.lineSeparator())
        sb.append("  Expected: A subsection")
        sb.append(System.lineSeparator())
        sb.append("  Received: ")
        sb.append(e)
        sb.append(System.lineSeparator())
        parseError(e, sb.toString())
      }
      is KSSectionContent.KSSectionSubsection        -> {
        KSResult.succeed<KSBlockSubsection<KSParse>, KSParseError>(e.subsection)
      }
    }

  private fun parseBlockSection(
    e : KSExpressionList,
    c : Context)
    : KSResult<KSBlockSection<KSParse>, KSParseError> {
    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpressionSymbol)

    when {
      KSExpressionMatch.matches(e, CommandMatchers.section_with_type_id) -> {
        Assertive.require(e.elements.size >= 4)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val type = parseAttributeType(
          e.elements[2] as KSExpressionList)
        val id = parseAttributeID(
          e.elements[3] as KSExpressionList, c)
        val rest = e.elements.subList(4, e.elements.size)
        val act_content = parseSectionContents(rest, c)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            parseBlockSectionActual(
              c, content, e, Optional.of(id), Optional.of(type), title)
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.section_with_id_type) -> {
        Assertive.require(e.elements.size >= 4)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val id = parseAttributeID(
          e.elements[2] as KSExpressionList, c)
        val type = parseAttributeType(
          e.elements[3] as KSExpressionList)
        val rest = e.elements.subList(4, e.elements.size)
        val act_content = parseSectionContents(rest, c)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            parseBlockSectionActual(
              c, content, e, Optional.of(id), Optional.of(type), title)
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.section_with_type)    -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val type = parseAttributeType(
          e.elements[2] as KSExpressionList)
        val rest = e.elements.subList(3, e.elements.size)
        val act_content = parseSectionContents(rest, c)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            parseBlockSectionActual(
              c, content, e, Optional.empty(), Optional.of(type), title)
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.section_with_id)      -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val id = parseAttributeID(
          e.elements[2] as KSExpressionList, c)
        val rest = e.elements.subList(3, e.elements.size)
        val act_content = parseSectionContents(rest, c)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            parseBlockSectionActual(
              c, content, e, Optional.of(id), Optional.empty(), title)
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.section_none)         -> {
        Assertive.require(e.elements.size >= 2)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val rest = e.elements.subList(2, e.elements.size)
        val act_content = parseSectionContents(rest, c)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            parseBlockSectionActual(
              c, content, e, Optional.empty(), Optional.empty(), title)
          }
        }
      }
    }

    return failedToMatchResult(e, listOf(
      CommandMatchers.section_none,
      CommandMatchers.section_with_id,
      CommandMatchers.section_with_id_type,
      CommandMatchers.section_with_type,
      CommandMatchers.section_with_type_id))
  }

  private fun parseBlockSectionActual(
    c : Context,
    content : List<KSSectionContent<KSParse>>,
    e : KSExpressionList,
    id : Optional<KSID<KSParse>>,
    type : Optional<String>,
    title : List<KSInlineText<KSParse>>)
    : KSResult<KSBlockSection<KSParse>, KSParseError> {
    return if (content.isEmpty()) {
      parseError(e, "Sections cannot be empty")
    } else {
      val c0 = content[0]
      when (c0) {
        is KSSectionSubsectionContent -> {
          val act_contents =
            KSResult.listMap({ cc ->
              sectionContentToSubsectionContent(cc, c)
            }, content)
          act_contents flatMap { contents ->
            KSResult.succeed<KSBlockSection<KSParse>, KSParseError>(
              KSBlockSection.KSBlockSectionWithContent(
                e.position, e.square, KSParse(c.context), type, id, title, contents))
          }
        }
        is KSSectionSubsection        -> {
          val act_contents =
            KSResult.listMap({ cc ->
              sectionContentToSubsection(cc, c)
            }, content)
          act_contents flatMap { contents ->
            KSResult.succeed<KSBlockSection<KSParse>, KSParseError>(
              KSBlockSection.KSBlockSectionWithSubsections(
                e.position, e.square, KSParse(c.context), type, id, title, contents))
          }
        }
      }
    }
  }

  private fun parseSubsectionContent(
    e : KSExpression,
    c : Context)
    : KSResult<KSSubsectionContent<KSParse>, KSParseError> {
    return parseBlockAny(e, c) flatMap { k -> anyToSubsectionContent(k, c) }
  }

  private fun parseSectionContent(
    e : KSExpression,
    c : Context)
    : KSResult<KSSectionContent<KSParse>, KSParseError> {
    return parseBlockAny(e, c) flatMap { k -> anyToSectionContent(k, c) }
  }

  private fun parseBlockPart(
    e : KSExpressionList,
    c : Context)
    : KSResult<KSBlockPart<KSParse>, KSParseError> {
    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpressionSymbol)

    when {
      KSExpressionMatch.matches(e, CommandMatchers.part_with_type_id) -> {
        Assertive.require(e.elements.size >= 4)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val type = parseAttributeType(
          e.elements[2] as KSExpressionList)
        val id = parseAttributeID(
          e.elements[3] as KSExpressionList, c)
        val rest = e.elements.subList(4, e.elements.size)
        val act_content =
          KSResult.listMap({ s -> parseSectionAny(s, c) }, rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSResult.succeed<KSBlockPart<KSParse>, KSParseError>(KSBlockPart(
              e.position,
              e.square,
              KSParse(c.context),
              Optional.of(type),
              Optional.of(id),
              title,
              content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.part_with_id_type) -> {
        Assertive.require(e.elements.size >= 4)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val id = parseAttributeID(
          e.elements[2] as KSExpressionList, c)
        val type = parseAttributeType(
          e.elements[3] as KSExpressionList)
        val rest = e.elements.subList(4, e.elements.size)
        val act_content =
          KSResult.listMap({ s -> parseSectionAny(s, c) }, rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSResult.succeed<KSBlockPart<KSParse>, KSParseError>(KSBlockPart(
              e.position,
              e.square,
              KSParse(c.context),
              Optional.of(type),
              Optional.of(id),
              title,
              content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.part_with_type)    -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val type = parseAttributeType(
          e.elements[2] as KSExpressionList)
        val rest = e.elements.subList(3, e.elements.size)
        val act_content =
          KSResult.listMap({ s -> parseSectionAny(s, c) }, rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSResult.succeed<KSBlockPart<KSParse>, KSParseError>(KSBlockPart(
              e.position,
              e.square,
              KSParse(c.context),
              Optional.of(type),
              Optional.empty(),
              title,
              content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.part_with_id)      -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val id = parseAttributeID(
          e.elements[2] as KSExpressionList, c)
        val rest = e.elements.subList(3, e.elements.size)
        val act_content =
          KSResult.listMap({ s -> parseSectionAny(s, c) }, rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSResult.succeed<KSBlockPart<KSParse>, KSParseError>(KSBlockPart(
              e.position,
              e.square,
              KSParse(c.context),
              Optional.empty(),
              Optional.of(id),
              title,
              content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.part_none)         -> {
        Assertive.require(e.elements.size >= 2)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val rest = e.elements.subList(2, e.elements.size)
        val act_content =
          KSResult.listMap({ s -> parseSectionAny(s, c) }, rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSResult.succeed<KSBlockPart<KSParse>, KSParseError>(KSBlockPart(
              e.position,
              e.square,
              KSParse(c.context),
              Optional.empty(),
              Optional.empty(),
              title,
              content))
          }
        }
      }
    }

    return failedToMatchResult(e, listOf(
      CommandMatchers.part_none,
      CommandMatchers.part_with_id,
      CommandMatchers.part_with_id_type,
      CommandMatchers.part_with_type,
      CommandMatchers.part_with_type_id))
  }

  private fun parseSectionAny(
    e : KSExpression,
    c : Context)
    : KSResult<KSBlockSection<KSParse>, KSParseError> {
    return parseBlockAny(e, c) flatMap { ee -> anyToSection(ee, c) }
  }

  private fun parseInlineTexts(
    e : List<KSExpression>,
    c : Context)
    : KSResult<List<KSInlineText<KSParse>>, KSParseError> {
    return KSResult.listMap({ k -> parseInlineText(c, k) }, e)
  }

  private fun parseInlineText(
    c : Context,
    e : KSExpression)
    : KSResult<KSInlineText<KSParse>, KSParseError> {
    return inlines.invoke(c.context, e, c.file) flatMap { r ->
      checkInlineText(e, r)
    }
  }

  private fun checkInlineText(
    e : KSExpression,
    r : KSInline<KSParse>)
    : KSResult<KSInlineText<KSParse>, KSParseError> {
    return when (r) {
      is KSInlineText  ->
        KSResult.succeed<KSInlineText<KSParse>, KSParseError>(r)
      is KSInlineLink,
      is KSInlineVerbatim,
      is KSInlineTerm,
      is KSInlineFootnoteReference,
      is KSInlineListOrdered,
      is KSInlineListUnordered,
      is KSInlineTable,
      is KSInlineInclude,
      is KSInlineImage -> {
        val sb = StringBuilder()
        sb.append("Expected text.")
        sb.append(System.lineSeparator())
        sb.append("  Expected: text")
        sb.append(System.lineSeparator())
        sb.append("  Received: ")
        sb.append(e)
        sb.append(System.lineSeparator())
        parseError(r, sb.toString())
      }
    }
  }

  private fun parseInlines(
    e : List<KSExpression>,
    c : Context)
    : KSResult<List<KSInline<KSParse>>, KSParseError> {
    return KSResult.listMap({ k ->
      val r = this.inlines.invoke(c.context, k, c.file)
      r flatMap { z -> KSResult.succeed<KSInline<KSParse>, KSParseError>(z) }
    }, e)
  }

  private fun parseSubsectionContents(
    e : List<KSExpression>,
    c : Context)
    : KSResult<List<KSSubsectionContent<KSParse>>, KSParseError> {
    return KSResult.listMap({ k -> parseSubsectionContent(k, c) }, e)
  }

  private fun parseSectionContents(
    e : List<KSExpression>,
    c : Context)
    : KSResult<List<KSSectionContent<KSParse>>, KSParseError> {
    return KSResult.listMap({ k -> parseSectionContent(k, c) }, e)
  }

  tailrec private fun chaseImport(
    e : KSBlockImport<KSParse>,
    c : Context)
    : KSBlock<KSParse> {

    Assertive.require(c.context.import_paths_by_element.containsKey(e))
    val path = c.context.import_paths_by_element[e]!!
    Assertive.require(c.context.imports_by_path.containsKey(path))
    val imported = c.context.imports_by_path[path]!!
    return when (imported) {
      is KSBlockDocument,
      is KSBlockSection,
      is KSBlockSubsection,
      is KSBlockParagraph,
      is KSBlockFormalItem,
      is KSBlockFootnote,
      is KSBlockPart   ->
        imported
      is KSBlockImport ->
        chaseImport(imported, c)
    }
  }

  tailrec private fun anyToDocumentContent(
    e : KSBlock<KSParse>,
    c : Context)
    : KSResult<KSDocumentContent<KSParse>, KSParseError> {
    return when (e) {
      is KSBlockDocument,
      is KSBlockSubsection,
      is KSBlockFormalItem,
      is KSBlockFootnote,
      is KSBlockParagraph -> {
        val sb = StringBuilder()
        sb.append("Expected document content.")
        sb.append(System.lineSeparator())
        sb.append("  Expected: Document content")
        sb.append(System.lineSeparator())
        sb.append("  Received: ")
        sb.append(e)
        sb.append(System.lineSeparator())
        parseError(e, sb.toString())
      }

      is KSBlockSection   ->
        KSResult.succeed(KSDocumentSection(e))
      is KSBlockPart      ->
        KSResult.succeed(KSDocumentPart(e))
      is KSBlockImport    ->
        anyToDocumentContent(chaseImport(e, c), c)
    }
  }

  tailrec private fun anyToSection(
    e : KSBlock<KSParse>,
    c : Context)
    : KSResult<KSBlockSection<KSParse>, KSParseError> {
    return when (e) {
      is KSBlockSection   ->
        KSResult.succeed<KSBlockSection<KSParse>, KSParseError>(e)
      is KSBlockDocument,
      is KSBlockFormalItem,
      is KSBlockFootnote,
      is KSBlockPart,
      is KSBlockSubsection,
      is KSBlockParagraph -> {
        val sb = StringBuilder()
        sb.append("Expected section.")
        sb.append(System.lineSeparator())
        sb.append("  Expected: A section")
        sb.append(System.lineSeparator())
        sb.append("  Received: ")
        sb.append(e)
        sb.append(System.lineSeparator())
        parseError(e, sb.toString())
      }
      is KSBlockImport    ->
        anyToSection(chaseImport(e, c), c)
    }
  }

  tailrec private fun anyToSubsectionContent(
    e : KSBlock<KSParse>,
    c : Context)
    : KSResult<KSSubsectionContent<KSParse>, KSParseError> {
    return when (e) {
      is KSBlockDocument,
      is KSBlockSection,
      is KSBlockSubsection,
      is KSBlockPart       -> {
        val sb = StringBuilder()
        sb.append("Expected subsection content.")
        sb.append(System.lineSeparator())
        sb.append("  Expected: Subsection content")
        sb.append(System.lineSeparator())
        sb.append("  Received: ")
        sb.append(e)
        sb.append(System.lineSeparator())
        parseError(e, sb.toString())
      }
      is KSBlockParagraph  ->
        KSResult.succeed(KSSubsectionParagraph(e))
      is KSBlockFormalItem ->
        KSResult.succeed(KSSubsectionFormalItem(e))
      is KSBlockFootnote   ->
        KSResult.succeed(KSSubsectionFootnote(e))
      is KSBlockImport     ->
        anyToSubsectionContent(chaseImport(e, c), c)
    }
  }

  tailrec private fun anyToSectionContent(
    e : KSBlock<KSParse>,
    c : Context)
    : KSResult<KSSectionContent<KSParse>, KSParseError> {
    return when (e) {
      is KSBlockDocument,
      is KSBlockSection,
      is KSBlockPart       -> {
        val sb = StringBuilder()
        sb.append("Expected section content.")
        sb.append(System.lineSeparator())
        sb.append("  Expected: Section content")
        sb.append(System.lineSeparator())
        sb.append("  Received: ")
        sb.append(e)
        sb.append(System.lineSeparator())
        parseError(e, sb.toString())
      }
      is KSBlockSubsection ->
        KSResult.succeed(KSSectionSubsection(e))
      is KSBlockParagraph  ->
        KSResult.succeed(KSSectionSubsectionContent(KSSubsectionParagraph(e)))
      is KSBlockFormalItem ->
        KSResult.succeed(KSSectionSubsectionContent(KSSubsectionFormalItem(e)))
      is KSBlockFootnote   ->
        KSResult.succeed(KSSectionSubsectionContent(KSSubsectionFootnote(e)))
      is KSBlockImport     ->
        anyToSectionContent(chaseImport(e, c), c)
    }
  }

  private fun parseDocumentContent(
    e : KSExpression,
    c : Context)
    : KSResult<KSDocumentContent<KSParse>, KSParseError> {
    return parseBlockAny(e, c) flatMap { k -> anyToDocumentContent(k, c) }
  }

  private fun parseDocumentContents(
    e : List<KSExpression>,
    c : Context)
    : KSResult<List<KSDocumentContent<KSParse>>, KSParseError> {
    return KSResult.listMap({ k -> parseDocumentContent(k, c) }, e)
  }

  private fun parseBlockDocument(
    e : KSExpressionList,
    c : Context)
    : KSResult<KSBlockDocument<KSParse>, KSParseError> {
    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpressionSymbol)

    when {
      KSExpressionMatch.matches(e, CommandMatchers.document_with_type_id) -> {
        Assertive.require(e.elements.size >= 4)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val type = parseAttributeType(
          e.elements[2] as KSExpressionList)
        val id = parseAttributeID(
          e.elements[3] as KSExpressionList, c)
        val rest = e.elements.subList(4, e.elements.size)
        val act_content = parseDocumentContents(rest, c)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            parseBlockDocumentActual(
              content, e, Optional.of(id), Optional.of(type), title, c)
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.document_with_id_type) -> {
        Assertive.require(e.elements.size >= 4)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val id = parseAttributeID(
          e.elements[2] as KSExpressionList, c)
        val type = parseAttributeType(
          e.elements[3] as KSExpressionList)
        val rest = e.elements.subList(4, e.elements.size)
        val act_content = parseDocumentContents(rest, c)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            parseBlockDocumentActual(
              content, e, Optional.of(id), Optional.of(type), title, c)
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.document_with_type)    -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val type = parseAttributeType(
          e.elements[2] as KSExpressionList)
        val rest = e.elements.subList(3, e.elements.size)
        val act_content = parseDocumentContents(rest, c)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            parseBlockDocumentActual(
              content, e, Optional.empty(), Optional.of(type), title, c)
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.document_with_id)      -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val id = parseAttributeID(
          e.elements[2] as KSExpressionList, c)
        val rest = e.elements.subList(3, e.elements.size)
        val act_content = parseDocumentContents(rest, c)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            parseBlockDocumentActual(
              content, e, Optional.of(id), Optional.empty(), title, c)
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.document_none)         -> {
        Assertive.require(e.elements.size >= 2)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val rest = e.elements.subList(2, e.elements.size)
        val act_content = parseDocumentContents(rest, c)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            parseBlockDocumentActual(
              content, e, Optional.empty(), Optional.empty(), title, c)
          }
        }
      }
    }

    return failedToMatchResult(e, listOf(
      CommandMatchers.document_none,
      CommandMatchers.document_with_id,
      CommandMatchers.document_with_id_type,
      CommandMatchers.document_with_type,
      CommandMatchers.document_with_type_id))
  }

  private fun parseBlockDocumentActual(
    content : List<KSDocumentContent<KSParse>>,
    e : KSExpressionList,
    id : Optional<KSID<KSParse>>,
    type : Optional<String>,
    title : List<KSInlineText<KSParse>>,
    c : Context)
    : KSResult<KSBlockDocument<KSParse>, KSParseError> {
    return if (content.isEmpty()) {
      parseError(e, "Documents cannot be empty")
    } else {
      val c0 = content[0]
      when (c0) {
        is KSDocumentPart    ->
          toDocumentWithParts(c, content, e, id, title, type)
        is KSDocumentSection ->
          toDocumentWithSections(c, content, e, id, title, type)
      }
    }
  }

  private fun toDocumentWithSections(
    c : Context,
    content : List<KSDocumentContent<KSParse>>,
    e : KSExpressionList,
    id : Optional<KSID<KSParse>>,
    title : List<KSInlineText<KSParse>>,
    type : Optional<String>)
    : KSResult<KSBlockDocument<KSParse>, KSParseError> {
    val act_contents =
      KSResult.listMap({ c -> documentContentToSection(c) }, content)
    return act_contents flatMap { contents ->
      KSResult.succeed<KSBlockDocument<KSParse>, KSParseError>(
        KSBlockDocumentWithSections(
          e.position, e.square, KSParse(c.context), id, type, title, contents))
    }
  }

  private fun toDocumentWithParts(
    c : Context,
    content : List<KSDocumentContent<KSParse>>,
    e : KSExpressionList,
    id : Optional<KSID<KSParse>>,
    title : List<KSInlineText<KSParse>>,
    type : Optional<String>)
    : KSResult<KSBlockDocument<KSParse>, KSParseError> {
    val act_contents =
      KSResult.listMap({ ec -> documentContentToPart(ec) }, content)
    return act_contents flatMap { contents ->
      KSResult.succeed<KSBlockDocument<KSParse>, KSParseError>(
        KSBlockDocumentWithParts(
          e.position, e.square, KSParse(c.context), id, type, title, contents))
    }
  }

  private fun documentContentToSection(
    e : KSDocumentContent<KSParse>)
    : KSResult<KSBlockSection<KSParse>, KSParseError> =
    when (e) {
      is KSDocumentContent.KSDocumentSection ->
        KSResult.succeed(e.section)
      is KSDocumentContent.KSDocumentPart    -> {
        val sb = StringBuilder()
        sb.append("Expected section.")
        sb.append(System.lineSeparator())
        sb.append("  Expected: A section")
        sb.append(System.lineSeparator())
        sb.append("  Received: ")
        sb.append(e)
        sb.append(System.lineSeparator())
        parseError(e, sb.toString())
      }
    }

  private fun documentContentToPart(
    e : KSDocumentContent<KSParse>)
    : KSResult<KSBlockPart<KSParse>, KSParseError> =
    when (e) {
      is KSDocumentContent.KSDocumentPart    ->
        KSResult.succeed(e.part)
      is KSDocumentContent.KSDocumentSection -> {
        val sb = StringBuilder()
        sb.append("Expected part.")
        sb.append(System.lineSeparator())
        sb.append("  Expected: A part")
        sb.append(System.lineSeparator())
        sb.append("  Received: ")
        sb.append(e)
        sb.append(System.lineSeparator())
        parseError(e, sb.toString())
      }
    }

  private val parsers : Map<String, ElementParser> =
    makeParsers()
  private val parserDescriptions : String =
    makeMapDescription(parsers)

  private fun makeParsers() : Map<String, ElementParser> {
    val m = HashMap<String, ElementParser>()
    m.put("paragraph", ElementParser("paragraph", {
      e, c ->
      parseBlockPara(e, c)
    }))
    m.put("formal-item", ElementParser("formal-item", {
      e, c ->
      parseBlockFormalItem(e, c)
    }))
    m.put("footnote", ElementParser("footnote", {
      e, c ->
      parseBlockFootnote(e, c)
    }))
    m.put("import", ElementParser("import", {
      e, c ->
      parseBlockImport(e, c)
    }))
    m.put("subsection", ElementParser("subsection", {
      e, c ->
      parseBlockSubsection(e, c)
    }))
    m.put("section", ElementParser("section", {
      e, c ->
      parseBlockSection(e, c)
    }))
    m.put("part", ElementParser("part", {
      e, c ->
      parseBlockPart(e, c)
    }))
    m.put("document", ElementParser("document", {
      e, c ->
      parseBlockDocument(e, c)
    }))
    return m
  }

  private data class ElementParser(
    val name : String,
    val parser : (KSExpressionList, Context) -> KSResult<KSBlock<KSParse>, KSParseError>)

  private fun makeMapDescription(m : Map<String, Any>) : String {
    val sb = StringBuilder()
    sb.append("{")
    val iter = m.keys.iterator()
    while (iter.hasNext()) {
      sb.append(iter.next())
      if (iter.hasNext()) {
        sb.append(" | ")
      }
    }
    sb.append("}")
    return sb.toString()
  }

  private fun elementName(e : KSExpressionList) : String {
    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpressionSymbol)
    return (e.elements[0] as KSExpressionSymbol).value
  }

  private val isElement =
    KSExpressionMatch.prefixOfList(
      listOf(KSExpressionMatch.MatchSymbol(
        { s -> parsers.containsKey(s) },
        parserDescriptions)))

  private fun parseBlockAny(
    e : KSExpression,
    c : Context)
    : KSResult<KSBlock<KSParse>, KSParseError> {
    if (!KSExpressionMatch.matches(e, isElement)) {
      val sb = StringBuilder()
      sb.append("Expected a block command.")
      sb.append(System.lineSeparator())
      sb.append("  Expected: ")
      sb.append(isElement)
      sb.append(System.lineSeparator())
      sb.append("  Received: ")
      sb.append(e)
      sb.append(System.lineSeparator())
      return parseError(e, sb.toString())
    }

    val el = e as KSExpressionList
    val name = elementName(el)
    Assertive.require(parsers.containsKey(name))
    val ic = parsers.get(name)!!
    Assertive.require(ic.name == name)
    return ic.parser.invoke(el, c)
  }

  override fun parse(
    context : KSParseContextType,
    expression : KSExpression,
    file : Path) : KSResult<KSBlock<KSParse>, KSParseError> {
    return parseBlockAny(expression, Context(context, file))
  }
}
