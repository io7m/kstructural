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

import com.io7m.junreachable.UnreachableCodeException
import com.io7m.kstructural.core.KSBlock
import com.io7m.kstructural.core.KSBlock.KSBlockDocument
import com.io7m.kstructural.core.KSBlock.KSBlockDocument.KSBlockDocumentWithParts
import com.io7m.kstructural.core.KSBlock.KSBlockDocument.KSBlockDocumentWithSections
import com.io7m.kstructural.core.KSBlock.KSBlockParagraph
import com.io7m.kstructural.core.KSBlock.KSBlockPart
import com.io7m.kstructural.core.KSBlock.KSBlockSection
import com.io7m.kstructural.core.KSBlock.KSBlockSubsection
import com.io7m.kstructural.core.KSDocumentContent
import com.io7m.kstructural.core.KSDocumentContent.KSDocumentPart
import com.io7m.kstructural.core.KSDocumentContent.KSDocumentSection
import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSInline
import com.io7m.kstructural.core.KSInline.KSInlineImage
import com.io7m.kstructural.core.KSInline.KSInlineLink
import com.io7m.kstructural.core.KSInline.KSInlineTerm
import com.io7m.kstructural.core.KSInline.KSInlineText
import com.io7m.kstructural.core.KSInline.KSInlineVerbatim
import com.io7m.kstructural.core.KSLexicalType
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.core.KSSectionContent
import com.io7m.kstructural.core.KSSectionContent.KSSectionSubsection
import com.io7m.kstructural.core.KSSectionContent.KSSectionSubsectionContent
import com.io7m.kstructural.core.KSSubsectionContent
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionParagraph
import org.valid4j.Assertive
import java.util.HashMap
import java.util.Optional

class KSBlockParser private constructor(
  private val inlines : KSInlineParserType) : KSBlockParserType {

  companion object {

    fun get(p : KSInlineParserType) : KSBlockParserType =
      KSBlockParser(p)

    private fun failedToMatch(
      e : KSExpression.KSExpressionList,
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
      e : KSExpression.KSExpressionList,
      m : List<KSExpressionMatch>) : KSResult<A, KSParseError> =
      KSResult.fail(failedToMatch(e, m))

    private fun <A : Any> parseError(
      e : KSLexicalType, m : String) : KSResult<A, KSParseError> =
      KSResult.fail<A, KSParseError>(KSParseError(e.position, m))

    private fun parseAttributeType(e : KSExpression.KSExpressionList) : String {
      Assertive.require(e.elements.size == 2)
      Assertive.require(e.elements[0] is KSExpression.KSExpressionSymbol)
      Assertive.require(e.elements[1] is KSExpression.KSExpressionSymbol)
      return (e.elements[1] as KSExpression.KSExpressionSymbol).text
    }

    private fun parseAttributeID(e : KSExpression.KSExpressionList) : KSID<Unit> {
      Assertive.require(e.elements.size == 2)
      Assertive.require(e.elements[0] is KSExpression.KSExpressionSymbol)
      Assertive.require(e.elements[1] is KSExpression.KSExpressionSymbol)
      return KSID(e.position, (e.elements[1] as KSExpression.KSExpressionSymbol).text, Unit)
    }

    private fun parseAttributeTarget(e : KSExpression.KSExpressionList) : String {
      Assertive.require(e.elements.size == 2)
      Assertive.require(e.elements[0] is KSExpression.KSExpressionSymbol)
      val target = e.elements[1]
      return when (target) {
        is KSExpression.KSExpressionList   -> throw UnreachableCodeException()
        is KSExpression.KSExpressionSymbol -> target.text
        is KSExpression.KSExpressionQuoted -> target.text
      }
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

    val type_name =
      KSExpressionMatch.exactSymbol("type")
    val type =
      KSExpressionMatch.allOfList(listOf(type_name, symbol))

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
    e : KSExpression.KSExpressionList) : KSResult<List<KSInlineText<Unit>>, KSParseError> {
    Assertive.require(e.elements.size >= 2)
    Assertive.require(e.elements[0] is KSExpression.KSExpressionSymbol)
    val texts = e.elements.subList(1, e.elements.size)
    return parseInlineTexts(texts)
  }

  private fun parseBlockPara(
    e : KSExpression.KSExpressionList)
    : KSResult<KSBlockParagraph<Unit>, KSParseError> {
    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpression.KSExpressionSymbol)

    when {
      KSExpressionMatch.matches(e, CommandMatchers.para_with_id_type) -> {
        Assertive.require(e.elements.size >= 3)
        val id = parseAttributeID(
          e.elements[1] as KSExpression.KSExpressionList)
        val type = parseAttributeType(
          e.elements[2] as KSExpression.KSExpressionList)
        val rest = e.elements.subList(3, e.elements.size)
        val act_content = parseInlines(rest)

        return act_content flatMap { content ->
          KSResult.succeed<KSBlockParagraph<Unit>, KSParseError>(
            newBlockParagraph(content, e, Optional.of(id), Optional.of(type)))
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.para_with_type_id) -> {
        Assertive.require(e.elements.size >= 3)
        val type = parseAttributeType(
          e.elements[1] as KSExpression.KSExpressionList)
        val id = parseAttributeID(
          e.elements[2] as KSExpression.KSExpressionList)
        val rest = e.elements.subList(3, e.elements.size)
        val act_content = parseInlines(rest)

        return act_content flatMap { content ->
          KSResult.succeed<KSBlockParagraph<Unit>, KSParseError>(
            newBlockParagraph(content, e, Optional.of(id), Optional.of(type)))
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.para_with_id)      -> {
        Assertive.require(e.elements.size >= 2)
        val id = parseAttributeID(e.elements[1] as KSExpression.KSExpressionList)
        val rest = e.elements.subList(2, e.elements.size)
        val act_content = parseInlines(rest)

        return act_content flatMap { content ->
          KSResult.succeed<KSBlockParagraph<Unit>, KSParseError>(
            newBlockParagraph(content, e, Optional.of(id), Optional.empty()))
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.para_with_type)    -> {
        Assertive.require(e.elements.size >= 2)
        val type = parseAttributeType(
          e.elements[1] as KSExpression.KSExpressionList)
        val rest = e.elements.subList(2, e.elements.size)
        val act_content = parseInlines(rest)

        return act_content flatMap { content ->
          KSResult.succeed<KSBlockParagraph<Unit>, KSParseError>(
            newBlockParagraph(content, e, Optional.empty(), Optional.of(type)))
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.para_any)          -> {
        Assertive.require(e.elements.size >= 1)
        val rest = e.elements.subList(1, e.elements.size)
        val act_content = parseInlines(rest)

        return act_content flatMap { content ->
          KSResult.succeed<KSBlockParagraph<Unit>, KSParseError>(
            newBlockParagraph(content, e, Optional.empty(), Optional.empty()))
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
    content : List<KSInline<Unit>>,
    e : KSExpression.KSExpressionList,
    id : Optional<KSID<Unit>>,
    type : Optional<String>) : KSBlockParagraph<Unit> {
    return KSBlockParagraph(e.position, Unit, type, id, content)
  }

  private fun parseBlockSubsection(
    e : KSExpression.KSExpressionList)
    : KSResult<KSBlockSubsection<Unit>, KSParseError> {
    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpression.KSExpressionSymbol)

    when {
      KSExpressionMatch.matches(e, CommandMatchers.subsection_with_type_id) -> {
        Assertive.require(e.elements.size >= 4)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpression.KSExpressionList)
        val type = parseAttributeType(
          e.elements[2] as KSExpression.KSExpressionList)
        val id = parseAttributeID(
          e.elements[3] as KSExpression.KSExpressionList)
        val rest = e.elements.subList(4, e.elements.size)
        val act_content = parseSubsectionContents(rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSResult.succeed<KSBlockSubsection<Unit>, KSParseError>(
              KSBlockSubsection(
                e.position,
                Unit,
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
          e.elements[1] as KSExpression.KSExpressionList)
        val id = parseAttributeID(
          e.elements[2] as KSExpression.KSExpressionList)
        val type = parseAttributeType(
          e.elements[3] as KSExpression.KSExpressionList)
        val rest = e.elements.subList(4, e.elements.size)
        val act_content = parseSubsectionContents(rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSResult.succeed<KSBlockSubsection<Unit>, KSParseError>(
              KSBlockSubsection(
                e.position,
                Unit,
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
          e.elements[1] as KSExpression.KSExpressionList)
        val type = parseAttributeType(
          e.elements[2] as KSExpression.KSExpressionList)
        val rest = e.elements.subList(3, e.elements.size)
        val act_content = parseSubsectionContents(rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSResult.succeed<KSBlockSubsection<Unit>, KSParseError>(
              KSBlockSubsection(
                e.position,
                Unit,
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
          e.elements[1] as KSExpression.KSExpressionList)
        val id = parseAttributeID(
          e.elements[2] as KSExpression.KSExpressionList)
        val rest = e.elements.subList(3, e.elements.size)
        val act_content = parseSubsectionContents(rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSResult.succeed<KSBlockSubsection<Unit>, KSParseError>(
              KSBlockSubsection(
                e.position,
                Unit,
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
          e.elements[1] as KSExpression.KSExpressionList)
        val rest = e.elements.subList(2, e.elements.size)
        val act_content = parseSubsectionContents(rest)

        return act_title flatMap { title ->
          act_content flatMap { content ->
            KSResult.succeed<KSBlockSubsection<Unit>, KSParseError>(
              KSBlockSubsection(
                e.position,
                Unit,
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
    e : KSSectionContent<Unit>)
    : KSResult<KSSubsectionContent<Unit>, KSParseError> =
    when (e) {
      is KSSectionContent.KSSectionSubsectionContent ->
        KSResult.succeed<KSSubsectionContent<Unit>, KSParseError>(e.content)
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
    e : KSSectionContent<Unit>)
    : KSResult<KSBlockSubsection<Unit>, KSParseError> =
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
        KSResult.succeed<KSBlockSubsection<Unit>, KSParseError>(e.subsection)
      }
    }

  private fun parseBlockSection(
    e : KSExpression.KSExpressionList)
    : KSResult<KSBlockSection<Unit>, KSParseError> {
    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpression.KSExpressionSymbol)

    when {
      KSExpressionMatch.matches(e, CommandMatchers.section_with_type_id) -> {
        Assertive.require(e.elements.size >= 4)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpression.KSExpressionList)
        val type = parseAttributeType(
          e.elements[2] as KSExpression.KSExpressionList)
        val id = parseAttributeID(
          e.elements[3] as KSExpression.KSExpressionList)
        val rest = e.elements.subList(4, e.elements.size)
        val act_content = parseSectionContents(rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            parseBlockSectionActual(
              content, e, Optional.of(id), Optional.of(type), title)
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.section_with_id_type) -> {
        Assertive.require(e.elements.size >= 4)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpression.KSExpressionList)
        val id = parseAttributeID(
          e.elements[2] as KSExpression.KSExpressionList)
        val type = parseAttributeType(
          e.elements[3] as KSExpression.KSExpressionList)
        val rest = e.elements.subList(4, e.elements.size)
        val act_content = parseSectionContents(rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            parseBlockSectionActual(
              content, e, Optional.of(id), Optional.of(type), title)
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.section_with_type)    -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpression.KSExpressionList)
        val type = parseAttributeType(
          e.elements[2] as KSExpression.KSExpressionList)
        val rest = e.elements.subList(3, e.elements.size)
        val act_content = parseSectionContents(rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            parseBlockSectionActual(
              content, e, Optional.empty(), Optional.of(type), title)
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.section_with_id)      -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpression.KSExpressionList)
        val id = parseAttributeID(
          e.elements[2] as KSExpression.KSExpressionList)
        val rest = e.elements.subList(3, e.elements.size)
        val act_content = parseSectionContents(rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            parseBlockSectionActual(
              content, e, Optional.of(id), Optional.empty(), title)
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.section_none)         -> {
        Assertive.require(e.elements.size >= 2)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpression.KSExpressionList)
        val rest = e.elements.subList(2, e.elements.size)
        val act_content = parseSectionContents(rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            parseBlockSectionActual(
              content, e, Optional.empty(), Optional.empty(), title)
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
    content : List<KSSectionContent<Unit>>,
    e : KSExpression.KSExpressionList,
    id : Optional<KSID<Unit>>,
    type : Optional<String>,
    title : List<KSInlineText<Unit>>)
    : KSResult<KSBlockSection<Unit>, KSParseError> {
    return if (content.isEmpty()) {
      parseError(e, "Sections cannot be empty")
    } else {
      when (content[0]) {
        is KSSectionSubsectionContent -> {
          val act_contents =
            KSResult.listMap({ c -> sectionContentToSubsectionContent(c) }, content)
          act_contents flatMap { contents ->
            KSResult.succeed<KSBlockSection<Unit>, KSParseError>(
              KSBlockSection.KSBlockSectionWithContent(
                e.position, Unit, type, id, title, contents))
          }
        }
        is KSSectionSubsection        -> {
          val act_contents =
            KSResult.listMap({ c -> sectionContentToSubsection(c) }, content)
          act_contents flatMap { contents ->
            KSResult.succeed<KSBlockSection<Unit>, KSParseError>(
              KSBlockSection.KSBlockSectionWithSubsections(
                e.position, Unit, type, id, title, contents))
          }
        }
      }
    }
  }

  private fun anyToSubsectionContent(
    e : KSBlock<Unit>) : KSResult<KSSubsectionContent<Unit>, KSParseError> {
    return when (e) {
      is KSBlock.KSBlockDocument,
      is KSBlockSection,
      is KSBlockSubsection,
      is KSBlockPart      -> {
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
      is KSBlockParagraph ->
        KSResult.succeed(KSSubsectionParagraph(e))
    }
  }

  private fun anyToSectionContent(
    e : KSBlock<Unit>) : KSResult<KSSectionContent<Unit>, KSParseError> {
    return when (e) {
      is KSBlock.KSBlockDocument,
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
    }
  }

  private fun parseSubsectionContent(
    e : KSExpression) : KSResult<KSSubsectionContent<Unit>, KSParseError> {
    return parseBlockAny(e) flatMap { k -> anyToSubsectionContent(k) }
  }

  private fun parseSectionContent(
    e : KSExpression) : KSResult<KSSectionContent<Unit>, KSParseError> {
    return parseBlockAny(e) flatMap { k -> anyToSectionContent(k) }
  }

  private fun parseBlockPart(
    e : KSExpression.KSExpressionList)
    : KSResult<KSBlockPart<Unit>, KSParseError> {
    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpression.KSExpressionSymbol)

    when {
      KSExpressionMatch.matches(e, CommandMatchers.part_with_type_id) -> {
        Assertive.require(e.elements.size >= 4)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpression.KSExpressionList)
        val type = parseAttributeType(
          e.elements[2] as KSExpression.KSExpressionList)
        val id = parseAttributeID(
          e.elements[3] as KSExpression.KSExpressionList)
        val rest = e.elements.subList(4, e.elements.size)
        val act_content =
          KSResult.listMap({ s -> parseSectionAny(s) }, rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSResult.succeed<KSBlockPart<Unit>, KSParseError>(KSBlockPart(
              e.position, Unit, Optional.of(type), Optional.of(id), title, content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.part_with_id_type) -> {
        Assertive.require(e.elements.size >= 4)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpression.KSExpressionList)
        val id = parseAttributeID(
          e.elements[2] as KSExpression.KSExpressionList)
        val type = parseAttributeType(
          e.elements[3] as KSExpression.KSExpressionList)
        val rest = e.elements.subList(4, e.elements.size)
        val act_content =
          KSResult.listMap({ s -> parseSectionAny(s) }, rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSResult.succeed<KSBlockPart<Unit>, KSParseError>(KSBlockPart(
              e.position, Unit, Optional.of(type), Optional.of(id), title, content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.part_with_type)    -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpression.KSExpressionList)
        val type = parseAttributeType(
          e.elements[2] as KSExpression.KSExpressionList)
        val rest = e.elements.subList(3, e.elements.size)
        val act_content =
          KSResult.listMap({ s -> parseSectionAny(s) }, rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSResult.succeed<KSBlockPart<Unit>, KSParseError>(KSBlockPart(
              e.position, Unit, Optional.of(type), Optional.empty(), title, content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.part_with_id)      -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpression.KSExpressionList)
        val id = parseAttributeID(
          e.elements[2] as KSExpression.KSExpressionList)
        val rest = e.elements.subList(3, e.elements.size)
        val act_content =
          KSResult.listMap({ s -> parseSectionAny(s) }, rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSResult.succeed<KSBlockPart<Unit>, KSParseError>(KSBlockPart(
              e.position, Unit, Optional.empty(), Optional.of(id), title, content))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.part_none)         -> {
        Assertive.require(e.elements.size >= 2)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpression.KSExpressionList)
        val rest = e.elements.subList(2, e.elements.size)
        val act_content =
          KSResult.listMap({ s -> parseSectionAny(s) }, rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            KSResult.succeed<KSBlockPart<Unit>, KSParseError>(KSBlockPart(
              e.position, Unit, Optional.empty(), Optional.empty(), title, content))
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
    e : KSExpression) : KSResult<KSBlockSection<Unit>, KSParseError> {
    return parseBlockAny(e) flatMap { ee ->
      when (ee) {
        is KSBlock.KSBlockSection   ->
          KSResult.succeed<KSBlockSection<Unit>, KSParseError>(ee)
        is KSBlock.KSBlockDocument,
        is KSBlock.KSBlockPart,
        is KSBlock.KSBlockSubsection,
        is KSBlock.KSBlockParagraph -> {
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
    }
  }

  private fun parseInlineTexts(
    e : List<KSExpression>) : KSResult<List<KSInlineText<Unit>>, KSParseError> {
    return KSResult.listMap({ k ->
      inlines.parse(k) flatMap { r ->
        when (r) {
          is KSInlineText  ->
            KSResult.succeed<KSInlineText<Unit>, KSParseError>(r)
          is KSInlineLink,
          is KSInlineVerbatim,
          is KSInlineTerm,
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
    }, e)
  }

  private fun parseInlines(
    e : List<KSExpression>) : KSResult<List<KSInline<Unit>>, KSParseError> {
    return KSResult.listMap({ k ->
      val r = this.inlines.parse(k)
      r flatMap { z -> KSResult.succeed<KSInline<Unit>, KSParseError>(z) }
    }, e)
  }

  private fun parseSubsectionContents(
    e : List<KSExpression>)
    : KSResult<List<KSSubsectionContent<Unit>>, KSParseError> {
    return KSResult.listMap({ k -> parseSubsectionContent(k) }, e)
  }

  private fun parseSectionContents(
    e : List<KSExpression>)
    : KSResult<List<KSSectionContent<Unit>>, KSParseError> {
    return KSResult.listMap({ k -> parseSectionContent(k) }, e)
  }

  private fun anyToDocumentContent(
    e : KSBlock<Unit>) : KSResult<KSDocumentContent<Unit>, KSParseError> {
    return when (e) {
      is KSBlock.KSBlockDocument,
      is KSBlockSubsection,
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
    }
  }

  private fun parseDocumentContent(
    e : KSExpression) : KSResult<KSDocumentContent<Unit>, KSParseError> {
    return parseBlockAny(e) flatMap { k -> anyToDocumentContent(k) }
  }

  private fun parseDocumentContents(
    e : List<KSExpression>)
    : KSResult<List<KSDocumentContent<Unit>>, KSParseError> {
    return KSResult.listMap({ k -> parseDocumentContent(k) }, e)
  }

  private fun parseBlockDocument(
    e : KSExpression.KSExpressionList)
    : KSResult<KSBlockDocument<Unit>, KSParseError> {
    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpression.KSExpressionSymbol)

    when {
      KSExpressionMatch.matches(e, CommandMatchers.document_with_type_id) -> {
        Assertive.require(e.elements.size >= 4)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpression.KSExpressionList)
        val type = parseAttributeType(
          e.elements[2] as KSExpression.KSExpressionList)
        val id = parseAttributeID(
          e.elements[3] as KSExpression.KSExpressionList)
        val rest = e.elements.subList(4, e.elements.size)
        val act_content = parseDocumentContents(rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            parseBlockDocumentActual(
              content, e, Optional.of(id), Optional.of(type), title)
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.document_with_id_type) -> {
        Assertive.require(e.elements.size >= 4)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpression.KSExpressionList)
        val id = parseAttributeID(
          e.elements[2] as KSExpression.KSExpressionList)
        val type = parseAttributeType(
          e.elements[3] as KSExpression.KSExpressionList)
        val rest = e.elements.subList(4, e.elements.size)
        val act_content = parseDocumentContents(rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            parseBlockDocumentActual(
              content, e, Optional.of(id), Optional.of(type), title)
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.document_with_type)    -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpression.KSExpressionList)
        val type = parseAttributeType(
          e.elements[2] as KSExpression.KSExpressionList)
        val rest = e.elements.subList(3, e.elements.size)
        val act_content = parseDocumentContents(rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            parseBlockDocumentActual(
              content, e, Optional.empty(), Optional.of(type), title)
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.document_with_id)      -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpression.KSExpressionList)
        val id = parseAttributeID(
          e.elements[2] as KSExpression.KSExpressionList)
        val rest = e.elements.subList(3, e.elements.size)
        val act_content = parseDocumentContents(rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            parseBlockDocumentActual(
              content, e, Optional.of(id), Optional.empty(), title)
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.document_none)         -> {
        Assertive.require(e.elements.size >= 2)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpression.KSExpressionList)
        val rest = e.elements.subList(2, e.elements.size)
        val act_content = parseDocumentContents(rest)

        return act_content flatMap { content ->
          act_title flatMap { title ->
            parseBlockDocumentActual(
              content, e, Optional.empty(), Optional.empty(), title)
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
    content : List<KSDocumentContent<Unit>>,
    e : KSExpression.KSExpressionList,
    id : Optional<KSID<Unit>>,
    type : Optional<String>,
    title : List<KSInlineText<Unit>>)
    : KSResult<KSBlockDocument<Unit>, KSParseError> {
    return if (content.isEmpty()) {
      parseError(e, "Documents cannot be empty")
    } else {
      when (content[0]) {
        is KSDocumentPart    -> {
          val act_contents =
            KSResult.listMap({ c -> documentContentToPart(c) }, content)
          act_contents flatMap { contents ->
            KSResult.succeed<KSBlockDocument<Unit>, KSParseError>(
              KSBlockDocumentWithParts(
                e.position, Unit, id, type, title, contents))
          }
        }
        is KSDocumentSection -> {
          val act_contents =
            KSResult.listMap({ c -> documentContentToSection(c) }, content)
          act_contents flatMap { contents ->
            KSResult.succeed<KSBlockDocument<Unit>, KSParseError>(
              KSBlockDocumentWithSections(
                e.position, Unit, id, type, title, contents))
          }
        }
      }
    }
  }

  private fun documentContentToSection(
    e : KSDocumentContent<Unit>) : KSResult<KSBlockSection<Unit>, KSParseError> =
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
    e : KSDocumentContent<Unit>) : KSResult<KSBlockPart<Unit>, KSParseError> =
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
    m.put("paragraph", ElementParser("paragraph", { e -> parseBlockPara(e) }))
    m.put("subsection", ElementParser("subsection", { e -> parseBlockSubsection(e) }))
    m.put("section", ElementParser("section", { e -> parseBlockSection(e) }))
    m.put("part", ElementParser("part", { e -> parseBlockPart(e) }))
    m.put("document", ElementParser("document", { e -> parseBlockDocument(e) }))
    return m
  }

  private data class ElementParser(
    val name : String,
    val parser : (KSExpression.KSExpressionList) -> KSResult<out KSBlock<Unit>, KSParseError>)

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

  private fun elementName(e : KSExpression.KSExpressionList) : String {
    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpression.KSExpressionSymbol)
    return (e.elements[0] as KSExpression.KSExpressionSymbol).text
  }

  private val isElement =
    KSExpressionMatch.prefixOfList(
      listOf(KSExpressionMatch.MatchSymbol(
        { s -> parsers.containsKey(s) },
        parserDescriptions)))

  private fun parseBlockAny(
    e : KSExpression) : KSResult<KSBlock<Unit>, KSParseError> {
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

    val el = e as KSExpression.KSExpressionList
    val name = elementName(el)
    Assertive.require(parsers.containsKey(name))
    val ic = parsers.get(name)!!
    Assertive.require(ic.name == name)
    return ic.parser.invoke(el)
  }

  override fun parse(
    e : KSExpression) : KSResult<KSBlock<Unit>, KSParseError> {
    return parseBlockAny(e)
  }
}
