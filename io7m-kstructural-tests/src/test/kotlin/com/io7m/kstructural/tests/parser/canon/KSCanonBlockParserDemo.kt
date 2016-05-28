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

package com.io7m.kstructural.tests.parser.canon

import com.io7m.jeucreader.UnicodeCharacterReader
import com.io7m.jsx.lexer.JSXLexer
import com.io7m.jsx.lexer.JSXLexerConfiguration
import com.io7m.jsx.parser.JSXParser
import com.io7m.jsx.parser.JSXParserConfiguration
import com.io7m.kstructural.core.KSElement
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSParseContext
import com.io7m.kstructural.core.KSParseContextType
import com.io7m.kstructural.core.KSParseError
import com.io7m.kstructural.core.KSParserDriverConstructorType
import com.io7m.kstructural.core.KSParserDriverType
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.core.KSResult.KSFailure
import com.io7m.kstructural.core.KSResult.KSSuccess
import com.io7m.kstructural.parser.KSExpression
import com.io7m.kstructural.parser.KSExpressionParsers
import com.io7m.kstructural.parser.KSIncluder
import com.io7m.kstructural.parser.canon.KSCanonBlockParser
import com.io7m.kstructural.parser.canon.KSCanonInlineParser
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Optional

object KSBlockParserDemo {

  private val LOG = LoggerFactory.getLogger(KSBlockParserDemo::class.java)

  fun main(args : Array<String>) : Unit {
    if (args.size != 1) {
      System.err.println("usage: file.sd")
      System.exit(1)
    }

    val path = Paths.get(args[0])

    val lcb = JSXLexerConfiguration.newBuilder()
    lcb.setNewlinesInQuotedStrings(true)
    lcb.setSquareBrackets(true)
    val lc = lcb.build()

    val reader = UnicodeCharacterReader.newReader(getReader(args))
    val lex = JSXLexer.newLexer(lc, reader)
    val pcb = JSXParserConfiguration.newBuilder()
    pcb.preserveLexicalInformation(true)
    val pc = pcb.build()
    val p = JSXParser.newParser(pc, lex)

    val ip = KSCanonInlineParser.create(KSIncluder.create(Paths.get("")))
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

    val bp = KSCanonBlockParser.create(ip, importers)
    val pcontext = KSParseContext.empty(path.parent)

    var eof = false
    while (!eof) {
      val e_opt = p.parseExpressionOrEOF()
      if (e_opt.isPresent) {
        val ee = KSExpression.of(e_opt.get())
        val r = bp.parse(pcontext, ee, path)

        when (r) {
          is KSSuccess -> {
            System.out.println(r.result)
          }
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
      } else {
        eof = true
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
  KSBlockParserDemo.main(args)
}
