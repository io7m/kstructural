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

package com.io7m.kstructural.parser.imperative;

import com.io7m.jfunctional.Unit;
import com.io7m.jlexing.core.LexicalPositionType;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
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
import com.io7m.kstructural.parser.canon.KSCanonInlineParser;
import com.io7m.kstructural.parser.canon.KSCanonInlineParserType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Optional;

/**
 * A driver for imperative parsers.
 */

public final class KSImperativeParserDriver implements KSParserDriverType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(KSImperativeParserDriver.class);
  }

  private boolean block_failed;
  private final Optional<LexicalPositionType<Path>> last_pos;
  private final ArrayDeque<KSParseError> errors;
  private final KSParserDriverConstructorType parsers;

  /**
   * Construct a new driver for imperative parsers.
   *
   * @param in_parsers A driver constructor
   *
   * @return A new driver
   */

  public static KSParserDriverType newDriver(
    final KSParserDriverConstructorType in_parsers)
  {
    return new KSImperativeParserDriver(in_parsers);
  }

  private KSImperativeParserDriver(
    final KSParserDriverConstructorType in_parsers)
  {
    this.last_pos = Optional.empty();
    this.errors = new ArrayDeque<>(128);
    this.block_failed = false;
    this.parsers = NullCheck.notNull(in_parsers);
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

  @Override
  @NotNull
  public KSResult<KSElement.KSBlock<KSParse>, KSParseError> parseBlock(
    @NotNull final KSParseContextType context,
    @NotNull final Path file)
    throws IOException
  {
    final Path file_abs = file.toAbsolutePath();
    final Path base = context.getBaseDirectory();
    if (KSImperativeParserDriver.LOG.isTraceEnabled()) {
      KSImperativeParserDriver.LOG.trace("base: {}", base);
      KSImperativeParserDriver.LOG.trace("file: {}", file_abs);
    }

    if (!file_abs.startsWith(base)) {
      return KSImperativeParserDriver.failOutsideBase(file, base);
    }

    return this.doParse(context, file, base);
  }

  @NotNull
  private KSResult<KSElement.KSBlock<KSParse>, KSParseError> doParse(
    final KSParseContextType context,
    final Path file,
    final Path base)
  {
    final KSCanonInlineParserType inlines =
      KSCanonInlineParser.Companion.create(
        KSIncluder.Companion.create(base));

    final KSExpressionParserType s_expressions =
      KSExpressionParsers.INSTANCE.create(file);
    final KSImperativeParserType ibp =
      KSImperativeParser.Companion.create(inlines, this.parsers);
    final KSImperativeBuilderType ibb =
      KSImperativeBuilder.Companion.create();

    while (true) {
      final Optional<KSExpression> e_opt = s_expressions.parse();

      /**
       * Has EOF been reached?
       */

      if (!e_opt.isPresent()) {
        final KSImperative.KSImperativeEOF eof =
          new KSImperative.KSImperativeEOF(this.last_pos);
        final KSResult<Optional<KSElement.KSBlock<KSParse>>, KSParseError> rr =
          ibb.add(context, eof);

        /**
         * If the last parse fails, the result is failure with all of
         * the errors that have been accumulated.
         */

        if (rr instanceof KSResult.KSFailure) {
          final KSResult.KSFailure<Optional<KSElement.KSBlock<KSParse>>, KSParseError> rf =
            (KSResult.KSFailure<Optional<KSElement.KSBlock<KSParse>>, KSParseError>) rr;
          this.errors.addAll(rf.getErrors());
          return new KSResult.KSFailure<>(Optional.empty(), this.errors);
        }

        /**
         * Otherwise, if *any* parse has failed, the result is failure with
         * all of the errors that have been accumulated.
         */

        if (!this.errors.isEmpty()) {
          return new KSResult.KSFailure<>(Optional.empty(), this.errors);
        }

        /**
         * Otherwise, this and every other parse must have succeeded.
         */

        final KSResult.KSSuccess<Optional<KSElement.KSBlock<KSParse>>, KSParseError> rs =
          (KSResult.KSSuccess<Optional<KSElement.KSBlock<KSParse>>, KSParseError>) rr;

        /**
         * However... If the final parse does not actually yield a result,
         * the result is still failure! Note: This is not actually reachable
         * via a working implementation of the imperative builder interface,
         * but the types say it may happen, so it may.
         */

        final Optional<KSElement.KSBlock<KSParse>> rs_opt = rs.getResult();
        if (!rs_opt.isPresent()) {
          this.errors.add(new KSParseError(
            this.last_pos, "Parsed file did not yield a single block"));
          return new KSResult.KSFailure<>(Optional.empty(), this.errors);
        }

        return new KSResult.KSSuccess<>(rs_opt.get());
      }

      /**
       * Try to parse an imperative command.
       */

      final KSExpression expr = e_opt.get();
      final KSResult<KSImperative, KSParseError> ic =
        ibp.parse(context, expr, file);

      /**
       * On failure, accumulate errors and continue.
       */

      if (ic instanceof KSResult.KSFailure) {
        final KSResult.KSFailure<KSImperative, KSParseError> icf =
          (KSResult.KSFailure<KSImperative, KSParseError>) ic;
        this.errors.addAll(icf.getErrors());
        continue;
      }

      final KSResult.KSSuccess<KSImperative, KSParseError> ics =
        (KSResult.KSSuccess<KSImperative, KSParseError>) ic;

      final KSImperative i = ics.getResult();
      KSImperativeMatch.INSTANCE.match(
        this,
        i,

        /**
         * If adding a block command fails, note the failure as having occurred
         * and accumulate errors.
         */

        (c, command) -> {
          final KSResult<Optional<KSElement.KSBlock<KSParse>>, KSParseError> rbb =
            ibb.add(context, command);

          if (rbb instanceof KSResult.KSFailure) {
            final KSResult.KSFailure<Optional<KSElement.KSBlock<KSParse>>, KSParseError> rbf =
              (KSResult.KSFailure<Optional<KSElement.KSBlock<KSParse>>, KSParseError>) rbb;
            c.errors.addAll(rbf.getErrors());
            c.block_failed = true;
          } else {
            c.block_failed = false;
          }

          return Unit.unit();
        },

        /**
         * If the most recent block command failed, don't submit inline
         * content as this would likely result in hundreds of spurious
         * errors.
         */

        (c, inline) -> {
          if (!c.block_failed) {
            final KSResult<Optional<KSElement.KSBlock<KSParse>>, KSParseError> rbb =
              ibb.add(context, inline);
            if (rbb instanceof KSResult.KSFailure) {
              final KSResult.KSFailure<Optional<KSElement.KSBlock<KSParse>>, KSParseError> rbf =
                (KSResult.KSFailure<Optional<KSElement.KSBlock<KSParse>>, KSParseError>) rbb;
              c.errors.addAll(rbf.getErrors());
            }
          } else {
            KSImperativeParserDriver.LOG.trace("ignoring inline until next block command");
          }
          return Unit.unit();
        },

        (c, eof) -> {
          throw new UnreachableCodeException();
        });
    }
  }
}
