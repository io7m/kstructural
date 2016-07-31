package com.io7m.kstructural.tests.plain

import com.io7m.jeucreader.UnicodeCharacterReader
import com.io7m.jeucreader.UnicodeCharacterReaderPushBackType
import com.io7m.jsx.lexer.JSXLexer
import com.io7m.jsx.lexer.JSXLexerConfiguration
import com.io7m.jsx.parser.JSXParser
import com.io7m.jsx.parser.JSXParserConfiguration
import com.io7m.junreachable.UnreachableCodeException
import com.io7m.kstructural.core.KSElement
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
import com.io7m.kstructural.plain.KSPlainLayout
import com.io7m.kstructural.plain.KSPlainLayoutType
import com.io7m.kstructural.tests.KSTestFilesystems
import com.io7m.kstructural.tests.core.KSEvaluatorContract
import com.io7m.kstructural.tests.core.KSEvaluatorTest
import com.io7m.kstructural.tests.parser.canon.KSCanonBlockParserTest
import org.slf4j.LoggerFactory
import java.io.StringReader
import java.nio.file.FileSystem
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Optional

class KSPlainTest : KSPlainContract() {

  companion object {
    private val LOG = LoggerFactory.getLogger(KSPlainTest::class.java)
  }

  override fun newFilesystem() : FileSystem =
    KSTestFilesystems.newUnixFilesystem()

  override fun newEvaluatorForString(text : String) : KSEvaluatorContract.Evaluator {
    val r = UnicodeCharacterReader.newReader(StringReader(text))
    return evaluatorForReader(r)
  }

  private fun evaluatorForReader(r : UnicodeCharacterReaderPushBackType) : KSEvaluatorContract.Evaluator {
    val lcb = JSXLexerConfiguration.newBuilder()
    lcb.setNewlinesInQuotedStrings(true)
    lcb.setSquareBrackets(true)
    val lc = lcb.build()

    val lex = JSXLexer.newLexer(lc, r)
    val pcb = JSXParserConfiguration.newBuilder()
    pcb.preserveLexicalInformation(true)
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
            : KSResult<KSElement.KSBlock<KSParse>, KSParseError> {
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
        document : KSElement.KSBlock.KSBlockDocument<KSParse>,
        document_file : Path)
        : KSResult<KSElement.KSBlock.KSBlockDocument<KSEvaluation>, KSEvaluationError> {

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

    return KSEvaluatorContract.Evaluator(eval, { path ->
      val se = KSExpression.of(p.parseExpression())
      val c = KSParseContext.empty(Paths.get(""))
      val d = bp.parse(c, se, path)
      when (d) {
        is KSResult.KSSuccess -> {
          when (d.result) {
            is KSElement.KSBlock.KSBlockDocument -> {
              d.result as KSElement.KSBlock.KSBlockDocument<KSParse>
            }
            is KSElement.KSBlock.KSBlockSection,
            is KSElement.KSBlock.KSBlockSubsection,
            is KSElement.KSBlock.KSBlockParagraph,
            is KSElement.KSBlock.KSBlockFormalItem,
            is KSElement.KSBlock.KSBlockFootnote,
            is KSElement.KSBlock.KSBlockImport,
            is KSElement.KSBlock.KSBlockPart     -> {
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

  override fun layout() : KSPlainLayoutType =
    KSPlainLayout

}