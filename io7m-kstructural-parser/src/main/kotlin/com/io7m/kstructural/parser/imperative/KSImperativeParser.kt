package com.io7m.kstructural.parser.imperative

import com.io7m.kstructural.core.KSElement.KSBlock
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
import com.io7m.kstructural.core.KSParserConstructorType
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.core.KSType
import com.io7m.kstructural.parser.KSExpression
import com.io7m.kstructural.parser.KSExpression.KSExpressionList
import com.io7m.kstructural.parser.KSExpression.KSExpressionSymbol
import com.io7m.kstructural.parser.KSExpressionMatch
import com.io7m.kstructural.parser.canon.KSCanonInlineParserType
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.KSImperativeDocument
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.KSImperativeFootnote
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.KSImperativeFormalItem
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.KSImperativeImport
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.KSImperativeParagraph
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.KSImperativePart
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.KSImperativeSection
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.KSImperativeSubsection
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeInline
import org.slf4j.LoggerFactory
import org.valid4j.Assertive
import java.nio.file.Path
import java.util.HashMap
import java.util.Optional

class KSImperativeParser private constructor(
  private val inlines : KSCanonInlineParserType,
  private val importers : KSParserConstructorType)
: KSImperativeParserType {

  companion object {

    private val LOG = LoggerFactory.getLogger(KSImperativeParser::class.java)

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

    private fun parseAttributeType(
      e : KSExpressionList,
      c : Context)
      : KSResult<KSType<KSParse>, KSParseError> {
      Assertive.require(e.elements.size == 2)
      Assertive.require(e.elements[0] is KSExpressionSymbol)
      Assertive.require(e.elements[1] is KSExpressionSymbol)
      val text = (e.elements[1] as KSExpressionSymbol).value
      val kp = KSParse(c.context)
      val position = e.position
      return if (KSType.isValidType(text)) {
        KSResult.succeed(KSType.create(position, text, kp))
      } else {
        KSResult.fail(KSParseError(position, "Not a valid type identifier"))
      }
    }

    private fun parseAttributeID(
      e : KSExpressionList,
      c : Context)
      : KSResult<KSID<KSParse>, KSParseError> {
      Assertive.require(e.elements.size == 2)
      Assertive.require(e.elements[0] is KSExpressionSymbol)
      Assertive.require(e.elements[1] is KSExpressionSymbol)
      val text = (e.elements[1] as KSExpressionSymbol).value
      val kp = KSParse(c.context)
      return if (KSID.isValidID(text)) {
        KSResult.succeed(KSID.create(e.position, text, kp))
      } else {
        KSResult.fail(KSParseError(e.position, "Not a valid identifier"))
      }
    }

    fun create(
      inlines : KSCanonInlineParserType,
      importers : KSParserConstructorType) : KSImperativeParserType =
      KSImperativeParser(inlines, importers)
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
      KSExpressionMatch.allOfList(listOf(para_name))
    val para_with_id =
      KSExpressionMatch.allOfList(listOf(para_name, id))
    val para_with_id_type =
      KSExpressionMatch.allOfList(listOf(para_name, id, type))
    val para_with_type_id =
      KSExpressionMatch.allOfList(listOf(para_name, type, id))
    val para_with_type =
      KSExpressionMatch.allOfList(listOf(para_name, type))

    val title_name =
      KSExpressionMatch.exactSymbol("title")
    val title =
      KSExpressionMatch.prefixOfList(listOf(title_name, symbol_or_string))

    val formal_item_name =
      KSExpressionMatch.exactSymbol("formal-item")
    val formal_item_none =
      KSExpressionMatch.allOfList(listOf(formal_item_name, title))
    val formal_item_with_id =
      KSExpressionMatch.allOfList(listOf(formal_item_name, title, id))
    val formal_item_with_id_type =
      KSExpressionMatch.allOfList(listOf(formal_item_name, title, id, type))
    val formal_item_with_type_id =
      KSExpressionMatch.allOfList(listOf(formal_item_name, title, type, id))
    val formal_item_with_type =
      KSExpressionMatch.allOfList(listOf(formal_item_name, title, type))

    val subsection_name =
      KSExpressionMatch.exactSymbol("subsection")
    val subsection_none =
      KSExpressionMatch.allOfList(listOf(subsection_name, title))
    val subsection_with_id =
      KSExpressionMatch.allOfList(listOf(subsection_name, title, id))
    val subsection_with_id_type =
      KSExpressionMatch.allOfList(listOf(subsection_name, title, id, type))
    val subsection_with_type_id =
      KSExpressionMatch.allOfList(listOf(subsection_name, title, type, id))
    val subsection_with_type =
      KSExpressionMatch.allOfList(listOf(subsection_name, title, type))

    val section_name =
      KSExpressionMatch.exactSymbol("section")
    val section_none =
      KSExpressionMatch.allOfList(listOf(section_name, title))
    val section_with_id =
      KSExpressionMatch.allOfList(listOf(section_name, title, id))
    val section_with_id_type =
      KSExpressionMatch.allOfList(listOf(section_name, title, id, type))
    val section_with_type_id =
      KSExpressionMatch.allOfList(listOf(section_name, title, type, id))
    val section_with_type =
      KSExpressionMatch.allOfList(listOf(section_name, title, type))

    val part_name =
      KSExpressionMatch.exactSymbol("part")
    val part_none =
      KSExpressionMatch.allOfList(listOf(part_name, title))
    val part_with_id =
      KSExpressionMatch.allOfList(listOf(part_name, title, id))
    val part_with_id_type =
      KSExpressionMatch.allOfList(listOf(part_name, title, id, type))
    val part_with_type_id =
      KSExpressionMatch.allOfList(listOf(part_name, title, type, id))
    val part_with_type =
      KSExpressionMatch.allOfList(listOf(part_name, title, type))

    val document_name =
      KSExpressionMatch.exactSymbol("document")
    val document_none =
      KSExpressionMatch.allOfList(listOf(document_name, title))
    val document_with_id =
      KSExpressionMatch.allOfList(listOf(document_name, title, id))
    val document_with_id_type =
      KSExpressionMatch.allOfList(listOf(document_name, title, id, type))
    val document_with_type_id =
      KSExpressionMatch.allOfList(listOf(document_name, title, type, id))
    val document_with_type =
      KSExpressionMatch.allOfList(listOf(document_name, title, type))
  }

  private data class Context(
    val context : KSParseContextType,
    val file : Path)

  private data class ElementParser(
    val name : String,
    val parser : (KSExpressionList, Context) -> KSResult<KSImperative, KSParseError>)

  private val parsers : Map<String, ElementParser> =
    makeParsers()
  private val parserDescriptions : String =
    makeMapDescription(parsers)

  private fun makeParsers() : Map<String, ElementParser> {
    val m = HashMap<String, ElementParser>()
    m.put("document", ElementParser("document", {
      e, c ->
      parseDocument(e, c)
    }))
    m.put("footnote", ElementParser("footnote", {
      e, c ->
      parseFootnote(e, c)
    }))
    m.put("formal-item", ElementParser("formal-item", {
      e, c ->
      parseFormalItem(e, c)
    }))
    m.put("import", ElementParser("import", {
      e, c ->
      parseImport(e, c)
    }))
    m.put("paragraph", ElementParser("paragraph", {
      e, c ->
      parsePara(e, c)
    }))
    m.put("part", ElementParser("part", {
      e, c ->
      parsePart(e, c)
    }))
    m.put("section", ElementParser("section", {
      e, c ->
      parseSection(e, c)
    }))
    m.put("subsection", ElementParser("subsection", {
      e, c ->
      parseSubsection(e, c)
    }))
    return m
  }

  private fun parsePara(
    e : KSExpressionList,
    c : Context) : KSResult<KSImperative, KSParseError> {

    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpressionSymbol)

    when {
      KSExpressionMatch.matches(e, CommandMatchers.para_with_id_type) -> {
        Assertive.require(e.elements.size >= 3)
        val act_id = parseAttributeID(
          e.elements[1] as KSExpressionList, c)
        val act_type = parseAttributeType(
          e.elements[2] as KSExpressionList, c)

        return act_id.flatMap { id ->
          act_type.flatMap { type ->
            KSResult.succeed<KSImperative, KSParseError>(
              KSImperativeParagraph(
                e.position, e.square, Optional.of(type), Optional.of(id)))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.para_with_type_id) -> {
        Assertive.require(e.elements.size >= 3)
        val act_type = parseAttributeType(
          e.elements[1] as KSExpressionList, c)
        val act_id = parseAttributeID(
          e.elements[2] as KSExpressionList, c)

        return act_id.flatMap { id ->
          act_type.flatMap { type ->
            KSResult.succeed<KSImperative, KSParseError>(
              KSImperativeParagraph(
                e.position, e.square, Optional.of(type), Optional.of(id)))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.para_with_id)      -> {
        Assertive.require(e.elements.size >= 2)
        val act_id = parseAttributeID(
          e.elements[1] as KSExpressionList, c)

        return act_id.flatMap { id ->
          KSResult.succeed<KSImperative, KSParseError>(
            KSImperativeParagraph(
              e.position, e.square, Optional.empty(), Optional.of(id)))
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.para_with_type)    -> {
        Assertive.require(e.elements.size >= 2)
        val act_type = parseAttributeType(
          e.elements[1] as KSExpressionList, c)
        return act_type.flatMap { type ->
          KSResult.succeed<KSImperative, KSParseError>(
            KSImperativeParagraph(
              e.position, e.square, Optional.of(type), Optional.empty()))
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.para_any)          -> {
        Assertive.require(e.elements.size >= 1)
        return KSResult.succeed(
          KSImperativeParagraph(
            e.position, e.square, Optional.empty(), Optional.empty()))
      }
    }

    return failedToMatchResult(e, listOf(
      CommandMatchers.para_any,
      CommandMatchers.para_with_id,
      CommandMatchers.para_with_id_type,
      CommandMatchers.para_with_type_id,
      CommandMatchers.para_with_type))
  }

  private fun parseFootnote(
    e : KSExpressionList,
    c : Context) : KSResult<KSImperative, KSParseError> {

    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpressionSymbol)

    when {
      KSExpressionMatch.matches(e, CommandMatchers.footnote_type) -> {
        Assertive.require(e.elements.size >= 3)
        val act_id = parseAttributeID(
          e.elements[1] as KSExpressionList, c)
        val act_type = parseAttributeType(
          e.elements[2] as KSExpressionList, c)

        return act_id.flatMap { id ->
          act_type.flatMap { type ->
            KSResult.succeed<KSImperative, KSParseError>(
              KSImperativeFootnote(
                e.position, e.square, Optional.of(type), id))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.footnote)      -> {
        Assertive.require(e.elements.size >= 2)
        val act_id = parseAttributeID(
          e.elements[1] as KSExpressionList, c)

        return act_id.flatMap { id ->
          KSResult.succeed<KSImperative, KSParseError>(
            KSImperativeFootnote(
              e.position, e.square, Optional.empty(), id))
        }
      }
    }

    return failedToMatchResult(e, listOf(
      CommandMatchers.footnote,
      CommandMatchers.footnote_type))
  }

  private fun parseAttributeTitle(
    e : KSExpressionList,
    c : Context)
    : KSResult<List<KSInlineText<KSParse>>, KSParseError> {
    Assertive.require(e.elements.size >= 2)
    Assertive.require(e.elements[0] is KSExpressionSymbol)
    val texts = e.elements.subList(1, e.elements.size)
    return KSResult.listMap({ ic ->
      this.inlines.parse(c.context, ic, c.file).flatMap { x ->
        when (x) {
          is KSInlineText    ->
            KSResult.succeed(x)
          is KSInlineLink,
          is KSInlineVerbatim,
          is KSInlineTerm,
          is KSInlineFootnoteReference,
          is KSInlineImage,
          is KSInlineListOrdered,
          is KSInlineListUnordered,
          is KSInlineTable,
          is KSInlineInclude -> {
            val sb = StringBuilder()
            sb.append("Expected inline text.")
            sb.append(System.lineSeparator())
            sb.append("Expected: Inline text")
            sb.append(System.lineSeparator())
            sb.append("Received: ")
            sb.append(x)
            sb.append(System.lineSeparator())
            KSResult.fail<KSInlineText<KSParse>, KSParseError>(
              KSParseError(x.position, sb.toString()))
          }
        }
      }
    }, texts)
  }

  private fun parsePart(
    e : KSExpressionList,
    c : Context) : KSResult<KSImperative, KSParseError> {

    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpressionSymbol)

    when {
      KSExpressionMatch.matches(e, CommandMatchers.part_with_id_type) -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val act_id = parseAttributeID(
          e.elements[2] as KSExpressionList, c)
        val act_type = parseAttributeType(
          e.elements[3] as KSExpressionList, c)

        return act_id.flatMap { id ->
          act_title.flatMap { title ->
            act_type.flatMap { type ->
              KSResult.succeed<KSImperative, KSParseError>(
                KSImperativePart(
                  e.position,
                  e.square,
                  Optional.of(type),
                  Optional.of(id),
                  title))
            }
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.part_with_type_id) -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val act_type = parseAttributeType(
          e.elements[2] as KSExpressionList, c)
        val act_id = parseAttributeID(
          e.elements[3] as KSExpressionList, c)

        return act_id.flatMap { id ->
          act_title.flatMap { title ->
            act_type.flatMap { type ->
              KSResult.succeed<KSImperative, KSParseError>(
                KSImperativePart(
                  e.position,
                  e.square,
                  Optional.of(type),
                  Optional.of(id),
                  title))
            }
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.part_with_id)      -> {
        Assertive.require(e.elements.size >= 2)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val act_id = parseAttributeID(
          e.elements[2] as KSExpressionList, c)

        return act_id.flatMap { id ->
          act_title.flatMap { title ->
            KSResult.succeed<KSImperative, KSParseError>(
              KSImperativePart(
                e.position,
                e.square,
                Optional.empty(),
                Optional.of(id),
                title))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.part_with_type)    -> {
        Assertive.require(e.elements.size >= 2)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val act_type = parseAttributeType(
          e.elements[2] as KSExpressionList, c)

        return act_title.flatMap { title ->
          act_type.flatMap { type ->
            KSResult.succeed<KSImperative, KSParseError>(
              KSImperativePart(
                e.position,
                e.square,
                Optional.of(type),
                Optional.empty(),
                title))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.part_none)         -> {
        Assertive.require(e.elements.size >= 1)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)

        return act_title.flatMap { title ->
          KSResult.succeed<KSImperative, KSParseError>(
            KSImperativePart(
              e.position,
              e.square,
              Optional.empty(),
              Optional.empty(),
              title))
        }
      }
    }

    return failedToMatchResult(e, listOf(
      CommandMatchers.part_none,
      CommandMatchers.part_with_id,
      CommandMatchers.part_with_id_type,
      CommandMatchers.part_with_type_id,
      CommandMatchers.part_with_type))
  }

  private fun parseDocument(
    e : KSExpressionList,
    c : Context) : KSResult<KSImperative, KSParseError> {

    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpressionSymbol)

    when {
      KSExpressionMatch.matches(e, CommandMatchers.document_with_id_type) -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val act_id = parseAttributeID(
          e.elements[2] as KSExpressionList, c)
        val act_type = parseAttributeType(
          e.elements[3] as KSExpressionList, c)

        return act_id.flatMap { id ->
          act_title.flatMap { title ->
            act_type.flatMap { type ->
              KSResult.succeed<KSImperative, KSParseError>(
                KSImperativeDocument(
                  e.position,
                  e.square,
                  Optional.of(type),
                  Optional.of(id),
                  title))
            }
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.document_with_type_id) -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val act_type = parseAttributeType(
          e.elements[2] as KSExpressionList, c)
        val act_id = parseAttributeID(
          e.elements[3] as KSExpressionList, c)

        return act_id.flatMap { id ->
          act_title.flatMap { title ->
            act_type.flatMap { type ->
              KSResult.succeed<KSImperative, KSParseError>(
                KSImperativeDocument(
                  e.position,
                  e.square,
                  Optional.of(type),
                  Optional.of(id),
                  title))
            }
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.document_with_id)      -> {
        Assertive.require(e.elements.size >= 2)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val act_id = parseAttributeID(
          e.elements[2] as KSExpressionList, c)

        return act_id.flatMap { id ->
          act_title.flatMap { title ->
            KSResult.succeed<KSImperative, KSParseError>(
              KSImperativeDocument(
                e.position,
                e.square,
                Optional.empty(),
                Optional.of(id),
                title))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.document_with_type)    -> {
        Assertive.require(e.elements.size >= 2)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val act_type = parseAttributeType(
          e.elements[2] as KSExpressionList, c)

        return act_title.flatMap { title ->
          act_type.flatMap { type ->
            KSResult.succeed<KSImperative, KSParseError>(
              KSImperativeDocument(
                e.position,
                e.square,
                Optional.of(type),
                Optional.empty(),
                title))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.document_none)         -> {
        Assertive.require(e.elements.size >= 1)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)

        return act_title.flatMap { title ->
          KSResult.succeed<KSImperative, KSParseError>(
            KSImperativeDocument(
              e.position,
              e.square,
              Optional.empty(),
              Optional.empty(),
              title))
        }
      }
    }

    return failedToMatchResult(e, listOf(
      CommandMatchers.document_none,
      CommandMatchers.document_with_id,
      CommandMatchers.document_with_id_type,
      CommandMatchers.document_with_type_id,
      CommandMatchers.document_with_type))
  }

  private fun parseSection(
    e : KSExpressionList,
    c : Context) : KSResult<KSImperative, KSParseError> {

    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpressionSymbol)

    when {
      KSExpressionMatch.matches(e, CommandMatchers.section_with_id_type) -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val act_id = parseAttributeID(
          e.elements[2] as KSExpressionList, c)
        val act_type = parseAttributeType(
          e.elements[3] as KSExpressionList, c)

        return act_id.flatMap { id ->
          act_title.flatMap { title ->
            act_type.flatMap { type ->
              KSResult.succeed<KSImperative, KSParseError>(
                KSImperativeSection(
                  e.position,
                  e.square,
                  Optional.of(type),
                  Optional.of(id),
                  title))
            }
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.section_with_type_id) -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val act_type = parseAttributeType(
          e.elements[2] as KSExpressionList, c)
        val act_id = parseAttributeID(
          e.elements[3] as KSExpressionList, c)

        return act_id.flatMap { id ->
          act_title.flatMap { title ->
            act_type.flatMap { type ->
              KSResult.succeed<KSImperative, KSParseError>(
                KSImperativeSection(
                  e.position,
                  e.square,
                  Optional.of(type),
                  Optional.of(id),
                  title))
            }
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.section_with_id)      -> {
        Assertive.require(e.elements.size >= 2)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val act_id = parseAttributeID(
          e.elements[2] as KSExpressionList, c)

        return act_id.flatMap { id ->
          act_title.flatMap { title ->
            KSResult.succeed<KSImperative, KSParseError>(
              KSImperativeSection(
                e.position,
                e.square,
                Optional.empty(),
                Optional.of(id),
                title))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.section_with_type)    -> {
        Assertive.require(e.elements.size >= 2)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val act_type = parseAttributeType(
          e.elements[2] as KSExpressionList, c)

        return act_title.flatMap { title ->
          act_type.flatMap { type ->
            KSResult.succeed<KSImperative, KSParseError>(
              KSImperativeSection(
                e.position,
                e.square,
                Optional.of(type),
                Optional.empty(),
                title))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.section_none)         -> {
        Assertive.require(e.elements.size >= 1)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)

        return act_title.flatMap { title ->
          KSResult.succeed<KSImperative, KSParseError>(
            KSImperativeSection(
              e.position,
              e.square,
              Optional.empty(),
              Optional.empty(),
              title))
        }
      }
    }

    return failedToMatchResult(e, listOf(
      CommandMatchers.section_none,
      CommandMatchers.section_with_id,
      CommandMatchers.section_with_id_type,
      CommandMatchers.section_with_type_id,
      CommandMatchers.section_with_type))
  }

  private fun parseSubsection(
    e : KSExpressionList,
    c : Context) : KSResult<KSImperative, KSParseError> {

    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpressionSymbol)

    when {
      KSExpressionMatch.matches(e, CommandMatchers.subsection_with_id_type) -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val act_id = parseAttributeID(
          e.elements[2] as KSExpressionList, c)
        val act_type = parseAttributeType(
          e.elements[3] as KSExpressionList, c)

        return act_id.flatMap { id ->
          act_title.flatMap { title ->
            act_type.flatMap { type ->
              KSResult.succeed<KSImperative, KSParseError>(
                KSImperativeSubsection(
                  e.position,
                  e.square,
                  Optional.of(type),
                  Optional.of(id),
                  title))
            }
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.subsection_with_type_id) -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val act_type = parseAttributeType(
          e.elements[2] as KSExpressionList, c)
        val act_id = parseAttributeID(
          e.elements[3] as KSExpressionList, c)

        return act_id.flatMap { id ->
          act_title.flatMap { title ->
            act_type.flatMap { type ->
              KSResult.succeed<KSImperative, KSParseError>(
                KSImperativeSubsection(
                  e.position,
                  e.square,
                  Optional.of(type),
                  Optional.of(id),
                  title))
            }
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.subsection_with_id)      -> {
        Assertive.require(e.elements.size >= 2)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val act_id = parseAttributeID(
          e.elements[2] as KSExpressionList, c)

        return act_id.flatMap { id ->
          act_title.flatMap { title ->
            KSResult.succeed<KSImperative, KSParseError>(
              KSImperativeSubsection(
                e.position,
                e.square,
                Optional.empty(),
                Optional.of(id),
                title))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.subsection_with_type)    -> {
        Assertive.require(e.elements.size >= 2)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val act_type = parseAttributeType(
          e.elements[2] as KSExpressionList, c)

        return act_title.flatMap { title ->
          act_type.flatMap { type ->
            KSResult.succeed<KSImperative, KSParseError>(
              KSImperativeSubsection(
                e.position,
                e.square,
                Optional.of(type),
                Optional.empty(),
                title))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.subsection_none)         -> {
        Assertive.require(e.elements.size >= 1)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)

        return act_title.flatMap { title ->
          KSResult.succeed<KSImperative, KSParseError>(
            KSImperativeSubsection(
              e.position,
              e.square,
              Optional.empty(),
              Optional.empty(),
              title))
        }
      }
    }

    return failedToMatchResult(e, listOf(
      CommandMatchers.subsection_none,
      CommandMatchers.subsection_with_id,
      CommandMatchers.subsection_with_id_type,
      CommandMatchers.subsection_with_type_id,
      CommandMatchers.subsection_with_type))
  }

  private fun parseFormalItem(
    e : KSExpressionList,
    c : Context) : KSResult<KSImperative, KSParseError> {

    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpressionSymbol)

    when {
      KSExpressionMatch.matches(e, CommandMatchers.formal_item_with_id_type) -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val act_id = parseAttributeID(
          e.elements[2] as KSExpressionList, c)
        val act_type = parseAttributeType(
          e.elements[3] as KSExpressionList, c)

        return act_id.flatMap { id ->
          act_title.flatMap { title ->
            act_type.flatMap { type ->
              KSResult.succeed<KSImperative, KSParseError>(
                KSImperativeFormalItem(
                  e.position,
                  e.square,
                  Optional.of(type),
                  Optional.of(id),
                  title))
            }
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.formal_item_with_type_id) -> {
        Assertive.require(e.elements.size >= 3)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val act_type = parseAttributeType(
          e.elements[2] as KSExpressionList, c)
        val act_id = parseAttributeID(
          e.elements[3] as KSExpressionList, c)

        return act_id.flatMap { id ->
          act_title.flatMap { title ->
            act_type.flatMap { type ->
              KSResult.succeed<KSImperative, KSParseError>(
                KSImperativeFormalItem(
                  e.position,
                  e.square,
                  Optional.of(type),
                  Optional.of(id),
                  title))
            }
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.formal_item_with_id)      -> {
        Assertive.require(e.elements.size >= 2)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val act_id = parseAttributeID(
          e.elements[2] as KSExpressionList, c)

        return act_id.flatMap { id ->
          act_title.flatMap { title ->
            KSResult.succeed<KSImperative, KSParseError>(
              KSImperativeFormalItem(
                e.position,
                e.square,
                Optional.empty(),
                Optional.of(id),
                title))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.formal_item_with_type)    -> {
        Assertive.require(e.elements.size >= 2)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)
        val act_type = parseAttributeType(
          e.elements[2] as KSExpressionList, c)

        return act_title.flatMap { title ->
          act_type.flatMap { type ->
            KSResult.succeed<KSImperative, KSParseError>(
              KSImperativeFormalItem(
                e.position,
                e.square,
                Optional.of(type),
                Optional.empty(),
                title))
          }
        }
      }

      KSExpressionMatch.matches(e, CommandMatchers.formal_item_none)         -> {
        Assertive.require(e.elements.size >= 1)
        val act_title = parseAttributeTitle(
          e.elements[1] as KSExpressionList, c)

        return act_title.flatMap { title ->
          KSResult.succeed<KSImperative, KSParseError>(
            KSImperativeFormalItem(
              e.position,
              e.square,
              Optional.empty(),
              Optional.empty(),
              title))
        }
      }
    }

    return failedToMatchResult(e, listOf(
      CommandMatchers.formal_item_none,
      CommandMatchers.formal_item_with_id,
      CommandMatchers.formal_item_with_id_type,
      CommandMatchers.formal_item_with_type_id,
      CommandMatchers.formal_item_with_type))
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

  private fun loadImport(
    e : KSExpressionList,
    c : Context,
    f : KSInlineText<KSParse>)
    : KSResult<KSImperativeImport, KSParseError> {

    val base_abs = c.file.toAbsolutePath()
    val real = base_abs.resolveSibling(f.text)

    val import_e = KSBlock.KSBlockImport(
      e.position,
      e.square,
      KSParse(c.context),
      Optional.empty(),
      Optional.empty(),
      f)

    return c.context.checkImportCycle(
      importer = base_abs,
      import = import_e,
      imported_path = real).flatMap {

      val r : KSResult<KSBlock<KSParse>, KSParseError> =
        try {
          LOG.debug("import: {}", real)
          val importer = this.importers.create(c.context, real)
          importer.parseBlock(c.context, real)
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

      r.flatMap { b ->
        c.context.addImport(
          importer = base_abs,
          import = import_e,
          imported_path = real,
          imported = b)

        val re = KSImperativeImport(e.position, e.square, import_e, b)
        KSResult.succeed<KSImperativeImport, KSParseError>(re)
      }
    }
  }

  private fun parseInlineText(
    c : Context,
    e : KSExpression)
    : KSResult<KSInlineText<KSParse>, KSParseError> {
    return inlines.parse(c.context, e, c.file).flatMap { r ->
      checkInlineText(e, r)
    }
  }

  private fun parseImport(
    e : KSExpressionList,
    c : Context)
    : KSResult<KSImperativeImport, KSParseError> {

    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpressionSymbol)

    when {
      KSExpressionMatch.matches(e, CommandMatchers.import) -> {
        Assertive.require(e.elements.size == 2)
        return parseInlineText(c, e.elements[1]).flatMap { file ->
          loadImport(e, c, file)
        }
      }
    }

    return failedToMatchResult(e, listOf(CommandMatchers.import))
  }


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

  private val isElement =
    KSExpressionMatch.prefixOfList(
      listOf(KSExpressionMatch.MatchSymbol(
        { s -> parsers.containsKey(s) },
        parserDescriptions)))

  private fun elementName(e : KSExpressionList) : String {
    Assertive.require(e.elements.size > 0)
    Assertive.require(e.elements[0] is KSExpressionSymbol)
    return (e.elements[0] as KSExpressionSymbol).value
  }

  override fun parse(
    context : KSParseContextType,
    expression : KSExpression,
    file : Path) : KSResult<KSImperative, KSParseError> {

    return when (expression) {
      is KSExpression.KSExpressionQuoted,
      is KSExpression.KSExpressionSymbol ->
        inlines.parse(context, expression, file).flatMap { i ->
          KSResult.succeed<KSImperative, KSParseError>(KSImperativeInline(i))
        }
      is KSExpression.KSExpressionList   -> {
        if (!KSExpressionMatch.matches(expression, isElement)) {
          if (inlines.maybe(expression)) {
            return inlines.parse(context, expression, file).flatMap { i ->
              KSResult.succeed<KSImperative, KSParseError>(KSImperativeInline(i))
            }
          }

          val sb = StringBuilder()
          sb.append("Expected a command or inline content.")
          sb.append(System.lineSeparator())
          sb.append("  Expected: ")
          sb.append(isElement)
          sb.append(System.lineSeparator())
          sb.append("  Received: ")
          sb.append(expression)
          sb.append(System.lineSeparator())
          return parseError(expression, sb.toString())
        }

        val name = elementName(expression)
        Assertive.require(parsers.containsKey(name))
        val ic = parsers.get(name)!!
        Assertive.require(ic.name == name)
        return ic.parser.invoke(expression, Context(context, file))
      }
    }
  }
}
