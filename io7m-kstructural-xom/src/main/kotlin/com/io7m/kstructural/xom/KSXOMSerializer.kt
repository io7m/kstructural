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
import com.io7m.kstructural.core.KSElement.KSInline.KSListItem
import com.io7m.kstructural.core.KSElement.KSInline.KSTableBody
import com.io7m.kstructural.core.KSElement.KSInline.KSTableBodyCell
import com.io7m.kstructural.core.KSElement.KSInline.KSTableBodyRow
import com.io7m.kstructural.core.KSElement.KSInline.KSTableHead
import com.io7m.kstructural.core.KSElement.KSInline.KSTableHeadColumnName
import com.io7m.kstructural.core.KSElement.KSInline.KSTableSummary
import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSLink
import com.io7m.kstructural.core.KSLinkContent
import com.io7m.kstructural.core.KSSubsectionContent
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionFootnote
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionFormalItem
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionParagraph
import com.io7m.kstructural.schema.KSSchemaNamespaces
import nu.xom.Attribute
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import java.util.Optional
import java.util.function.Function

class KSXOMSerializer<T> private constructor(
  private val imports : Function<KSBlock<T>, Optional<KSBlockImport<T>>>,
  private val includes : Function<KSInlineText<T>, Optional<KSInlineInclude<T>>>
) : KSXOMSerializerType<T> {

  override fun serialize(e : KSElement<T>) : Node {
    return when (e) {
      is KSBlock               -> serializeBlock(e)
      is KSInline              -> serializeInline(e)
      is KSListItem            -> serializeListItem(e)
      is KSTableHeadColumnName -> serializeTableHeadColumnName(e)
      is KSTableHead           -> serializeTableHead(e)
      is KSTableBodyCell       -> serializeTableBodyCell(e)
      is KSTableBodyRow        -> serializeTableBodyRow(e)
      is KSTableBody           -> serializeTableBody(e)
      is KSTableSummary        -> serializeTableSummary(e)
    }
  }

  private fun serializeInline(e : KSInline<T>) : Node {
    return when (e) {
      is KSInlineLink              -> serializeInlineLink(e)
      is KSInlineText              -> serializeInlineText(e)
      is KSInlineVerbatim          -> serializeInlineVerbatim(e)
      is KSInlineTerm              -> serializeInlineTerm(e)
      is KSInlineFootnoteReference -> serializeInlineFootnoteReference(e)
      is KSInlineImage             -> serializeInlineImage(e)
      is KSInlineListOrdered       -> serializeInlineListOrdered(e)
      is KSInlineListUnordered     -> serializeInlineListUnordered(e)
      is KSInlineTable             -> serializeInlineTable(e)
      is KSInlineInclude           -> serializeInlineInclude(e)
    }
  }

  private fun serializeInlineFootnoteReference(
    e : KSInlineFootnoteReference<T>) : Node {
    val xe = Element("s:footnote-ref", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    xe.addAttribute(Attribute(
      "s:target", KSSchemaNamespaces.NAMESPACE_URI_TEXT, e.target.toString()))
    return xe
  }

  private fun serializeInlineTable(e : KSInlineTable<T>) : Node {
    val xe = Element("s:table", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    xe.addAttribute(serializeTableSummary(e.summary))
    e.head.ifPresent { head -> xe.appendChild(serializeTableHead(head)) }
    xe.appendChild(serializeTableBody(e.body))
    return xe
  }

  private fun serializeTableBody(e : KSTableBody<T>) : Node {
    val xe = Element("s:body", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    e.rows.map { c -> xe.appendChild(serializeTableBodyRow(c)) }
    return xe
  }

  private fun serializeTableBodyRow(e : KSTableBodyRow<T>) : Node {
    val xe = Element("s:row", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    e.cells.map { c -> xe.appendChild(serializeTableBodyCell(c)) }
    return xe
  }

  private fun serializeTableBodyCell(e : KSTableBodyCell<T>) : Node {
    val xe = Element("s:cell", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    KSXOM.inlinesAppend(xe, e.content, { c -> serializeInline(c) })
    return xe
  }

  private fun serializeTableHead(e : KSTableHead<T>) : Node {
    val xe = Element("s:head", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    e.column_names.map { c -> xe.appendChild(serializeTableHeadColumnName(c)) }
    return xe
  }

  private fun serializeTableSummary(e : KSTableSummary<T>) : Attribute {
    val sb = StringBuilder()
    val max = e.content.size - 1
    for (i in 0 .. max) {
      sb.append(e.content[i].text)
      if (i < max) {
        sb.append(" ")
      }
    }

    return Attribute(
      "s:summary", KSSchemaNamespaces.NAMESPACE_URI_TEXT, sb.toString())
  }

  private fun serializeTableHeadColumnName(e : KSTableHeadColumnName<T>) : Node {
    val xe = Element("s:name", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    KSXOM.inlinesAppend(xe, e.content, { c -> serializeInline(c) })
    return xe
  }

  private fun serializeListItem(e : KSListItem<T>) : Node {
    val xe = Element("s:item", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    KSXOM.inlinesAppend(xe, e.content, { c -> serializeInline(c) })
    return xe
  }

  private fun serializeInlineListUnordered(e : KSInlineListUnordered<T>) : Node {
    val xe = Element("s:list-unordered", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    e.content.map { c -> xe.appendChild(serializeListItem(c)) }
    return xe
  }

  private fun serializeInlineListOrdered(e : KSInlineListOrdered<T>) : Node {
    val xe = Element("s:list-ordered", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    e.content.map { c -> xe.appendChild(serializeListItem(c)) }
    return xe
  }

  private fun serializeInlineInclude(e : KSInlineInclude<T>) : Node {
    val xe = Element("xi:xinclude", KSSchemaNamespaces.XINCLUDE_NAMESPACE_URI_TEXT)
    xe.addAttribute(Attribute(
      "xi:href", KSSchemaNamespaces.XINCLUDE_NAMESPACE_URI_TEXT, e.file.text))
    xe.addAttribute(Attribute(
      "xi:parse", KSSchemaNamespaces.XINCLUDE_NAMESPACE_URI_TEXT, "text"))
    return xe
  }

  private fun serializeInlineLink(e : KSInlineLink<T>) : Node {
    return when (e.actual) {
      is KSLink.KSLinkExternal ->
        serializeLinkExternal(e.actual as KSLink.KSLinkExternal<T>)
      is KSLink.KSLinkInternal ->
        serializeLink(e.actual as KSLink.KSLinkInternal<T>)
    }
  }

  private fun serializeLinkExternal(e : KSLink.KSLinkExternal<T>) : Node {
    val xe = Element("s:link-external", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    xe.addAttribute(Attribute(
      "s:target", KSSchemaNamespaces.NAMESPACE_URI_TEXT, e.target.toString()))
    KSXOM.inlinesAppend(xe, e.content, { c -> serializeLinkContent(c) })
    return xe
  }

  private fun serializeLinkContent(c : KSLinkContent<T>) : Node {
    return when (c) {
      is KSLinkContent.KSLinkText  -> serializeInlineText(c.actual)
      is KSLinkContent.KSLinkImage -> serializeInlineImage(c.actual)
    }
  }

  private fun serializeLink(e : KSLink.KSLinkInternal<T>) : Node {
    val xe = Element("s:link", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    xe.addAttribute(Attribute(
      "s:target", KSSchemaNamespaces.NAMESPACE_URI_TEXT, e.target.value))
    KSXOM.inlinesAppend(xe, e.content, { c -> serializeLinkContent(c) })
    return xe
  }

  private fun serializeInlineImage(e : KSInlineImage<T>) : Node {
    val xe = Element("s:image", KSSchemaNamespaces.NAMESPACE_URI_TEXT)

    xe.addAttribute(Attribute(
      "s:target", KSSchemaNamespaces.NAMESPACE_URI_TEXT, e.target.toString()))

    e.size.ifPresent { size ->
      xe.addAttribute(Attribute(
        "s:width", KSSchemaNamespaces.NAMESPACE_URI_TEXT, size.width.toString()))
      xe.addAttribute(Attribute(
        "s:height", KSSchemaNamespaces.NAMESPACE_URI_TEXT, size.height.toString()))
    }

    addType(e.type, xe)

    KSXOM.inlinesAppend(xe, e.content, { c -> serializeInlineText(c) })
    return xe
  }

  private fun serializeInlineTerm(e : KSInlineTerm<T>) : Node {
    val xe = Element("s:term", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    addType(e.type, xe)

    KSXOM.inlinesAppend(xe, e.content, { c -> serializeInlineText(c) })
    return xe
  }

  private fun serializeInlineVerbatim(e : KSInlineVerbatim<T>) : Node {
    val xe = Element("s:verbatim", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    addType(e.type, xe)

    xe.appendChild(serializeInlineText(e.text))
    return xe
  }

  private fun serializeInlineText(e : KSInlineText<T>) : Node {
    val i = includes.apply(e)
    return if (i.isPresent) {
      serializeInlineInclude(i.get())
    } else {
      Text(e.text)
    }
  }

  private fun serializeBlock(e : KSBlock<T>) : Node {
    val i = imports.apply(e)
    if (i.isPresent) {
      return serializeBlockImport(i.get())
    }

    return when (e) {
      is KSBlockDocument   -> serializeBlockDocument(e)
      is KSBlockSection    -> serializeBlockSection(e)
      is KSBlockSubsection -> serializeBlockSubsection(e)
      is KSBlockParagraph  -> serializeBlockParagraph(e)
      is KSBlockFormalItem -> serializeBlockFormalItem(e)
      is KSBlockFootnote   -> serializeBlockFootnote(e)
      is KSBlockPart       -> serializeBlockPart(e)
      is KSBlockImport     -> serializeBlockImport(e)
    }
  }

  private fun serializeBlockImport(e : KSBlockImport<T>) : Node {
    val xe = Element("xi:xinclude", KSSchemaNamespaces.XINCLUDE_NAMESPACE_URI_TEXT)
    xe.addAttribute(Attribute(
      "xi:href", KSSchemaNamespaces.XINCLUDE_NAMESPACE_URI_TEXT, e.file.text))
    xe.addAttribute(Attribute(
      "xi:parse", KSSchemaNamespaces.XINCLUDE_NAMESPACE_URI_TEXT, "xml"))
    return xe
  }

  private fun serializeBlockDocument(e : KSBlockDocument<T>) : Node {
    return when (e) {
      is KSBlockDocumentWithParts    -> serializeBlockDocumentWithParts(e)
      is KSBlockDocumentWithSections -> serializeBlockDocumentWithSections(e)
    }
  }

  private fun serializeBlockDocumentWithSections(
    e : KSBlockDocumentWithSections<T>) : Node {
    val xe = Element("s:document", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    addType(e.type, xe)
    addId(e.id, xe)
    addTitle(e.title, xe)
    e.content.map { c -> xe.appendChild(serializeBlockSection(c)) }
    return xe
  }

  private fun serializeBlockDocumentWithParts(
    e : KSBlockDocumentWithParts<T>) : Node {
    val xe = Element("s:document", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    addType(e.type, xe)
    addId(e.id, xe)
    addTitle(e.title, xe)
    e.content.map { c -> xe.appendChild(serializeBlockPart(c)) }
    return xe
  }

  private fun serializeBlockSubsection(e : KSBlockSubsection<T>) : Node {
    val xe = Element("s:subsection", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    addType(e.type, xe)
    addId(e.id, xe)
    addTitle(e.title, xe)
    e.content.map { c -> xe.appendChild(serializeSubsectionContent(c)) }
    return xe
  }

  private fun serializeBlockPart(e : KSBlockPart<T>) : Node {
    val xe = Element("s:part", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    addType(e.type, xe)
    addId(e.id, xe)
    addTitle(e.title, xe)
    e.content.map { c -> xe.appendChild(serializeBlockSection(c)) }
    return xe
  }

  private fun serializeBlockSection(e : KSBlockSection<T>) : Node {
    return when (e) {
      is KSBlockSectionWithSubsections -> serializeBlockSectionWithSubsections(e)
      is KSBlockSectionWithContent     -> serializeBlockSectionWithContent(e)
    }
  }

  private fun serializeBlockSectionWithContent(
    e : KSBlockSectionWithContent<T>) : Node {
    val xe = Element("s:section", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    addType(e.type, xe)
    addId(e.id, xe)
    addTitle(e.title, xe)
    e.content.map { c -> xe.appendChild(serializeSubsectionContent(c)) }
    return xe
  }

  private fun addTitle(title : List<KSInlineText<T>>, xe : Element) {
    val sb = StringBuilder()
    val max = title.size - 1
    for (i in 0 .. max) {
      sb.append(title[i].text)
      if (i < max) {
        sb.append(" ")
      }
    }

    xe.addAttribute(
      Attribute("s:title", KSSchemaNamespaces.NAMESPACE_URI_TEXT, sb.toString()))
  }

  private fun serializeSubsectionContent(c : KSSubsectionContent<T>) : Node {
    return when (c) {
      is KSSubsectionParagraph  -> serializeBlockParagraph(c.paragraph)
      is KSSubsectionFormalItem -> serializeBlockFormalItem(c.formal)
      is KSSubsectionFootnote   -> serializeBlockFootnote(c.footnote)
    }
  }

  private fun serializeBlockFootnote(e : KSBlockFootnote<T>) : Node {
    val xe = Element("s:footnote", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    addType(e.type, xe)
    addId(e.id, xe)
    KSXOM.inlinesAppend(xe, e.content, { c -> serializeInline(c) })
    return xe
  }

  private fun serializeBlockFormalItem(e : KSBlockFormalItem<T>) : Node {
    val xe = Element("s:formal-item", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    addType(e.type, xe)
    addId(e.id, xe)
    addTitle(e.title, xe)
    KSXOM.inlinesAppend(xe, e.content, { c -> serializeInline(c) })
    return xe
  }

  private fun serializeBlockSectionWithSubsections(
    e : KSBlockSectionWithSubsections<T>) : Node {
    val xe = Element("s:section", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    addType(e.type, xe)
    addId(e.id, xe)
    addTitle(e.title, xe)
    e.content.map { c -> xe.appendChild(serializeBlock(c)) }
    return xe
  }

  private fun serializeBlockParagraph(e : KSBlockParagraph<T>) : Node {
    val xe = Element("s:paragraph", KSSchemaNamespaces.NAMESPACE_URI_TEXT)
    addType(e.type, xe)
    addId(e.id, xe)
    KSXOM.inlinesAppend(xe, e.content, { c -> serializeInline(c) })
    return xe
  }

  private fun addId(id_opt : Optional<KSID<T>>, xe : Element) {
    id_opt.ifPresent { id ->
      xe.addAttribute(Attribute(
        "xml:id", KSSchemaNamespaces.XML_NAMESPACE_URI_TEXT, id.value))
    }
  }

  private fun addType(type_opt : Optional<String>, xe : Element) {
    type_opt.ifPresent { type ->
      xe.addAttribute(Attribute(
        "s:type", KSSchemaNamespaces.NAMESPACE_URI_TEXT, type))
    }
  }

  companion object {
    fun <T> create(
      imports : Function<KSBlock<T>, Optional<KSBlockImport<T>>>,
      includes : Function<KSInlineText<T>, Optional<KSInlineInclude<T>>>)
      : KSXOMSerializerType<T> {
      return KSXOMSerializer(imports, includes)
    }
  }

}