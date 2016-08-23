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

import com.io7m.kstructural.frontend.KSOpCompileLaTeX;
import com.io7m.kstructural.frontend.KSOpType;
import com.io7m.kstructural.latex.KSLaTeXEmphasis;
import com.io7m.kstructural.latex.KSLaTeXSettings;
import com.io7m.kstructural.latex.KSLaTeXTypeMap;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Mojo(name = "compileLaTeX", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public final class KSCompileLaTeXMojo extends AbstractMojo
{
  /**
   * The input document. All other files referenced from this document are
   * resolved relative to the document's location.
   */

  @Parameter(name = "documentFile", required = true)
  private String documentFile;

  /**
   * The directory that will be used to contain generated LaTeX files.
   */

  @Parameter(name = "outputDirectory", required = true)
  private String outputDirectory;

  /**
   * The file that contains type mappings.
   */

  @Parameter(name = "typeMap", required = false)
  private String typeMap;

  public KSCompileLaTeXMojo()
  {

  }

  @Override
  public void execute()
    throws MojoExecutionException, MojoFailureException
  {
    try {
      if (this.documentFile == null) {
        throw new IllegalArgumentException("input document not specified");
      }
      if (this.outputDirectory == null) {
        throw new IllegalArgumentException("output directory not specified");
      }

      final Log log = this.getLog();
      log.info("documentFile               : " + this.documentFile);
      log.info("outputDirectory            : " + this.outputDirectory);
      log.info("typeMap                    : " + this.typeMap);

      final FileSystem fs = FileSystems.getDefault();
      final Path input_path =
        fs.getPath(this.documentFile).toAbsolutePath();
      final Path output_path =
        fs.getPath(this.outputDirectory).toAbsolutePath();

      log.info("documentFile (resolved)    : " + input_path);
      log.info("outputDirectory (resolved) : " + output_path);

      final Map<String, KSLaTeXEmphasis> emphasis = new HashMap<>();
      if (this.typeMap != null) {
        final Path type_map_path = fs.getPath(this.typeMap).toAbsolutePath();
        log.info("typeMap (resolved)         : " + type_map_path);

        try (final InputStream type_map_stream =
               Files.newInputStream(type_map_path)) {
          KSLaTeXTypeMap.fromStream(type_map_path, type_map_stream);
        }
      }

      final KSLaTeXSettings s =
        new KSLaTeXSettings(emphasis);

      final KSOpType op =
        KSOpCompileLaTeX.create(
          input_path,
          output_path,
          s);
      op.call();

    } catch (final Throwable e) {
      throw new MojoExecutionException("Transform error", e);
    }
  }
}
