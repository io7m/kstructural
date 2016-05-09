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
import com.io7m.jsx.lexer.JSXLexer
import com.io7m.jsx.lexer.JSXLexerConfiguration
import com.io7m.jsx.parser.JSXParser
import com.io7m.jsx.parser.JSXParserConfiguration
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.core.KSResult.KSFailure
import com.io7m.kstructural.core.KSResult.KSSuccess
import com.io7m.kstructural.parser.KSBlockParser
import com.io7m.kstructural.parser.KSExpression
import com.io7m.kstructural.parser.KSInlineParser
import com.io7m.kstructural.core.KSParseContext
import org.apache.commons.io.IOUtils
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

object KSBlockParserDemo {

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

    val ip = KSInlineParser.get { path ->
      Files.newInputStream(path).use { s ->
        try {
          KSResult.succeed(IOUtils.toString(s, StandardCharsets.UTF_8))
        } catch (x : Throwable) {
          KSResult.fail(x)
        }
      }
    }

    val bp = KSBlockParser.get(
      inlines = { c, e, f ->
        ip.parse(c, e, f)
      },
      importer = { c, p ->
        throw UnsupportedOperationException()
      })

    val pcontext = KSParseContext.empty()

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
