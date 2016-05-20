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

package com.io7m.kstructural.frontend;

import com.io7m.junreachable.UnreachableCodeException;

import java.nio.file.FileSystem;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

final class KSFileSuffixes
{
  private KSFileSuffixes()
  {
    throw new UnreachableCodeException();
  }

  static Path replace(
    final Path file,
    final String sd)
    throws NoSuchFileException
  {
    final Path parent = file.getParent();

    final Path f_name = file.getFileName();
    if (f_name == null) {
      return file;
    }

    final FileSystem fs = file.getFileSystem();
    return parent.resolve(fs.getPath(
      KSFileSuffixes.replaceSuffix(file.getFileName().toString(), sd)));
  }

  static String replaceSuffix(
    final String file,
    final String new_suffix) {
    final int i = file.lastIndexOf('.');
    if (i >= 0) {
      return file.substring(0, i) + "." + new_suffix;
    }
    return file;
  }
}
