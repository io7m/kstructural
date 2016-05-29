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

package com.io7m.kstructural.xom;

import com.io7m.junreachable.UnreachableCodeException;

/**
 * Functions to escape illegal XML characters.
 */

public final class KSXOMEscapeCharacters
{
  private KSXOMEscapeCharacters()
  {
    throw new UnreachableCodeException();
  }

  /**
   * @param codepoint The codepoint
   *
   * @return {@code true} iff the given codepoint is allowed to appear in XML
   * 1.0 documents
   */

  public static boolean isAllowedXML1_0(final int codepoint)
  {
    if (codepoint == 0x0009) {
      return true;
    }
    if (codepoint == 0x000A) {
      return true;
    }
    if (codepoint == 0x000D) {
      return true;
    }
    if (codepoint >= 0x0020 && codepoint <= 0xD7FF) {
      return true;
    }
    if (codepoint >= 0xE000 && codepoint <= 0xFFFD) {
      return true;
    }
    return codepoint >= 0x10000 && codepoint <= 0x10FFFF;
  }

  /**
   * @param s A piece of text
   *
   * @return {@code s} with illegal characters converted to {@code U+FFFD}
   */

  public static String filterXML10(final String s)
  {
    final StringBuilder b = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i = s.offsetByCodePoints(i, 1)) {
      final int cp = s.codePointAt(i);
      if (!KSXOMEscapeCharacters.isAllowedXML1_0(cp)) {
        b.appendCodePoint(0xFFFD);
      } else {
        b.appendCodePoint(cp);
      }
    }
    return b.toString();
  }
}
