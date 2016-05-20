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
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.kstructural.core.KSElement.KSBlock;
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument;
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockImport;
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineInclude;
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineText;
import com.io7m.kstructural.core.KSImportPathEdge;
import com.io7m.kstructural.core.evaluator.KSEvaluation;
import com.io7m.kstructural.core.evaluator.KSEvaluationContextType;
import com.io7m.kstructural.pretty.KSPrettyPrinterType;
import com.io7m.kstructural.pretty.canon.KSCanonPrettyPrinter;
import com.io7m.kstructural.pretty.imperative.KSImperativePrettyPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public final class KSExporter implements KSExporterType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(KSExporter.class);
  }

  private final KSInputFormat format;

  private KSExporter(final KSInputFormat in_format)
  {
    this.format = NullCheck.notNull(in_format);
  }

  public static KSExporterType newExporter(final KSInputFormat in_format)
  {
    return new KSExporter(in_format);
  }

  @Override
  public Set<Path> export(
    final Path base_directory,
    final KSBlockDocument<KSEvaluation> in_document,
    final Path out_directory,
    final boolean reconstruct_imports)
    throws IOException
  {
    if (reconstruct_imports) {
      KSExporter.LOG.debug("exporting with imports");
      return this.exportReconstructingImports(
        base_directory, in_document, out_directory);
    }

    KSExporter.LOG.debug("exporting without imports");
    return this.exportWithoutImports(in_document, out_directory);
  }

  private Set<Path> exportWithoutImports(
    final KSBlockDocument<KSEvaluation> in_document,
    final Path out_directory)
    throws IOException
  {
    final Path main_out = out_directory.resolve("main." + this.suffix());
    KSExporter.LOG.debug("write: {}", main_out);

    final Set<Path> s = new HashSet<>(64);
    s.add(main_out);

    this.pretty(
      in_document,
      main_out,
      (b) -> Optional.empty(),
      (i) -> Optional.empty());
    return s;
  }

  private Set<Path> exportReconstructingImports(
    final Path base_directory,
    final KSBlockDocument<KSEvaluation> in_document,
    final Path out_directory)
    throws IOException
  {
    final KSEvaluation eval = in_document.getData();
    final KSEvaluationContextType context = eval.getContext();
    final Map<KSBlockImport<KSEvaluation>, KSImportPathEdge> edges =
      context.getImportsPaths();
    final Map<KSInlineText<KSEvaluation>, KSInlineInclude<KSEvaluation>> include_map =
      context.getIncludesByText();
    final Map<KSBlock<KSEvaluation>, KSBlockImport<KSEvaluation>> import_map =
      context.getImports();

    final Function<KSInlineText<KSEvaluation>, Optional<KSInlineInclude<KSEvaluation>>> includes =
      (include) -> {
        if (include_map.containsKey(include)) {
          return Optional.of(include_map.get(include));
        }
        return Optional.empty();
      };

    final Path main_out = out_directory.resolve("main." + this.suffix());
    final Set<Path> s = new HashSet<>(64);
    s.add(main_out);

    {
      final Function<KSBlock<KSEvaluation>, Optional<KSBlockImport<KSEvaluation>>>
        imports = (block) -> {
        if (Objects.equals(block, in_document)) {
          return Optional.empty();
        }
        if (import_map.containsKey(block)) {
          return Optional.of(this.filterImport(import_map.get(block)));
        }
        return Optional.empty();
      };

      this.pretty(in_document, main_out, includes, imports);
    }

    {
      for (final KSBlock<KSEvaluation> b : import_map.keySet()) {
        final KSBlockImport<KSEvaluation> i = import_map.get(b);
        final KSImportPathEdge edge = edges.get(i);
        final Path imported = edge.getTo();
        final Path imported_rel = base_directory.relativize(imported);
        final Path exported = out_directory.resolve(imported_rel);
        final Path exported_suf = KSFileSuffixes.replace(
          exported, this.suffix());

        KSExporter.LOG.debug(
          "mapping {} -> {} -> {} -> {}",
          imported,
          imported_rel,
          exported,
          exported_suf);

        final Function<KSBlock<KSEvaluation>, Optional<KSBlockImport<KSEvaluation>>>
          imports = (block) -> {
          if (Objects.equals(block, b)) {
            return Optional.empty();
          }
          if (import_map.containsKey(block)) {
            return Optional.of(this.filterImport(import_map.get(block)));
          }
          return Optional.empty();
        };

        s.add(exported_suf);
        this.pretty(b, exported_suf, includes, imports);
      }
    }

    return s;
  }

  private String suffix()
  {
    switch (this.format) {
      case KS_INPUT_CANONICAL:
        return "sd";
      case KS_INPUT_IMPERATIVE:
        return "sdi";
      case KS_INPUT_XML:
        return "xml";
    }

    throw new UnreachableCodeException();
  }

  private void pretty(
    final KSBlock<KSEvaluation> b,
    final Path out,
    final Function<KSInlineText<KSEvaluation>, Optional<KSInlineInclude<KSEvaluation>>> includes,
    final Function<KSBlock<KSEvaluation>, Optional<KSBlockImport<KSEvaluation>>> imports)
    throws IOException
  {
    final String name_tmp = out.getFileName() + ".tmp";
    final Path out_tmp = out.resolveSibling(name_tmp);

    KSExporter.LOG.debug("write: {} -> {}", out_tmp, out);
    try (final KSPrettyPrinterType<KSEvaluation> pretty =
           KSExporter.prettyForPath(this.format, out_tmp, imports, includes)) {
      pretty.pretty(b);
    }

    Files.move(out_tmp, out, StandardCopyOption.ATOMIC_MOVE);
  }

  private KSBlockImport<KSEvaluation> filterImport(
    final KSBlockImport<KSEvaluation> imp)
  {
    final KSInlineText<KSEvaluation> orig_file = imp.getFile();
    final String new_file_path =
      KSFileSuffixes.replaceSuffix(orig_file.getText(), this.suffix());
    KSExporter.LOG.debug(
      "filter {} -> {}", orig_file.getText(), new_file_path);

    final KSInlineText<KSEvaluation> new_file = new KSInlineText<>(
      orig_file.getPosition(),
      orig_file.getSquare(),
      orig_file.getData(),
      orig_file.getQuote(),
      new_file_path);
    return new KSBlockImport<>(
      imp.getPosition(),
      imp.getSquare(),
      imp.getData(),
      imp.getType(),
      imp.getId(),
      new_file);
  }

  private static KSPrettyPrinterType<KSEvaluation> prettyForPath(
    final KSInputFormat format,
    final Path out,
    final Function<KSBlock<KSEvaluation>, Optional<KSBlockImport<KSEvaluation>>> imports,
    final Function<KSInlineText<KSEvaluation>, Optional<KSInlineInclude<KSEvaluation>>> includes)
    throws IOException
  {
    final OutputStream os = Files.newOutputStream(
      out,
      StandardOpenOption.TRUNCATE_EXISTING,
      StandardOpenOption.CREATE);
    final OutputStreamWriter w = new OutputStreamWriter(os);

    switch (format) {
      case KS_INPUT_CANONICAL:
        return KSCanonPrettyPrinter.Companion.create(
          w, 80, 2, imports, includes);
      case KS_INPUT_IMPERATIVE:
        return KSImperativePrettyPrinter.Companion.create(
          w, 80, imports, includes);
      case KS_INPUT_XML:
        throw new UnimplementedCodeException();
    }

    throw new UnreachableCodeException();
  }
}
