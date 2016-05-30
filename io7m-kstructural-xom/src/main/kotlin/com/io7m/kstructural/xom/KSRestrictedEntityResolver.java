package com.io7m.kstructural.xom;

import com.io7m.jnull.NullCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * A restricted entity resolver that refuses to resolve URIs that are either not
 * file URIs or refer to files that are not descendants of a given base
 * directory.
 */

public final class KSRestrictedEntityResolver implements EntityResolver
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(KSRestrictedEntityResolver.class);
  }

  private final Path base_directory;

  /**
   * Construct a new resolver that will only allow access to files that are
   * descendants of the given base directory.
   *
   * @param base The base
   */

  public KSRestrictedEntityResolver(final Path base)
  {
    this.base_directory = NullCheck.notNull(base);
  }

  @Override
  public InputSource resolveEntity(
    final String public_id,
    final String system_id)
    throws SAXException, IOException
  {
    KSRestrictedEntityResolver.LOG.trace(
      "resolve: {} {}", public_id, system_id);

    try {
      final URI content_uri = new URI(system_id);
      if ("file".equals(content_uri.getScheme())) {
        final FileSystem fs = this.base_directory.getFileSystem();
        final Path path = fs.getPath(content_uri.getPath()).toAbsolutePath();
        if (path.startsWith(this.base_directory)) {
          try {
            final InputStream stream =
              Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS);
            final InputSource source = new InputSource(stream);
            source.setSystemId(system_id);
            return source;
          } catch (final FileNotFoundException | NoSuchFileException e) {
            final StringBuilder sb = new StringBuilder(128);
            sb.append("File not found.");
            sb.append(System.lineSeparator());
            sb.append("  File: ");
            sb.append(path);
            sb.append(System.lineSeparator());
            throw new IOException(sb.toString(), e);
          }
        }
      }

      final StringBuilder sb = new StringBuilder(128);
      sb.append("Refusing to import file outside of the base directory.");
      sb.append(System.lineSeparator());
      sb.append("  Base: ");
      sb.append(this.base_directory);
      sb.append(System.lineSeparator());
      sb.append("  URI:  ");
      sb.append(content_uri);
      sb.append(System.lineSeparator());
      throw new IOException(sb.toString());

    } catch (final URISyntaxException e) {
      throw new IOException(e);
    }
  }
}
