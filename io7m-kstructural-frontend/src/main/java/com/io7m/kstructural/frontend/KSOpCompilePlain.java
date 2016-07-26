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
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument;
import com.io7m.kstructural.core.evaluator.KSEvaluation;
import com.io7m.kstructural.plain.KSPlainSettings;
import com.io7m.kstructural.plain.KSPlainWriter;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * An operation that compiles a document to plain text.
 */

public final class KSOpCompilePlain implements KSOpType
{
  private static final org.slf4j.Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(KSOpCompilePlain.class);
  }

  private final KSPlainSettings settings;
  private final Path path;
  private final Path output_path;

  private KSOpCompilePlain(
    final Path in_path,
    final Path in_output_path,
    final KSPlainSettings in_settings)
  {
    this.path = NullCheck.notNull(in_path).toAbsolutePath();
    this.output_path = NullCheck.notNull(in_output_path);
    this.settings = NullCheck.notNull(in_settings);
  }

  /**
   * Construct a new operation.
   *
   * @param in_path        The input file
   * @param in_output_path The output directory
   * @param in_settings    The export settings
   *
   * @return A new operation
   */

  public static KSOpType create(
    final Path in_path,
    final Path in_output_path,
    final KSPlainSettings in_settings)
  {
    return new KSOpCompilePlain(
      in_path,
      in_output_path,
      in_settings);
  }

  @Override
  public Unit call()
    throws Exception
  {
    final KSBlockDocument<KSEvaluation> document =
      KSParseAndEvaluate.parseAndEvaluate(this.path.getParent(), this.path);

    Files.createDirectories(this.output_path);

    try (final OutputStream os = Files.newOutputStream(
      this.output_path.resolve("main.txt"))) {
      try (final Writer out =
             new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
        KSPlainWriter.INSTANCE.write(this.settings, document, out);
      }
    }

    return Unit.unit();
  }
}
