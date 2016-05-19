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

import com.io7m.jfunctional.Unit;
import com.io7m.jlexing.core.ImmutableLexicalPosition;
import com.io7m.jlexing.core.ImmutableLexicalPositionType;
import com.io7m.jlexing.core.LexicalPositionType;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.kstructural.core.KSElement.KSBlock;
import com.io7m.kstructural.core.KSParse;
import com.io7m.kstructural.core.KSParseContextType;
import com.io7m.kstructural.core.KSParseError;
import com.io7m.kstructural.core.KSParserConstructorType;
import com.io7m.kstructural.core.KSParserType;
import com.io7m.kstructural.core.KSResult;
import com.io7m.kstructural.core.KSResult.KSFailure;
import com.io7m.kstructural.core.KSResult.KSSuccess;
import com.io7m.kstructural.core.KSResults;
import com.io7m.kstructural.parser.KSExpression;
import com.io7m.kstructural.parser.KSExpressionParserType;
import com.io7m.kstructural.parser.KSExpressionParsers;
import com.io7m.kstructural.parser.KSIncluderType;
import com.io7m.kstructural.parser.canon.KSCanonBlockParser;
import com.io7m.kstructural.parser.canon.KSCanonBlockParserType;
import com.io7m.kstructural.parser.canon.KSCanonInlineParser;
import com.io7m.kstructural.parser.canon.KSCanonInlineParserType;
import com.io7m.kstructural.parser.imperative.KSImperative;
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeEOF;
import com.io7m.kstructural.parser.imperative.KSImperativeBuilder;
import com.io7m.kstructural.parser.imperative.KSImperativeBuilderType;
import com.io7m.kstructural.parser.imperative.KSImperativeMatch;
import com.io7m.kstructural.parser.imperative.KSImperativeParser;
import com.io7m.kstructural.parser.imperative.KSImperativeParserType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Optional;

public final class KSParsers implements KSParserConstructorType
{
  private static final org.slf4j.Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(KSParsers.class);
  }

  private static KSParsers INSTANCE = new KSParsers();

  public static KSParsers getInstance()
  {
    return KSParsers.INSTANCE;
  }

  private KSParsers()
  {

  }

  @Override
  public KSParserType create(
    final KSParseContextType context,
    final Path file)
    throws IOException
  {
    final Path f_name = file.getFileName();
    if (f_name == null) {
      throw new NoSuchFileException("Not a file: " + file.toString());
    }

    final String name = f_name.toString();
    final int i = name.lastIndexOf('.');
    if (i >= 0) {
      final String suffix = name.substring(i + 1);
      switch (suffix) {
        case "xml": {
          KSParsers.LOG.trace("file suffix is 'xml', assuming XML format");
          return KSParsers.createXML(context);
        }
        case "sdi": {
          KSParsers.LOG.trace("file suffix is 'sdi', assuming imperative format");
          return KSParsers.createImperative(context);
        }
      }
    }

    KSParsers.LOG.trace("assuming canon format");
    return KSParsers.createCanonical(context);
  }

  private static final KSIncluderType INCLUDER = path -> {
    try {
      final byte[] ba = Files.readAllBytes(path);
      final String s = new String(ba, StandardCharsets.UTF_8);
      return new KSResult.KSSuccess<>(s);
    } catch (final IOException e) {
      return KSResults.fail((Throwable) e);
    }
  };

  public static KSParserType createCanonical(
    final KSParseContextType context)
  {
    return (c, ff_file) -> {
      final KSCanonInlineParserType inlines =
        KSCanonInlineParser.Companion.create(KSParsers.INCLUDER);
      final KSCanonBlockParserType bp =
        KSCanonBlockParser.Companion.create(inlines, KSParsers.INSTANCE);
      final KSExpressionParserType s_expressions =
        KSExpressionParsers.INSTANCE.create(ff_file);

      final Optional<KSExpression> e_opt = s_expressions.parse();
      if (e_opt.isPresent()) {
        return bp.parse(context, e_opt.get(), ff_file);
      }

      final ImmutableLexicalPositionType<Path> pos =
        ImmutableLexicalPosition.newPositionWithFile(0, 0, ff_file);
      return KSResults.fail(
        new KSParseError(Optional.of(pos), "Unexpected EOF"));
    };
  }

  private static final class Imperative implements KSParserType
  {
    private boolean block_failed;
    private Optional<LexicalPositionType<Path>> last_pos;
    private ArrayDeque<KSParseError> errors;

    Imperative()
    {
      this.last_pos = Optional.empty();
      this.errors = new ArrayDeque<>(128);
      this.block_failed = false;
    }

    public KSResult<KSBlock<KSParse>, KSParseError> parseBlock(
      @NotNull final KSParseContextType context,
      @NotNull final Path file)
      throws IOException
    {
      final KSCanonInlineParserType inlines =
        KSCanonInlineParser.Companion.create(KSParsers.INCLUDER);

      final KSExpressionParserType s_expressions =
        KSExpressionParsers.INSTANCE.create(file);
      final KSImperativeParserType ibp =
        KSImperativeParser.Companion.create(inlines, KSParsers.INSTANCE);
      final KSImperativeBuilderType ibb =
        KSImperativeBuilder.Companion.create();

      while (true) {
        final Optional<KSExpression> e_opt = s_expressions.parse();

        /**
         * Has EOF been reached?
         */

        if (!e_opt.isPresent()) {
          final KSImperativeEOF eof = new KSImperativeEOF(this.last_pos);
          final KSResult<Optional<KSBlock<KSParse>>, KSParseError> rr =
            ibb.add(context, eof);

          /**
           * If the last parse fails, the result is failure with all of
           * the errors that have been accumulated.
           */

          if (rr instanceof KSFailure) {
            final KSFailure<Optional<KSBlock<KSParse>>, KSParseError> rf =
              (KSFailure<Optional<KSBlock<KSParse>>, KSParseError>) rr;
            this.errors.addAll(rf.getErrors());
            return new KSFailure<>(Optional.empty(), this.errors);
          }

          /**
           * Otherwise, if *any* parse has failed, the result is failure with
           * all of the errors that have been accumulated.
           */

          if (!this.errors.isEmpty()) {
            return new KSFailure<>(Optional.empty(), this.errors);
          }

          /**
           * Otherwise, this and every other parse must have succeeded.
           */

          final KSSuccess<Optional<KSBlock<KSParse>>, KSParseError> rs =
            (KSSuccess<Optional<KSBlock<KSParse>>, KSParseError>) rr;

          /**
           * However... If the final parse does not actually yield a result,
           * the result is still failure!
           */

          final Optional<KSBlock<KSParse>> rs_opt = rs.getResult();
          if (!rs_opt.isPresent()) {
            this.errors.add(new KSParseError(
              this.last_pos, "Parsed file did not yield a single block"));
            return new KSFailure<>(Optional.empty(), this.errors);
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

        if (ic instanceof KSFailure) {
          final KSFailure<KSImperative, KSParseError> icf =
            (KSFailure<KSImperative, KSParseError>) ic;
          this.errors.addAll(icf.getErrors());
          continue;
        }

        final KSSuccess<KSImperative, KSParseError> ics =
          (KSSuccess<KSImperative, KSParseError>) ic;

        final KSImperative i = ics.getResult();
        KSImperativeMatch.INSTANCE.match(
          this,
          i,

          /**
           * If adding a block command fails, note the failure as having occurred
           * and accumulate errors.
           */

          (c, command) -> {
            final KSResult<Optional<KSBlock<KSParse>>, KSParseError> rbb =
              ibb.add(context, command);

            if (rbb instanceof KSFailure) {
              final KSFailure<Optional<KSBlock<KSParse>>, KSParseError> rbf =
                (KSFailure<Optional<KSBlock<KSParse>>, KSParseError>) rbb;
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
              final KSResult<Optional<KSBlock<KSParse>>, KSParseError> rbb =
                ibb.add(context, inline);
              if (rbb instanceof KSFailure) {
                final KSFailure<Optional<KSBlock<KSParse>>, KSParseError> rbf =
                  (KSFailure<Optional<KSBlock<KSParse>>, KSParseError>) rbb;
                c.errors.addAll(rbf.getErrors());
              }
            } else {
              KSParsers.LOG.trace("ignoring inline until next block command");
            }
            return Unit.unit();
          },

          (c, eof) -> {
            throw new UnreachableCodeException();
          });
      }
    }
  }

  public static KSParserType createImperative(
    final KSParseContextType context)
  {
    return new Imperative();
  }

  public static KSParserType createXML(
    final KSParseContextType context)
  {
    throw new UnimplementedCodeException();
  }
}
