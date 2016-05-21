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

import com.io7m.jfunctional.PartialProcedureType;
import com.io7m.jnull.NullCheck;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Code for appending and/or prepending branding to XHTML documents.
 */

public final class KSBrandAppender
{
  private final Optional<Element> brand_start;
  private final Optional<Element> brand_end;
  private final PartialProcedureType<Element, IOException> appender_start;
  private final PartialProcedureType<Element, IOException> appender_end;

  private KSBrandAppender(
    final Optional<Element> in_brand_top,
    final Optional<Element> in_brand_bottom)
  {
    this.brand_start = NullCheck.notNull(in_brand_top);
    this.brand_end = NullCheck.notNull(in_brand_bottom);
    this.appender_start = (e) -> {
      if (this.brand_start.isPresent()) {
        e.insertChild(this.brand_start.get().copy(), 0);
      }
    };
    this.appender_end = (e) -> {
      if (this.brand_end.isPresent()) {
        e.insertChild(this.brand_end.get().copy(), 0);
      }
    };
  }

  private static Optional<Element> getBrand(
    final Optional<Path> brand)
    throws IOException, ParsingException
  {
    if (brand.isPresent()) {
      final Path path = brand.get();
      try (final InputStream is = Files.newInputStream(path)) {
        final Builder b = new Builder();
        final Document d = b.build(is);
        return Optional.of(d.getRootElement());
      }
    }

    return Optional.empty();
  }

  /**
   * Construct a new appender.
   *
   * @param start The optional "top" brand file
   * @param end   The optional "bottom" brand file
   *
   * @return A new appender
   *
   * @throws IOException      On I/O errors
   * @throws ParsingException On brand parse errors
   */

  public static KSBrandAppender newAppender(
    final Optional<Path> start,
    final Optional<Path> end)
    throws IOException, ParsingException
  {
    return new KSBrandAppender(
      getBrand(start),
      getBrand(end)
    );
  }

  /**
   * @return A procedure that will append a brand to the start of a document
   */

  public PartialProcedureType<Element, IOException> getAppenderStart()
  {
    return this.appender_start;
  }

  /**
   * @return A procedure that will append a brand to the end of a document
   */

  public PartialProcedureType<Element, IOException> getAppenderEnd()
  {
    return this.appender_end;
  }
}
