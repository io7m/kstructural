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

import com.io7m.junreachable.UnreachableCodeException
import com.io7m.kstructural.core.KSElement.KSBlock
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockFootnote
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockFormalItem
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
import com.io7m.kstructural.core.KSLink
import com.io7m.kstructural.core.KSLink.KSLinkExternal
import com.io7m.kstructural.core.KSLink.KSLinkInternal
import com.io7m.kstructural.core.KSLinkContent
import com.io7m.kstructural.core.KSTextUtilities
import com.io7m.kstructural.core.evaluator.KSEvaluation
import com.io7m.kstructural.core.evaluator.KSNumber
import nu.xom.Attribute
import nu.xom.DocType
import nu.xom.Document
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.valid4j.Assertive
import java.net.URI
import java.util.Optional

internal object KSXOM {

  private val XHTML_URI = URI.create("http://www.w3.org/1999/xhtml")
  private val XHTML_URI_TEXT = XHTML_URI.toString()
  private const val ATTRIBUTE_PREFIX = "st300"

  sealed class NavigationBarPosition {
    object Top : NavigationBarPosition()

    object Bottom : NavigationBarPosition()
  }

  fun document() : Document {
    val dt = DocType(
      "html",
      "-//W3C//DTD XHTML 1.0 Strict//EN",
      "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd")
    val root = Element("html", XHTML_URI_TEXT)
    val doc = Document(root)
    doc.docType = dt
    return doc
  }

  fun newPage(
    settings : KSXOMSettings,
    document : KSBlockDocument<out Any>,
    number : Optional<KSNumber>,
    title : List<KSInlineText<KSEvaluation>>) : Pair<Document, Element> {

    val rd = KSXOM.document()
    rd.rootElement.appendChild(KSXOM.head(settings, document, number, title))
    val body = KSXOM.body()
    val body_container = KSXOM.bodyContainer()
    body.appendChild(body_container)
    rd.rootElement.appendChild(body)
    return Pair(rd, body_container);
  }

  fun body() : Element =
    Element("body", XHTML_URI_TEXT)

  fun head(
    settings : KSXOMSettings,
    document : KSBlockDocument<out Any>,
    number : Optional<KSNumber>,
    title : List<KSInlineText<KSEvaluation>>) : Element {

    val sb = StringBuilder()
    sb.append(KSTextUtilities.concatenate(document.title))
    if (number.isPresent) {
      sb.append(": ")
      sb.append(number.get())
      sb.append(". ")
      sb.append(KSTextUtilities.concatenate(title))
    }

    val e = Element("head", XHTML_URI_TEXT)
    e.appendChild(title(sb.toString()))
    settings.styles.forEach { s -> e.appendChild(css(s)) }
    return e
  }

  private fun css(u : URI) : Element {
    val e = Element("link", XHTML_URI_TEXT);
    e.addAttribute(Attribute("rel", null, "stylesheet"));
    e.addAttribute(Attribute("type", null, "text/css"));
    e.addAttribute(Attribute("href", null, u.toString()));
    return e;
  }

  fun title(text : String) : Element {
    val e = Element("title", XHTML_URI_TEXT)
    e.appendChild(text)
    return e
  }

  fun bodyContainer() : Element {
    val e = Element("div", XHTML_URI_TEXT)
    e.addAttribute(Attribute("class", null, prefixedName("body")))
    return e
  }

  fun sectionContainer(
    prov : KSXOMLinkProviderType,
    s : KSBlockSection<KSEvaluation>) : Element {
    val number_opt = s.data.number
    Assertive.require(number_opt.isPresent)
    val number = number_opt.get()

    val sc = Element("div", XHTML_URI_TEXT)
    sc.addAttribute(Attribute("class", null, prefixedName("section_container")))

    s.id.ifPresent { id ->
      val stid_a = Element("a", XHTML_URI_TEXT)
      stid_a.addAttribute(Attribute("id", null, id.value))
      sc.appendChild(stid_a)
    }

    val stn = Element("div", XHTML_URI_TEXT)
    stn.addAttribute(Attribute("class", null, prefixedName("section_title_number")))

    val title_text = KSTextUtilities.concatenate(s.title)
    val title = StringBuilder()
    title.append("Section ")
    title.append(number)
    title.append(": ")
    title.append(title_text)

    val stn_a = Element("a", XHTML_URI_TEXT)
    stn_a.addAttribute(Attribute("id", null, prov.numberAnchorID(number)))
    stn_a.addAttribute(Attribute("href", null, prov.numberAnchor(number)))
    stn_a.addAttribute(Attribute("title", null, title.toString()))
    stn_a.appendChild(number.toString())
    stn.appendChild(stn_a)

    val st = Element("div", XHTML_URI_TEXT)
    st.addAttribute(Attribute("class", null, prefixedName("section_title")))
    st.appendChild(title_text)

    sc.appendChild(stn)
    sc.appendChild(st)
    return sc
  }

  fun prefixedName(s : String) : String {
    val sb = StringBuilder()
    sb.append(ATTRIBUTE_PREFIX)
    sb.append("_")
    sb.append(s)
    return sb.toString()
  }

  fun subsectionContainer(
    prov : KSXOMLinkProviderType,
    ss : KSBlockSubsection<KSEvaluation>) : Element {
    val number_opt = ss.data.number
    Assertive.require(number_opt.isPresent)
    val number = number_opt.get()

    val sc = Element("div", XHTML_URI_TEXT)
    sc.addAttribute(Attribute("class", null, prefixedName("subsection_container")))

    ss.id.ifPresent { id ->
      val stid_a = Element("a", XHTML_URI_TEXT)
      stid_a.addAttribute(Attribute("id", null, id.value))
      sc.appendChild(stid_a)
    }

    val stn = Element("div", XHTML_URI_TEXT)
    stn.addAttribute(Attribute("class", null, prefixedName("subsection_title_number")))

    val title_text = KSTextUtilities.concatenate(ss.title)
    val title = StringBuilder()
    title.append("Subsection ")
    title.append(number)
    title.append(": ")
    title.append(title_text)

    val stn_a = Element("a", XHTML_URI_TEXT)
    stn_a.addAttribute(Attribute("id", null, prov.numberAnchorID(number)))
    stn_a.addAttribute(Attribute("href", null, prov.numberAnchor(number)))
    stn_a.addAttribute(Attribute("title", null, title.toString()))
    stn_a.appendChild(number.toString())
    stn.appendChild(stn_a)

    val st = Element("div", XHTML_URI_TEXT)
    st.addAttribute(Attribute("class", null, prefixedName("subsection_title")))
    st.appendChild(title_text)

    sc.appendChild(stn)
    sc.appendChild(st)
    return sc
  }

  data class Paragraph(
    val container : Element,
    val content : Element)

  fun paragraphContainer(
    prov : KSXOMLinkProviderType,
    p : KSBlockParagraph<KSEvaluation>) : Paragraph {

    val number_opt = p.data.number
    Assertive.require(number_opt.isPresent)
    val number = number_opt.get()

    val sc = Element("div", XHTML_URI_TEXT)
    sc.addAttribute(Attribute("class", null, prefixedName("paragraph_container")))

    p.id.ifPresent { id ->
      val stid_a = Element("a", XHTML_URI_TEXT)
      stid_a.addAttribute(Attribute("id", null, id.value))
      sc.appendChild(stid_a)
    }

    val stn = Element("div", XHTML_URI_TEXT)
    stn.addAttribute(Attribute("class", null, prefixedName("paragraph_number")))

    val title = StringBuilder()
    title.append("Paragraph ")
    title.append(number)

    val stn_a = Element("a", XHTML_URI_TEXT)
    stn_a.addAttribute(Attribute("id", null, prov.numberAnchorID(number)))
    stn_a.addAttribute(Attribute("href", null, prov.numberAnchor(number)))
    stn_a.addAttribute(Attribute("title", null, title.toString()))
    stn_a.appendChild(number.least.toString())
    stn.appendChild(stn_a)

    val scc = Element("div", XHTML_URI_TEXT)
    scc.addAttribute(Attribute("class", null, prefixedName("paragraph")))

    sc.appendChild(stn)
    sc.appendChild(scc)
    return Paragraph(sc, scc)
  }

  fun inline(
    prov : KSXOMLinkProviderType,
    c : KSInline<KSEvaluation>) : Node =
    when (c) {
      is KSInlineLink              -> inlineLink(prov, c)
      is KSInlineText              -> inlineText(c)
      is KSInlineVerbatim          -> inlineVerbatim(c)
      is KSInlineTerm              -> inlineTerm(c)
      is KSInlineImage             -> inlineImage(c)
      is KSInlineListOrdered       -> inlineListOrdered(prov, c)
      is KSInlineListUnordered     -> inlineListUnordered(prov, c)
      is KSInlineTable             -> inlineTable(prov, c)
      is KSInlineFootnoteReference -> inlineFootnoteReference(prov, c)
      is KSInlineInclude           -> inlineInclude(c)
    }

  private fun inlineInclude(
    c : KSInlineInclude<KSEvaluation>) : Node {
    throw TODO()
  }

  private fun inlineFootnoteReference(
    prov : KSXOMLinkProviderType,
    c : KSInlineFootnoteReference<KSEvaluation>) : Node {

    val ref = c.data.context.footnoteReferenceForInline(c)
    val fn = c.data.context.elementForID(ref.id) as KSBlockFootnote<KSEvaluation>

    val title = StringBuilder()
    title.append("Jump to footnote ")
    title.append(ref.id.value)
    title.append(" (reference ")
    title.append(ref.index)
    title.append(")")

    val a = Element("a", XHTML_URI_TEXT)
    a.addAttribute(Attribute("href", null, prov.footnoteLink(fn, ref.index)))
    a.addAttribute(Attribute("id", null, prov.footnoteReferenceAnchor(ref)))
    a.addAttribute(Attribute("title", null, title.toString()))
    a.appendChild(fn.data.index.toString())

    val e = Element("span", XHTML_URI_TEXT)
    e.addAttribute(Attribute("class", null, prefixedName("footnote_reference")))
    e.appendChild("[")
    e.appendChild(a)
    e.appendChild("]")
    return e
  }

  private fun inlineTable(
    prov : KSXOMLinkProviderType,
    t : KSInlineTable<KSEvaluation>) : Node {

    val classes = mutableListOf<String>()
    classes.add(prefixedName("table"))
    t.type.ifPresent { ty -> classes.add(ty.value) }
    val classes_text = KSTextUtilities.concatenate(classes)

    val sc = Element("table", XHTML_URI_TEXT)
    sc.addAttribute(Attribute("class", null, classes_text))

    val summary_text = KSTextUtilities.concatenate(t.summary.content)
    sc.addAttribute(Attribute("summary", null, summary_text))

    if (t.head.isPresent) {
      val th = t.head.get()
      val tsc = Element("thead", XHTML_URI_TEXT)
      tsc.addAttribute(Attribute("class", null, prefixedName("table_head")))
      val tsc_row = Element("tr", XHTML_URI_TEXT)
      tsc.appendChild(tsc_row)

      th.column_names.forEach { n ->
        val tsc_cell = Element("th", XHTML_URI_TEXT)
        tsc_cell.addAttribute(Attribute("class", null, prefixedName("table_column_name")))
        KSXOMSpacing.appendWithSpace(tsc_cell, n.content, { ic -> inline(prov, ic) })
        tsc_row.appendChild(tsc_cell)
      }

      sc.appendChild(tsc)
    }

    val tsc = Element("tbody", XHTML_URI_TEXT)
    tsc.addAttribute(Attribute("class", null, prefixedName("table_body")))
    sc.appendChild(tsc)

    t.body.rows.forEach { row ->
      val tsc_row = Element("tr", XHTML_URI_TEXT)
      tsc.appendChild(tsc_row)

      row.cells.forEach { cell ->
        val tsc_cell = Element("td", XHTML_URI_TEXT)
        tsc_cell.addAttribute(Attribute("class", null, prefixedName("table_cell")))
        tsc_row.appendChild(tsc_cell)
        KSXOMSpacing.appendWithSpace(tsc_cell, cell.content, { ic -> inline(prov, ic) })
      }
    }

    return sc
  }

  private fun inlineListUnordered(
    prov : KSXOMLinkProviderType,
    c : KSInlineListUnordered<KSEvaluation>) : Node {

    val classes = mutableListOf<String>()
    classes.add(prefixedName("list_unordered"))
    val classes_text = KSTextUtilities.concatenate(classes)

    val sc = Element("ul", XHTML_URI_TEXT)
    sc.addAttribute(Attribute("class", null, classes_text))
    c.content.forEach { i ->
      val ie = Element("li", XHTML_URI_TEXT)
      ie.addAttribute(Attribute("class", null, prefixedName("list_item")))
      sc.appendChild(ie)
      KSXOMSpacing.appendWithSpace(ie, i.content, { ic -> inline(prov, ic) })
    }
    return sc
  }

  private fun inlineListOrdered(
    prov : KSXOMLinkProviderType,
    c : KSInlineListOrdered<KSEvaluation>) : Node {

    val classes = mutableListOf<String>()
    classes.add(prefixedName("list_ordered"))
    val classes_text = KSTextUtilities.concatenate(classes)

    val sc = Element("ul", XHTML_URI_TEXT)
    sc.addAttribute(Attribute("class", null, classes_text))
    c.content.forEach { i ->
      val ie = Element("li", XHTML_URI_TEXT)
      ie.addAttribute(Attribute("class", null, prefixedName("list_item")))
      sc.appendChild(ie)
      KSXOMSpacing.appendWithSpace(ie, i.content, { ic -> inline(prov, ic) })
    }
    return sc
  }

  fun linkContent(c : KSLinkContent<KSEvaluation>) : Node =
    when (c) {
      is KSLinkContent.KSLinkText  -> inlineText(c.actual)
      is KSLinkContent.KSLinkImage -> inlineImage(c.actual)
    }

  private fun inlineImage(c : KSInlineImage<KSEvaluation>) : Node {
    val classes = mutableListOf<String>()
    classes.add(prefixedName("image"))
    c.type.ifPresent { type -> classes.add(type.value) }
    val classes_text = KSTextUtilities.concatenate(classes)

    val sc = Element("img", XHTML_URI_TEXT)
    sc.addAttribute(Attribute("class", null, classes_text))
    sc.addAttribute(Attribute("alt", null, KSTextUtilities.concatenate(c.content)))
    sc.addAttribute(Attribute("src", null, c.target.toString()))
    c.size.ifPresent { size ->
      sc.addAttribute(Attribute("width", null, size.width.toString()))
      sc.addAttribute(Attribute("height", null, size.height.toString()))
    }
    return sc
  }

  private fun inlineTerm(c : KSInlineTerm<KSEvaluation>) : Node {

    val classes = mutableListOf<String>()
    classes.add(prefixedName("term"))
    c.type.ifPresent { type -> classes.add(type.value) }
    val classes_text = KSTextUtilities.concatenate(classes)

    val sc = Element("span", XHTML_URI_TEXT)
    sc.addAttribute(Attribute("class", null, classes_text))
    sc.appendChild(KSTextUtilities.concatenate(c.content))
    return sc
  }

  private fun inlineVerbatim(c : KSInlineVerbatim<KSEvaluation>) : Node {
    val classes = mutableListOf<String>()
    classes.add(prefixedName("verbatim"))
    c.type.ifPresent { type -> classes.add(type.value) }
    val classes_text = KSTextUtilities.concatenate(classes)

    val sc = Element("pre", XHTML_URI_TEXT)
    sc.addAttribute(Attribute("class", null, classes_text))
    sc.appendChild(c.text.text)
    return sc
  }

  private fun inlineLink(
    prov : KSXOMLinkProviderType,
    c : KSInlineLink<KSEvaluation>) : Node =
    when (c.actual) {
      is KSLink.KSLinkExternal ->
        linkExternal(c.actual as KSLinkExternal<KSEvaluation>)
      is KSLink.KSLinkInternal ->
        linkInternal(prov, c.actual as KSLinkInternal<KSEvaluation>)
    }

  private fun linkInternal(
    prov : KSXOMLinkProviderType,
    link : KSLinkInternal<KSEvaluation>) : Node {
    val sc = Element("a", XHTML_URI_TEXT)
    sc.addAttribute(Attribute("class", null, prefixedName("link")))
    sc.addAttribute(Attribute("href", null, prov.idLink(link.target)))
    KSXOMSpacing.appendWithSpace(sc, link.content, { c -> linkContent(c) })
    return sc
  }

  private fun linkExternal(link : KSLinkExternal<KSEvaluation>) : Node {
    val sc = Element("a", XHTML_URI_TEXT)
    sc.addAttribute(Attribute("class", null, prefixedName("link_external")))
    sc.addAttribute(Attribute("href", null, link.target.toString()))
    KSXOMSpacing.appendWithSpace(sc, link.content, { c -> linkContent(c) })
    return sc
  }

  fun inlineText(c : KSInlineText<KSEvaluation>) =
    Text(c.text)

  fun partContainer(
    prov : KSXOMLinkProviderType,
    p : KSBlockPart<KSEvaluation>) : Element {

    val number_opt = p.data.number
    Assertive.require(number_opt.isPresent)
    val number = number_opt.get()

    val sc = Element("div", XHTML_URI_TEXT)
    sc.addAttribute(Attribute("class", null, prefixedName("part_container")))

    p.id.ifPresent { id ->
      val stid_a = Element("a", XHTML_URI_TEXT)
      stid_a.addAttribute(Attribute("id", null, id.value))
      sc.appendChild(stid_a)
    }

    val stn = Element("div", XHTML_URI_TEXT)
    stn.addAttribute(Attribute("class", null, prefixedName("part_title_number")))

    val title_text = KSTextUtilities.concatenate(p.title)
    val title = StringBuilder()
    title.append("Part ")
    title.append(number)
    title.append(": ")
    title.append(title_text)

    val stn_a = Element("a", XHTML_URI_TEXT)
    stn_a.addAttribute(Attribute("id", null, prov.numberAnchorID(number)))
    stn_a.addAttribute(Attribute("href", null, prov.numberAnchor(number)))
    stn_a.addAttribute(Attribute("title", null, title.toString()))
    stn_a.appendChild(number.toString())
    stn.appendChild(stn_a)

    val st = Element("div", XHTML_URI_TEXT)
    st.addAttribute(Attribute("class", null, prefixedName("part_title")))
    st.appendChild(title_text)

    sc.appendChild(stn)
    sc.appendChild(st)
    return sc
  }

  fun navigationBar(
    prov : KSXOMLinkProviderType,
    b : KSBlock<KSEvaluation>,
    pos : NavigationBarPosition) : Element {

    val classes = mutableListOf<String>()
    classes.add(prefixedName("navbar"))
    when (pos) {
      is NavigationBarPosition.Top    ->
        classes.add(prefixedName("navbar_top"))
      is NavigationBarPosition.Bottom ->
        classes.add(prefixedName("navbar_bottom"))
    }
    val classes_text = KSTextUtilities.concatenate(classes)

    val sc = Element("div", XHTML_URI_TEXT)
    sc.addAttribute(Attribute("class", null, classes_text))

    when (pos) {
      is NavigationBarPosition.Top    -> {

      }
      is NavigationBarPosition.Bottom -> {
        val hr = Element("hr", XHTML_URI_TEXT)
        hr.addAttribute(Attribute("class", null, prefixedName("hr")))
        sc.appendChild(hr)
      }
    }

    val titles_prev = Element("td", XHTML_URI_TEXT)
    titles_prev.addAttribute(
      Attribute("class", null, prefixedName("navbar_prev_title_cell")))
    b.data.context.elementSegmentPrevious(b).ifPresent {
      block ->
      navigationBarCellTitle(block, titles_prev)
    }

    val titles_up = Element("td", XHTML_URI_TEXT)
    titles_up.addAttribute(
      Attribute("class", null, prefixedName("navbar_up_title_cell")))
    b.data.context.elementSegmentUp(b).ifPresent {
      block ->
      navigationBarCellTitle(block, titles_up)
    }

    val titles_next = Element("td", XHTML_URI_TEXT)
    titles_next.addAttribute(
      Attribute("class", null, prefixedName("navbar_next_title_cell")))
    b.data.context.elementSegmentNext(b).ifPresent {
      block ->
      navigationBarCellTitle(block, titles_next)
    }

    val titles = Element("tr", XHTML_URI_TEXT)
    titles.appendChild(titles_prev)
    titles.appendChild(titles_up)
    titles.appendChild(titles_next)

    val files_prev = Element("td", XHTML_URI_TEXT)
    files_prev.addAttribute(
      Attribute("class", null, prefixedName("navbar_prev_file_cell")))
    b.data.context.elementSegmentPrevious(b).ifPresent {
      block ->
      navigationBarCellFile(
        block, prov, files_prev, "previous", "Go to previous page")
    }

    val files_up = Element("td", XHTML_URI_TEXT)
    files_up.addAttribute(
      Attribute("class", null, prefixedName("navbar_up_file_cell")))
    b.data.context.elementSegmentUp(b).ifPresent {
      block ->
      navigationBarCellFile(
        block, prov, files_up, "up", "Go to parent page")
    }

    val files_next = Element("td", XHTML_URI_TEXT)
    files_next.addAttribute(
      Attribute("class", null, prefixedName("navbar_next_file_cell")))
    b.data.context.elementSegmentNext(b).ifPresent {
      block ->
      navigationBarCellFile(
        block, prov, files_next, "next", "Go to next page")
    }

    val files = Element("tr", XHTML_URI_TEXT)
    files.appendChild(files_prev)
    files.appendChild(files_up)
    files.appendChild(files_next)

    val te = Element("table", XHTML_URI_TEXT)
    te.addAttribute(Attribute("class", null, prefixedName("navbar_table")))
    te.addAttribute(Attribute("summary", null, prefixedName("Navigation bar")))
    sc.appendChild(te)

    when (pos) {
      is NavigationBarPosition.Top    -> {
        te.appendChild(titles)
        te.appendChild(files)
      }
      is NavigationBarPosition.Bottom -> {
        te.appendChild(files)
        te.appendChild(titles)
      }
    }

    when (pos) {
      is NavigationBarPosition.Top    -> {
        val hr = Element("hr", XHTML_URI_TEXT)
        hr.addAttribute(Attribute("class", null, prefixedName("hr")))
        sc.appendChild(hr)
      }
      is NavigationBarPosition.Bottom -> {

      }
    }

    return sc
  }

  private fun navigationBarCellTitle(
    block : KSBlock<KSEvaluation>,
    parent : Element) {

    val text_sb = StringBuilder(128)
    block.data.number.ifPresent { number ->
      text_sb.append(number.toString())
      text_sb.append(". ")
    }

    when (block) {
      is KSBlockDocument   ->
        KSTextUtilities.concatenateInto(text_sb, block.title)
      is KSBlockSection    ->
        KSTextUtilities.concatenateInto(text_sb, block.title)
      is KSBlockSubsection ->
        KSTextUtilities.concatenateInto(text_sb, block.title)
      is KSBlockPart       ->
        KSTextUtilities.concatenateInto(text_sb, block.title)
      is KSBlockParagraph  ->
        throw UnreachableCodeException()
    }

    parent.appendChild(text_sb.toString())
  }

  private fun navigationBarCellFile(
    block : KSBlock<KSEvaluation>,
    prov : KSXOMLinkProviderType,
    parent : Element,
    relation : String,
    title : String) {

    val e = Element("a", XHTML_URI_TEXT)
    e.addAttribute(Attribute("rel", null, relation))

    if (block.data.number.isPresent) {
      val number = block.data.number.get()
      e.addAttribute(Attribute("href", null, prov.numberLink(number)))
    } else {
      e.addAttribute(Attribute("href", null, prov.documentAnchor()))
    }
    e.addAttribute(Attribute("title", null, title))

    val fc = relation.get(0).toTitleCase()
    val rest = relation.slice(1 .. relation.length - 1)
    e.appendChild(fc + rest)
    parent.appendChild(e)
  }

  fun documentIndexTitle(d : KSBlockDocument<KSEvaluation>) : Element {
    val e = Element("div", XHTML_URI_TEXT)
    e.addAttribute(Attribute("class", null, prefixedName("document_title")))
    e.appendChild(KSTextUtilities.concatenate(d.title))
    return e
  }

  private fun linkToPartTitleText(
    s : KSBlockPart<KSEvaluation>) : String {
    val title = KSTextUtilities.concatenate(s.title)
    val text = StringBuilder()
    text.append("Link to part ")
    text.append(s.data.number.get().toString())
    text.append(": ")
    text.append(title)
    return text.toString()
  }

  private fun linkToSectionTitleText(
    s : KSBlockSection<KSEvaluation>) : String {
    val title = KSTextUtilities.concatenate(s.title)
    val text = StringBuilder()
    text.append("Link to section ")
    text.append(s.data.number.get().toString())
    text.append(": ")
    text.append(title)
    return text.toString()
  }

  private fun linkToSubsectionTitleText(
    s : KSBlockSubsection<KSEvaluation>) : String {
    val title = KSTextUtilities.concatenate(s.title)
    val text = StringBuilder()
    text.append("Link to subsection ")
    text.append(s.data.number.get().toString())
    text.append(": ")
    text.append(title)
    return text.toString()
  }

  fun contentsForSection(
    prov : KSXOMLinkProviderType,
    s : KSBlockSection<KSEvaluation>) : Element {

    val classes = listOf(
      prefixedName("contents"),
      prefixedName("section_contents_outer"),
      prefixedName("section_contents"))

    val e = Element("ul", XHTML_URI_TEXT)
    e.addAttribute(
      Attribute("class", null, KSTextUtilities.concatenate(classes)))

    when (s) {
      is KSBlockSection.KSBlockSectionWithContent     -> {

      }
      is KSBlockSection.KSBlockSectionWithSubsections -> {
        s.content.forEach { ss ->
          val ss_li = Element("li", XHTML_URI_TEXT)
          val ss_li_classes = listOf(
            prefixedName("contents_item"),
            prefixedName("contents_item1"),
            prefixedName("contents_item_subsection"))
          ss_li.addAttribute(
            Attribute("class", null, KSTextUtilities.concatenate(ss_li_classes)))
          val ss_a = Element("a", XHTML_URI_TEXT)
          val ss_number = ss.data.number.get()
          ss_a.addAttribute(
            Attribute("href", null, prov.numberLink(ss_number)))
          ss_a.addAttribute(
            Attribute("title", null, linkToSubsectionTitleText(ss)))

          val ss_title = KSTextUtilities.concatenate(ss.title)
          val ss_a_text = StringBuilder()
          ss_a_text.append(ss_number.toString())
          ss_a_text.append(". ")
          ss_a_text.append(ss_title)
          ss_a.appendChild(ss_a_text.toString())
          ss_li.appendChild(ss_a)
          e.appendChild(ss_li)
        }
      }
    }

    return e
  }

  fun contentsForPart(
    prov : KSXOMLinkProviderType,
    p : KSBlockPart<KSEvaluation>) : Element {

    val classes = listOf(
      prefixedName("contents"),
      prefixedName("part_contents_outer"),
      prefixedName("part_contents"))

    val e = Element("ul", XHTML_URI_TEXT)
    e.addAttribute(
      Attribute("class", null, KSTextUtilities.concatenate(classes)))

    p.content.forEach { s ->
      val part_li = Element("li", XHTML_URI_TEXT)
      val part_li_classes = listOf(
        prefixedName("contents_item"),
        prefixedName("contents_item1"),
        prefixedName("contents_item_section"))
      part_li.addAttribute(
        Attribute("class", null, KSTextUtilities.concatenate(part_li_classes)))
      val part_a = Element("a", XHTML_URI_TEXT)
      val part_number = s.data.number.get()
      part_a.addAttribute(
        Attribute("href", null, prov.numberLink(part_number)))
      part_a.addAttribute(
        Attribute("title", null, linkToSectionTitleText(s)))

      val section_title = KSTextUtilities.concatenate(s.title)
      val part_a_text = StringBuilder()
      part_a_text.append(part_number.toString())
      part_a_text.append(". ")
      part_a_text.append(section_title)
      part_a.appendChild(part_a_text.toString())
      part_li.appendChild(part_a)
      e.appendChild(part_li)

      when (s) {
        is KSBlockSection.KSBlockSectionWithContent     -> {

        }
        is KSBlockSection.KSBlockSectionWithSubsections -> {
          s.content.forEach { ss ->
            val ss_li = Element("li", XHTML_URI_TEXT)
            val ss_li_classes = listOf(
              prefixedName("contents_item"),
              prefixedName("contents_item2"),
              prefixedName("contents_item_subsection"))
            ss_li.addAttribute(
              Attribute("class", null, KSTextUtilities.concatenate(ss_li_classes)))
            val ss_a = Element("a", XHTML_URI_TEXT)
            val ss_number = ss.data.number.get()
            ss_a.addAttribute(
              Attribute("href", null, prov.numberLink(ss_number)))
            ss_a.addAttribute(
              Attribute("title", null, linkToSubsectionTitleText(ss)))

            val ss_title = KSTextUtilities.concatenate(ss.title)
            val ss_a_text = StringBuilder()
            ss_a_text.append(ss_number.toString())
            ss_a_text.append(". ")
            ss_a_text.append(ss_title)
            ss_a.appendChild(ss_a_text.toString())
            ss_li.appendChild(ss_a)
            part_li.appendChild(ss_li)
          }
        }
      }
    }

    return e
  }

  fun contentsForDocument(
    prov : KSXOMLinkProviderType,
    d : KSBlockDocument<KSEvaluation>) : Element {

    val classes = listOf(
      prefixedName("contents"),
      prefixedName("document_contents"))

    val e = Element("ul", XHTML_URI_TEXT)
    e.addAttribute(
      Attribute("class", null, KSTextUtilities.concatenate(classes)))

    return when (d) {
      is KSBlock.KSBlockDocument.KSBlockDocumentWithParts    -> {
        d.content.forEach { p ->
          val part_li = Element("li", XHTML_URI_TEXT)
          val part_li_classes = listOf(
            prefixedName("contents_item"),
            prefixedName("contents_item1"),
            prefixedName("contents_item_part"))
          part_li.addAttribute(
            Attribute("class", null, KSTextUtilities.concatenate(part_li_classes)))
          val part_a = Element("a", XHTML_URI_TEXT)
          val part_number = p.data.number.get()
          part_a.addAttribute(
            Attribute("href", null, prov.numberLink(part_number)))
          part_a.addAttribute(
            Attribute("title", null, linkToPartTitleText(p)))

          val part_a_text = StringBuilder()
          part_a_text.append(part_number.toString())
          part_a_text.append(". ")
          KSTextUtilities.concatenateInto(part_a_text, p.title)
          part_a.appendChild(part_a_text.toString())
          part_li.appendChild(part_a)
          e.appendChild(part_li)

          val part_ul_classes = listOf(
            prefixedName("contents"),
            prefixedName("part_contents"))

          val part_ul = Element("ul", XHTML_URI_TEXT)
          part_ul.addAttribute(
            Attribute("class", null, KSTextUtilities.concatenate(part_ul_classes)))

          p.content.forEach { s ->
            val s_li = Element("li", XHTML_URI_TEXT)
            val s_li_clases = listOf(
              prefixedName("contents_item"),
              prefixedName("contents_item2"),
              prefixedName("contents_item_section"))
            s_li.addAttribute(
              Attribute("class", null, KSTextUtilities.concatenate(s_li_clases)))
            val s_a = Element("a", XHTML_URI_TEXT)
            val s_number = s.data.number.get()
            s_a.addAttribute(
              Attribute("href", null, prov.numberLink(s_number)))
            s_a.addAttribute(
              Attribute("title", null, linkToSectionTitleText(s)))

            val s_a_text = StringBuilder()
            s_a_text.append(s_number.toString())
            s_a_text.append(". ")
            KSTextUtilities.concatenateInto(s_a_text, s.title)
            s_a.appendChild(s_a_text.toString())
            s_li.appendChild(s_a)
            part_ul.appendChild(s_li)
          }

          part_li.appendChild(part_ul)
        }

        e
      }
      is KSBlock.KSBlockDocument.KSBlockDocumentWithSections -> {
        d.content.forEach { s ->
          val s_li = Element("li", XHTML_URI_TEXT)
          val s_li_classes = listOf(
            prefixedName("contents_item"),
            prefixedName("contents_item1"),
            prefixedName("contents_item_section"))
          s_li.addAttribute(
            Attribute("class", null, KSTextUtilities.concatenate(s_li_classes)))
          val s_a = Element("a", XHTML_URI_TEXT)
          val s_number = s.data.number.get()
          s_a.addAttribute(
            Attribute("href", null, prov.numberLink(s_number)))

          val s_a_text = StringBuilder()
          s_a_text.append(s_number.toString())
          s_a_text.append(". ")
          KSTextUtilities.concatenateInto(s_a_text, s.title)
          s_a.appendChild(s_a_text.toString())
          s_li.appendChild(s_a)
          e.appendChild(s_li)
        }

        e
      }
    }
  }

  fun formalItemContainer(
    prov : KSXOMLinkProviderType,
    f : KSBlockFormalItem<KSEvaluation>) : Pair<Element, Element> {

    val number_opt = f.data.number
    Assertive.require(number_opt.isPresent)
    val number = number_opt.get()

    val sc = Element("div", XHTML_URI_TEXT)
    sc.addAttribute(Attribute("class", null, prefixedName("formal_item")))

    f.id.ifPresent { id ->
      val stid_a = Element("a", XHTML_URI_TEXT)
      stid_a.addAttribute(Attribute("id", null, id.value))
      sc.appendChild(stid_a)
    }

    val st = Element("div", XHTML_URI_TEXT)
    st.addAttribute(Attribute("class", null, prefixedName("formal_item_title")))

    val title_text = KSTextUtilities.concatenate(f.title)
    val title = StringBuilder()
    title.append("Formal item ")
    title.append(number)
    title.append(": ")
    title.append(title_text)

    val stn_a = Element("a", XHTML_URI_TEXT)
    stn_a.addAttribute(Attribute("id", null, prov.numberAnchorID(number)))
    stn_a.addAttribute(Attribute("href", null, prov.numberAnchor(number)))
    stn_a.addAttribute(Attribute("title", null, title.toString()))
    stn_a.appendChild(number.toString())
    stn_a.appendChild(" ")
    stn_a.appendChild(title_text)
    st.appendChild(stn_a)

    sc.appendChild(st)

    val scc = Element("div", XHTML_URI_TEXT)
    scc.addAttribute(Attribute("class", null, prefixedName("formal_item_content")))

    sc.appendChild(scc)
    return Pair(sc, scc)
  }

  fun footnotes(
    prov : KSXOMLinkProviderType,
    footnotes : Map<KSID<KSEvaluation>, KSBlockFootnote<KSEvaluation>>) : Element {

    val e = Element("div", XHTML_URI_TEXT)
    e.addAttribute(Attribute("class", null, prefixedName("footnotes")))
    e.appendChild(Element("hr", XHTML_URI_TEXT))

    for (fn in footnotes) {
      val c = fn.value.data.context
      val refs = c.footnoteReferencesForFootnote(fn.value)

      refs.forEach { r ->
        val ee = Element("div", XHTML_URI_TEXT)
        ee.addAttribute(
          Attribute("class", null, prefixedName("footnote_container")))
        val eid = Element("div", XHTML_URI_TEXT)
        eid.addAttribute(
          Attribute("class", null, prefixedName("footnote_id")))
        eid.appendChild("[")
        val eaa = Element("a", XHTML_URI_TEXT)
        eaa.addAttribute(
          Attribute("id", null, prov.footnoteAnchor(fn.value, r.key)))
        eaa.addAttribute(
          Attribute("href", null, prov.footnoteReferenceLink(r.value)))

        val title = StringBuilder()
        title.append("Jump back to reference ")
        title.append(r.value.index)
        title.append(" of footnote ")
        title.append(fn.key.value)
        eaa.addAttribute(Attribute("title", null, title.toString()))

        eaa.appendChild(fn.value.data.index.toString())
        eid.appendChild(eaa)
        eid.appendChild("]")

        val ec = Element("div", XHTML_URI_TEXT)
        ec.addAttribute(Attribute("class", null, prefixedName("footnote_body")))
        KSXOMSpacing.appendWithSpace(ec, fn.value.content, { c -> inline(prov, c) })

        ee.appendChild(eid)
        ee.appendChild(ec)
        e.appendChild(ee)
      }
    }

    return e
  }
}
