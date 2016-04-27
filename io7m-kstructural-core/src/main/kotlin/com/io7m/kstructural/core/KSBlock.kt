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

package com.io7m.kstructural.core

import com.io7m.jlexing.core.LexicalPositionType
import java.nio.file.Path
import java.util.Optional

sealed class KSBlock<T>(
  override val position : Optional<LexicalPositionType<Path>>,
  val data : T)
: KSLexicalType, KSTypeableType, KSIDableType<T> {

  sealed class KSBlockSection<T>(
    position : Optional<LexicalPositionType<Path>>,
    data : T) : KSBlock<T>(position, data) {

    class KSBlockSectionWithSubsections<T>(
      position : Optional<LexicalPositionType<Path>>,
      data : T,
      override val type : Optional<String>,
      override val id : Optional<KSID<T>>,
      val title : List<KSInline.KSInlineText<T>>,
      val content : List<KSBlockSubsection<T>>) : KSBlockSection<T>(position, data) {

      override fun toString() : String {
        val sb = StringBuilder()
        sb.append("[section ")

        sb.append("[title ")
        KSTextUtilities.concatenateInto(sb, this.title)
        sb.append("]")

        if (type.isPresent) {
          sb.append(" [type ")
          sb.append(type.get())
          sb.append("]")
        }
        if (id.isPresent) {
          sb.append(" [id ")
          sb.append(id.get())
          sb.append("]")
        }

        sb.append(" ")
        KSTextUtilities.concatenateInto(sb, this.content)
        sb.append("]")
        return sb.toString()
      }
    }

    class KSBlockSectionWithContent<T>(
      position : Optional<LexicalPositionType<Path>>,
      data : T,
      override val type : Optional<String>,
      override val id : Optional<KSID<T>>,
      val title : List<KSInline.KSInlineText<T>>,
      val content : List<KSSubsectionContent<T>>) : KSBlockSection<T>(position, data) {

      override fun toString() : String {
        val sb = StringBuilder()
        sb.append("[section ")

        sb.append("[title ")
        KSTextUtilities.concatenateInto(sb, this.title)
        sb.append("]")

        if (type.isPresent) {
          sb.append(" [type ")
          sb.append(type.get())
          sb.append("]")
        }
        if (id.isPresent) {
          sb.append(" [id ")
          sb.append(id.get())
          sb.append("]")
        }

        sb.append(" ")
        KSTextUtilities.concatenateInto(sb, this.content)
        sb.append("]")
        return sb.toString()
      }
    }
  }

  class KSBlockSubsection<T>(
    position : Optional<LexicalPositionType<Path>>,
    data : T,
    override val type : Optional<String>,
    override val id : Optional<KSID<T>>,
    val title : List<KSInline.KSInlineText<T>>,
    val content : List<KSSubsectionContent<T>>)
  : KSBlock<T>(position, data) {

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append("[subsection ")

      sb.append("[title ")
      KSTextUtilities.concatenateInto(sb, this.title)
      sb.append("]")

      if (type.isPresent) {
        sb.append(" [type ")
        sb.append(type.get())
        sb.append("]")
      }
      if (id.isPresent) {
        sb.append(" [id ")
        sb.append(id.get())
        sb.append("]")
      }

      sb.append(" ")
      KSTextUtilities.concatenateInto(sb, this.content)
      sb.append("]")
      return sb.toString()
    }
  }

  class KSBlockParagraph<T>(
    position : Optional<LexicalPositionType<Path>>,
    data : T,
    override val type : Optional<String>,
    override val id : Optional<KSID<T>>,
    val content : List<KSInline<T>>)
  : KSBlock<T>(position, data) {

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append("[paragraph")
      if (type.isPresent) {
        sb.append(" [type ")
        sb.append(type.get())
        sb.append("]")
      }
      if (id.isPresent) {
        sb.append(" [id ")
        sb.append(id.get())
        sb.append("]")
      }

      sb.append(" ")
      KSTextUtilities.concatenateInto(sb, this.content)
      sb.append("]")
      return sb.toString()
    }
  }

  class KSBlockPart<T>(
    position : Optional<LexicalPositionType<Path>>,
    data : T,
    override val type : Optional<String>,
    override val id : Optional<KSID<T>>,
    val title : List<KSInline.KSInlineText<T>>,
    val content : List<KSBlockSection<T>>)
  : KSBlock<T>(position, data) {

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append("[part ")

      sb.append("[title ")
      KSTextUtilities.concatenateInto(sb, this.title)
      sb.append("]")

      if (type.isPresent) {
        sb.append(" [type ")
        sb.append(type.get())
        sb.append("]")
      }
      if (id.isPresent) {
        sb.append(" [id ")
        sb.append(id.get())
        sb.append("]")
      }

      sb.append(" ")
      KSTextUtilities.concatenateInto(sb, this.content)
      sb.append("]")
      return sb.toString()
    }
  }

}
