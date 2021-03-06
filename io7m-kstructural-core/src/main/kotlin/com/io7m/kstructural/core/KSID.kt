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
import java.util.regex.Pattern

class KSID<T> private constructor(
  override val position : Optional<LexicalPositionType<Path>>,
  val value : String,
  val data : T) : KSLexicalType {

  override fun equals(other : Any?) : Boolean {
    if (this === other) return true
    if (other?.javaClass != javaClass) return false
    other as KSID<*>
    return value == other.value
  }

  override fun hashCode() : Int = value.hashCode()
  override fun toString() : String = value

  companion object {

    @Throws(IllegalArgumentException::class)
    fun <T> create(
      position : Optional<LexicalPositionType<Path>>,
      value : String,
      data : T)
      : KSID<T> {
      if (isValidID(value)) {
        return KSID(position, value, data)
      } else {
        throw IllegalArgumentException("Not a valid identifier")
      }
    }

    val ID_FORMAT = Pattern.compile(
      "[\\p{IsLetter}\\p{IsDigit}_\\-\\.]+",
      Pattern.UNICODE_CHARACTER_CLASS);

    fun isValidID(s : String) : Boolean =
      ID_FORMAT.matcher(s).matches()

  }
}
