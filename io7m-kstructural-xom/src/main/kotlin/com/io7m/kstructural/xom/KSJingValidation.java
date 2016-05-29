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

import com.io7m.kstructural.schema.KSSchemaResources;
import com.thaiopensource.util.PropertyMap;
import com.thaiopensource.util.PropertyMapBuilder;
import com.thaiopensource.validate.IncorrectSchemaException;
import com.thaiopensource.validate.Schema;
import com.thaiopensource.validate.ValidateProperty;
import com.thaiopensource.validate.Validator;
import com.thaiopensource.validate.auto.AutoSchemaReader;
import com.thaiopensource.validate.prop.rng.RngProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Convenient RELAX-NG validation with Jing.
 */

public final class KSJingValidation
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(KSJingValidation.class);
  }

  private KSJingValidation()
  {

  }

  /**
   * Validate the file {@code p} using the base directory {@code
   * base_directory}.
   *
   * @param base_directory The base directory
   * @param p              The file
   * @param is             A stream pointing to {@code p}
   *
   * @return {@code true} iff validation succeeds
   *
   * @throws IOException  On I/O errors
   * @throws SAXException On XML errors
   */

  public static boolean validate(
    final Path base_directory,
    final Path p,
    final InputStream is)
    throws IOException, SAXException
  {
    /**
     * Create a new error handler that records the fact that errors occurred.
     */

    final AtomicBoolean error_occurred = new AtomicBoolean(false);
    final ErrorHandler error_handler = new ErrorHandler()
    {
      @Override
      public void warning(final SAXParseException e)
        throws SAXException
      {
        KSJingValidation.LOG.warn(
          "validation: {}: {}:{}: {}",
          e.getPublicId(),
          Integer.valueOf(e.getLineNumber()),
          Integer.valueOf(e.getColumnNumber()),
          e.getMessage());
        error_occurred.set(true);
      }

      @Override
      public void error(final SAXParseException e)
        throws SAXException
      {
        KSJingValidation.LOG.error(
          "validation: {}: {}:{}: {}",
          e.getPublicId(),
          Integer.valueOf(e.getLineNumber()),
          Integer.valueOf(e.getColumnNumber()),
          e.getMessage());
        error_occurred.set(true);
      }

      @Override
      public void fatalError(final SAXParseException e)
        throws SAXException
      {
        KSJingValidation.LOG.error(
          "validation: {}: {}:{}: {}",
          e.getPublicId(),
          Integer.valueOf(e.getLineNumber()),
          Integer.valueOf(e.getColumnNumber()),
          e.getMessage());
        error_occurred.set(true);
      }
    };

    try {

      /**
       * Create a new RELAX-NG validator that also checks ID references.
       */

      final PropertyMapBuilder prop_builder = new PropertyMapBuilder();
      prop_builder.put(RngProperty.CHECK_ID_IDREF, null);
      prop_builder.put(ValidateProperty.ERROR_HANDLER, error_handler);
      final PropertyMap props = prop_builder.toPropertyMap();

      final InputSource schema_source =
        new InputSource(KSSchemaResources.getSchemaAsStream());
      final AutoSchemaReader schema_reader =
        new AutoSchemaReader();
      final Schema schema = schema_reader.createSchema(
        new SAXSource(schema_source), props);
      final Validator validator =
        schema.createValidator(props);

      /**
       * Create a new XInclude-aware parser that delegates content to
       * the validator created above.
       */

      final SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating(false);
      factory.setNamespaceAware(true);
      factory.setXIncludeAware(true);
      factory.setFeature("http://apache.org/xml/features/xinclude", true);

      final InputSource file_source = new InputSource(is);
      file_source.setSystemId(p.toString());

      final SAXParser file_parser = factory.newSAXParser();
      final XMLReader file_reader = file_parser.getXMLReader();
      file_reader.setContentHandler(validator.getContentHandler());
      file_reader.setEntityResolver(
        new KSRestrictedEntityResolver(base_directory));
      file_reader.parse(file_source);

      return !error_occurred.get();
    } catch (final ParserConfigurationException | IncorrectSchemaException e) {
      throw new IOException(e);
    }
  }
}
