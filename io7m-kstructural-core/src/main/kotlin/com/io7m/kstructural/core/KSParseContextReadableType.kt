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

import com.io7m.kstructural.core.KSElement.KSBlock
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockImport
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineInclude
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineText
import java.nio.file.Path

interface KSParseContextReadableType {

  val baseDirectory : Path

  val includes : Map<Path, String>

  val includePaths : Map<KSInlineInclude<KSParse>, Path>

  val includesByTexts : MutableMap<KSInlineText<KSParse>, KSInlineInclude<KSParse>>

  val importsByPath : Map<Path, KSBlock<KSParse>>

  val importPathsByElement : Map<KSBlockImport<KSParse>, Path>

  val importPathsEdgesByElement : Map<KSBlockImport<KSParse>, KSImportPathEdge>

  val importsByElement : Map<KSBlock<KSParse>, KSBlockImport<KSParse>>
}