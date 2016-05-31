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
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockPart
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection
import java.nio.file.Path
import java.util.Optional

sealed class KSDocumentContent<T>(
  override val position : Optional<LexicalPositionType<Path>>) : KSLexicalType {

  class KSDocumentSection<T>(val section : KSBlockSection<T>)
  : KSDocumentContent<T>(section.position) {
    override fun toString() : String = section.toString()
  }

  class KSDocumentPart<T>(val part : KSBlockPart<T>)
  : KSDocumentContent<T>(part.position) {
    override fun toString() : String = part.toString()
  }
}