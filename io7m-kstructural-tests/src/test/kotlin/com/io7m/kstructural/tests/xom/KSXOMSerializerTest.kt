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

import com.io7m.kstructural.core.KSElement
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSParseContext
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.xom.KSXOMBlockParser
import com.io7m.kstructural.xom.KSXOMInlineParser
import com.io7m.kstructural.xom.KSXOMSerializer
import nu.xom.Builder
import nu.xom.Document
import nu.xom.Element
import nu.xom.Serializer
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.Optional
import java.util.function.Function

class KSXOMSerializerTest : KSXOMSerializerContract() {

  override fun serializeXML(e : KSElement<KSParse>) : String {
    val s = KSXOMSerializer.create<KSParse>(
      Function { b -> Optional.empty() },
      Function { b -> Optional.empty() })
    val t = s.serialize(e)
    val ss = ByteArrayOutputStream(128)
    val xs = Serializer(ss)
    xs.indent = 0
    xs.lineSeparator = "\n"
    xs.write(Document(t as Element))
    xs.flush()
    return String(ss.toByteArray(), StandardCharsets.UTF_8)
  }

  override fun parseInlineXML(text : String) : KSElement<KSParse> {
    val b = Builder()
    val d = b.build(StringReader(text))
    val p = KSXOMInlineParser.create()
    val c = KSParseContext.empty(Paths.get(""))
    val r = p.parse(c, d.rootElement)
    return (r as KSResult.KSSuccess).result
  }

  override fun parseBlockXML(text : String) : KSElement<KSParse> {
    val b = Builder()
    val d = b.build(StringReader(text))
    val p = KSXOMBlockParser.create(KSXOMInlineParser.create())
    val c = KSParseContext.empty(Paths.get(""))
    val r = p.parse(c, d.rootElement)
    return (r as KSResult.KSSuccess).result
  }

}