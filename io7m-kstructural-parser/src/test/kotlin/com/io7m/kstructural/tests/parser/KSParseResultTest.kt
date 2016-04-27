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

package com.io7m.kstructural.tests.parser

import com.io7m.jlexing.core.ImmutableLexicalPosition
import com.io7m.jlexing.core.LexicalPositionType
import com.io7m.kstructural.parser.KSParseError
import com.io7m.kstructural.parser.KSParseResult
import com.io7m.kstructural.parser.KSParseResult.KSParseFailure
import com.io7m.kstructural.parser.KSParseResult.KSParseSuccess
import net.java.quickcheck.Generator
import net.java.quickcheck.QuickCheck
import net.java.quickcheck.characteristic.AbstractCharacteristic
import net.java.quickcheck.generator.support.IntegerGenerator
import net.java.quickcheck.generator.support.StringGenerator
import org.junit.Assert
import org.junit.Test
import java.nio.file.Path
import java.util.ArrayDeque
import java.util.ArrayList
import java.util.Optional

class KSParseResultTest {

  class BooleanGenerator(val trues : Double) : Generator<Boolean> {
    override fun next() : Boolean = Math.random() <= trues
  }

  class KSParseErrorGenerator(
    val bools : Generator<Boolean> = BooleanGenerator(0.5),
    val ints : Generator<Int> = IntegerGenerator(),
    val msgs : Generator<String> = StringGenerator()) : Generator<KSParseError> {
    override fun next() : KSParseError {
      val pos : Optional<LexicalPositionType<Path>> = if (bools.next()) {
        Optional.of(ImmutableLexicalPosition.newPosition<Path>(ints.next(), ints.next()))
      } else {
        Optional.empty()
      }
      return KSParseError(pos, msgs.next())
    }
  }

  class KSParseResultGenerator(
    val bools : Generator<Boolean> = BooleanGenerator(0.5),
    val ints : Generator<Int> = IntegerGenerator(0, 8),
    val error_gen : Generator<KSParseError> = KSParseErrorGenerator())
  : Generator<KSParseResult<Int>> {
    override fun next() : KSParseResult<Int> {
      val partial =
        if (bools.next()) {
          Optional.of(ints.next())
        } else {
          Optional.empty()
        }

      val errors = ArrayDeque<KSParseError>()
      val error_count = ints.next()
      for (i in 0 .. error_count - 1) {
        errors.add(error_gen.next())
      }

      return if (bools.next()) {
        KSParseFailure(partial, errors)
      } else {
        KSParseResult.succeed(ints.next())
      }
    }
  }

  private val hash0 = { x : Int ->
    val h = x.hashCode()
    if (h and 0b1 == 0b1) {
      KSParseResult.succeed(h)
    } else {
      KSParseResult.fail(KSParseError(Optional.empty(), "Arbitrarily failed " + x))
    }
  }

  private val hash1 = { x : Int ->
    val h = x.hashCode()
    if (h and 0b1 != 0b1) {
      KSParseResult.succeed(h)
    } else {
      KSParseResult.fail(KSParseError(Optional.empty(), "Arbitrarily failed " + x))
    }
  }

  @Test fun testMap() {
    val failOdd = { x : Int ->
      if (isEven(x)) {
        KSParseResult.succeed(x)
      } else {
        KSParseResult.failPartial(x, KSParseError(Optional.empty(), "Number is odd"))
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

    val r = KSParseResult.map(failOdd, xs)
    System.out.println(r)

    if (odds) {
      r as KSParseResult.KSParseFailure
      val ys = r.partial.get()
      Assert.assertEquals(xs, ys)
    } else {
      r as KSParseSuccess
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
          val r0 = KSParseResult.flatMap(KSParseResult.succeed(a), f)
          val r1 = f(a)
          Assert.assertEquals(r0, r1)
        }
      })
  }

  /**
   * {@code m >>= return ≡ m}
   */

  @Test fun testRightIdentity() {
    QuickCheck.forAllVerbose(KSParseResultGenerator(),
      object : AbstractCharacteristic<KSParseResult<Int>>() {
        override fun doSpecify(m : KSParseResult<Int>) {
          val r0 = KSParseResult.flatMap(m, { KSParseResult.succeed(it) })
          Assert.assertEquals(r0, m)
        }
      })
  }

  /**
   * {@code (m >>= f) >>= g ≡ m >>= (\x -> f x >>= g)}
   */

  @Test fun testAssociativity() {
    QuickCheck.forAllVerbose(KSParseResultGenerator(),
      object : AbstractCharacteristic<KSParseResult<Int>>() {
        override fun doSpecify(m : KSParseResult<Int>) {
          val f = hash0
          val g = hash1
          val r0 = KSParseResult.flatMap(KSParseResult.flatMap(m, f), g)
          val r1 = KSParseResult.flatMap(m, { x -> KSParseResult.flatMap(f(x), g) })
          Assert.assertEquals(r0, r1)
        }
      })
  }

}
