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

package com.io7m.kstructural.parser.imperative

import com.io7m.jlexing.core.LexicalPositionType
import com.io7m.kstructural.core.KSElement
import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSIDableType
import com.io7m.kstructural.core.KSLexicalType
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSTypeableType
import java.nio.file.Path
import java.util.Optional

sealed class KSImperative(
  override val position : Optional<LexicalPositionType<Path>>)
: KSLexicalType {

  sealed class KSImperativeCommand(
    position : Optional<LexicalPositionType<Path>>,
    val square : Boolean,
    override val type : Optional<String>,
    override val id : Optional<KSID<KSParse>>)
  : KSImperative(position), KSTypeableType, KSIDableType<KSParse> {

    class KSImperativeParagraph(
      position : Optional<LexicalPositionType<Path>>,
      square : Boolean,
      type : Optional<String>,
      id : Optional<KSID<KSParse>>)
    : KSImperativeCommand(position, square, type, id)
  }

  class KSImperativeInline(
    val value : KSElement.KSInline<KSParse>)
  : KSImperative(value.position)
}