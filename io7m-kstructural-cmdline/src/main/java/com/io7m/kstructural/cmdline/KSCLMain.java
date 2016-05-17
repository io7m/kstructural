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
import com.io7m.kstructural.frontend.KSOpCheck;
import com.io7m.kstructural.frontend.KSOpDump;
import com.io7m.kstructural.frontend.KSOpType;
import org.slf4j.LoggerFactory;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
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
    final CommandDump dump = new CommandDump();

    this.commands = new HashMap<>(8);
    this.commands.put("check", check);
    this.commands.put("dump", dump);

    this.commander = new JCommander(r);
    this.commander.setProgramName("kstructural");
    this.commander.addCommand("check", check);
    this.commander.addCommand("dump", dump);
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
      description = "Set logging verbosity level")
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
  private class CommandCheck extends CommandRoot
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
    commandDescription = "Dump a document in canonical format to standard out")
  private class CommandDump extends CommandRoot
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
      final KSOpType op = KSOpDump.create(p);
      return op.call();
    }
  }
}
