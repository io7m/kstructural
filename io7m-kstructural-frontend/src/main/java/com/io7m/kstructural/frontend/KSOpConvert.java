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
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public final class KSOpConvert implements KSOpType
{
  private static final org.slf4j.Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(KSOpConvert.class);
  }

  private final Path path;
  private final Path output_path;
  private final KSInputFormat output_format;
  private final boolean reconstruct_imports;
  private final int indent;
  private final int width;

  private KSOpConvert(
    final Path in_path,
    final Path in_output_path,
    final KSInputFormat in_output_format,
    final boolean in_reconstruct_imports,
    final int in_indent,
    final int in_width)
  {
    this.path = NullCheck.notNull(in_path);
    this.output_path = NullCheck.notNull(in_output_path);
    this.output_format = NullCheck.notNull(in_output_format);
    this.reconstruct_imports = in_reconstruct_imports;
    this.indent = in_indent;
    this.width = in_width;
  }

  public static KSOpType create(
    final Path in_path,
    final Path in_output_path,
    final KSInputFormat in_output_format,
    final boolean in_reconstruct_imports,
    final int in_indent,
    final int in_width)
  {
    return new KSOpConvert(
      in_path,
      in_output_path,
      in_output_format,
      in_reconstruct_imports,
      in_indent,
      in_width);
  }

  @Override
  public Unit call()
    throws Exception
  {
    final KSBlockDocument<KSEvaluation> document =
      KSParseAndEvaluate.parseAndEvaluate(this.path);

    Files.createDirectories(this.output_path);

    final KSExporterType export =
      KSExporter.newExporter(this.output_format, this.indent, this.width);

    export.export(
      this.path.getParent(),
      document,
      this.output_path,
      this.reconstruct_imports);
    return Unit.unit();
  }
}
