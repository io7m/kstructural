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

package com.io7m.kstructural.parser

import com.io7m.jeucreader.UnicodeCharacterReader
import com.io7m.jsx.api.lexer.JSXLexerConfiguration
import com.io7m.jsx.api.parser.JSXParserConfiguration
import com.io7m.jsx.lexer.JSXLexer
import com.io7m.jsx.parser.JSXParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.util.Optional

object KSExpressionParsers {

  fun create(file : Path) : KSExpressionParserType {
    val s = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)
    return createWithReader(
      file, BufferedReader(InputStreamReader(s, StandardCharsets.UTF_8)))
  }

  fun createWithReader(
    file : Path,
    reader : Reader) : KSExpressionParserType {

    val lcb = JSXLexerConfiguration.builder();
    lcb.setNewlinesInQuotedStrings(true)
    lcb.setSquareBrackets(true)
    lcb.setFile(Optional.of(file))
    val lc = lcb.build();

    val r = UnicodeCharacterReader.newReader(reader)
    val lex = JSXLexer.newLexer(lc, r)
    val pcb = JSXParserConfiguration.builder()
    pcb.setPreserveLexical(true)
    val pc = pcb.build()
    val p = JSXParser.newParser(pc, lex)

    return object:KSExpressionParserType {
      override fun close() {
        reader.close()
      }
      override fun parse() : Optional<KSExpression> {
        return p.parseExpressionOrEOF().map { ee -> KSExpression.of(ee) }
      }
    }
  }
}