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

package com.io7m.kstructural.tests.core

import com.io7m.kstructural.core.KSResult
import net.java.quickcheck.Generator
import net.java.quickcheck.QuickCheck
import net.java.quickcheck.characteristic.AbstractCharacteristic
import net.java.quickcheck.generator.support.IntegerGenerator
import org.junit.Assert
import org.junit.Test
import java.util.ArrayDeque
import java.util.ArrayList
import java.util.Optional

class KSResultTest {

  class BooleanGenerator(val trues : Double) : Generator<Boolean> {
    override fun next() : Boolean = Math.random() <= trues
  }

  class AnyGenerator : Generator<Any> {
    override fun next() : Any = Object()
  }

  class KSResultGenerator<E : Any>(
    val bools : Generator<Boolean> = BooleanGenerator(0.5),
    val ints : Generator<Int> = IntegerGenerator(0, 8),
    val error_gen : Generator<E>)
  : Generator<KSResult<Int, E>> {
    override fun next() : KSResult<Int, E> {
      val partial =
        if (bools.next()) {
          Optional.of(ints.next())
        } else {
          Optional.empty()
        }

      val errors = ArrayDeque<E>()
      val error_count = ints.next()
      for (i in 0 .. error_count - 1) {
        errors.add(error_gen.next())
      }

      return if (bools.next()) {
        KSResult.KSFailure(partial, errors)
      } else {
        KSResult.succeed(ints.next())
      }
    }
  }

  private val hash0 : (Int) -> KSResult<Int, Any> = { x : Int ->
    val h = x.hashCode()
    if (h and 0b1 == 0b1) {
      KSResult.succeed(h)
    } else {
      KSResult.fail("Arbitrarily failed " + x)
    }
  }

  private val hash1 : (Int) -> KSResult<Int, Any> = { x : Int ->
    val h = x.hashCode()
    if (h and 0b1 != 0b1) {
      KSResult.succeed(h)
    } else {
      KSResult.fail("Arbitrarily failed " + x)
    }
  }

  @Test fun testMap() {
    val failOdd = { x : Int ->
      if (isEven(x)) {
        KSResult.succeed(x)
      } else {
        KSResult.failPartial(x, "Number is odd")
      }
    }

    val gen = IntegerGenerator(1, 100)
    val max = gen.nextInt()
    val xs = ArrayList<Int>(max)
    var odds = false
    for (i in 0 .. max) {
      val x = gen.nextInt()
      odds = odds || (!isEven(x))
      xs.add(x)
    }

    val r = KSResult.listMap(failOdd, xs)
    System.out.println(r)

    if (odds) {
      r as KSResult.KSFailure
      val ys = r.partial.get()
      Assert.assertEquals(xs, ys)
    } else {
      r as KSResult.KSSuccess
      val ys = r.result
      Assert.assertEquals(xs, ys)
    }
  }

  @Test fun testMapIndexed() {
    val indices = mutableSetOf<Int>()
    val failOddIndexed = { x : Int, i : Int ->
      indices.add(i)
      if (isEven(x)) {
        KSResult.succeed(x)
      } else {
        KSResult.failPartial(x, "Number is odd")
      }
    }

    val gen = IntegerGenerator(1, 100)
    val max = gen.nextInt()
    val xs = ArrayList<Int>(max)
    var odds = false
    for (i in 0 .. max) {
      val x = gen.nextInt()
      odds = odds || (!isEven(x))
      xs.add(x)
    }

    val r = KSResult.listMapIndexed(failOddIndexed, xs)
    System.out.println(r)

    for (i in 0 .. max) {
      Assert.assertTrue(indices.contains(i))
    }

    if (odds) {
      r as KSResult.KSFailure
      val ys = r.partial.get()
      Assert.assertEquals(xs, ys)
    } else {
      r as KSResult.KSSuccess
      val ys = r.result
      Assert.assertEquals(xs, ys)
    }
  }

  private fun isEven(x : Int) = x % 2 == 0

  /**
   * {@code return a >>= f ≡ f a}
   */

  @Test fun testLeftIdentity() {
    QuickCheck.forAllVerbose(IntegerGenerator(),
      object : AbstractCharacteristic<Int>() {
        override fun doSpecify(a : Int) {
          val f = hash0
          val r0 = KSResult.flatMap(KSResult.succeed(a), f)
          val r1 = f(a)
          Assert.assertEquals(r0, r1)
        }
      })
  }

  /**
   * {@code m >>= return ≡ m}
   */

  @Test fun testRightIdentity() {
    QuickCheck.forAllVerbose(KSResultGenerator(error_gen = AnyGenerator()),
      object : AbstractCharacteristic<KSResult<Int, Any>>() {
        override fun doSpecify(m : KSResult<Int, Any>) {
          val r0 = KSResult.flatMap(m, { KSResult.succeed<Int, Any>(it) })
          Assert.assertEquals(r0, m)
        }
      })
  }

  /**
   * {@code (m >>= f) >>= g ≡ m >>= (\x -> f x >>= g)}
   */

  @Test fun testAssociativity() {
    QuickCheck.forAllVerbose(KSResultGenerator(error_gen = AnyGenerator()),
      object : AbstractCharacteristic<KSResult<Int, Any>>() {
        override fun doSpecify(m : KSResult<Int, Any>) {
          val f = hash0
          val g = hash1
          val r0 = KSResult.flatMap(KSResult.flatMap(m, f), g)
          val r1 = KSResult.flatMap(m, { x -> KSResult.flatMap(f(x), g) })
          Assert.assertEquals(r0, r1)
        }
      })
  }

}

