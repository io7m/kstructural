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

import com.io7m.kstructural.core.KSElement
import com.io7m.kstructural.core.KSElement.KSBlock
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSParseContextType
import com.io7m.kstructural.core.KSParseError
import com.io7m.kstructural.core.KSParserDriverConstructorType
import com.io7m.kstructural.core.KSParserDriverType
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.frontend.KSParsers
import com.io7m.kstructural.parser.KSExpression
import com.io7m.kstructural.parser.KSExpressionParsers
import com.io7m.kstructural.parser.KSIncluder
import com.io7m.kstructural.parser.canon.KSCanonBlockParser
import com.io7m.kstructural.parser.canon.KSCanonBlockParserType
import com.io7m.kstructural.parser.canon.KSCanonInlineParser
import com.io7m.kstructural.parser.canon.KSCanonInlineParserType
import com.io7m.kstructural.tests.KSTestFilesystems
import org.slf4j.LoggerFactory
import java.io.StringReader
import java.nio.file.FileSystem
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Optional

class KSCanonBlockParserTest : KSCanonBlockParserContract() {

  companion object {
    private val LOG = LoggerFactory.getLogger(KSCanonBlockParserTest::class.java)
  }

  override fun newFilesystem() : FileSystem {
    return KSTestFilesystems.newUnixFilesystem()
  }

  override fun newParserForString(text : String) : Parser {

    val ip = KSCanonInlineParser.create(KSIncluder.create(super.rootDirectory()))
    val bp = KSCanonBlockParser.create(ip, KSParsers.getInstance())

    val bpp = object : KSCanonBlockParserType {
      override fun parse(
        c : KSParseContextType,
        e : KSExpression,
        f : Path)
        : KSResult<KSBlock<KSParse>, KSParseError> {
        val r = bp.parse(c, e, f)
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

    var ep = KSExpressionParsers.createWithReader(defaultFile(), StringReader(text))
    return Parser(bpp, { ep.parse().get() })
  }

}
