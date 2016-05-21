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

package com.io7m.kstructural.tests

import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.parser.KSIncluderType
import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

object KSTestIO {

  val utf8_includer : KSIncluderType = object : KSIncluderType {
    override fun include(path : Path) : KSResult<String, Throwable> {
      return Files.newInputStream(path).use { s ->
        try {
          KSResult.succeed(IOUtils.toString(s, StandardCharsets.UTF_8))
        } catch (x : Throwable) {
          KSResult.fail(x)
        }
      }
    }
  }

}

