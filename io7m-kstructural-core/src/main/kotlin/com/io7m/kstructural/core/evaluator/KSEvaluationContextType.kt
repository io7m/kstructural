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

package com.io7m.kstructural.core.evaluator

import com.io7m.kstructural.core.KSElement
import com.io7m.kstructural.core.KSElement.KSBlock
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockFootnote
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockImport
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineFootnoteReference
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineInclude
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineText
import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSImportPathEdge
import java.util.Optional

interface KSEvaluationContextType {

  val document : KSBlockDocument<KSEvaluation>

  val includesByText : Map<KSInlineText<KSEvaluation>, KSInlineInclude<KSEvaluation>>

  val imports : Map<KSBlock<KSEvaluation>, KSBlockImport<KSEvaluation>>

  val importedBlocks : Map<KSBlockImport<KSEvaluation>, KSBlock<KSEvaluation>>

  val importsPaths : Map<KSBlockImport<KSEvaluation>, KSImportPathEdge>

  fun elementForID(
    id : KSID<KSEvaluation>) : KSBlock<KSEvaluation>

  fun elementForNumber(
    n : KSNumber) : KSBlock<KSEvaluation>

  fun elementForSerial(
    s : KSSerial) : Optional<KSElement<KSEvaluation>>

  fun elementSegmentPrevious(
    b : KSBlock<KSEvaluation>) : Optional<KSBlock<KSEvaluation>>

  fun elementSegmentNext(
    b : KSBlock<KSEvaluation>) : Optional<KSBlock<KSEvaluation>>

  fun elementSegmentUp(
    b : KSBlock<KSEvaluation>) : Optional<KSBlock<KSEvaluation>>

  fun footnotesForSection(
    n : KSNumber.HasSectionType) : Map<KSID<KSEvaluation>, KSBlockFootnote<KSEvaluation>>

  val footnotesAll : Map<KSID<KSEvaluation>, KSBlockFootnote<KSEvaluation>>

  fun footnoteReferenceForInline(
    f : KSInlineFootnoteReference<KSEvaluation>) : KSFootnoteReference<KSEvaluation>

  fun footnoteReferencesForFootnote(
    f : KSBlockFootnote<KSEvaluation>) : Map<Long, KSFootnoteReference<KSEvaluation>>
}
