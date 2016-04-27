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

package com.io7m.kstructural.parser

import com.io7m.jlexing.core.LexicalPositionType
import com.io7m.jsx.SExpressionListType
import com.io7m.jsx.SExpressionMatcherType
import com.io7m.jsx.SExpressionQuotedStringType
import com.io7m.jsx.SExpressionSymbolType
import com.io7m.jsx.SExpressionType
import com.io7m.junreachable.UnreachableCodeException
import com.io7m.kstructural.core.KSLexicalType
import com.io7m.kstructural.core.KSTextUtilities
import java.nio.file.Path
import java.util.Optional

sealed class KSExpression(
  override val position : Optional<LexicalPositionType<Path>>) : KSLexicalType {

  class KSExpressionSymbol(
    position : Optional<LexicalPositionType<Path>>,
    val text : String) : KSExpression(position) {
    override fun toString() : String = text
  }

  class KSExpressionList(
    position : Optional<LexicalPositionType<Path>>,
    val elements : List<KSExpression>) : KSExpression(position) {
    override fun toString() : String {
      val sb = StringBuilder()
      sb.append("[")
      KSTextUtilities.concatenateInto(sb, this.elements)
      sb.append("]")
      return sb.toString()
    }
  }

  class KSExpressionQuoted(
    position : Optional<LexicalPositionType<Path>>,
    val text : String) : KSExpression(position) {
    override fun toString() : String = "\"" + text + "\""
  }

  companion object {
    fun of(e : SExpressionType) : KSExpression {
      return e.matchExpression(
        object : SExpressionMatcherType<KSExpression, UnreachableCodeException> {
          override fun list(e : SExpressionListType) : KSExpression {
            return KSExpressionList(e.lexicalInformation, e.map { of(it) })
          }

          override fun quotedString(e : SExpressionQuotedStringType) : KSExpression {
            return KSExpressionQuoted(e.lexicalInformation, e.text)
          }

          override fun symbol(e : SExpressionSymbolType) : KSExpression {
            return KSExpressionSymbol(e.lexicalInformation, e.text)
          }
        })
    }
  }

}
