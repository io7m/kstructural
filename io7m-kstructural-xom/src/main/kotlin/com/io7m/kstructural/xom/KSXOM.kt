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
import com.io7m.kstructural.core.KSBlock
import com.io7m.kstructural.core.KSBlock.KSBlockDocument
import com.io7m.kstructural.core.KSBlock.KSBlockParagraph
import com.io7m.kstructural.core.KSBlock.KSBlockPart
import com.io7m.kstructural.core.KSBlock.KSBlockSection
import com.io7m.kstructural.core.KSBlock.KSBlockSubsection
import com.io7m.kstructural.core.KSInline
import com.io7m.kstructural.core.KSInline.KSInlineImage
import com.io7m.kstructural.core.KSInline.KSInlineLink
import com.io7m.kstructural.core.KSInline.KSInlineTerm
import com.io7m.kstructural.core.KSInline.KSInlineText
import com.io7m.kstructural.core.KSInline.KSInlineVerbatim
import com.io7m.kstructural.core.KSLink
import com.io7m.kstructural.core.KSLink.KSLinkExternal
import com.io7m.kstructural.core.KSLink.KSLinkInternal
import com.io7m.kstructural.core.KSLinkContent
import com.io7m.kstructural.core.KSTextUtilities
import com.io7m.kstructural.core.evaluator.KSEvaluation
import nu.xom.Attribute
import nu.xom.DocType
import nu.xom.Document
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.valid4j.Assertive
import java.net.URI

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

  fun newPage(d : KSBlockDocument<out Any>) : Pair<Document, Element> {
    val title = KSTextUtilities.concatenate(d.title)
    val rd = KSXOM.document()
    rd.rootElement.appendChild(KSXOM.head(title))
    val body = KSXOM.body()
    val body_container = KSXOM.bodyContainer()
    body.appendChild(body_container)
    rd.rootElement.appendChild(body)
    return Pair(rd, body_container);
  }

  fun body() : Element =
    Element("body", XHTML_URI_TEXT)

  fun head(title : String) : Element {
    val e = Element("head", XHTML_URI_TEXT)
    e.appendChild(title(title))
    e.appendChild(css(URI.create("kstructural-layout.css")))
    e.appendChild(css(URI.create("kstructural-colour.css")))
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

  fun sectionContainer(s : KSBlockSection<KSEvaluation>) : Element {
    val number_opt = s.data.number
    Assertive.require(number_opt.isPresent)
    val number = number_opt.get()
    val number_text = number.toAnchor()

    val sc = Element("div", XHTML_URI_TEXT)
    sc.addAttribute(Attribute("class", null, prefixedName("section_container")))

    s.id.ifPresent { id ->
      val stid_a = Element("a", XHTML_URI_TEXT)
      stid_a.addAttribute(Attribute("id", null, id.value))
      sc.appendChild(stid_a)
    }

    val stn = Element("div", XHTML_URI_TEXT)
    stn.addAttribute(Attribute("class", null, prefixedName("section_title_number")))

    val stn_a = Element("a", XHTML_URI_TEXT)
    stn_a.addAttribute(Attribute("id", null, prefixedName(number_text)))
    stn_a.addAttribute(Attribute("href", null, "#" + prefixedName(number_text)))
    stn_a.appendChild(number.toString())
    stn.appendChild(stn_a)

    val st = Element("div", XHTML_URI_TEXT)
    st.addAttribute(Attribute("class", null, prefixedName("section_title")))
    st.appendChild(KSTextUtilities.concatenate(s.title))

    sc.appendChild(stn)
    sc.appendChild(st)
    return sc
  }

  private fun prefixedName(s : String) : String {
    val sb = StringBuilder()
    sb.append(ATTRIBUTE_PREFIX)
    sb.append("_")
    sb.append(s)
    return sb.toString()
  }

  fun subsectionContainer(ss : KSBlockSubsection<KSEvaluation>) : Element {
    val number_opt = ss.data.number
    Assertive.require(number_opt.isPresent)
    val number = number_opt.get()
    val number_text = number.toAnchor()

    val sc = Element("div", XHTML_URI_TEXT)
    sc.addAttribute(Attribute("class", null, prefixedName("subsection_container")))

    ss.id.ifPresent { id ->
      val stid_a = Element("a", XHTML_URI_TEXT)
      stid_a.addAttribute(Attribute("id", null, id.value))
      sc.appendChild(stid_a)
    }

    val stn = Element("div", XHTML_URI_TEXT)
    stn.addAttribute(Attribute("class", null, prefixedName("subsection_title_number")))

    val stn_a = Element("a", XHTML_URI_TEXT)
    stn_a.addAttribute(Attribute("id", null, prefixedName(number_text)))
    stn_a.addAttribute(Attribute("href", null, "#" + prefixedName(number_text)))
    stn_a.appendChild(number.toString())
    stn.appendChild(stn_a)

    val st = Element("div", XHTML_URI_TEXT)
    st.addAttribute(Attribute("class", null, prefixedName("subsection_title")))
    st.appendChild(KSTextUtilities.concatenate(ss.title))

    sc.appendChild(stn)
    sc.appendChild(st)
    return sc
  }

  data class Paragraph(
    val container : Element,
    val content : Element)

  fun paragraphContainer(p : KSBlockParagraph<KSEvaluation>) : Paragraph {
    val number_opt = p.data.number
    Assertive.require(number_opt.isPresent)
    val number = number_opt.get()
    val number_text = number.toAnchor()

    val sc = Element("div", XHTML_URI_TEXT)
    sc.addAttribute(Attribute("class", null, prefixedName("paragraph_container")))

    p.id.ifPresent { id ->
      val stid_a = Element("a", XHTML_URI_TEXT)
      stid_a.addAttribute(Attribute("id", null, id.value))
      sc.appendChild(stid_a)
    }

    val stn = Element("div", XHTML_URI_TEXT)
    stn.addAttribute(Attribute("class", null, prefixedName("paragraph_number")))

    val stn_a = Element("a", XHTML_URI_TEXT)
    stn_a.addAttribute(Attribute("id", null, prefixedName(number_text)))
    stn_a.addAttribute(Attribute("href", null, "#" + prefixedName(number_text)))
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
      is KSInline.KSInlineLink     -> inlineLink(prov, c)
      is KSInline.KSInlineText     -> inlineText(c)
      is KSInline.KSInlineVerbatim -> inlineVerbatim(c)
      is KSInline.KSInlineTerm     -> inlineTerm(c)
      is KSInline.KSInlineImage    -> inlineImage(c)
    }

  fun linkContent(c : KSLinkContent<KSEvaluation>) : Node =
    when (c) {
      is KSLinkContent.KSLinkText  -> inlineText(c.actual)
      is KSLinkContent.KSLinkImage -> inlineImage(c.actual)
    }

  private fun inlineImage(c : KSInlineImage<KSEvaluation>) : Node {
    val classes = mutableListOf<String>()
    classes.add(prefixedName("image"))
    c.type.ifPresent { type -> classes.add(type) }
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
    c.type.ifPresent { type -> classes.add(type) }
    val classes_text = KSTextUtilities.concatenate(classes)

    val sc = Element("span", XHTML_URI_TEXT)
    sc.addAttribute(Attribute("class", null, classes_text))
    sc.appendChild(KSTextUtilities.concatenate(c.content))
    return sc
  }

  private fun inlineVerbatim(c : KSInlineVerbatim<KSEvaluation>) : Node {
    val classes = mutableListOf<String>()
    classes.add(prefixedName("verbatim"))
    c.type.ifPresent { type -> classes.add(type) }
    val classes_text = KSTextUtilities.concatenate(classes)

    val sc = Element("pre", XHTML_URI_TEXT)
    sc.addAttribute(Attribute("class", null, classes_text))
    sc.appendChild(c.text)
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
    sc.addAttribute(Attribute("href", null, prov.anchorForID(link.target)))
    inlinesAppend(sc, link.content, { c -> linkContent(c) })
    return sc
  }

  private fun linkExternal(link : KSLinkExternal<KSEvaluation>) : Node {
    val sc = Element("a", XHTML_URI_TEXT)
    sc.addAttribute(Attribute("class", null, prefixedName("link_external")))
    sc.addAttribute(Attribute("href", null, link.target.toString()))
    inlinesAppend(sc, link.content, { c -> linkContent(c) })
    return sc
  }

  fun inlineText(c : KSInlineText<KSEvaluation>) =
    Text(c.text)

  fun <T> inlinesAppend(
    e : Element,
    q : List<T>,
    f : (T) -> Node) : Element {

    val es = q.map { k -> f(k) }
    val max = q.size - 1
    for (i in 0 .. max) {
      val e_now = es[i]

      if (i > 0) {
        if (es[i - 1] is Text) {
          e.appendChild(" ")
        }
      }

      e.appendChild(e_now)
    }

    return e
  }

  fun partContainer(p : KSBlockPart<KSEvaluation>) : Element {
    val number_opt = p.data.number
    Assertive.require(number_opt.isPresent)
    val number = number_opt.get()
    val number_text = number.toAnchor()

    val sc = Element("div", XHTML_URI_TEXT)
    sc.addAttribute(Attribute("class", null, prefixedName("part_container")))

    p.id.ifPresent { id ->
      val stid_a = Element("a", XHTML_URI_TEXT)
      stid_a.addAttribute(Attribute("id", null, id.value))
      sc.appendChild(stid_a)
    }

    val stn = Element("div", XHTML_URI_TEXT)
    stn.addAttribute(Attribute("class", null, prefixedName("part_title_number")))

    val stn_a = Element("a", XHTML_URI_TEXT)
    stn_a.addAttribute(Attribute("id", null, prefixedName(number_text)))
    stn_a.addAttribute(Attribute("href", null, "#" + prefixedName(number_text)))
    stn_a.appendChild(number.toString())
    stn.appendChild(stn_a)

    val st = Element("div", XHTML_URI_TEXT)
    st.addAttribute(Attribute("class", null, prefixedName("part_title")))
    st.appendChild(KSTextUtilities.concatenate(p.title))

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
      block -> navigationBarCellTitle(block, titles_prev)
    }

    val titles_up = Element("td", XHTML_URI_TEXT)
    titles_up.addAttribute(
      Attribute("class", null, prefixedName("navbar_up_title_cell")))
    b.data.context.elementSegmentUp(b).ifPresent {
      block -> navigationBarCellTitle(block, titles_up)
    }

    val titles_next = Element("td", XHTML_URI_TEXT)
    titles_next.addAttribute(
      Attribute("class", null, prefixedName("navbar_next_title_cell")))
    b.data.context.elementSegmentNext(b).ifPresent {
      block -> navigationBarCellTitle(block, titles_next)
    }

    val titles = Element("tr", XHTML_URI_TEXT)
    titles.appendChild(titles_prev)
    titles.appendChild(titles_up)
    titles.appendChild(titles_next)

    val files_prev = Element("td", XHTML_URI_TEXT)
    files_prev.addAttribute(
      Attribute("class", null, prefixedName("navbar_prev_file_cell")))
    b.data.context.elementSegmentPrevious(b).ifPresent {
      block -> navigationBarCellFile(block, prov, files_prev, "previous")
    }

    val files_up = Element("td", XHTML_URI_TEXT)
    files_up.addAttribute(
      Attribute("class", null, prefixedName("navbar_up_file_cell")))
    b.data.context.elementSegmentUp(b).ifPresent {
      block -> navigationBarCellFile(block, prov, files_up, "up")
    }

    val files_next = Element("td", XHTML_URI_TEXT)
    files_next.addAttribute(
      Attribute("class", null, prefixedName("navbar_next_file_cell")))
    b.data.context.elementSegmentNext(b).ifPresent {
      block -> navigationBarCellFile(block, prov, files_next, "next")
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
    relation : String) {
    val e = Element("a", XHTML_URI_TEXT)
    e.addAttribute(Attribute("rel", null, relation))

    if (block.data.number.isPresent) {
      val number = block.data.number.get()
      e.addAttribute(Attribute("href", null, prov.anchorForNumber(number)))
    } else {
      e.addAttribute(Attribute("href", null, prov.anchorForDocument()))
    }

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

  fun sectionContents(
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
      is KSBlockSection.KSBlockSectionWithContent -> {

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
            Attribute("href", null, prov.anchorForNumber(ss_number)))

          val ss_a_text = StringBuilder()
          ss_a_text.append(ss_number.toString())
          ss_a_text.append(". ")
          KSTextUtilities.concatenateInto(ss_a_text, ss.title)
          ss_a.appendChild(ss_a_text.toString())
          ss_li.appendChild(ss_a)
          e.appendChild(ss_li)
        }
      }
    }

    return e
  }

  fun partContents(
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
        Attribute("href", null, prov.anchorForNumber(part_number)))

      val part_a_text = StringBuilder()
      part_a_text.append(part_number.toString())
      part_a_text.append(". ")
      KSTextUtilities.concatenateInto(part_a_text, s.title)
      part_a.appendChild(part_a_text.toString())
      part_li.appendChild(part_a)
      e.appendChild(part_li)

      when (s) {
        is KSBlockSection.KSBlockSectionWithContent -> {

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
              Attribute("href", null, prov.anchorForNumber(ss_number)))

            val ss_a_text = StringBuilder()
            ss_a_text.append(ss_number.toString())
            ss_a_text.append(". ")
            KSTextUtilities.concatenateInto(ss_a_text, ss.title)
            ss_a.appendChild(ss_a_text.toString())
            ss_li.appendChild(ss_a)
            part_li.appendChild(ss_li)
          }
        }
      }
    }

    return e
  }

  fun documentContents(
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
            Attribute("href", null, prov.anchorForNumber(part_number)))

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
              Attribute("href", null, prov.anchorForNumber(s_number)))

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
            Attribute("href", null, prov.anchorForNumber(s_number)))

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
}
