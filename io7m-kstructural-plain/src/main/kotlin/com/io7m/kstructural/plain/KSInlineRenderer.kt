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

import com.io7m.kstructural.core.KSElement
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineFootnoteReference
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineImage
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineLink
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineText
import com.io7m.kstructural.core.KSLink.KSLinkExternal
import com.io7m.kstructural.core.KSLink.KSLinkInternal
import com.io7m.kstructural.core.KSLinkContent
import com.io7m.kstructural.core.KSLinkContent.KSLinkImage
import com.io7m.kstructural.core.KSLinkContent.KSLinkText
import com.io7m.kstructural.core.evaluator.KSEvaluation
import java.io.Writer
import java.util.regex.Pattern

object KSInlineRenderer {

  private val TRAILING_WHITESPACE = Pattern.compile("\\s+$")

  fun dividerLight(output : Writer, width : Int) {
    divider('─', output, width)
  }

  fun dividerHeavy(output : Writer, width : Int) {
    divider('━', output, width)
  }

  fun divider(c : Char, output : Writer, width : Int) {
    for (i in 0 .. width - 1) output.write("" + c)
  }

  fun trimTrailing(text : String) : String =
    TRAILING_WHITESPACE.matcher(text).replaceAll("")

  fun text(text : KSInlineText<KSEvaluation>) : List<String> =
    text.text.split("\\s+")

  fun <T> image(content : KSInlineImage<T>) : List<String> {
    val xs = mutableListOf<String>()
    xs.add("[image:")
    xs.add(content.target.toString() + "]")

    content.content.forEachIndexed { index, item ->
      if (index == 0) {
        xs.add("(" + item)
      } else if (index == content.content.size - 1) {
        xs.add(item.text + ")")
      } else {
        xs.add(item.text)
      }
    }

    return xs.toList()
  }

  fun link(content : KSInlineLink<KSEvaluation>) : List<String> {
    val xs = mutableListOf<String>()

    val actual = content.actual
    return when (actual) {
      is KSLinkExternal -> {
        val subcontent = linkContents(actual.content)
        subcontent.forEachIndexed { index, item ->
          xs.add(item)
        }
        xs.add("[url:")
        xs.add(actual.target.toString() + "]")
        xs.toList()
      }
      is KSLinkInternal -> {
        val subcontent = linkContents(actual.content)
        subcontent.forEachIndexed { index, item ->
          xs.add(item)
        }
        xs.add("[ref:")
        xs.add(actual.target.toString() + "]")
        xs.toList()
      }
    }
  }

  private fun linkContents(
    content : List<KSLinkContent<KSEvaluation>>) : List<String> {
    val xs = mutableListOf<String>()
    content.forEach { c -> xs.addAll(linkContent(c)) }
    return xs.toList()
  }

  private fun linkContent(content : KSLinkContent<KSEvaluation>) : List<String> {
    return when (content) {
      is KSLinkText  -> text(content.actual)
      is KSLinkImage -> image(content.actual)
    }
  }

  fun footnoteReference(
    content : KSInlineFootnoteReference<KSEvaluation>) : String {
    return "[" + content.data.index + "]"
  }

  fun term(e_current : KSElement.KSInline.KSInlineTerm<KSEvaluation>) : List<String> =
    e_current.content.map { text -> text.text }

}