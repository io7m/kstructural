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

package com.io7m.kstructural.tests.pretty.imperative

import com.io7m.kstructural.core.KSElement.KSBlock
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockImport
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineInclude
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineText
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSParseContext
import com.io7m.kstructural.core.KSParseError
import com.io7m.kstructural.core.KSResult.KSFailure
import com.io7m.kstructural.core.KSResult.KSSuccess
import com.io7m.kstructural.core.evaluator.KSEvaluation
import com.io7m.kstructural.core.evaluator.KSEvaluationError
import com.io7m.kstructural.core.evaluator.KSEvaluator
import com.io7m.kstructural.frontend.KSParsers
import com.io7m.kstructural.pretty.imperative.KSImperativePrettyPrinter
import com.io7m.kstructural.tests.KSTestFilesystems
import com.io7m.kstructural.tests.pretty.imperative.KSImperativePrettyPrinterContract
import org.slf4j.LoggerFactory
import java.io.StringWriter
import java.nio.file.FileSystem
import java.nio.file.Path
import java.util.Optional
import java.util.function.Function

class KSImperativePrettyPrinterTest : KSImperativePrettyPrinterContract() {

  private val LOG = LoggerFactory.getLogger(KSImperativePrettyPrinterTest::class.java)

  override fun newFilesystem() : FileSystem =
    KSTestFilesystems.newUnixFilesystem()

  override fun parse(file : Path) : KSBlockDocument<KSEvaluation> {
    val pc = KSParseContext.empty(file.parent)
    val pp = KSParsers.createImperative(pc)
    val rr = pp.parseBlock(pc, file)

    if (rr is KSFailure) {
      LOG.error("errors: {}", rr.errors)
      throw AssertionError()
    }

    val dr = (rr as KSSuccess<KSBlockDocument<KSParse>, KSParseError>)
    val eval = KSEvaluator.evaluate(dr.result, defaultFile())
    if (eval is KSFailure) {
      LOG.error("errors: {}", eval.errors)
      throw AssertionError()
    }
    return (eval as KSSuccess<KSBlockDocument<KSEvaluation>, KSEvaluationError>).result
  }

  override fun serialize(
    text : KSBlockDocument<KSEvaluation>,
    imports : Boolean) : String {

    val import_map = text.data.context.imports
    val import_fun : Function<KSBlock<KSEvaluation>, Optional<KSBlockImport<KSEvaluation>>> =
      if (imports) {
        Function { b ->
          if (import_map.containsKey(b)) {
            Optional.of(import_map[b])
          } else {
            Optional.empty()
          }
        }
      } else {
        Function { b -> Optional.empty() }
      }

    val include_map = text.data.context.includesByText
    val include_run : Function<KSInlineText<KSEvaluation>, Optional<KSInlineInclude<KSEvaluation>>> =
      if (imports) {
        Function { b ->
          if (include_map.containsKey(b)) {
            Optional.of(include_map[b])
          } else {
            Optional.empty()
          }
        }
      } else {
        Function { b -> Optional.empty() }
      }

    val w = StringWriter(4096)
    val pp = KSImperativePrettyPrinter.create(w, 80, import_fun, include_run)
    pp.pretty(text)
    pp.finish()
    return w.buffer.toString()
  }

}