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
import com.io7m.kstructural.parser.KSBlockParser
import com.io7m.kstructural.parser.KSExpression
import com.io7m.kstructural.parser.KSInlineParser
import java.io.StringReader

class KSBlockParserTest : KSBlockParserContract() {

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
    return Parser(KSBlockParser.get(KSInlineParser), { KSExpression.of(p.parseExpression()) })
  }

}
