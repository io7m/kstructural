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
import com.io7m.kstructural.core.KSResult.KSFailure
import com.io7m.kstructural.core.KSResult.KSSuccess
import com.io7m.kstructural.core.evaluator.KSEvaluator
import com.io7m.kstructural.parser.KSBlockParser
import com.io7m.kstructural.parser.KSExpression
import com.io7m.kstructural.parser.KSInlineParser
import com.io7m.kstructural.xom.KSXOMSettings
import com.io7m.kstructural.xom.KSXOMXHTMLMultiWriter
import nu.xom.Serializer
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.Reader
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Optional

object KSXOMXHTMLMultiWriterDemo {

  fun main(args : Array<String>) : Unit {
    val lcb = JSXLexerConfiguration.newBuilder()
    lcb.setFile(if (args.size > 0) {
      Optional.of(Paths.get(args[0]))
    } else {
      Optional.empty()
    })
    lcb.setNewlinesInQuotedStrings(true)
    lcb.setSquareBrackets(true)
    val lc = lcb.build()

    val reader = UnicodeCharacterReader.newReader(getReader(args))
    val lex = JSXLexer.newLexer(lc, reader)

    val pcb = JSXParserConfiguration.newBuilder()
    pcb.preserveLexicalInformation(true)
    val pc = pcb.build()
    val p = JSXParser.newParser(pc, lex)
    val bp = KSBlockParser.get(KSInlineParser)

    val e_opt = p.parseExpressionOrEOF()
    if (e_opt.isPresent) {
      val r = bp.parse(KSExpression.of(e_opt.get()))
      when (r) {
        is KSSuccess ->
          evaluate(r.result, Paths.get(System.getProperty("java.io.tmpdir")))
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

  private fun evaluate(result : KSBlock<Unit>, out : Path) =
    when (result) {
      is KSBlock.KSBlockSection    -> TODO()
      is KSBlock.KSBlockSubsection -> TODO()
      is KSBlock.KSBlockParagraph  -> TODO()
      is KSBlock.KSBlockPart       -> TODO()
      is KSBlock.KSBlockFormalItem -> TODO()
      is KSBlock.KSBlockFootnote   -> TODO()
      is KSBlock.KSBlockDocument   -> {
        val rr = KSEvaluator.evaluate(result)
        when (rr) {
          is KSSuccess -> {
            val settings = KSXOMSettings(
              styles = mutableListOf(
                URI.create("kstructural-layout.css"),
                URI.create("kstructural-colour.css"),
                URI.create("custom.css")))
            val docs = KSXOMXHTMLMultiWriter.write(settings, rr.result)
            val ksdir = out.resolve("kstructural")
            Files.createDirectories(ksdir)

            docs.entries.forEach { p ->
              var out_file = ksdir.resolve(p.key)
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
