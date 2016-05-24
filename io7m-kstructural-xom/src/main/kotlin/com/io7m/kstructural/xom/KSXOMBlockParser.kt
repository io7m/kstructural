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

import com.io7m.jlexing.core.LexicalPositionType
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
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineText
import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSParseContextType
import com.io7m.kstructural.core.KSParseError
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.core.KSSubsectionContent
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionFootnote
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionFormalItem
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionParagraph
import com.io7m.kstructural.core.KSType
import com.io7m.kstructural.schema.KSSchemaNamespaces
import nu.xom.Element
import nu.xom.Node
import org.slf4j.LoggerFactory
import org.valid4j.Assertive
import java.nio.file.Path
import java.util.Optional

class KSXOMBlockParser private constructor(
  private val inlines : KSXOMInlineParserType) : KSXOMBlockParserType {

  override fun parse(
    context : KSParseContextType,
    element : Element) : KSResult<KSBlock<KSParse>, KSParseError> {

    return when (element.localName) {
      "paragraph"   -> parseParagraph(context, element)
      "formal-item" -> parseFormalItem(context, element)
      "footnote"    -> parseFootnote(context, element)
      "section"     -> parseSection(context, element)
      "subsection"  -> parseSubsection(context, element)
      "part"        -> parsePart(context, element)
      "document"    -> parseDocument(context, element)

      else          -> {
        fail(KSParseError(no_lex, "Unrecognized element: " + element.localName))
      }
    }
  }

  private fun listOfChildren(element : Element) : List<Node> {
    val xs = mutableListOf<Node>()
    for (i in 0 .. element.childCount - 1) {
      xs.add(element.getChild(i))
    }
    return xs
  }

  private fun listOfChildElements(element : Element) : List<Element> {
    val xs = mutableListOf<Element>()
    for (i in 0 .. element.childCount - 1) {
      val ec = element.getChild(i)
      if (ec is Element) xs.add(ec)
    }
    return xs
  }

  private fun parseType(
    context : KSParseContextType,
    element : Element)
    : KSResult<Optional<KSType<KSParse>>, KSParseError> {
    val ta = element.getAttribute("type", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    return if (ta != null) {
      if (KSType.isValidType(ta.value)) {
        KSResult.succeed(Optional.of(
          KSType.create(no_lex, ta.value, KSParse(context))))
      } else {
        KSResult.fail(KSParseError(no_lex, "Not a valid identifier"))
      }
    } else {
      KSResult.succeed(Optional.empty())
    }
  }

  private fun parseID(
    context : KSParseContextType,
    element : Element)
    : KSResult<Optional<KSID<KSParse>>, KSParseError> {
    val ta = element.getAttribute("id", KSSchemaNamespaces.XML_NAMESPACE_URI_TEXT)
    return if (ta != null) {
      val v = ta.value
      if (KSID.isValidID(v)) {
        KSResult.succeed<Optional<KSID<KSParse>>, KSParseError>(
          Optional.of(KSID.create(no_lex, ta.value, KSParse(context))))
      } else {
        KSResult.fail(KSParseError(no_lex, "Not a valid identifier"))
      }
    } else {
      KSResult.succeed(Optional.empty())
    }
  }

  private fun parseIDNonOptional(
    context : KSParseContextType,
    element : Element)
    : KSResult<KSID<KSParse>, KSParseError> {
    val ta = element.getAttribute("id", KSSchemaNamespaces.XML_NAMESPACE_URI_TEXT)
    val v = ta.value
    return if (KSID.isValidID(v)) {
      KSResult.succeed<KSID<KSParse>, KSParseError>(
        KSID.create(no_lex, ta.value, KSParse(context)))
    } else {
      KSResult.fail(KSParseError(no_lex, "Not a valid identifier"))
    }
  }

  private fun parseDocument(
    context : KSParseContextType,
    element : Element) : KSResult<KSBlockDocument<KSParse>, KSParseError> {
    Assertive.require(element.localName == "document")

    val kp = KSParse(context)
    val act_id = parseID(context, element)
    val act_type = parseType(context, element)
    val title = parseTitle(context, element)

    val act_content =
      KSResult.listMap({ c -> parse(context, c) },
        listOfChildElements(element))

    return act_id.flatMap { id ->
      act_content.flatMap { content ->
        act_type.flatMap { type ->
          if (content.size > 0) {
            if (content[0] is KSBlockPart) {
              KSResult.listMap({ s -> toPart(s) }, content).flatMap {
                parts ->
                succeed(KSBlockDocument.KSBlockDocumentWithParts(
                  no_lex, false, kp, id, type, title, parts))
              }
            } else {
              KSResult.listMap({ s -> toSection(s) }, content).flatMap {
                sections ->
                succeed(KSBlockDocumentWithSections(
                  no_lex, false, kp, id, type, title, sections))
              }
            }
          } else {
            succeed(KSBlockDocumentWithParts(
              no_lex, false, kp, id, type, title, listOf()))
          }
        }
      }
    }
  }

  private fun toPart(
    s : KSBlock<KSParse>) : KSResult<KSBlockPart<KSParse>, KSParseError> {
    return when (s) {
      is KSBlockPart   -> KSResult.succeed(s)
      is KSBlockParagraph,
      is KSBlockFormalItem,
      is KSBlockFootnote,
      is KSBlockSection,
      is KSBlockDocument,
      is KSBlockSubsection,
      is KSBlockImport -> {
        val sb = StringBuilder()
        sb.append("Unexpected element.")
        sb.append(System.lineSeparator())
        sb.append("  Expected: A part")
        sb.append(System.lineSeparator())
        sb.append("  Received: ")
        sb.append(s)
        sb.append(System.lineSeparator())
        KSResult.fail(KSParseError(no_lex, sb.toString()))
      }
    }
  }

  private fun parseSubsection(
    context : KSParseContextType,
    element : Element) : KSResult<KSBlockSubsection<KSParse>, KSParseError> {
    Assertive.require(element.localName == "subsection")

    val kp = KSParse(context)
    val act_id = parseID(context, element)
    val act_type = parseType(context, element)
    val title = parseTitle(context, element)

    val act_content =
      KSResult.listMap({ c -> parse(context, c) },
        listOfChildElements(element))

    return act_id.flatMap { id ->
      act_content.flatMap { content ->
        act_type.flatMap { type ->
          KSResult.listMap({ s -> toSubsectionContent(s) }, content).flatMap { content ->
            succeed(KSBlockSubsection(no_lex, false, kp, type, id, title, content))
          }
        }
      }
    }
  }

  private fun parseSection(
    context : KSParseContextType,
    element : Element) : KSResult<KSBlockSection<KSParse>, KSParseError> {
    Assertive.require(element.localName == "section")

    val kp = KSParse(context)
    val act_id = parseID(context, element)
    val act_type = parseType(context, element)
    val title = parseTitle(context, element)

    val act_content =
      KSResult.listMap({ c -> parse(context, c) },
        listOfChildElements(element))

    return act_id.flatMap { id ->
      act_content.flatMap { content ->
        act_type.flatMap { type ->
          if (content.size > 0) {
            if (content[0] is KSBlockSubsection) {
              KSResult.listMap({ s -> checkSubsection(s) }, content).flatMap {
                subsections ->
                succeed(KSBlockSection.KSBlockSectionWithSubsections(
                  no_lex, false, kp, type, id, title, subsections))
              }
            } else {
              KSResult.listMap({ s -> toSubsectionContent(s) }, content).flatMap {
                content ->
                succeed(KSBlockSection.KSBlockSectionWithContent(
                  no_lex, false, kp, type, id, title, content))
              }
            }
          } else {
            succeed(KSBlockSection.KSBlockSectionWithSubsections(
              no_lex, false, kp, type, id, title, listOf()))
          }
        }
      }
    }
  }

  private fun parseTitle(
    context : KSParseContextType,
    element : Element) : List<KSInlineText<KSParse>> {
    val tt = element.getAttribute("title", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    return listOf(KSInlineText(no_lex, false, KSParse(context), false, tt.value))
  }

  private fun toSubsectionContent(
    s : KSBlock<KSParse>)
    : KSResult<KSSubsectionContent<KSParse>, KSParseError> {
    return when (s) {
      is KSBlockSection,
      is KSBlockDocument,
      is KSBlockSubsection,
      is KSBlockPart,
      is KSBlockImport     -> {
        val sb = StringBuilder()
        sb.append("Unexpected element.")
        sb.append(System.lineSeparator())
        sb.append("  Expected: Subsection content")
        sb.append(System.lineSeparator())
        sb.append("  Received: ")
        sb.append(s)
        sb.append(System.lineSeparator())
        KSResult.fail(KSParseError(no_lex, sb.toString()))
      }

      is KSBlockParagraph  -> KSResult.succeed(KSSubsectionParagraph(s))
      is KSBlockFormalItem -> KSResult.succeed(KSSubsectionFormalItem(s))
      is KSBlockFootnote   -> KSResult.succeed(KSSubsectionFootnote(s))
    }
  }

  private fun checkSubsection(s : KSBlock<KSParse>)
    : KSResult<KSBlockSubsection<KSParse>, KSParseError> {
    return when (s) {
      is KSBlockSubsection -> KSResult.succeed(s)
      is KSBlockDocument,
      is KSBlockSection,
      is KSBlockParagraph,
      is KSBlockFormalItem,
      is KSBlockFootnote,
      is KSBlockPart,
      is KSBlockImport     -> {
        val sb = StringBuilder()
        sb.append("Unexpected element.")
        sb.append(System.lineSeparator())
        sb.append("  Expected: A subsection")
        sb.append(System.lineSeparator())
        sb.append("  Received: ")
        sb.append(s)
        sb.append(System.lineSeparator())
        KSResult.fail(KSParseError(no_lex, sb.toString()))
      }
    }
  }

  private fun parseParagraph(
    context : KSParseContextType,
    element : Element) : KSResult<KSBlockParagraph<KSParse>, KSParseError> {
    Assertive.require(element.localName == "paragraph")

    val kp = KSParse(context)
    val act_id = parseID(context, element)
    val act_type = parseType(context, element)
    val act_content =
      KSResult.listMap({ c -> inlines.parse(context, c) },
        listOfChildren(element))

    return act_id.flatMap { id ->
      act_content.flatMap { content ->
        act_type.flatMap { type ->
          succeed(KSBlockParagraph(no_lex, false, kp, type, id, content))
        }
      }
    }
  }

  private fun parseFootnote(
    context : KSParseContextType,
    element : Element) : KSResult<KSBlockFootnote<KSParse>, KSParseError> {
    Assertive.require(element.localName == "footnote")

    val kp = KSParse(context)
    val act_id = parseIDNonOptional(context, element)
    val act_type = parseType(context, element)
    val act_content =
      KSResult.listMap({ c -> inlines.parse(context, c) },
        listOfChildren(element))

    return act_id.flatMap { id ->
      act_content.flatMap { content ->
        act_type.flatMap { type ->
          succeed(KSBlockFootnote(no_lex, false, kp, id, type, content))
        }
      }
    }
  }

  private fun parseFormalItem(
    context : KSParseContextType,
    element : Element) : KSResult<KSBlockFormalItem<KSParse>, KSParseError> {
    Assertive.require(element.localName == "formal-item")

    val kp = KSParse(context)
    val act_id = parseID(context, element)
    val act_type = parseType(context, element)
    val act_content =
      KSResult.listMap({ c -> inlines.parse(context, c) },
        listOfChildren(element))

    val title = parseTitle(context, element)
    return act_id.flatMap { id ->
      act_content.flatMap { content ->
        act_type.flatMap { type ->
          succeed(KSBlockFormalItem(no_lex, false, kp, type, id, title, content))
        }
      }
    }
  }

  private fun parsePart(
    context : KSParseContextType,
    element : Element) : KSResult<KSBlockPart<KSParse>, KSParseError> {
    Assertive.require(element.localName == "part")

    val kp = KSParse(context)
    val act_id = parseID(context, element)
    val act_type = parseType(context, element)
    val title = parseTitle(context, element)

    val act_content =
      KSResult.listMap({ c -> parse(context, c).flatMap { s -> toSection(s) } },
        listOfChildElements(element))

    return act_id.flatMap { id ->
      act_content.flatMap { content ->
        act_type.flatMap { type ->
          succeed(KSBlockPart(no_lex, false, kp, type, id, title, content))
        }
      }
    }
  }

  private fun toSection(
    s : KSBlock<KSParse>) : KSResult<KSBlockSection<KSParse>, KSParseError> {
    return when (s) {
      is KSBlockSection -> KSResult.succeed(s)
      is KSBlockDocument,
      is KSBlockSubsection,
      is KSBlockParagraph,
      is KSBlockFormalItem,
      is KSBlockFootnote,
      is KSBlockPart,
      is KSBlockImport  -> {
        val sb = StringBuilder()
        sb.append("Unexpected element.")
        sb.append(System.lineSeparator())
        sb.append("  Expected: A section")
        sb.append(System.lineSeparator())
        sb.append("  Received: ")
        sb.append(s)
        sb.append(System.lineSeparator())
        KSResult.fail(KSParseError(no_lex, sb.toString()))
      }
    }
  }

  companion object {

    private val no_lex : Optional<LexicalPositionType<Path>> = Optional.empty()

    private fun <T : Any> succeed(x : T) : KSResult<T, KSParseError> =
      KSResult.succeed<T, KSParseError>(x)

    private fun fail(x : KSParseError) : KSResult<KSBlock<KSParse>, KSParseError> =
      KSResult.fail(x)

    fun create(inlines : KSXOMInlineParserType) : KSXOMBlockParserType {
      return KSXOMBlockParser(inlines)
    }
  }

}