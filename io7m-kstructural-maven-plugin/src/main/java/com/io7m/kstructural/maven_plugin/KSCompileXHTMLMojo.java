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

package com.io7m.kstructural.maven_plugin;

import com.io7m.kstructural.frontend.KSBrandAppender;
import com.io7m.kstructural.frontend.KSOpCompileXHTML;
import com.io7m.kstructural.frontend.KSOpType;
import com.io7m.kstructural.xom.KSXOMSettings;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mojo(name = "compileXHTML", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public final class KSCompileXHTMLMojo extends AbstractMojo
{
  /**
   * The pagination type.
   */

  @Parameter(name = "pagination", required = true)
  private KSOpCompileXHTML.XHTMLPagination pagination;

  /**
   * The input document. All other files referenced from this document are
   * resolved relative to the document's location.
   */

  @Parameter(name = "documentFile", required = true)
  private String documentFile;

  /**
   * The directory that will be used to contain generated XHTML files.
   */

  @Parameter(name = "outputDirectory", required = true)
  private String outputDirectory;

  /**
   * An XML file containing branding for the generated documents.
   */

  @Parameter(name = "brandTopFile", required = false)
  private String brandTopFile;

  /**
   * An XML file containing branding for the generated documents.
   */

  @Parameter(name = "brandBottomFile", required = false)
  private String brandBottomFile;

  /**
   * Render a table of contents at the document level?
   */

  @Parameter(name = "renderTOCDocument", required = false)
  private boolean renderTOCDocument = true;

  /**
   * Render a table of contents at the part level?
   */

  @Parameter(name = "renderTOCPart", required = false)
  private boolean renderTOCPart = true;

  /**
   * Render a table of contents at the section level?
   */

  @Parameter(name = "renderTOCSection", required = false)
  private boolean renderTOCSection = true;

  /**
   * A list of extra CSS styles (as URIs) that will be used for each page.
   */

  @Parameter(name = "cssExtraStyles", required = false)
  private List<URI> cssExtraStyles = new ArrayList<>();

  /**
   * Include links to the default CSS files?
   */

  @Parameter(name = "cssIncludeDefault", required = false)
  private boolean cssIncludeDefault = true;

  /**
   * Create the default CSS files in the output directory?
   */

  @Parameter(name = "cssCreateDefault", required = false)
  private boolean cssCreateDefault = true;

  /**
   * Parameter to allow skipping of the generation.
   */

  @Parameter(name = "skip", property = "kstructural.skip", required = false)
  private boolean skip = false;

  public KSCompileXHTMLMojo()
  {

  }

  @Override
  public void execute()
    throws MojoExecutionException, MojoFailureException
  {
    try {
      if (this.skip) {
        return;
      }

      if (this.documentFile == null) {
        throw new IllegalArgumentException("input document not specified");
      }
      if (this.outputDirectory == null) {
        throw new IllegalArgumentException("output directory not specified");
      }
      if (this.pagination == null) {
        throw new IllegalArgumentException("pagination type not specified");
      }

      final Log log = this.getLog();
      log.info("documentFile               : " + this.documentFile);
      log.info("outputDirectory            : " + this.outputDirectory);
      log.info("brandTopFile               : " + this.brandTopFile);
      log.info("brandBottomFile            : " + this.brandBottomFile);
      log.info("pagination                 : " + this.pagination);
      log.info("renderTOCDocument          : " + this.renderTOCDocument);
      log.info("renderTOCSection           : " + this.renderTOCSection);
      log.info("renderTOCPart              : " + this.renderTOCPart);
      log.info("cssExtraStyles             : " + this.cssExtraStyles);
      log.info("cssIncludeDefault          : " + this.cssIncludeDefault);
      log.info("cssCreateDefault           : " + this.cssCreateDefault);

      final List<URI> styles = new ArrayList<>(8);
      if (this.cssIncludeDefault) {
        styles.add(KSXOMSettings.Companion.getCSSDefaultLayout());
        styles.add(KSXOMSettings.Companion.getCSSDefaultColour());
      }
      styles.addAll(this.cssExtraStyles);

      final FileSystem fs = FileSystems.getDefault();
      final Path input_path =
        fs.getPath(this.documentFile).toAbsolutePath();
      final Path output_path =
        fs.getPath(this.outputDirectory).toAbsolutePath();

      log.info("documentFile (resolved)    : " + input_path);
      log.info("outputDirectory (resolved) : " + output_path);

      final KSBrandAppender appender = KSBrandAppender.newAppender(
        Optional.ofNullable(this.brandTopFile).flatMap(
          name -> Optional.of(fs.getPath(name).toAbsolutePath())),
        Optional.ofNullable(this.brandBottomFile).flatMap(
          name -> Optional.of(fs.getPath(name).toAbsolutePath())));

      final KSXOMSettings s = new KSXOMSettings(
        this.renderTOCDocument,
        this.renderTOCPart,
        this.renderTOCSection,
        styles,
        appender.getAppenderStart(),
        appender.getAppenderEnd());

      final KSOpType op =
        KSOpCompileXHTML.create(
          input_path,
          output_path,
          s,
          this.pagination,
          this.cssCreateDefault);
      op.call();

    } catch (final Throwable e) {
      throw new MojoExecutionException("Transform error", e);
    }
  }
}
