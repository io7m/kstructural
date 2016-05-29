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

import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.kstructural.core.KSBlockMatch;
import com.io7m.kstructural.core.KSElement;
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument;
import com.io7m.kstructural.core.KSParse;
import com.io7m.kstructural.core.KSParseContext;
import com.io7m.kstructural.core.KSParseContextType;
import com.io7m.kstructural.core.KSParseError;
import com.io7m.kstructural.core.KSParserDriverType;
import com.io7m.kstructural.core.KSResult;
import com.io7m.kstructural.core.evaluator.KSEvaluation;
import com.io7m.kstructural.core.evaluator.KSEvaluationError;
import com.io7m.kstructural.core.evaluator.KSEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Deque;

final class KSParseAndEvaluate
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(KSParseAndEvaluate.class);
  }

  private KSParseAndEvaluate()
  {
    throw new UnreachableCodeException();
  }

  static KSBlockDocument<KSEvaluation> parseAndEvaluate(
    final Path base,
    final Path file)
    throws IOException, KSOpFailed
  {
    KSParseAndEvaluate.LOG.debug("base directory: {}", base);
    KSParseAndEvaluate.LOG.debug("checking:       {}", file);

    final KSParseContextType context = KSParseContext.Companion.empty(base);
    final KSParsers parsers = KSParsers.getInstance();
    final KSParserDriverType p = parsers.create(context, file);

    KSParseAndEvaluate.LOG.debug("parsing");

    final KSResult<KSElement.KSBlock<KSParse>, KSParseError> parse_r;
    try {
      parse_r = p.parseBlock(context, file);
    } catch (final NoSuchFileException e) {
      KSParseAndEvaluate.LOG.error("file not found: {}", p);
      throw e;
    } catch (final IOException e) {
      KSParseAndEvaluate.LOG.error("i/o error: {}", p);
      throw e;
    }

    if (parse_r instanceof KSResult.KSFailure) {
      final KSResult.KSFailure<KSElement.KSBlock<KSParse>, KSParseError> f =
        (KSResult.KSFailure<KSElement.KSBlock<KSParse>, KSParseError>) parse_r;

      final Deque<KSParseError> errors = f.getErrors();
      for (final KSParseError e : errors) {
        KSParseAndEvaluate.LOG.error("{}", e.show());
      }
      throw new KSOpFailed();
    }

    if (parse_r instanceof KSResult.KSSuccess) {
      KSParseAndEvaluate.LOG.debug("parsed successfully");

      final KSResult.KSSuccess<KSElement.KSBlock<KSParse>, KSParseError> s =
        (KSResult.KSSuccess<KSElement.KSBlock<KSParse>, KSParseError>) parse_r;

      final KSElement.KSBlock<KSParse> r = s.getResult();
      return KSBlockMatch.INSTANCE.match(
        file,
        r,
        (c, doc) -> {
          KSParseAndEvaluate.LOG.debug("evaluating document");

          final KSResult<KSElement.KSBlock.KSBlockDocument<KSEvaluation>, KSEvaluationError> eval_r =
            KSEvaluator.INSTANCE.evaluate(doc, c);

          if (eval_r instanceof KSResult.KSFailure) {
            final KSResult.KSFailure<KSElement.KSBlock.KSBlockDocument<KSEvaluation>, KSEvaluationError> f =
              (KSResult.KSFailure<KSElement.KSBlock.KSBlockDocument<KSEvaluation>, KSEvaluationError>) eval_r;

            final Deque<KSEvaluationError> errors = f.getErrors();
            for (final KSEvaluationError e : errors) {
              KSParseAndEvaluate.LOG.error("{}", e.show());
            }
            throw new KSOpFailed();
          }

          KSParseAndEvaluate.LOG.debug("evaluated successfully");
          final KSResult.KSSuccess<KSElement.KSBlock.KSBlockDocument<KSEvaluation>, KSEvaluationError> rs =
            (KSResult.KSSuccess<KSElement.KSBlock.KSBlockDocument<KSEvaluation>, KSEvaluationError>) eval_r;
          return rs.getResult();
        },
        (c, section) -> {
          throw KSParseAndEvaluate.notADocument("section");
        },
        (c, subsection) -> {
          throw KSParseAndEvaluate.notADocument("subsection");
        },
        (c, paragraph) -> {
          throw KSParseAndEvaluate.notADocument("paragraph");
        },
        (c, formal) -> {
          throw KSParseAndEvaluate.notADocument("formal item");
        },
        (c, footnote) -> {
          throw KSParseAndEvaluate.notADocument("footnote");
        },
        (c, part) -> {
          throw KSParseAndEvaluate.notADocument("part");
        },
        (c, import_e) -> {
          throw KSParseAndEvaluate.notADocument("import");
        }
      );
    }

    throw new UnreachableCodeException();
  }

  private static KSOpFailed notADocument(final String type)
    throws KSOpFailed
  {
    KSParseAndEvaluate.LOG.error(
      "evaluated file yielded a {}; only documents may be compiled", type);
    return new KSOpFailed();
  }
}
