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

package com.io7m.kstructural.plain

import com.io7m.jorchard.core.JOTreeNodeType
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockFootnote
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockFormalItem
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockParagraph
import com.io7m.kstructural.core.evaluator.KSEvaluation

interface KSPlainLayoutType {

  fun layoutParagraph(
    page_width : Int,
    paragraph : KSBlockParagraph<KSEvaluation>)
    : JOTreeNodeType<KSPlainLayoutBox>

  fun layoutFormal(
    page_width : Int,
    formal : KSBlockFormalItem<KSEvaluation>)
    : JOTreeNodeType<KSPlainLayoutBox>

  fun layoutFootnote(
    page_width : Int,
    footnote : KSBlockFootnote<KSEvaluation>)
    : JOTreeNodeType<KSPlainLayoutBox>

}