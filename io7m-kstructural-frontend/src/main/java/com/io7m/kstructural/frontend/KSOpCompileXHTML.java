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
import com.io7m.kstructural.xom.KSXOMSettings;
import com.io7m.kstructural.xom.KSXOMXHTMLMultiWriter;
import com.io7m.kstructural.xom.KSXOMXHTMLSingleWriter;
import com.io7m.kstructural.xom.KSXOMXHTMLWriterType;
import nu.xom.Document;
import nu.xom.Serializer;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.function.Supplier;

/**
 * An operation that compiles a document to XHTML.
 */

public final class KSOpCompileXHTML implements KSOpType
{
  private static final org.slf4j.Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(KSOpCompileXHTML.class);
  }

  private final KSXOMSettings settings;
  private final XHTMLPagination pagination;
  private final Path path;
  private final Path output_path;
  private final boolean css_create_default;

  private KSOpCompileXHTML(
    final Path in_path,
    final Path in_output_path,
    final KSXOMSettings in_settings,
    final XHTMLPagination in_pagination,
    final boolean in_css_create_default)
  {
    this.path = NullCheck.notNull(in_path).toAbsolutePath();
    this.output_path = NullCheck.notNull(in_output_path);
    this.settings = NullCheck.notNull(in_settings);
    this.pagination = NullCheck.notNull(in_pagination);
    this.css_create_default = in_css_create_default;
  }

  /**
   * Construct a new operation.
   *
   * @param in_path               The input file
   * @param in_output_path        The output directory
   * @param in_settings           The export settings
   * @param in_pagination         The pagination type (single or multi-page,
   *                              etc)
   * @param in_css_create_default {@code true} iff the default provided CSS
   *                              files should be written to the output
   *                              directory
   *
   * @return A new operation
   */

  public static KSOpType create(
    final Path in_path,
    final Path in_output_path,
    final KSXOMSettings in_settings,
    final XHTMLPagination in_pagination,
    final boolean in_css_create_default)
  {
    return new KSOpCompileXHTML(
      in_path,
      in_output_path,
      in_settings,
      in_pagination,
      in_css_create_default);
  }

  private static void writeCSS(
    final Supplier<Path> get_path,
    final Supplier<InputStream> get_stream)
    throws IOException
  {
    final Path file = get_path.get();
    KSOpCompileXHTML.LOG.debug("write css: {}", file);
    try (final OutputStream os = Files.newOutputStream(
      file,
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING)) {
      try (final InputStream is = get_stream.get()) {
        KSOpCompileXHTML.copyStream(os, is);
      }
    }
  }

  private static void copyStream(
    final OutputStream os,
    final InputStream is)
    throws IOException
  {
    final byte[] buffer = new byte[4096];
    while (true) {
      final int r = is.read(buffer);
      if (r == -1) {
        break;
      }
      os.write(buffer, 0, r);
    }
  }

  @Override
  public Unit call()
    throws Exception
  {
    final KSBlockDocument<KSEvaluation> document =
      KSParseAndEvaluate.parseAndEvaluate(this.path.getParent(), this.path);

    KSXOMXHTMLWriterType w = null;
    switch (this.pagination) {
      case XHTML_SINGLE_PAGE: {
        w = KSXOMXHTMLSingleWriter.INSTANCE;
        break;
      }
      case XHTML_MULTI_PAGE: {
        w = KSXOMXHTMLMultiWriter.INSTANCE;
        break;
      }
    }

    Files.createDirectories(this.output_path);

    final Map<String, Document> pages = w.write(this.settings, document);
    for (final String name : pages.keySet()) {
      final Document doc = pages.get(name);
      final Path file = this.output_path.resolve(name);

      KSOpCompileXHTML.LOG.debug("write {}", file);
      try (final OutputStream os = Files.newOutputStream(
        file,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING)) {
        final Serializer s = new Serializer(os, "UTF-8");
        s.write(doc);
        s.flush();
      }
    }

    if (this.css_create_default) {
      KSOpCompileXHTML.writeCSS(
        () -> this.output_path.resolve(KSXOMSettings.Companion.getCSSDefaultLayout().getPath()),
        KSXOMSettings.Companion::getCSSDefaultLayoutStream);
      KSOpCompileXHTML.writeCSS(
        () -> this.output_path.resolve(KSXOMSettings.Companion.getCSSDefaultColour().getPath()),
        KSXOMSettings.Companion::getCSSDefaultColourStream);
    }

    return Unit.unit();
  }

  /**
   * The type of XHTML pagination.
   */

  public enum XHTMLPagination
  {
    /**
     * The output will be a single XHTML page.
     */

    XHTML_SINGLE_PAGE("single"),

    /**
     * The output will be written to multiple XHTML pages, one page per {@code
     * document}, {@code section}, and {@code part}.
     */

    XHTML_MULTI_PAGE("multi");

    private final String name;

    XHTMLPagination(final String in_name)
    {
      this.name = NullCheck.notNull(in_name);
    }

    @Override
    public String toString()
    {
      return this.name;
    }

    /**
     * @return The pagination type name
     */

    public String getName()
    {
      return this.name;
    }
  }

}
