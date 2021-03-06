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

package com.io7m.kstructural.tests.pretty.canon

import com.io7m.kstructural.core.KSElement.KSBlock
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockImport
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineInclude
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineText
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSParseContext
import com.io7m.kstructural.core.KSParseContextType
import com.io7m.kstructural.core.KSParseError
import com.io7m.kstructural.core.KSParserDriverConstructorType
import com.io7m.kstructural.core.KSParserDriverType
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.core.KSResult.KSFailure
import com.io7m.kstructural.core.KSResult.KSSuccess
import com.io7m.kstructural.core.evaluator.KSEvaluation
import com.io7m.kstructural.core.evaluator.KSEvaluationError
import com.io7m.kstructural.core.evaluator.KSEvaluator
import com.io7m.kstructural.parser.KSExpressionParsers
import com.io7m.kstructural.parser.KSIncluder
import com.io7m.kstructural.parser.canon.KSCanonBlockParser
import com.io7m.kstructural.parser.canon.KSCanonInlineParser
import com.io7m.kstructural.pretty.canon.KSCanonPrettyPrinter
import com.io7m.kstructural.tests.KSTestFilesystems
import org.slf4j.LoggerFactory
import java.io.StringWriter
import java.nio.file.FileSystem
import java.nio.file.Path
import java.util.Optional
import java.util.function.Function

class KSCanonPrettyPrinterTest : KSCanonPrettyPrinterContract() {

  private val LOG = LoggerFactory.getLogger(KSCanonPrettyPrinterTest::class.java)

  override fun newFilesystem() : FileSystem =
    KSTestFilesystems.newUnixFilesystem()

  override fun parse(file : Path) : KSBlockDocument<KSEvaluation> {
    val sp = KSExpressionParsers.create(file)
    val ip = KSCanonInlineParser.create(KSIncluder.create(file.parent))
    val importers = object : KSParserDriverConstructorType {
      override fun create(
        context : KSParseContextType,
        file : Path)
        : KSParserDriverType {

        LOG.trace("instantiating parser for {}", file)
        val iis = this
        return object : KSParserDriverType {
          override fun parseBlock(
            context : KSParseContextType,
            file : Path)
            : KSResult<KSBlock<KSParse>, KSParseError> {
            val pp = KSCanonBlockParser.create(ip, iis)
            val ep = KSExpressionParsers.create(file)
            val eo = ep.parse()
            return if (eo.isPresent) {
              pp.parse(context, eo.get(), file)
            } else {
              KSResult.fail(KSParseError(Optional.empty(), "Unexpected EOF"))
            }
          }
        }
      }
    }

    val bp = KSCanonBlockParser.create(ip, importers)

    val e = sp.parse().get()
    val c = KSParseContext.empty(file.parent)
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
        Function {
          b ->
          Optional.empty()
        }
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
        Function {
          b ->
          Optional.empty()
        }
      }

    val w = StringWriter(4096)
    val pp = KSCanonPrettyPrinter.create(w, 80, 2, import_fun, include_run)
    pp.pretty(text)
    pp.finish()
    return w.buffer.toString()
  }

}