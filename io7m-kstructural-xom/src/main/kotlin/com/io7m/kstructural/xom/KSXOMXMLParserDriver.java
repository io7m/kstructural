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

package com.io7m.kstructural.xom;

import com.io7m.jfunctional.Unit;
import com.io7m.jlexing.core.ImmutableLexicalPosition;
import com.io7m.jlexing.core.ImmutableLexicalPositionType;
import com.io7m.jnull.NullCheck;
import com.io7m.kstructural.core.KSElement;
import com.io7m.kstructural.core.KSParse;
import com.io7m.kstructural.core.KSParseContextType;
import com.io7m.kstructural.core.KSParseError;
import com.io7m.kstructural.core.KSParserDriverConstructorType;
import com.io7m.kstructural.core.KSParserDriverType;
import com.io7m.kstructural.core.KSResult;
import com.io7m.kstructural.core.KSResults;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;
import nu.xom.ValidityException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Optional;

/**
 * A driver for XML parsers.
 */

public final class KSXOMXMLParserDriver implements KSParserDriverType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(KSXOMXMLParserDriver.class);
  }

  private final KSParserDriverConstructorType parsers;

  /**
   * Construct a new driver for XML parsers.
   *
   * @param in_parsers A driver constructor
   *
   * @return A new driver
   */

  public static KSParserDriverType newDriver(
    final KSParserDriverConstructorType in_parsers)
  {
    return new KSXOMXMLParserDriver(in_parsers);
  }

  private KSXOMXMLParserDriver(
    final KSParserDriverConstructorType in_parsers)
  {
    this.parsers = NullCheck.notNull(in_parsers);
  }

  private static KSResult<Unit, KSParseError> validateDocument(
    final Path base_directory,
    final Path file)
  {
    try (final InputStream is =
           Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
      if (!KSJingValidation.validate(base_directory, file, is)) {
        final KSParseError pe =
          new KSParseError(Optional.empty(), "Validation failed");
        return KSResults.fail(pe);
      }
      return new KSResult.KSSuccess<>(Unit.unit());
    } catch (final SAXException e) {
      final KSParseError pe =
        new KSParseError(Optional.empty(), e.getMessage());
      return KSResults.fail(pe);
    } catch (final IOException e) {
      final KSParseError pe =
        new KSParseError(Optional.empty(), e.getMessage());
      return KSResults.fail(pe);
    }
  }

  private static KSResult<Document, KSParseError> parseDocument(
    final Path base_directory,
    final Path file)
    throws IOException
  {
    try (final InputStream is =
           Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
      final Builder b = KSXOMXMLParserDriver.newBuilder(base_directory);
      final Document e = b.build(is, file.toString());
      return new KSResult.KSSuccess<>(e);
    } catch (final ValidityException e) {
      final FileSystem fs = file.getFileSystem();
      final ImmutableLexicalPositionType<Path> pos =
        ImmutableLexicalPosition.newPositionWithFile(
          e.getLineNumber(),
          e.getColumnNumber(),
          fs.getPath(e.getURI()));
      final StringBuilder sb = new StringBuilder(128);
      sb.append("Parsing failed.");
      sb.append(System.lineSeparator());
      sb.append("  Cause: ");
      sb.append(e);
      sb.append(System.lineSeparator());

      final KSParseError pe =
        new KSParseError(Optional.of(pos), sb.toString());
      return KSResults.fail(pe);
    } catch (final ParsingException e) {
      if (KSXOMXMLParserDriver.LOG.isDebugEnabled()) {
        KSXOMXMLParserDriver.LOG.debug("parsing exception: ", e);
      }

      final FileSystem fs = file.getFileSystem();
      final ImmutableLexicalPositionType<Path> pos =
        ImmutableLexicalPosition.newPositionWithFile(
          e.getLineNumber(),
          e.getColumnNumber(),
          fs.getPath(e.getURI()));

      final StringBuilder sb = new StringBuilder(128);
      sb.append("Parsing failed.");
      sb.append(System.lineSeparator());
      sb.append("  Cause: ");
      sb.append(e);
      sb.append(System.lineSeparator());

      final KSParseError pe =
        new KSParseError(Optional.of(pos), sb.toString());
      return KSResults.fail(pe);
    } catch (final ParserConfigurationException e) {
      final StringBuilder sb = new StringBuilder(128);
      sb.append("Parsing failed.");
      sb.append(System.lineSeparator());
      sb.append("  Cause: ");
      sb.append(e);
      sb.append(System.lineSeparator());
      final KSParseError pe = new KSParseError(Optional.empty(), sb.toString());
      return KSResults.fail(pe);
    } catch (final SAXException e) {
      final StringBuilder sb = new StringBuilder(128);
      sb.append("Parsing failed.");
      sb.append(System.lineSeparator());
      sb.append("  Cause: ");
      sb.append(e);
      sb.append(System.lineSeparator());
      final KSParseError pe = new KSParseError(Optional.empty(), sb.toString());
      return KSResults.fail(pe);
    }
  }

  @NotNull
  private static Builder newBuilder(final Path base)
    throws ParserConfigurationException, SAXException
  {
    final SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setValidating(false);
    factory.setNamespaceAware(true);
    factory.setXIncludeAware(true);
    factory.setFeature("http://apache.org/xml/features/xinclude", true);

    final SAXParser file_parser = factory.newSAXParser();
    final XMLReader file_reader = file_parser.getXMLReader();
    file_reader.setEntityResolver(new KSRestrictedEntityResolver(base));
    return new Builder(file_reader);
  }

  private static KSResult<KSElement.KSBlock<KSParse>, KSParseError> failOutsideBase(
    final Path file,
    final Path base)
  {
    final StringBuilder sb = new StringBuilder(128);
    sb.append("Refusing to import file outside of the base directory.");
    sb.append(System.lineSeparator());
    sb.append("  Base: ");
    sb.append(base);
    sb.append(System.lineSeparator());
    sb.append("  File: ");
    sb.append(file);
    sb.append(System.lineSeparator());
    return KSResults.fail(new KSParseError(Optional.empty(), sb.toString()));
  }

  @NotNull
  @Override
  public KSResult<KSElement.KSBlock<KSParse>, KSParseError> parseBlock(
    @NotNull final KSParseContextType context,
    @NotNull final Path file)
    throws IOException
  {
    final Path file_abs = file.toAbsolutePath();
    final Path base = context.getBaseDirectory();
    if (KSXOMXMLParserDriver.LOG.isTraceEnabled()) {
      KSXOMXMLParserDriver.LOG.trace("base: {}", base);
      KSXOMXMLParserDriver.LOG.trace("file: {}", file_abs);
    }

    if (!file_abs.startsWith(base)) {
      return KSXOMXMLParserDriver.failOutsideBase(file, base);
    }

    final KSResult<Document, KSParseError> d =
      KSXOMXMLParserDriver.parseDocument(context.getBaseDirectory(), file);

    return d.flatMap(
      document ->
        KSXOMXMLParserDriver.validateDocument(
          context.getBaseDirectory(), file).flatMap(x -> {
          final KSXOMInlineParserType ip =
            KSXOMInlineParser.Companion.create();
          final KSXOMBlockParserType bp =
            KSXOMBlockParser.Companion.create(ip);
          return bp.parse(context, document.getRootElement());
        }));
  }
}
