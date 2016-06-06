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

package com.io7m.kstructural.parser.imperative

import com.io7m.jlexing.core.LexicalPositionType
import com.io7m.kstructural.core.KSElement
import com.io7m.kstructural.core.KSElement.KSBlock
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockImport
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineText
import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSIDableType
import com.io7m.kstructural.core.KSLexicalType
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSType
import com.io7m.kstructural.core.KSTypeableType
import java.nio.file.Path
import java.util.Optional

sealed class KSImperative(
  override val position : Optional<LexicalPositionType<Path>>)
: KSLexicalType {

  sealed class KSImperativeCommand(
    private val name : String,
    position : Optional<LexicalPositionType<Path>>,
    val square : Boolean,
    override val type : Optional<KSType<KSParse>>,
    override val id : Optional<KSID<KSParse>>)
  : KSImperative(position), KSTypeableType<KSParse>, KSIDableType<KSParse> {

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append(if (square) "[" else "(")
      sb.append(name)
      sb.append(" ")
      id.ifPresent { id ->
        sb.append("[id ")
        sb.append(id.value)
        sb.append("]")
      }
      type.ifPresent { type ->
        sb.append("[type ")
        sb.append(type.value)
        sb.append("]")
      }
      sb.append(if (square) "]" else ")")
      return sb.toString()
    }

    class KSImperativeParagraph(
      position : Optional<LexicalPositionType<Path>>,
      square : Boolean,
      type : Optional<KSType<KSParse>>,
      id : Optional<KSID<KSParse>>)
    : KSImperativeCommand("paragraph", position, square, type, id)

    class KSImperativeFootnote(
      position : Optional<LexicalPositionType<Path>>,
      square : Boolean,
      type : Optional<KSType<KSParse>>,
      val id_real : KSID<KSParse>)
    : KSImperativeCommand("footnote", position, square, type, Optional.of(id_real))

    class KSImperativeDocument(
      position : Optional<LexicalPositionType<Path>>,
      square : Boolean,
      type : Optional<KSType<KSParse>>,
      id : Optional<KSID<KSParse>>,
      val title : List<KSInlineText<KSParse>>)
    : KSImperativeCommand("document", position, square, type, id)

    class KSImperativePart(
      position : Optional<LexicalPositionType<Path>>,
      square : Boolean,
      type : Optional<KSType<KSParse>>,
      id : Optional<KSID<KSParse>>,
      val title : List<KSInlineText<KSParse>>)
    : KSImperativeCommand("part", position, square, type, id)

    class KSImperativeSection(
      position : Optional<LexicalPositionType<Path>>,
      square : Boolean,
      type : Optional<KSType<KSParse>>,
      id : Optional<KSID<KSParse>>,
      val title : List<KSInlineText<KSParse>>)
    : KSImperativeCommand("section", position, square, type, id)

    class KSImperativeSubsection(
      position : Optional<LexicalPositionType<Path>>,
      square : Boolean,
      type : Optional<KSType<KSParse>>,
      id : Optional<KSID<KSParse>>,
      val title : List<KSInlineText<KSParse>>)
    : KSImperativeCommand("subsection", position, square, type, id)

    class KSImperativeFormalItem(
      position : Optional<LexicalPositionType<Path>>,
      square : Boolean,
      type : Optional<KSType<KSParse>>,
      id : Optional<KSID<KSParse>>,
      val title : List<KSInlineText<KSParse>>)
    : KSImperativeCommand("formal-item", position, square, type, id)

    class KSImperativeImport(
      position : Optional<LexicalPositionType<Path>>,
      square : Boolean,
      val import : KSBlockImport<KSParse>,
      val content : KSBlock<KSParse>)
    : KSImperativeCommand(
      "import", position, square, Optional.empty(), Optional.empty())
  }

  class KSImperativeEOF(
    position : Optional<LexicalPositionType<Path>>)
  : KSImperative(position) {
    override fun toString() : String =
      "EOF"
  }

  class KSImperativeInline(
    val value : KSElement.KSInline<KSParse>)
  : KSImperative(value.position) {
    override fun toString() : String = value.toString()
  }

}