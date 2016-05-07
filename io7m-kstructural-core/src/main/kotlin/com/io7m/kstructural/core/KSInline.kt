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
import java.math.BigInteger
import java.net.URI
import java.nio.file.Path
import java.util.Optional

sealed class KSInline<T>(
  override val position : Optional<LexicalPositionType<Path>>,
  val data : T) : KSLexicalType {

  class KSInlineLink<T>(
    position : Optional<LexicalPositionType<Path>>,
    data : T,
    val actual : KSLink<T>)
  : KSInline<T>(position, data) {
    override fun toString() : String = actual.toString()
  }

  class KSInlineText<T>(
    position : Optional<LexicalPositionType<Path>>,
    data : T,
    val text : String) : KSInline<T>(position, data) {
    override fun toString() : String = text
  }

  class KSInlineVerbatim<T>(
    position : Optional<LexicalPositionType<Path>>,
    data : T,
    override val type : Optional<String>,
    val text : String) : KSInline<T>(position, data), KSTypeableType {

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append("[verbatim ")
      if (type.isPresent) {
        sb.append("[type ")
        sb.append(type.get())
        sb.append("] ")
      }
      sb.append("\"")

      val max = text.length - 1
      for (i in 0 .. max) {
        val c = text.get(i)
        if (c == '"') {
          sb.append("\\\"")
        } else {
          sb.append(c)
        }
      }

      sb.append("\"")
      sb.append("]")
      return sb.toString()
    }
  }

  class KSInlineTerm<T>(
    position : Optional<LexicalPositionType<Path>>,
    data : T,
    override val type : Optional<String>,
    val content : List<KSInlineText<T>>)
  : KSInline<T>(position, data), KSTypeableType {

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append("[term ")
      if (type.isPresent) {
        sb.append("[type ")
        sb.append(type.get())
        sb.append("] ")
      }

      KSTextUtilities.concatenateInto(sb, this.content)
      sb.append("]")
      return sb.toString()
    }
  }

  data class KSSize(
    val width : BigInteger,
    val height : BigInteger)

  class KSInlineImage<T>(
    position : Optional<LexicalPositionType<Path>>,
    data : T,
    override val type : Optional<String>,
    val target : URI,
    val size : Optional<KSSize>,
    val content : List<KSInlineText<T>>)
  : KSInline<T>(position, data), KSTypeableType {

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append("[image ")
      if (type.isPresent) {
        sb.append("[type ")
        sb.append(type.get())
        sb.append("] ")
      }

      KSTextUtilities.concatenateInto(sb, this.content)
      sb.append("]")
      return sb.toString()
    }
  }

  class KSListItem<T>(
    override val position : Optional<LexicalPositionType<Path>>,
    val data : T,
    val content : List<KSInline<T>>) : KSLexicalType {

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append("[item ")
      KSTextUtilities.concatenateInto(sb, this.content)
      sb.append("]")
      return sb.toString()
    }
  }

  class KSInlineListOrdered<T>(
    position : Optional<LexicalPositionType<Path>>,
    data : T,
    val content : List<KSListItem<T>>) : KSInline<T>(position, data) {

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append("[list-ordered ")
      KSTextUtilities.concatenateInto(sb, this.content)
      sb.append("]")
      return sb.toString()
    }
  }

  class KSInlineListUnordered<T>(
    position : Optional<LexicalPositionType<Path>>,
    data : T,
    val content : List<KSListItem<T>>) : KSInline<T>(position, data) {

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append("[list-unordered ")
      KSTextUtilities.concatenateInto(sb, this.content)
      sb.append("]")
      return sb.toString()
    }
  }

  class KSTableHeadColumnName<T>(
    override val position : Optional<LexicalPositionType<Path>>,
    val data : T,
    val content : List<KSInlineText<T>>) : KSLexicalType {

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append("[name ")
      KSTextUtilities.concatenateInto(sb, this.content)
      sb.append("]")
      return sb.toString()
    }
  }

  class KSTableHead<T>(
    override val position : Optional<LexicalPositionType<Path>>,
    val data : T,
    val column_names : List<KSTableHeadColumnName<T>>) : KSLexicalType {

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append("[head ")
      KSTextUtilities.concatenateInto(sb, this.column_names)
      sb.append("]")
      return sb.toString()
    }
  }

  class KSTableBodyCell<T>(
    override val position : Optional<LexicalPositionType<Path>>,
    val data : T,
    val content : List<KSInline<T>>) : KSLexicalType {

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append("[cell ")
      KSTextUtilities.concatenateInto(sb, this.content)
      sb.append("]")
      return sb.toString()
    }
  }

  class KSTableBodyRow<T>(
    override val position : Optional<LexicalPositionType<Path>>,
    val data : T,
    val cells : List<KSTableBodyCell<T>>) : KSLexicalType {

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append("[row ")
      KSTextUtilities.concatenateInto(sb, this.cells)
      sb.append("]")
      return sb.toString()
    }
  }

  class KSTableBody<T>(
    override val position : Optional<LexicalPositionType<Path>>,
    val data : T,
    val rows : List<KSTableBodyRow<T>>) : KSLexicalType {

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append("[body ")
      KSTextUtilities.concatenateInto(sb, this.rows)
      sb.append("]")
      return sb.toString()
    }
  }

  class KSTableSummary<T>(
    override val position : Optional<LexicalPositionType<Path>>,
    val data : T,
    val content : List<KSInlineText<T>>) : KSLexicalType {

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append("[summary ")
      KSTextUtilities.concatenateInto(sb, this.content)
      sb.append("]")
      return sb.toString()
    }
  }

  class KSInlineTable<T>(
    position : Optional<LexicalPositionType<Path>>,
    data : T,
    override val type : Optional<String>,
    val summary : KSTableSummary<T>,
    val head : Optional<KSTableHead<T>>,
    val body : KSTableBody<T>) : KSInline<T>(position, data), KSTypeableType {

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append("[table ")
      type.ifPresent { t ->
        sb.append(t)
        sb.append(" ")
      }
      sb.append(summary)
      sb.append(" ")
      head.ifPresent { h ->
        sb.append(h)
        sb.append(" ")
      }
      sb.append(body)
      sb.append("]")
      return sb.toString()
    }
  }
}
