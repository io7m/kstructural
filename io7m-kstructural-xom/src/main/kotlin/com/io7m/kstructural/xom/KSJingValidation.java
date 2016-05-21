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
import com.thaiopensource.util.PropertyMapBuilder;
import com.thaiopensource.validate.ValidateProperty;
import com.thaiopensource.validate.ValidationDriver;
import com.thaiopensource.validate.prop.rng.RngProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

public final class KSJingValidation
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(KSJingValidation.class);
  }

  private KSJingValidation()
  {

  }

  public static boolean validate(
    final Path p,
    final InputStream is)
    throws IOException, SAXException
  {
    final ErrorHandler eh = new ErrorHandler()
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
      }
    };

    /**
     * Enable IDref checking.
     */

    final PropertyMapBuilder props = new PropertyMapBuilder();
    props.put(RngProperty.CHECK_ID_IDREF, null);
    props.put(ValidateProperty.ERROR_HANDLER, eh);

    final ValidationDriver driver = new ValidationDriver(props.toPropertyMap());
    driver.loadSchema(new InputSource(KSSchemaResources.getSchemaAsStream()));
    final InputSource s = new InputSource(is);
    s.setPublicId(p.toString());
    return driver.validate(s);
  }
}
