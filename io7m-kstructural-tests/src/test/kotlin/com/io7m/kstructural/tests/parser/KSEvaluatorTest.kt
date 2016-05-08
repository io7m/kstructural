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

package com.io7m.kstructural.tests.parser

import com.io7m.jeucreader.UnicodeCharacterReader
import com.io7m.jeucreader.UnicodeCharacterReaderPushBackType
import com.io7m.jsx.lexer.JSXLexer
import com.io7m.jsx.lexer.JSXLexerConfiguration
import com.io7m.jsx.parser.JSXParser
import com.io7m.jsx.parser.JSXParserConfiguration
import com.io7m.junreachable.UnreachableCodeException
import com.io7m.kstructural.core.KSElement.KSBlock
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.core.evaluator.KSEvaluation
import com.io7m.kstructural.core.evaluator.KSEvaluationError
import com.io7m.kstructural.core.evaluator.KSEvaluator
import com.io7m.kstructural.core.evaluator.KSEvaluatorType
import com.io7m.kstructural.parser.KSBlockParser
import com.io7m.kstructural.parser.KSBlockParserType
import com.io7m.kstructural.parser.KSExpression
import com.io7m.kstructural.parser.KSInlineParser
import com.io7m.kstructural.parser.KSParseError
import org.slf4j.LoggerFactory
import java.io.InputStreamReader
import java.io.StringReader
import java.nio.file.Path
import java.nio.file.Paths

class KSEvaluatorTest : KSEvaluatorContract() {

  override fun readTextForPath(p : Path) : KSResult<String, Throwable> {
    throw UnsupportedOperationException()
  }

  override fun defaultPath() : Path {
    return Paths.get("")
  }

  override fun newEvaluatorForFile(file : String) : Evaluator {
    val s = KSEvaluatorTest::class.java.getResourceAsStream(file)
    val r = UnicodeCharacterReader.newReader(InputStreamReader(s))
    return evaluatorForReader(r)
  }

  companion object {
    private val LOG = LoggerFactory.getLogger(KSBlockParserTest::class.java)
  }

  override fun newEvaluatorForString(text : String) : KSEvaluatorContract.Evaluator {
    val r = UnicodeCharacterReader.newReader(StringReader(text))
    return evaluatorForReader(r)
  }

  private fun evaluatorForReader(r : UnicodeCharacterReaderPushBackType) : Evaluator {
    val lcb = JSXLexerConfiguration.newBuilder()
    lcb.setNewlinesInQuotedStrings(true)
    lcb.setSquareBrackets(true)
    val lc = lcb.build()

    val lex = JSXLexer.newLexer(lc, r)
    val pcb = JSXParserConfiguration.newBuilder()
    pcb.preserveLexicalInformation(true)
    val pc = pcb.build()
    val p = JSXParser.newParser(pc, lex)
    val bp = KSBlockParser.get(KSInlineParser)
    val bpp = object : KSBlockParserType {
      override fun parse(e : KSExpression) : KSResult<KSBlock<Unit>, KSParseError> {
        val r = bp.parse(e)
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

    val eval = object : KSEvaluatorType {

      override fun evaluate(
        d : KSBlock.KSBlockDocument<Unit>,
        f : Path,
        r : (Path) -> KSResult<String, Throwable>)
        : KSResult<KSBlock.KSBlockDocument<KSEvaluation>, KSEvaluationError> {

        val r = KSEvaluator.evaluate(d,f,r)
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

    return Evaluator(eval, {
      val se = KSExpression.of(p.parseExpression())
      val d = bpp.parse(se)
      when (d) {
        is KSResult.KSSuccess -> {
          when (d.result) {
            is KSBlock.KSBlockDocument -> {
              d.result as KSBlock.KSBlockDocument<Unit>
            }
            is KSBlock.KSBlockSection,
            is KSBlock.KSBlockSubsection,
            is KSBlock.KSBlockParagraph,
            is KSBlock.KSBlockFormalItem,
            is KSBlock.KSBlockFootnote,
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
