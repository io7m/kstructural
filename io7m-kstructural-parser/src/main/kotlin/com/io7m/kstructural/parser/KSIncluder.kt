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

import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.core.KSResults
import org.slf4j.LoggerFactory
import org.valid4j.Assertive
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class KSIncluder private constructor(
  private val baseDirectory : Path) : KSIncluderType {

  init {
    Assertive.require(baseDirectory.isAbsolute)
  }

  override fun include(path : Path) : KSResult<String, Throwable> {
    try {
      val path_abs = path.toAbsolutePath()

      if (LOG.isTraceEnabled) {
        LOG.trace("base:    {}", baseDirectory)
        LOG.trace("include: {}", path_abs)
      }

      if (path_abs.startsWith(baseDirectory)) {
        val ba = Files.readAllBytes(path)
        val s = String(ba, StandardCharsets.UTF_8)
        return KSResult.KSSuccess<String, Throwable>(s)
      } else {
        val sb = StringBuilder(128)
        sb.append("Refusing to include a file outside of the base directory.")
        sb.append(System.lineSeparator())
        sb.append("  Base:    ")
        sb.append(baseDirectory)
        sb.append(System.lineSeparator())
        sb.append("  Include: ")
        sb.append(path_abs)
        sb.append(System.lineSeparator())
        throw IOException(sb.toString())
      }
    } catch (e : IOException) {
      return KSResults.fail<String, Throwable>(e as Throwable)
    }
  }

  companion object {

    private val LOG = LoggerFactory.getLogger(KSIncluder::class.java)

    fun create(base : Path) : KSIncluderType {
      return KSIncluder(base.toAbsolutePath())
    }
  }
}