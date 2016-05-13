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

package com.io7m.kstructural.tests.pretty

import com.io7m.junreachable.UnreachableCodeException
import com.io7m.kstructural.core.KSElement
import com.io7m.kstructural.core.KSElement.*
import com.io7m.kstructural.core.KSElement.KSBlock.*
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSParseContext
import com.io7m.kstructural.core.KSParseError
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.core.KSResult.*
import com.io7m.kstructural.core.evaluator.KSEvaluation
import com.io7m.kstructural.core.evaluator.KSEvaluationError
import com.io7m.kstructural.core.evaluator.KSEvaluator
import com.io7m.kstructural.parser.KSBlockParser
import com.io7m.kstructural.parser.KSExpressionParsers
import com.io7m.kstructural.parser.KSInlineParser
import com.io7m.kstructural.pretty.KSPrettyPrinter
import com.io7m.kstructural.tests.KSTestFilesystems
import com.io7m.kstructural.tests.KSTestIO
import org.slf4j.LoggerFactory
import java.io.StringWriter
import java.nio.file.FileSystem
import java.nio.file.Path
import java.util.Optional

class KSPrettyPrinterTest : KSPrettyPrinterContract() {

  private val LOG = LoggerFactory.getLogger(KSPrettyPrinterTest::class.java)

  override fun newFilesystem() : FileSystem =
    KSTestFilesystems.newUnixFilesystem()

  override fun parse(file : Path) : KSBlockDocument<KSEvaluation> {
    val sp = KSExpressionParsers.create(file)
    val ip = KSInlineParser.get(KSTestIO.utf8_reader)
    val bp = KSBlockParser.get(
      inlines = { context, expr, file ->
        ip.parse (context, expr, file)
      },
      importer = { context, parser, file ->
        val ep = KSExpressionParsers.create(file)
        val eo = ep.invoke()
        if (eo.isPresent) {
          parser.parse(context, eo.get(), file)
        } else {
          KSResult.fail(KSParseError(Optional.empty(), "Unexpected EOF"))
        }
      })

    val e = sp.invoke().get()
    val c = KSParseContext.empty()
    val r = bp.parse(c, e, file)
    if (r is KSFailure) {
      LOG.error("errors: {}", r.errors)
      throw AssertionError()
    }
    val dr = (r as KSSuccess<KSBlockDocument<KSParse>, KSParseError>)
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
    val w = StringWriter(4096)
    val pp = KSPrettyPrinter.create(w, 80, 2, imports)
    pp.pretty(text)
    pp.finish()
    return w.buffer.toString()
  }

}