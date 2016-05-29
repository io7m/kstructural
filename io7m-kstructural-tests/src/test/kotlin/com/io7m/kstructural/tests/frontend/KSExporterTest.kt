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

package com.io7m.kstructural.tests.frontend

import com.io7m.kstructural.core.KSElement.KSBlock
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSParseContext
import com.io7m.kstructural.core.KSParseError
import com.io7m.kstructural.core.KSResult.KSSuccess
import com.io7m.kstructural.core.evaluator.KSEvaluation
import com.io7m.kstructural.core.evaluator.KSEvaluator
import com.io7m.kstructural.frontend.KSExporter
import com.io7m.kstructural.frontend.KSExporterType
import com.io7m.kstructural.frontend.KSInputFormat
import com.io7m.kstructural.frontend.KSParsers
import com.io7m.kstructural.tests.KSTestFilesystems
import java.nio.file.FileSystem
import java.nio.file.Path

class KSExporterTest : KSExporterContract() {

  override fun newFilesystem() : FileSystem =
    KSTestFilesystems.newUnixFilesystem()

  override fun newExporter(f : KSInputFormat) : KSExporterType =
    KSExporter.newExporter(f, 2, 80)

  override fun parse(file : Path) : KSBlockDocument<KSEvaluation> {
    val c = KSParseContext.empty(file.parent)
    val p = KSParsers.createCanonical(c);
    val rp = p.parseBlock(c, file) as
      KSSuccess<KSBlock<KSParse>, KSParseError>
    val pd = rp.result as
      KSBlockDocument<KSParse>
    val re = KSEvaluator.evaluate(pd, file) as
      KSSuccess<KSBlockDocument<KSEvaluation>, KSParseError>
    return re.result
  }
}
