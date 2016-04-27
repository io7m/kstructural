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

import java.util.ArrayDeque
import java.util.Deque
import java.util.Optional

sealed class KSParseResult<A : Any> {

  class KSParseSuccess<A : Any>(
    val result : A) : KSParseResult<A>() {

    override fun equals(other : Any?) : Boolean {
      if (this === other) return true
      if (other?.javaClass != javaClass) return false
      other as KSParseSuccess<*>
      return result == other.result
    }

    override fun hashCode() : Int =
      result.hashCode()

    override fun toString() : String {
      return "[KSParseSuccess $result]"
    }
  }

  class KSParseFailure<A : Any>(
    val partial : Optional<A>,
    val errors : Deque<KSParseError>) : KSParseResult<A>() {

    override fun equals(other : Any?) : Boolean {
      if (this === other) return true
      if (other?.javaClass != javaClass) return false
      other as KSParseFailure<*>
      return partial == other.partial
    }

    override fun hashCode() : Int =
      partial.hashCode()

    override fun toString() : String {
      return "[KSParseFailure [$errors] [$partial]]"
    }
  }

  infix fun <B : Any> flatMap(f : (A) -> KSParseResult<B>) : KSParseResult<B> =
    flatMap (this, f)

  companion object {

    fun <A : Any> succeed(x : A) : KSParseResult<A> =
      KSParseSuccess(x)

    fun <A : Any> fail(e : KSParseError) : KSParseResult<A> {
      val es = ArrayDeque<KSParseError>()
      es.add(e)
      return KSParseFailure(Optional.empty(), es)
    }

    fun <A : Any> failPartial(x : A, e : KSParseError) : KSParseResult<A> {
      val es = ArrayDeque<KSParseError>()
      es.add(e)
      return KSParseFailure(Optional.of(x), es)
    }

    fun <A : Any, B : Any> flatMap(
      x : KSParseResult<A>, f : (A) -> KSParseResult<B>) : KSParseResult<B> {
      return when (x) {
        is KSParseResult.KSParseSuccess -> {
          return f(x.result)
        }
        is KSParseResult.KSParseFailure ->
          if (x.partial.isPresent) {
            val rr = f(x.partial.get())
            return when (rr) {
              is KSParseResult.KSParseSuccess ->
                KSParseFailure(Optional.of(rr.result), x.errors)
              is KSParseResult.KSParseFailure -> {
                val ed = ArrayDeque<KSParseError>()
                ed.addAll(x.errors)
                ed.addAll(rr.errors)
                KSParseFailure(rr.partial, ed)
              }
            }
          } else {
            KSParseFailure(Optional.empty<B>(), x.errors)
          }
      }
    }

    fun <A : Any, B : Any> map(
      f : (A) -> KSParseResult<B>, xs : List<A>) : KSParseResult<List<B>> {
      val rs = xs.map(f)
      var current = KSParseResult.succeed(listOf<B>())
      val max = rs.size - 1
      for (i in 0 .. max) {
        current = current flatMap { bs ->
          rs[i] flatMap { b ->
            KSParseResult.succeed(bs.plus(b))
          }
        }
      }
      return current
    }
  }
}
