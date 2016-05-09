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

package com.io7m.kstructural.core

import org.slf4j.LoggerFactory
import org.valid4j.Assertive
import java.nio.file.Path
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineInclude
import com.io7m.kstructural.core.KSElement.KSBlock.*
import com.io7m.kstructural.core.KSElement.KSBlock
import java.util.HashMap
import java.util.IdentityHashMap

class KSParseContext private constructor(
  override val includes : MutableMap<Path, String>,
  override val include_paths : MutableMap<KSInlineInclude<KSParse>, Path>,
  override val imports : MutableMap<Path, KSBlock<KSParse>>,
  override val import_paths : MutableMap<KSBlockImport<KSParse>, Path>)
: KSParseContextType {

  override fun addImport(
    i : KSBlockImport<KSParse>,
    p : Path,
    e : KSBlock<KSParse>) {
    LOG.trace("import: {}: {}", p, e.javaClass.simpleName)
    Assertive.require(!import_paths.containsKey(i))
    this.imports[p] = e
    this.import_paths[i] = p
  }

  companion object {
    private val LOG = LoggerFactory.getLogger(KSParseContext::class.java)

    fun empty() : KSParseContextType {
      return KSParseContext(
        includes = HashMap(),
        include_paths = IdentityHashMap(),
        imports = HashMap(),
        import_paths = IdentityHashMap())
    }
  }

  override fun addInclude(
    i : KSInlineInclude<KSParse>,
    p : Path,
    s : String) {
    LOG.trace("include: {}: {}...", p, s.substring(0, Math.min(8, s.length)))
    Assertive.require(!include_paths.containsKey(i))
    this.includes[p] = s
    this.include_paths[i] = p
  }
}