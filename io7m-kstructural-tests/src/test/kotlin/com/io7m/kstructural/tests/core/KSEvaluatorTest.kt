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

package com.io7m.kstructural.tests.core

import com.io7m.jeucreader.UnicodeCharacterReader
import com.io7m.jeucreader.UnicodeCharacterReaderPushBackType
import com.io7m.jsx.api.lexer.JSXLexerConfiguration
import com.io7m.jsx.api.parser.JSXParserConfiguration
import com.io7m.jsx.lexer.JSXLexer
import com.io7m.jsx.parser.JSXParser
import com.io7m.junreachable.UnreachableCodeException
import com.io7m.kstructural.core.KSElement
import com.io7m.kstructural.core.KSElement.KSBlock
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSParseContext
import com.io7m.kstructural.core.KSParseContextType
import com.io7m.kstructural.core.KSParseError
import com.io7m.kstructural.core.KSParserDriverConstructorType
import com.io7m.kstructural.core.KSParserDriverType
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.core.evaluator.KSEvaluation
import com.io7m.kstructural.core.evaluator.KSEvaluationError
import com.io7m.kstructural.core.evaluator.KSEvaluator
import com.io7m.kstructural.core.evaluator.KSEvaluatorType
import com.io7m.kstructural.parser.KSExpression
import com.io7m.kstructural.parser.KSExpressionParsers
import com.io7m.kstructural.parser.KSIncluder
import com.io7m.kstructural.parser.canon.KSCanonBlockParser
import com.io7m.kstructural.parser.canon.KSCanonInlineParser
import com.io7m.kstructural.parser.canon.KSCanonInlineParserType
import com.io7m.kstructural.tests.KSTestFilesystems
import com.io7m.kstructural.tests.parser.canon.KSCanonBlockParserTest
import org.slf4j.LoggerFactory
import java.io.InputStreamReader
import java.io.StringReader
import java.nio.file.FileSystem
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Optional

class KSEvaluatorTest : KSEvaluatorContract() {

  override fun newFilesystem() : FileSystem {
    return KSTestFilesystems.newUnixFilesystem()
  }

  override fun newEvaluatorForFile(file : String) : Evaluator {
    val s = KSEvaluatorTest::class.java.getResourceAsStream(file)
    val r = UnicodeCharacterReader.newReader(InputStreamReader(s))
    return evaluatorForReader(r)
  }

  companion object {
    private val LOG = LoggerFactory.getLogger(KSCanonBlockParserTest::class.java)
  }

  override fun newEvaluatorForString(text : String) : KSEvaluatorContract.Evaluator {
    val r = UnicodeCharacterReader.newReader(StringReader(text))
    return evaluatorForReader(r)
  }

  private fun evaluatorForReader(r : UnicodeCharacterReaderPushBackType) : Evaluator {
    val lcb = JSXLexerConfiguration.builder()
    lcb.setNewlinesInQuotedStrings(true)
    lcb.setSquareBrackets(true)
    val lc = lcb.build()

    val lex = JSXLexer.newLexer(lc, r)
    val pcb = JSXParserConfiguration.builder()
    pcb.setPreserveLexical(true)
    val pc = pcb.build()
    val p = JSXParser.newParser(pc, lex)
    val ip = KSCanonInlineParser.create(KSIncluder.create(Paths.get("")))

    val ipp = object : KSCanonInlineParserType {
      override fun maybe(expression : KSExpression) : Boolean =
        ip.maybe(expression)

      override fun parse(
        context : KSParseContextType,
        expression : KSExpression,
        file : Path)
        : KSResult<KSElement.KSInline<KSParse>, KSParseError> {

        val r = ip.parse(context, expression, file)
        return when (r) {
          is KSResult.KSSuccess -> {
            LOG.debug("successfully parsed: {}", r.result)
            r
          }
          is KSResult.KSFailure -> {
            LOG.debug("failed to parse: {}", r.partial)
            r.errors.map { k -> LOG.debug("error: {}", k.message) }
            r
          }
        }
      }
    }

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

    val bp = KSCanonBlockParser.create(
      inlines = ipp,
      importers = importers)

    val eval = object : KSEvaluatorType {
      override fun evaluate(
        document : KSBlock.KSBlockDocument<KSParse>,
        document_file : Path)
        : KSResult<KSBlock.KSBlockDocument<KSEvaluation>, KSEvaluationError> {

        val r = KSEvaluator.evaluate(document, document_file)
        return when (r) {
          is KSResult.KSSuccess -> {
            r
          }
          is KSResult.KSFailure -> {
            LOG.debug("failed to evaluate: {}", r.partial)
            r.errors.map { k -> LOG.debug("error: {}", k.message) }
            r
          }
        }
      }
    }

    return Evaluator(eval, { path ->
      val se = KSExpression.of(p.parseExpression())
      val c = KSParseContext.empty(Paths.get(""))
      val d = bp.parse(c, se, path)
      when (d) {
        is KSResult.KSSuccess -> {
          when (d.result) {
            is KSBlock.KSBlockDocument -> {
              d.result as KSBlock.KSBlockDocument<KSParse>
            }
            is KSBlock.KSBlockSection,
            is KSBlock.KSBlockSubsection,
            is KSBlock.KSBlockParagraph,
            is KSBlock.KSBlockFormalItem,
            is KSBlock.KSBlockFootnote,
            is KSBlock.KSBlockImport,
            is KSBlock.KSBlockPart     -> {
              LOG.error("Parser unexpectedly returned: {}", d.result)
              throw UnreachableCodeException()
            }
          }
        }
        is KSResult.KSFailure -> {
          LOG.error("Parser unexpectedly failed: {}", d)
          throw UnreachableCodeException()
        }
      }
    })
  }
}
