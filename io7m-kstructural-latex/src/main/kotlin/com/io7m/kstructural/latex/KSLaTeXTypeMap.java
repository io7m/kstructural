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


package com.io7m.kstructural.latex;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.kstructural.core.KSType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A map from type names to LaTeX emphasis types.
 */

public final class KSLaTeXTypeMap
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(KSLaTeXTypeMap.class);
  }

  private KSLaTeXTypeMap()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Read a set of mappings from type names to emphasis types from the given
   * stream.
   *
   * @param file The file file, for error messages
   * @param is   The stream
   *
   * @return A set of mappings
   *
   * @throws IOException On errors
   */

  public static Map<String, KSLaTeXEmphasis> fromStream(
    final Path file,
    final InputStream is)
    throws IOException
  {
    NullCheck.notNull(file);
    NullCheck.notNull(is);

    final Map<String, KSLaTeXEmphasis> results = new HashMap<>(32);
    final StringBuilder errors = new StringBuilder(128);
    int line_number = 1;
    try (final BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
      while (true) {
        final String line = r.readLine();
        if (line == null) {
          break;
        }
        final String[] parts = line.split(":");
        if (parts.length == 2) {
          final String type_name = parts[0].trim();
          if (KSType.Companion.isValidType(type_name)) {
            final Optional<KSLaTeXEmphasis> emph =
              KSLaTeXEmphasis.Companion.fromName(parts[1].trim());
            if (emph.isPresent()) {
              final KSLaTeXEmphasis e = emph.get();
              KSLaTeXTypeMap.LOG.trace("map {} -> {}", type_name, e);
              results.put(type_name, e);
            } else {
              KSLaTeXTypeMap.error(file, line_number, "Invalid emphasis value");
            }
          } else {
            KSLaTeXTypeMap.error(file, line_number, "Invalid type");
          }
        } else {
          errors.append(
            KSLaTeXTypeMap.error(file, line_number, "Invalid mapping"));
        }

        line_number += 1;
      }
    }

    if (errors.length() > 0) {
      throw new IOException(errors.toString());
    }

    return results;
  }

  private static String error(
    final Path name,
    final int line_number,
    final String message)
  {
    final StringBuilder sb = new StringBuilder(128);
    sb.append(name);
    sb.append(": ");
    sb.append(line_number);
    sb.append(": ");
    sb.append(message);
    sb.append(System.lineSeparator());
    sb.append("Expected: <type-name> ':' { 'bold' | 'italic' | 'mono'}");
    sb.append(System.lineSeparator());
    return sb.toString();
  }
}
