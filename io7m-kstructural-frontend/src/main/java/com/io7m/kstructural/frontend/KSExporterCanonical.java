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
import com.io7m.kstructural.core.KSBlockMatch;
import com.io7m.kstructural.core.KSElement.KSBlock;
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument;
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockImport;
import com.io7m.kstructural.core.evaluator.KSEvaluation;
import com.io7m.kstructural.core.evaluator.KSEvaluationContextType;
import com.io7m.kstructural.pretty.KSPrettyPrinterType;
import com.io7m.kstructural.pretty.canon.KSCanonPrettyPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class KSExporterCanonical implements KSExporterType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(KSExporterCanonical.class);
  }

  private KSExporterCanonical()
  {

  }

  public static KSExporterType newExporter()
  {
    return new KSExporterCanonical();
  }

  @Override
  public Set<Path> export(
    final KSBlockDocument<KSEvaluation> in_document,
    final Path out_directory,
    final boolean reconstruct_imports)
    throws IOException
  {
    if (reconstruct_imports) {
      KSExporterCanonical.LOG.debug("exporting with imports");
      return this.exportReconstructingImports(in_document, out_directory);
    }

    KSExporterCanonical.LOG.debug("exporting without imports");
    return KSExporterCanonical.exportWithoutImports(in_document, out_directory);
  }

  private static Set<Path> exportWithoutImports(
    final KSBlockDocument<KSEvaluation> in_document,
    final Path out_directory)
    throws IOException
  {
    final KSEvaluation eval = in_document.getData();
    final KSEvaluationContextType context = eval.getContext();
    final Map<KSBlock<KSEvaluation>, KSBlockImport<KSEvaluation>> imports =
      context.getImports();

    final Path main_out = out_directory.resolve("main.sd");
    KSExporterCanonical.LOG.debug("write: {}", main_out);

    final Set<Path> s = new HashSet<>(64);
    s.add(main_out);

    try (final KSPrettyPrinterType pretty = KSExporterCanonical.prettyForPath(
      main_out, false)) {
      pretty.pretty(in_document);
    }
    return s;
  }

  private Set<Path> exportReconstructingImports(
    final KSBlockDocument<KSEvaluation> in_document,
    final Path out_directory)
    throws IOException
  {
    final KSEvaluation eval = in_document.getData();
    final KSEvaluationContextType context = eval.getContext();
    final Map<KSBlock<KSEvaluation>, KSBlockImport<KSEvaluation>> imports =
      context.getImports();

    final Path main_out = out_directory.resolve("main.sd");
    KSExporterCanonical.LOG.debug("write: {}", main_out);

    final Set<Path> s = new HashSet<>(64);
    s.add(main_out);

    if (imports.containsKey(in_document)) {
      final KSBlockImport<KSEvaluation> i = imports.get(in_document);
      try (final KSPrettyPrinterType pretty = KSExporterCanonical.prettyForPath(
        main_out, true)) {
        pretty.pretty(i);
      }
    } else {
      try (final KSPrettyPrinterType pretty = KSExporterCanonical.prettyForPath(
        main_out, true)) {
        pretty.pretty(in_document);
      }
    }

    KSBlockMatch.INSTANCE.match(
      this,
      in_document,
      (c, document) -> Unit.unit(),
      (c, section) -> Unit.unit(),
      (c, subsection) -> Unit.unit(),
      (c, paragraph) -> Unit.unit(),
      (c, formal) -> Unit.unit(),
      (c, footnote) -> Unit.unit(),
      (c, part) -> Unit.unit(),
      (c, import_e) -> Unit.unit());

    return s;
  }

  private static KSPrettyPrinterType prettyForPath(
    final Path out,
    final boolean reconstruct_imports)
    throws IOException
  {
    final OutputStream os = Files.newOutputStream(
      out,
      StandardOpenOption.TRUNCATE_EXISTING,
      StandardOpenOption.CREATE);
    final OutputStreamWriter w = new OutputStreamWriter(os);
    return KSCanonPrettyPrinter.Companion.create(w, 80, 2, reconstruct_imports);
  }
}
