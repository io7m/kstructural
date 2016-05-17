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
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.kstructural.core.KSBlockMatch;
import com.io7m.kstructural.core.KSElement.KSBlock;
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument;
import com.io7m.kstructural.core.KSParse;
import com.io7m.kstructural.core.KSParseContext;
import com.io7m.kstructural.core.KSParseContextType;
import com.io7m.kstructural.core.KSParseError;
import com.io7m.kstructural.core.KSParserType;
import com.io7m.kstructural.core.KSResult;
import com.io7m.kstructural.core.KSResult.KSFailure;
import com.io7m.kstructural.core.KSResult.KSSuccess;
import com.io7m.kstructural.core.evaluator.KSEvaluation;
import com.io7m.kstructural.core.evaluator.KSEvaluationError;
import com.io7m.kstructural.core.evaluator.KSEvaluator;
import com.io7m.kstructural.pretty.KSPrettyPrinter;
import com.io7m.kstructural.pretty.KSPrettyPrinterType;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Deque;

public final class KSOpDump implements KSOpType
{
  private static final org.slf4j.Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(KSOpDump.class);
  }

  private final Path path;

  private KSOpDump(final Path p)
  {
    this.path = NullCheck.notNull(p);
  }

  public static KSOpType create(final Path p)
  {
    return new KSOpDump(p);
  }

  @Override
  public Unit call()
    throws Exception
  {
    KSOpDump.LOG.debug("checking {}", this.path);

    final KSParseContextType context = KSParseContext.Companion.empty();
    final KSParsers parsers = KSParsers.getInstance();
    final KSParserType p = parsers.create(context, this.path);

    KSOpDump.LOG.debug("parsing");

    final KSResult<KSBlock<KSParse>, KSParseError> parse_r;
    try {
      parse_r = p.parseBlock(context, this.path);
    } catch (final NoSuchFileException e) {
      LOG.error("file not found: {}", this.path);
      throw e;
    } catch (final IOException e) {
      LOG.error("i/o error: {}", this.path);
      throw e;
    }

    if (parse_r instanceof KSFailure) {
      final KSFailure<KSBlock<KSParse>, KSParseError> f =
        (KSFailure<KSBlock<KSParse>, KSParseError>) parse_r;

      final Deque<KSParseError> errors = f.getErrors();
      for (final KSParseError e : errors) {
        KSOpDump.LOG.error("{}", e.show());
      }
      throw new KSOpFailed();
    }

    if (parse_r instanceof KSSuccess) {
      KSOpDump.LOG.debug("parsed successfully");

      final KSSuccess<KSBlock<KSParse>, KSParseError> s =
        (KSSuccess<KSBlock<KSParse>, KSParseError>) parse_r;

      final KSBlock<KSParse> r = s.getResult();
      return KSBlockMatch.INSTANCE.match(
        this,
        r,
        (c, doc) -> {
          KSOpDump.LOG.debug("evaluating document");

          final KSResult<KSBlockDocument<KSEvaluation>, KSEvaluationError> eval_r =
            KSEvaluator.INSTANCE.evaluate(doc, c.path);

          if (eval_r instanceof KSFailure) {
            final KSFailure<KSBlockDocument<KSEvaluation>, KSEvaluationError> f =
              (KSFailure<KSBlockDocument<KSEvaluation>, KSEvaluationError>) eval_r;

            final Deque<KSEvaluationError> errors = f.getErrors();
            for (final KSEvaluationError e : errors) {
              KSOpDump.LOG.error("{}", e.show());
            }
            throw new KSOpFailed();
          }

          KSOpDump.LOG.debug("evaluated successfully");

          final KSSuccess<KSBlockDocument<KSEvaluation>, KSEvaluationError> rs =
            (KSSuccess<KSBlockDocument<KSEvaluation>, KSEvaluationError>) eval_r;

          try (final OutputStreamWriter w = new OutputStreamWriter(System.out)) {
            final KSPrettyPrinterType pp =
              KSPrettyPrinter.Companion.create(w, 80, 2, false);
            pp.pretty(rs.getResult());
            pp.finish();
            w.write(System.lineSeparator());
            w.flush();
          }

          return Unit.unit();
        },
        (c, section) -> {
          return Unit.unit();
        },
        (c, subsection) -> {
          return Unit.unit();
        },
        (c, paragraph) -> {
          return Unit.unit();
        },
        (c, formal) -> {
          return Unit.unit();
        },
        (c, footnote) -> {
          return Unit.unit();
        },
        (c, part) -> {
          return Unit.unit();
        },
        (c, import_e) -> {
          return Unit.unit();
        }
      );
    }

    throw new UnreachableCodeException();
  }
}
