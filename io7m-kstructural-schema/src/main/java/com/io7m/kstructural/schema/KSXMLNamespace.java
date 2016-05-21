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

package com.io7m.kstructural.schema;

import java.net.URI;

public final class KSXMLNamespace
{
  public static final URI NAMESPACE_URI =
    URI.create("http://schemas.io7m.com/structural/3.0.0");

  public static final String NAMESPACE_URI_TEXT =
    KSXMLNamespace.NAMESPACE_URI.toString();

  public static final URI XML_NAMESPACE_URI =
    URI.create("http://www.w3.org/XML/1998/namespace");

  public static final String XML_NAMESPACE_URI_TEXT =
    KSXMLNamespace.XML_NAMESPACE_URI.toString();

  public static final URI XINCLUDE_NAMESPACE_URI =
    URI.create("http://www.w3.org/2001/XInclude");

  public static final String XINCLUDE_NAMESPACE_URI_TEXT =
    KSXMLNamespace.XINCLUDE_NAMESPACE_URI.toString();

  private KSXMLNamespace()
  {

  }
}
