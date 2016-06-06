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

package com.io7m.kstructural.pretty;

import com.io7m.junreachable.UnreachableCodeException;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;
import org.apache.commons.lang3.text.translate.LookupTranslator;

/**
 * Functions to escape strings.
 */

public final class KSSExprEscape
{
  /**
   * A translator to escape quotes in strings.
   */

  public static final CharSequenceTranslator SEXPR_STRING_ESCAPE;

  static {
    SEXPR_STRING_ESCAPE = new LookupTranslator(
      new String[][]{
        {"\"", "\\\""},
        {"\\", "\\\\"},
      });
  }

  private KSSExprEscape()
  {
    throw new UnreachableCodeException();
  }

  /**
   * @param text The text
   *
   * @return {@code true} iff {@code text} contains any characters that would
   * require escaping when rendered as s-expressions
   */

  public static boolean requiresQuoting(final String text)
  {
    int index = 0;
    final int length = text.length();
    while (true) {
      if (index < length) {
        final int cp = text.codePointAt(index);
        if (cp == '(') {
          return true;
        }
        if (cp == ')') {
          return true;
        }
        if (cp == '[') {
          return true;
        }
        if (cp == ']') {
          return true;
        }
        if (cp == '"') {
          return true;
        }
        index += Character.charCount(cp);
      } else {
        return false;
      }
    }
  }
}
