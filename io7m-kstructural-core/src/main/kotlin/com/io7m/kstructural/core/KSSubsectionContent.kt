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
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockFootnote
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockFormalItem
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockParagraph
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockImport
import java.nio.file.Path
import java.util.Optional

sealed class KSSubsectionContent<T>(
  override val position : Optional<LexicalPositionType<Path>>) : KSLexicalType {

  class KSSubsectionParagraph<T>(val paragraph : KSBlockParagraph<T>)
  : KSSubsectionContent<T>(paragraph.position) {
    override fun toString() : String = paragraph.toString()
  }

  class KSSubsectionFormalItem<T>(val formal : KSBlockFormalItem<T>)
  : KSSubsectionContent<T>(formal.position) {
    override fun toString() : String = formal.toString()
  }

  class KSSubsectionFootnote<T>(val footnote : KSBlockFootnote<T>)
  : KSSubsectionContent<T>(footnote.position) {
    override fun toString() : String = footnote.toString()
  }

  class KSSubsectionImport<T>(val import : KSBlockImport<T>)
  : KSSubsectionContent<T>(import.position) {
    override fun toString() : String = import.toString()
  }
}
