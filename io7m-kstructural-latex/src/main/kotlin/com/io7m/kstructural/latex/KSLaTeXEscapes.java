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

import com.io7m.junreachable.UnreachableCodeException;

/**
 * Functions to escape characters that should not appear in LaTeX documents.
 */

public final class KSLaTeXEscapes
{
  private KSLaTeXEscapes()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Escape all characters that cannot appear as arbitrary text in a LaTeX
   * document.
   *
   * @param s The input text
   *
   * @return A filtered version of {@code s}
   */

  public static String escapeAll(final String s)
  {
    final StringBuilder b = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i = s.offsetByCodePoints(i, 1)) {
      final int cp = s.codePointAt(i);

      if (cp >= 0x0000 && cp <= 0x0008) {
        b.appendCodePoint(0xFFFD);
        continue;
      }

      if (cp >= 0x000B && cp <= 0x001F) {
        b.appendCodePoint(0xFFFD);
        continue;
      }

      if (cp == 0x007F) {
        b.appendCodePoint(0xFFFD);
        continue;
      }

      if (cp == '#') {
        b.append("\\#");
        continue;
      }

      if (cp == '$') {
        b.append("\\$");
        continue;
      }

      if (cp == '%') {
        b.append("\\%");
        continue;
      }

      if (cp == '&') {
        b.append("\\&");
        continue;
      }

      if (cp == '\\') {
        b.append("\\textbackslash{}");
        continue;
      }

      if (cp == '^') {
        b.append("\\textasciicircum{}");
        continue;
      }

      if (cp == '_') {
        b.append("\\_");
        continue;
      }

      if (cp == '{') {
        b.append("\\{");
        continue;
      }

      if (cp == '}') {
        b.append("\\}");
        continue;
      }

      if (cp == '<') {
        b.append("\\textless");
        continue;
      }

      if (cp == '>') {
        b.append("\\textgreater");
        continue;
      }

      if (cp == '~') {
        b.append("\\textasciitilde{}");
        continue;
      }

      b.appendCodePoint(cp);
    }
    return b.toString();
  }

  /**
   * Escape all characters that cannot appear as arbitrary text in LaTeX
   * verbatim elements.
   *
   * @param s The input text
   *
   * @return A filtered version of {@code s}
   */

  public static String escapeForVerbatim(final String s)
  {
    final StringBuilder b = new StringBuilder(s.length());

    for (int i = 0; i < s.length(); i = s.offsetByCodePoints(i, 1)) {
      final int cp = s.codePointAt(i);

      if (cp >= 0x0000 && cp <= 0x0008) {
        b.appendCodePoint(0xFFFD);
        continue;
      }

      if (cp >= 0x000B && cp <= 0x001F) {
        b.appendCodePoint(0xFFFD);
        continue;
      }

      if (cp == 0x007F) {
        b.appendCodePoint(0xFFFD);
        continue;
      }

      if (cp == '\\') {
        final String end_command = "\\end{verbatim}";
        if (s.substring(i).startsWith(end_command)) {
          b.append("\\");
          b.append("\u200b");
          continue;
        }
      }

      b.appendCodePoint(cp);
    }
    return b.toString();
  }
}
