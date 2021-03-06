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

import com.io7m.jnull.NullCheck;

/**
 * The supported input formats for documents.
 */

public enum KSInputFormat
{
  /**
   * The canonical s-expression format.
   */

  KS_INPUT_CANONICAL("canonical"),

  /**
   * The imperative s-expression format.
   */

  KS_INPUT_IMPERATIVE("imperative"),

  /**
   * The XML format.
   */

  KS_INPUT_XML("xml");

  private final String name;

  KSInputFormat(final String in_name)
  {
    this.name = NullCheck.notNull(in_name);
  }

  /**
   * @return The name of the format
   */

  public String getName()
  {
    return this.name;
  }

  @Override
  public String toString()
  {
    return this.name;
  }
}
