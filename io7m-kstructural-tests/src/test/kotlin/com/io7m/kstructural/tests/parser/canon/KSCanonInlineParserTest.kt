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
import com.io7m.kstructural.core.KSElement.KSInline
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSParseContextType
import com.io7m.kstructural.core.KSParseError
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.parser.KSExpression
import com.io7m.kstructural.parser.canon.KSCanonInlineParser
import com.io7m.kstructural.parser.canon.KSCanonInlineParserType
import com.io7m.kstructural.tests.KSTestFilesystems
import com.io7m.kstructural.tests.KSTestIO
import org.slf4j.LoggerFactory
import java.io.StringReader
import java.nio.file.FileSystem
import java.nio.file.Path

class KSCanonInlineParserTest : KSCanonInlineParserContract() {

  override fun newFilesystem() : FileSystem {
    return KSTestFilesystems.newUnixFilesystem()
  }

  companion object {
    private val LOG = LoggerFactory.getLogger(KSCanonInlineParserTest::class.java)
  }

  override fun newParserForString(text : String) : Parser {
    val lcb = JSXLexerConfiguration.newBuilder();
    lcb.setNewlinesInQuotedStrings(true);
    lcb.setSquareBrackets(true)
    val lc = lcb.build();

    val r = UnicodeCharacterReader.newReader(StringReader(text));
    val lex = JSXLexer.newLexer(lc, r);
    val pcb = JSXParserConfiguration.newBuilder();
    pcb.preserveLexicalInformation(true);
    val pc = pcb.build();
    val p = JSXParser.newParser(pc, lex);

    val ip = KSCanonInlineParser.create(KSTestIO.utf8_includer)
    val ipp = object : KSCanonInlineParserType {
      override fun maybe(expression : KSExpression) : Boolean =
        ip.maybe(expression)

      override fun parse(
        context : KSParseContextType,
        expression : KSExpression,
        file : Path)
        : KSResult<KSInline<KSParse>, KSParseError> {

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

    return Parser(ipp, { KSExpression.Companion.of(p.parseExpression()) })
  }

}
