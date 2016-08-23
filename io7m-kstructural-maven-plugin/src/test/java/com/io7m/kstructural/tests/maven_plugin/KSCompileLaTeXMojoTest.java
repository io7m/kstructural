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

package com.io7m.kstructural.tests.maven_plugin;

import io.takari.maven.testing.TestMavenRuntime;
import io.takari.maven.testing.TestResources;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;

import static io.takari.maven.testing.TestResources.assertFilesNotPresent;
import static io.takari.maven.testing.TestResources.assertFilesPresent;
import static org.hamcrest.core.Is.isA;

public final class KSCompileLaTeXMojoTest
{
  @Rule
  public final TestResources resources = new TestResources();

  @Rule
  public final TestMavenRuntime maven = new TestMavenRuntime();

  @Rule
  public final ExpectedException expected = ExpectedException.none();

  @Test
  public void testNoFile()
    throws Exception
  {
    final File basedir = this.resources.getBasedir("latex-no-file");
    this.expected.expectCause(isA(IllegalArgumentException.class));
    this.maven.executeMojo(basedir, "compileLaTeX");
  }

  @Test
  public void testNoOutput()
    throws Exception
  {
    final File basedir = this.resources.getBasedir("latex-no-output");
    this.expected.expectCause(isA(IllegalArgumentException.class));
    this.maven.executeMojo(basedir, "compileLaTeX");
  }

  @Test
  public void testTrivial()
    throws Exception
  {
    final File basedir = this.resources.getBasedir("latex-trivial");
    this.maven.executeMojo(basedir, "compileLaTeX");
    assertFilesPresent(basedir, "target/out/main.tex");
  }

  @Test
  public void testTypeMap()
    throws Exception
  {
    final File basedir = this.resources.getBasedir("latex-typemap");
    this.maven.executeMojo(basedir, "compileLaTeX");
    assertFilesPresent(basedir, "target/out/main.tex");
  }

  @Test
  public void testTypeMapMissing()
    throws Exception
  {
    final File basedir = this.resources.getBasedir("latex-typemap-missing");
    this.expected.expectCause(isA(NoSuchFileException.class));
    this.maven.executeMojo(basedir, "compileLaTeX");
  }

  @Test
  public void testTypeMapInvalid()
    throws Exception
  {
    final File basedir = this.resources.getBasedir("latex-typemap-unparseable");
    this.expected.expectCause(isA(IOException.class));
    this.maven.executeMojo(basedir, "compileLaTeX");
  }
}
