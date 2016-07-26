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

import com.io7m.kstructural.core.KSParseContextType;
import com.io7m.kstructural.core.KSParserDriverConstructorType;
import com.io7m.kstructural.core.KSParserDriverType;
import com.io7m.kstructural.parser.canon.KSCanonParserDriver;
import com.io7m.kstructural.parser.imperative.KSImperativeParserDriver;
import com.io7m.kstructural.xom.KSXOMXMLParserDriver;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * The default set of provided parsers.
 */

public final class KSParsers implements KSParserDriverConstructorType
{
  private static final org.slf4j.Logger LOG;
  private static KSParsers INSTANCE = new KSParsers();

  static {
    LOG = LoggerFactory.getLogger(KSParsers.class);
  }

  private KSParsers()
  {

  }

  /**
   * @return Access to the default parsers
   */

  public static KSParsers getInstance()
  {
    return KSParsers.INSTANCE;
  }

  /**
   * Create a new canonical format parser.
   *
   * @param context A parse context
   *
   * @return A new parser
   */

  public static KSParserDriverType createCanonical(
    final KSParseContextType context)
  {
    return KSCanonParserDriver.newDriver(KSParsers.INSTANCE);
  }

  /**
   * Create a new imperative format parser.
   *
   * @param context A parse context
   *
   * @return A new parser
   */

  public static KSParserDriverType createImperative(
    final KSParseContextType context)
  {
    return KSImperativeParserDriver.newDriver(KSParsers.INSTANCE);
  }

  /**
   * Create a new XML format parser.
   *
   * @param context A parse context
   *
   * @return A new parser
   */

  public static KSParserDriverType createXML(
    final KSParseContextType context)
  {
    return KSXOMXMLParserDriver.newDriver(KSParsers.INSTANCE);
  }

  @NotNull
  @Override
  public KSParserDriverType create(
    @NotNull final KSParseContextType context,
    @NotNull final Path file)
    throws IOException
  {
    final Path f_name = file.getFileName();
    if (f_name == null) {
      throw new NoSuchFileException("Not a file: " + file.toString());
    }

    final String name = f_name.toString();
    final int i = name.lastIndexOf('.');
    if (i >= 0) {
      final String suffix = name.substring(i + 1);
      switch (suffix) {
        case "xml": {
          KSParsers.LOG.trace("file suffix is 'xml', assuming XML format");
          return KSParsers.createXML(context);
        }
        case "sdi": {
          KSParsers.LOG.trace("file suffix is 'sdi', assuming imperative format");
          return KSParsers.createImperative(context);
        }
      }
    }

    KSParsers.LOG.trace("assuming canon format");
    return KSParsers.createCanonical(context);
  }


}
