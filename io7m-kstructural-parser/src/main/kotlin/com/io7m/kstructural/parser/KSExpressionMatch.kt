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

sealed class KSExpressionMatch {

  class MatchAny : KSExpressionMatch() {
    override fun toString() : String = "*"
  }

  class MatchOneOf(
    val cases : List<KSExpressionMatch>) : KSExpressionMatch() {
    override fun toString() : String {
      val sb = StringBuilder()
      sb.append("{")
      val max = cases.size - 1
      for (i in 0 .. max) {
        sb.append(cases[i])
        if (i < max) {
          sb.append(" | ")
        }
      }
      sb.append("}")
      return sb.toString()
    }
  }

  class MatchSymbol(
    val name : (String) -> Boolean,
    val description : String) : KSExpressionMatch() {
    override fun toString() : String = "symbol:" + description
  }

  class MatchString(
    val content : (String) -> Boolean,
    val description : String) : KSExpressionMatch() {
    override fun toString() : String = "string:" + description
  }

  sealed class CheckPrefix {
    object Prefix : CheckPrefix()

    object All : CheckPrefix()
  }

  class MatchList(
    val prefix : CheckPrefix,
    val elements : List<KSExpressionMatch>) : KSExpressionMatch() {
    override fun toString() : String {
      val sb = StringBuilder()
      sb.append("[")
      val max = elements.size - 1
      for (i in 0 .. max) {
        sb.append(elements[i])
        if (i < max) {
          sb.append(" ")
        }
      }

      when (prefix) {
        is CheckPrefix.Prefix -> sb.append(" ... ")
        is CheckPrefix.All    -> Unit
      }

      sb.append("]")
      return sb.toString()
    }
  }

  companion object {

    fun anything() =
      MatchAny()

    fun exactSymbol(s : String) =
      MatchSymbol({ t -> t == s }, s)

    fun anySymbol() =
      MatchSymbol({ s -> true }, "*")

    fun anyString() =
      MatchString({ s -> true }, "*")

    fun oneOf(xs : List<KSExpressionMatch>) =
      MatchOneOf(xs)

    fun allOfList(xs : List<KSExpressionMatch>) =
      MatchList(CheckPrefix.All, xs)

    fun prefixOfList(xs : List<KSExpressionMatch>) =
      MatchList(CheckPrefix.Prefix, xs)

    fun matches(e : KSExpression, m : KSExpressionMatch) : Boolean {
      when (e) {
        is KSExpression.KSExpressionSymbol ->
          return when (m) {
            is MatchSymbol -> m.name.invoke(e.text)
            is MatchList   -> false
            is MatchAny    -> true
            is MatchString -> false
            is MatchOneOf  -> {
              for (i in 0 .. m.cases.size - 1) {
                if (matches(e, m.cases[i])) {
                  return true
                }
              }
              return false
            }
          }
        is KSExpression.KSExpressionList   ->
          return when (m) {
            is MatchSymbol -> false
            is MatchList   -> {

              /**
               * When checking only prefixes, there must be either more or the same amount of list
               * elements as there are match elements.
               */

              val check =
                when (m.prefix) {
                  is CheckPrefix.Prefix -> e.elements.size >= m.elements.size
                  is CheckPrefix.All    -> e.elements.size == m.elements.size
                }

              if (check) {
                val max = Math.min(e.elements.size, m.elements.size)
                for (i in 0 .. max - 1) {
                  if (!matches(e.elements[i], m.elements[i])) {
                    return false
                  }
                }
                true
              } else {
                false
              }
            }
            is MatchAny    -> true
            is MatchString -> false
            is MatchOneOf  -> {
              for (i in 0 .. m.cases.size - 1) {
                if (matches(e, m.cases[i])) {
                  return true
                }
              }
              return false
            }
          }
        is KSExpression.KSExpressionQuoted ->
          return when (m) {
            is MatchSymbol -> false
            is MatchList   -> false
            is MatchAny    -> true
            is MatchString -> m.content.invoke(e.text)
            is MatchOneOf  -> {
              for (i in 0 .. m.cases.size - 1) {
                if (matches(e, m.cases[i])) {
                  return true
                }
              }
              return false
            }
          }
      }
    }
  }
}
