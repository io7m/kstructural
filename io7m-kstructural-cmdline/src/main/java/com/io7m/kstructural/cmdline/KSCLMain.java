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

package com.io7m.kstructural.cmdline;

import ch.qos.logback.classic.Logger;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.kstructural.frontend.KSBrandAppender;
import com.io7m.kstructural.frontend.KSInputFormat;
import com.io7m.kstructural.frontend.KSOpCheck;
import com.io7m.kstructural.frontend.KSOpCompileXHTML;
import com.io7m.kstructural.frontend.KSOpConvert;
import com.io7m.kstructural.frontend.KSOpType;
import com.io7m.kstructural.xom.KSXOMSettings;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public final class KSCLMain implements Runnable
{
  private static final org.slf4j.Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(KSCLMain.class);
  }

  private final Map<String, CommandType> commands;
  private final JCommander commander;
  private final String[] args;
  private int exit_code = 0;

  private KSCLMain(final String[] in_args)
  {
    this.args = NullCheck.notNull(in_args);

    final CommandRoot r = new CommandRoot();
    final CommandCheck check = new CommandCheck();
    final CommandCompileXHTML comp_xhtml = new CommandCompileXHTML();
    final CommandConvert convert = new CommandConvert();

    this.commands = new HashMap<>(8);
    this.commands.put("check", check);
    this.commands.put("compile-xhtml", comp_xhtml);
    this.commands.put("convert", convert);

    this.commander = new JCommander(r);
    this.commander.setProgramName("kstructural");
    this.commander.addCommand("check", check);
    this.commander.addCommand("compile-xhtml", comp_xhtml);
    this.commander.addCommand("convert", convert);
  }

  public static void main(final String[] args)
  {
    final KSCLMain cm = new KSCLMain(args);
    cm.run();
    System.exit(cm.exitCode());
  }

  public int exitCode()
  {
    return this.exit_code;
  }

  @Override
  public void run()
  {
    try {
      this.commander.parse(this.args);

      final String cmd = this.commander.getParsedCommand();
      if (cmd == null) {
        final StringBuilder sb = new StringBuilder(128);
        this.commander.usage(sb);
        KSCLMain.LOG.info("Arguments required.\n{}", sb.toString());
        return;
      }

      final CommandType command = this.commands.get(cmd);
      command.call();

    } catch (final ParameterException e) {
      final StringBuilder sb = new StringBuilder(128);
      this.commander.usage(sb);
      KSCLMain.LOG.error("{}\n{}", e.getMessage(), sb.toString());
      this.exit_code = 1;
    } catch (final Exception e) {
      KSCLMain.LOG.error("{}", e.getMessage(), e);
      this.exit_code = 1;
    }
  }

  private interface CommandType extends Callable<Unit>
  {

  }

  private class CommandRoot implements CommandType
  {
    @Parameter(
      names = "-verbose",
      converter = KSCLLogLevelConverter.class,
      description = "Set the minimum logging verbosity level")
    protected KSCLLogLevel verbose = KSCLLogLevel.LOG_INFO;

    CommandRoot()
    {

    }

    @Override
    public Unit call()
      throws Exception
    {
      final ch.qos.logback.classic.Logger root =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
          Logger.ROOT_LOGGER_NAME);
      root.setLevel(this.verbose.toLevel());
      return Unit.unit();
    }
  }

  @Parameters(commandDescription = "Check document syntax and structure")
  private final class CommandCheck extends CommandRoot
  {
    @Parameter(
      names = "-file",
      description = "Input file",
      required = true)
    private String file;

    @Override
    public Unit call()
      throws Exception
    {
      super.call();

      final FileSystem fs = FileSystems.getDefault();
      final Path p = fs.getPath(this.file);
      final KSOpType op = KSOpCheck.create(p);
      return op.call();
    }
  }

  @Parameters(
    commandDescription = "Compile documents to XHTML")
  private final class CommandCompileXHTML extends CommandRoot
  {
    @Parameter(
      names = "-file",
      description = "Input file",
      required = true)
    private String file;

    @Parameter(
      names = "-output-dir",
      description = "The directory in which output files will be written",
      required = true)
    private String output;

    @Parameter(
      names = "-pagination",
      description = "The type of XHTML pagination that will be used",
      converter = KSCLXHTMLPaginationConverter.class,
      required = false)
    private KSOpCompileXHTML.XHTMLPagination pagination =
      KSOpCompileXHTML.XHTMLPagination.XHTML_MULTI_PAGE;

    @Parameter(
      names = "-render-toc-document",
      description = "Render a table of contents at the document level",
      required = false)
    private boolean render_toc_document = true;

    @Parameter(
      names = "-render-toc-part",
      description = "Render a table of contents at the part level",
      required = false)
    private boolean render_toc_part = true;

    @Parameter(
      names = "-render-toc-section",
      description = "Render a table of contents at the section level",
      required = false)
    private boolean render_toc_section = true;

    @Parameter(
      names = "-css-extra-styles",
      description = "A comma-separated list of extra CSS styles (as URIs) that will be used for each page",
      required = false)
    private List<URI> css_user = new ArrayList<>();

    @Parameter(
      names = "-css-include-default",
      description = "Include links to the default CSS files",
      required = false)
    private boolean css_default = true;

    @Parameter(
      names = "-css-create-default",
      description = "Create the default CSS files in the output directory",
      required = false)
    private boolean css_create_default = true;

    @Parameter(
      names = "-brand-top",
      description = "Prepend the contents of the given XML file to each XHTML page's body element",
      required = false)
    private String brand_top;

    @Parameter(
      names = "-brand-bottom",
      description = "Append the contents of the given XML file to each XHTML page's body element",
      required = false)
    private String brand_bottom;

    @Override
    public Unit call()
      throws Exception
    {
      super.call();

      final List<URI> styles = new ArrayList<>(8);
      if (this.css_default) {
        styles.add(KSXOMSettings.Companion.getCSSDefaultLayout());
        styles.add(KSXOMSettings.Companion.getCSSDefaultColour());
      }
      styles.addAll(this.css_user);

      final FileSystem fs = FileSystems.getDefault();
      final Path input_path = fs.getPath(this.file);
      final Path output_path = fs.getPath(this.output);

      final KSBrandAppender appender = KSBrandAppender.newAppender(
        Optional.ofNullable(this.brand_top).flatMap(
          name -> Optional.of(fs.getPath(name))),
        Optional.ofNullable(this.brand_bottom).flatMap(
          name -> Optional.of(fs.getPath(name))));

      final KSXOMSettings s = new KSXOMSettings(
        this.render_toc_document,
        this.render_toc_part,
        this.render_toc_section,
        styles,
        appender.getAppenderStart(),
        appender.getAppenderEnd());

      final KSOpType op =
        KSOpCompileXHTML.create(
          input_path,
          output_path,
          s,
          this.pagination,
          this.css_create_default);
      return op.call();
    }
  }

  @Parameters(
    commandDescription = "Convert documents between input formats")
  private final class CommandConvert extends CommandRoot
  {
    @Parameter(
      names = "-file",
      description = "Input file",
      required = true)
    private String file;

    @Parameter(
      names = "-output-dir",
      description = "The directory in which output files will be written",
      required = true)
    private String output;

    @Parameter(
      names = "-format",
      description = "The format that will be used for exported documents",
      converter = KSCLInputFormatConverter.class,
      required = false)
    private KSInputFormat export_format = KSInputFormat.KS_INPUT_CANONICAL;

    @Parameter(
      names = "-no-imports",
      description = "Export as one large document that does not contain any imports",
      required = false)
    private boolean no_imports = false;

    @Parameter(
      names = "-indent",
      description = "The number of spaces that will be used to indent documents",
      required = false)
    private int indent = 2;

    @Parameter(
      names = "-width",
      description = "The maximum width in characters that will be used when formatting documents",
      required = false)
    private int width = 80;

    @Override
    public Unit call()
      throws Exception
    {
      super.call();

      final FileSystem fs = FileSystems.getDefault();
      final Path input_path = fs.getPath(this.file);
      final Path output_path = fs.getPath(this.output);

      final KSOpType op =
        KSOpConvert.create(
          input_path,
          output_path,
          this.export_format,
          !this.no_imports,
          indent,
          width);
      return op.call();
    }
  }
}
