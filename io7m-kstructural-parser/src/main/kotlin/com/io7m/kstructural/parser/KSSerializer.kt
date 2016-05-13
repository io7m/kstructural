package com.io7m.kstructural.parser

import com.io7m.kstructural.core.KSElement
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
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection.KSBlockSectionWithContent
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection.KSBlockSectionWithSubsections
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSubsection
import com.io7m.kstructural.core.KSElement.KSInline
import com.io7m.kstructural.core.KSElement.KSInline.*
import com.io7m.kstructural.core.KSElement.KSInline.KSListItem
import com.io7m.kstructural.core.KSElement.KSInline.KSTableBody
import com.io7m.kstructural.core.KSElement.KSInline.KSTableBodyCell
import com.io7m.kstructural.core.KSElement.KSInline.KSTableBodyRow
import com.io7m.kstructural.core.KSElement.KSInline.KSTableHead
import com.io7m.kstructural.core.KSElement.KSInline.KSTableHeadColumnName
import com.io7m.kstructural.core.KSElement.KSInline.KSTableSummary
import com.io7m.kstructural.core.KSLink
import com.io7m.kstructural.core.KSLinkContent
import com.io7m.kstructural.core.KSSubsectionContent
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionFootnote
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionFormalItem
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionParagraph
import com.io7m.kstructural.core.evaluator.KSEvaluation
import com.io7m.kstructural.parser.KSExpression.KSExpressionList
import com.io7m.kstructural.parser.KSExpression.KSExpressionSymbol
import com.io7m.kstructural.parser.KSExpression.KSExpressionQuoted

class KSSerializer : KSSerializerType {

  override fun serialize(e : KSElement<KSEvaluation>) : KSExpression =
    when (e) {
      is KSBlock               -> serializeBlock(e)
      is KSInline              -> serializeInline(e)
      is KSListItem            -> TODO()
      is KSTableHeadColumnName -> TODO()
      is KSTableHead           -> TODO()
      is KSTableBodyCell       -> TODO()
      is KSTableBodyRow        -> TODO()
      is KSTableBody           -> TODO()
      is KSTableSummary        -> TODO()
    }

  private fun serializeInline(e : KSInline<KSEvaluation>) : KSExpression =
  when (e) {
    is KSInlineLink              -> serializeInlineLink(e)
    is KSInlineText              -> serializeText(e)
    is KSInlineVerbatim          -> serializeVerbatim(e)
    is KSInlineTerm              -> serializeTerm(e)
    is KSInlineFootnoteReference -> TODO()
    is KSInlineImage             -> TODO()
    is KSInlineListOrdered       -> TODO()
    is KSInlineListUnordered     -> TODO()
    is KSInlineTable             -> TODO()
    is KSInlineInclude           -> serializeInclude(e)
  }

  private fun serializeInclude(e : KSInlineInclude<KSEvaluation>) : KSExpression {
    val es = mutableListOf<KSExpression>()
    es.add(KSExpressionSymbol(e.position, "include"))
    es.add(KSExpressionQuoted(e.file.position, e.file.text))
    return KSExpressionList(e.position, false, es)
  }

  private fun serializeTerm(e : KSInlineTerm<KSEvaluation>) : KSExpression {
    val es = mutableListOf<KSExpression>()
    es.add(KSExpressionSymbol(e.position, "term"))
    es.addAll(e.content.map { c -> serialize(c) })
    return KSExpressionList(e.position, false, es)
  }

  private fun serializeVerbatim(e : KSInlineVerbatim<KSEvaluation>) : KSExpression {
    val es = mutableListOf<KSExpression>()
    es.add(KSExpressionSymbol(e.position, "verbatim"))
    es.add(KSExpressionQuoted(e.position, e.text.text))
    return KSExpressionList(e.position, false, es)
  }

  private fun serializeText(e : KSInlineText<KSEvaluation>) : KSExpression =
    KSExpressionSymbol(e.position, e.text)

  private fun serializeInlineLink(e : KSInlineLink<KSEvaluation>) : KSExpression =
  when (e.actual) {
    is KSLink.KSLinkExternal -> serializeLinkExternal(e.actual as KSLink.KSLinkExternal)
    is KSLink.KSLinkInternal -> serializeLinkInternal(e.actual as KSLink.KSLinkInternal)
  }

  private fun serializeLinkInternal(e : KSLink.KSLinkInternal<KSEvaluation>) : KSExpression {
    val es = mutableListOf<KSExpression>()
    es.add(KSExpressionSymbol(e.position, "link"))
    es.addAll(e.content.map { c -> serializeLinkContent(c) })
    return KSExpressionList(e.position, false, es)
  }

  private fun serializeLinkContent(c : KSLinkContent<KSEvaluation>) : KSExpression =
  when (c) {
    is KSLinkContent.KSLinkText    -> serialize(c.actual)
    is KSLinkContent.KSLinkImage   -> serialize(c.actual)
  }

  private fun serializeLinkExternal(e : KSLink.KSLinkExternal<KSEvaluation>) : KSExpression {
    val es = mutableListOf<KSExpression>()
    es.add(KSExpressionSymbol(e.position, "link-ext"))
    es.addAll(e.content.map { c -> serializeLinkContent(c) })
    return KSExpressionList(e.position, false, es)
  }

  private fun serializeBlock(
    e : KSElement.KSBlock<KSEvaluation>) : KSExpression =
    when (e) {
      is KSBlockDocument   -> serializeDocument(e)
      is KSBlockSection    -> serializeSection(e)
      is KSBlockSubsection -> serializeSubsection(e)
      is KSBlockParagraph  -> serializeParagraph(e)
      is KSBlockFormalItem -> TODO()
      is KSBlockFootnote   -> TODO()
      is KSBlockPart       -> serializePart(e)
      is KSBlockImport     -> TODO()
    }

  private fun serializeSubsection(e : KSBlockSubsection<KSEvaluation>) : KSExpression {
    val es = mutableListOf<KSExpression>()
    es.add(KSExpressionSymbol(e.position, "subsection"))
    es.addAll(e.content.map { c -> serializeSubsectionContent(c) })
    return KSExpressionList(e.position, false, es)
  }

  private fun serializeParagraph(e : KSBlockParagraph<KSEvaluation>) : KSExpression {
    val es = mutableListOf<KSExpression>()
    es.add(KSExpressionSymbol(e.position, "paragraph"))
    es.addAll(e.content.map { c -> serialize(c) })
    return KSExpressionList(e.position, false, es)
  }

  private fun serializeSection(e : KSBlockSection<KSEvaluation>) : KSExpression {
    val es = mutableListOf<KSExpression>()
    es.add(KSExpressionSymbol(e.position, "section"))
    es.addAll(when (e) {
      is KSBlockSectionWithSubsections -> {
        e.content.map { c -> serialize(c) }
      }
      is KSBlockSectionWithContent     -> {
        e.content.map { c -> serializeSubsectionContent(c) }
      }
    })
    return KSExpressionList(e.position, false, es)
  }

  private fun serializeSubsectionContent(c : KSSubsectionContent<KSEvaluation>) : KSExpression =
    when (c) {
      is KSSubsectionParagraph  -> serializeBlock(c.paragraph)
      is KSSubsectionFormalItem -> serializeBlock(c.formal)
      is KSSubsectionFootnote   -> serializeBlock(c.footnote)
    }

  private fun serializePart(e : KSBlockPart<KSEvaluation>) : KSExpression {
    val es = mutableListOf<KSExpression>()
    es.add(KSExpressionSymbol(e.position, "part"))
    es.addAll(e.content.map { c -> serialize(c) })
    return KSExpressionList(e.position, false, es)
  }

  private fun serializeDocument(e : KSBlockDocument<KSEvaluation>) : KSExpression {
    val es = mutableListOf<KSExpression>()
    es.add(KSExpressionSymbol(e.position, "document"))
    es.addAll(when (e) {
      is KSBlockDocumentWithParts    -> {
        e.content.map { c -> serialize(c) }
      }
      is KSBlockDocumentWithSections -> {
        e.content.map { c -> serialize(c) }
      }
    })
    return KSExpressionList(e.position, false, es)
  }


}