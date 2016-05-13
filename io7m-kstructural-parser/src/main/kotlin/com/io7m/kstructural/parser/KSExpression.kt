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
  override val position : Optional<LexicalPositionType<Path>>)
: KSLexicalType, SExpressionType {

  class KSExpressionSymbol(
    position : Optional<LexicalPositionType<Path>>,
    val value : String) : KSExpression(position), SExpressionSymbolType {

    override fun getLexicalInformation() : Optional<LexicalPositionType<Path>> =
      position

    override fun <A : Any, E : Exception> matchExpression(
      p0 : SExpressionMatcherType<A, E>) : A =
      p0.symbol(this)

    override fun getText() : String = value
    override fun toString() : String = value
  }

  class KSExpressionList(
    position : Optional<LexicalPositionType<Path>>,
    val square : Boolean,
    val elements : List<KSExpression>)
  : KSExpression(position), SExpressionListType {

    override fun size() : Int =
      elements.size

    override fun get(index : Int) : SExpressionType =
      elements.get(index)

    override fun isSquare() : Boolean = square

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append(if (square) "[" else "(")
      KSTextUtilities.concatenateInto(sb, this.elements)
      sb.append(if (square) "]" else ")")
      return sb.toString()
    }

    override fun getLexicalInformation() : Optional<LexicalPositionType<Path>> =
      position

    override fun <A : Any, E : Exception> matchExpression(
      p0 : SExpressionMatcherType<A, E>) : A =
      p0.list(this)
  }

  class KSExpressionQuoted(
    position : Optional<LexicalPositionType<Path>>,
    val value : String) : KSExpression(position), SExpressionQuotedStringType {

    override fun getLexicalInformation() : Optional<LexicalPositionType<Path>> =
      position

    override fun <A : Any, E : Exception> matchExpression(
      p0 : SExpressionMatcherType<A, E>) : A =
      p0.quotedString(this)

    override fun getText() : String = value
    override fun toString() : String = "\"" + value + "\""
  }

  companion object {
    fun of(e : SExpressionType) : KSExpression {
      return e.matchExpression(
        object : SExpressionMatcherType<KSExpression, UnreachableCodeException> {
          override fun list(e : SExpressionListType) : KSExpression {
            val xs = mutableListOf<KSExpression>()
            val max = e.size() - 1
            for (i in 0 .. max) {
              xs.add(of(e.get(i)))
            }
            return KSExpressionList(e.lexicalInformation, e.isSquare, xs)
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
