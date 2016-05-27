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

package com.io7m.kstructural.parser.canon;

import com.io7m.jlexing.core.ImmutableLexicalPosition;
import com.io7m.jlexing.core.ImmutableLexicalPositionType;
import com.io7m.jnull.NullCheck;
import com.io7m.kstructural.core.KSElement;
import com.io7m.kstructural.core.KSParse;
import com.io7m.kstructural.core.KSParseContextType;
import com.io7m.kstructural.core.KSParseError;
import com.io7m.kstructural.core.KSParserDriverConstructorType;
import com.io7m.kstructural.core.KSParserDriverType;
import com.io7m.kstructural.core.KSResult;
import com.io7m.kstructural.core.KSResults;
import com.io7m.kstructural.parser.KSExpression;
import com.io7m.kstructural.parser.KSExpressionParserType;
import com.io7m.kstructural.parser.KSExpressionParsers;
import com.io7m.kstructural.parser.KSIncluder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * A parser driver for a canonical format parser.
 */

public final class KSCanonParserDriver implements KSParserDriverType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(KSCanonParserDriver.class);
  }

  private final KSParserDriverConstructorType parsers;

  /**
   * Construct a new driver for canonical parsers.
   *
   * @param in_parsers A driver constructor
   *
   * @return A new driver
   */

  public static KSParserDriverType newDriver(
    final KSParserDriverConstructorType in_parsers)
  {
    return new KSCanonParserDriver(in_parsers);
  }

  private KSCanonParserDriver(
    final KSParserDriverConstructorType in_parsers)
  {
    this.parsers = NullCheck.notNull(in_parsers);
  }

  @NotNull
  @Override
  public KSResult<KSElement.KSBlock<KSParse>, KSParseError> parseBlock(
    @NotNull final KSParseContextType context,
    @NotNull final Path file)
    throws IOException
  {
    final Path file_abs = file.toAbsolutePath();
    final Path base = context.getBaseDirectory();
    if (KSCanonParserDriver.LOG.isTraceEnabled()) {
      KSCanonParserDriver.LOG.trace("base: {}", base);
      KSCanonParserDriver.LOG.trace("file: {}", file_abs);
    }

    final ImmutableLexicalPositionType<Path> pos =
      ImmutableLexicalPosition.newPositionWithFile(0, 0, file);

    if (file.startsWith(base)) {
      final KSCanonInlineParserType inlines =
        KSCanonInlineParser.Companion.create(
          KSIncluder.Companion.create(base));
      final KSCanonBlockParserType bp =
        KSCanonBlockParser.Companion.create(inlines, this.parsers);
      final KSExpressionParserType s_expressions =
        KSExpressionParsers.INSTANCE.create(file);

      final Optional<KSExpression> e_opt = s_expressions.parse();
      if (e_opt.isPresent()) {
        return bp.parse(context, e_opt.get(), file);
      }

      return KSResults.fail(
        new KSParseError(Optional.of(pos), "Unexpected EOF"));
    }

    return KSCanonParserDriver.failOutsideBase(file, base);
  }

  private static KSResult<KSElement.KSBlock<KSParse>, KSParseError> failOutsideBase(
    final Path file,
    final Path base)
  {
    final StringBuilder sb = new StringBuilder(128);
    sb.append("Refusing to import file outside of the base directory.");
    sb.append(System.lineSeparator());
    sb.append("  Base: ");
    sb.append(base);
    sb.append(System.lineSeparator());
    sb.append("  File: ");
    sb.append(file);
    sb.append(System.lineSeparator());
    return KSResults.fail(new KSParseError(Optional.empty(), sb.toString()));
  }
}
