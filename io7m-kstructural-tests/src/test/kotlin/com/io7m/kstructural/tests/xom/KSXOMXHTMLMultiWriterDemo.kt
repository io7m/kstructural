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

package com.io7m.kstructural.tests.xom

import com.io7m.jeucreader.UnicodeCharacterReader
import com.io7m.jsx.lexer.JSXLexer
import com.io7m.jsx.lexer.JSXLexerConfiguration
import com.io7m.jsx.parser.JSXParser
import com.io7m.jsx.parser.JSXParserConfiguration
import com.io7m.kstructural.core.KSElement.KSBlock
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSParseContext
import com.io7m.kstructural.core.KSParseContextType
import com.io7m.kstructural.core.KSParseError
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.core.KSResult.KSFailure
import com.io7m.kstructural.core.KSResult.KSSuccess
import com.io7m.kstructural.core.evaluator.KSEvaluator
import com.io7m.kstructural.parser.canon.KSCanonBlockParser
import com.io7m.kstructural.parser.KSExpression
import com.io7m.kstructural.parser.KSExpressionParsers
import com.io7m.kstructural.core.KSParserConstructorType
import com.io7m.kstructural.core.KSParserType
import com.io7m.kstructural.parser.canon.KSCanonInlineParser
import com.io7m.kstructural.tests.KSTestIO
import com.io7m.kstructural.tests.parser.KSSerializerDemo
import com.io7m.kstructural.tests.pretty.KSPrettyPrinterTest
import com.io7m.kstructural.xom.KSXOMSettings
import com.io7m.kstructural.xom.KSXOMXHTMLMultiWriter
import nu.xom.Serializer
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Optional

object KSXOMXHTMLMultiWriterDemo {

  private val LOG = LoggerFactory.getLogger(
    KSXOMXHTMLMultiWriterDemo::class.java)

  fun main(args : Array<String>) : Unit {

    if (args.size != 2) {
      System.err.println("usage: file.sd out-directory")
      System.exit(1)
    }

    val path = Paths.get(args[0])
    val outdir = Paths.get(args[1])

    val lcb = JSXLexerConfiguration.newBuilder()
    lcb.setFile(Optional.of(path))
    lcb.setNewlinesInQuotedStrings(true)
    lcb.setSquareBrackets(true)
    val lc = lcb.build()

    val reader = UnicodeCharacterReader.newReader(getReader(args))
    val lex = JSXLexer.newLexer(lc, reader)

    val pcb = JSXParserConfiguration.newBuilder()
    pcb.preserveLexicalInformation(true)
    val pc = pcb.build()
    val p = JSXParser.newParser(pc, lex)

    val ip = KSCanonInlineParser.create(KSTestIO.utf8_includer)
    val importers = object: KSParserConstructorType {
      override fun create(
        context : KSParseContextType,
        file : Path)
        : KSParserType {

        LOG.trace("instantiating parser for {}", file)
        val iis = this
        return object: KSParserType {
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
    val e_opt = p.parseExpressionOrEOF()
    if (e_opt.isPresent) {
      val pcontext = KSParseContext.empty()
      val ee = KSExpression.of(e_opt.get())
      val r = bp.parse(pcontext, ee, path)
      when (r) {
        is KSSuccess ->
          evaluate(r.result, path, outdir)
        is KSFailure -> {
          for (a in r.errors) {
            System.out.print("parse error: ")
            a.position.ifPresent {
              p ->
              System.out.print(p.toString() + ": ")
            }
            System.out.println(a.message)
          }
          r.partial.ifPresent { e -> System.out.println(e) }
        }
      }
    }
  }

  private fun evaluate(
    result : KSBlock<KSParse>,
    base : Path,
    out : Path) =
    when (result) {
      is KSBlock.KSBlockSection    -> TODO()
      is KSBlock.KSBlockSubsection -> TODO()
      is KSBlock.KSBlockParagraph  -> TODO()
      is KSBlock.KSBlockPart       -> TODO()
      is KSBlock.KSBlockFormalItem -> TODO()
      is KSBlock.KSBlockFootnote   -> TODO()
      is KSBlock.KSBlockImport     -> TODO()
      is KSBlock.KSBlockDocument   -> {

        val rr = KSEvaluator.evaluate(result, base)
        when (rr) {
          is KSSuccess -> {
            val settings = KSXOMSettings(
              styles = mutableListOf(
                URI.create("kstructural-layout.css"),
                URI.create("kstructural-colour.css"),
                URI.create("custom.css")))
            val docs = KSXOMXHTMLMultiWriter.write(settings, rr.result)
            Files.createDirectories(out)

            docs.entries.forEach { p ->
              var out_file = out.resolve(p.key)
              val os = Files.newOutputStream(out_file)
              os.use {
                val s = Serializer(os)
                s.write(p.value)
                s.flush()
                os.flush()
              }
            }
          }
          is KSFailure -> {
            for (a in rr.errors) {
              System.out.print("evaluation error: ")
              a.position.ifPresent {
                p ->
                System.out.print(p.toString() + ": ")
              }
              System.out.println(a.message)
            }
            rr.partial.ifPresent { e -> System.out.println(e) }
          }
        }
      }
    }

  private fun getReader(args : Array<String>) : Reader {
    if (args.size > 0) {
      return InputStreamReader(FileInputStream(args[0]))
    }
    return InputStreamReader(System.`in`)
  }

}

fun main(args : Array<String>) : Unit {
  KSXOMXHTMLMultiWriterDemo.main(args)
}
