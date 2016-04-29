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

import java.util.ArrayDeque
import java.util.ArrayList
import java.util.Deque
import java.util.Optional

sealed class KSResult<out A : Any, E : Any> {

  class KSSuccess<A : Any, E : Any>(
    val result : A) : KSResult<A, E>() {

    override fun equals(other : Any?) : Boolean {
      if (this === other) return true
      if (other?.javaClass != javaClass) return false
      other as KSSuccess<*, *>
      return result == other.result
    }

    override fun hashCode() : Int =
      result.hashCode()

    override fun toString() : String {
      return "[KSSuccess $result]"
    }
  }

  class KSFailure<A : Any, E : Any>(
    val partial : Optional<A>,
    val errors : Deque<E>) : KSResult<A, E>() {

    override fun equals(other : Any?) : Boolean {
      if (this === other) return true
      if (other?.javaClass != javaClass) return false
      other as KSFailure<*, *>
      return partial == other.partial
    }

    override fun hashCode() : Int =
      partial.hashCode()

    override fun toString() : String {
      return "[KSFailure [$errors] [$partial]]"
    }
  }

  infix fun <B : Any> flatMap(f : (A) -> KSResult<B, E>) : KSResult<B, E> =
    flatMap (this, f)

  infix fun <B : Any> map(f : (A) -> B) : KSResult<B, E> =
    map (this, f)

  companion object {

    fun <A : Any, E : Any> succeed(x : A) : KSResult<A, E> =
      KSSuccess(x)

    fun <A : Any, E : Any> fail(e : E) : KSResult<A, E> {
      val es = ArrayDeque<E>()
      es.add(e)
      return KSFailure(Optional.empty(), es)
    }

    fun <A : Any, E : Any> failPartial(x : A, e : E) : KSResult<A, E> {
      val es = ArrayDeque<E>()
      es.add(e)
      return KSFailure(Optional.of(x), es)
    }

    fun <A : Any, B : Any, E : Any> map(
      x : KSResult<A, E>, f : (A) -> B) : KSResult<B, E> {
      return when (x) {
        is KSSuccess -> {
          KSSuccess(f.invoke(x.result))
        }
        is KSFailure ->
          if (x.partial.isPresent) {
            KSFailure(Optional.of(f.invoke(x.partial.get())), x.errors)
          } else {
            KSFailure(Optional.empty<B>(), x.errors)
          }
      }
    }

    fun <A : Any, B : Any, E : Any> flatMap(
      x : KSResult<A, E>, f : (A) -> KSResult<B, E>) : KSResult<B, E> {
      return when (x) {
        is KSSuccess -> {
          return f(x.result)
        }
        is KSFailure ->
          if (x.partial.isPresent) {
            val rr = f(x.partial.get())
            return when (rr) {
              is KSSuccess ->
                KSFailure(Optional.of(rr.result), x.errors)
              is KSFailure -> {
                val ed = ArrayDeque<E>()
                ed.addAll(x.errors)
                ed.addAll(rr.errors)
                KSFailure(rr.partial, ed)
              }
            }
          } else {
            KSFailure(Optional.empty<B>(), x.errors)
          }
      }
    }

    fun <A : Any, B : Any, E : Any> listMap(
      f : (A) -> KSResult<B, E>, xs : List<A>) : KSResult<List<B>, E> {

      var fail = false
      val out = ArrayList<B>(xs.size)
      val err = ArrayDeque<E>(xs.size)
      val max = xs.size - 1
      for (i in 0 .. max) {
        val r = f(xs[i])
        when (r) {
          is KSSuccess -> {
            out.add(r.result)
          }
          is KSFailure -> {
            fail = true
            if (r.partial.isPresent) {
              out.add(r.partial.get())
            }
            err.addAll(r.errors)
          }
        }
      }

      return if (fail) {
        KSFailure(Optional.of(out as List<B>), err)
      } else {
        KSSuccess(out)
      }
    }

    fun <A : Any, B : Any, E : Any> listMapIndexed(
      f : (A, Int) -> KSResult<B, E>, xs : List<A>) : KSResult<List<B>, E> {

      var fail = false
      val out = ArrayList<B>(xs.size)
      val err = ArrayDeque<E>(xs.size)
      val max = xs.size - 1
      for (i in 0 .. max) {
        val r = f(xs[i], i)
        when (r) {
          is KSSuccess -> {
            out.add(r.result)
          }
          is KSFailure -> {
            fail = true
            if (r.partial.isPresent) {
              out.add(r.partial.get())
            }
            err.addAll(r.errors)
          }
        }
      }

      return if (fail) {
        KSFailure(Optional.of(out as List<B>), err)
      } else {
        KSSuccess(out)
      }
    }
  }
}
