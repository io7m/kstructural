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

package com.io7m.kstructural.core

import com.io7m.kstructural.core.KSElement.KSBlock
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockImport
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineInclude
import com.io7m.kstructural.core.KSElement.KSInline.KSInlineText
import org.jgrapht.alg.DijkstraShortestPath
import org.jgrapht.experimental.dag.DirectedAcyclicGraph
import org.slf4j.LoggerFactory
import org.valid4j.Assertive
import java.nio.file.Path
import java.util.HashMap
import java.util.IdentityHashMap

class KSParseContext private constructor(
  override val includes : MutableMap<Path, String>,
  override val includePaths : MutableMap<KSInlineInclude<KSParse>, Path>,
  override val includesByTexts : MutableMap<KSInlineText<KSParse>, KSInlineInclude<KSParse>>,
  override val importsByPath : MutableMap<Path, KSBlock<KSParse>>,
  override val importPathsByElement : MutableMap<KSBlockImport<KSParse>, Path>,
  override val importsByElement : MutableMap<KSBlock<KSParse>, KSBlockImport<KSParse>>,
  override val importPathsEdgesByElement : MutableMap<KSBlockImport<KSParse>, KSImportPathEdge>)
: KSParseContextType {

  override fun checkImportCycle(
    importer : Path,
    import : KSBlockImport<KSParse>,
    imported_path : Path)
    : KSResult<KSImportPathEdge, KSParseError> {

    LOG.trace("import: {} -> {}", importer, imported_path)
    Assertive.require(!importPathsByElement.containsKey(import))

    return try {
      val edge = KSImportPathEdge(from = importer, to = imported_path)
      import_graph.addVertex(importer)
      import_graph.addVertex(imported_path)
      import_graph.addDagEdge(importer, imported_path, edge)
      KSResult.succeed(edge)
    } catch (x : DirectedAcyclicGraph.CycleFoundException) {

      /**
       * Because a cycle as occurred on an insertion of edge A → B, then
       * there must be some path B → A already in the graph. Use a
       * shortest path algorithm to determine that path.
       */

      val path = DijkstraShortestPath(import_graph, imported_path, importer)

      val sb = StringBuilder()
      sb.append("Cyclic import detected.")
      sb.append(System.lineSeparator())
      sb.append("  Sequence: ")
      sb.append(System.lineSeparator())
      path.pathEdgeList.forEach { edge ->
        sb.append("    ")
        sb.append(edge.from)
        sb.append(" -> ")
        sb.append(edge.to)
        sb.append(System.lineSeparator())
      }
      val last = path.pathEdgeList[path.pathEdgeList.size - 1]
      sb.append("    ")
      sb.append(last.to)
      sb.append(" -> ")
      sb.append(imported_path)

      KSResult.fail(KSParseError(import.position, sb.toString()))
    }
  }

  private val import_graph : DirectedAcyclicGraph<Path, KSImportPathEdge> =
    DirectedAcyclicGraph(KSImportPathEdge::class.java)

  override fun addImport(
    importer : Path,
    import : KSBlockImport<KSParse>,
    imported_path : Path,
    imported : KSBlock<KSParse>)
    : KSResult<Unit, KSParseError> {

    LOG.trace("import: {}: {}", imported_path, imported.javaClass.simpleName)
    Assertive.require(!importPathsByElement.containsKey(import))
    Assertive.require(!importsByElement.containsKey(imported))

    return checkImportCycle(importer, import, imported_path) flatMap { edge ->
      importsByPath[imported_path] = imported
      importPathsByElement[import] = imported_path
      importsByElement[imported] = import
      importPathsEdgesByElement[import] = edge
      KSResult.succeed<Unit, KSParseError>(Unit)
    }
  }

  companion object {
    private val LOG = LoggerFactory.getLogger(KSParseContext::class.java)

    fun empty() : KSParseContextType {
      return KSParseContext(
        includes = HashMap(),
        includePaths = IdentityHashMap(),
        includesByTexts = IdentityHashMap(),
        importsByPath = HashMap(),
        importPathsByElement = IdentityHashMap(),
        importPathsEdgesByElement = IdentityHashMap(),
        importsByElement = IdentityHashMap())
    }
  }

  override fun addInclude(
    t : KSInlineText<KSParse>,
    i : KSInlineInclude<KSParse>,
    p : Path,
    s : String) {
    LOG.trace("include: {}: {}...", p, s.substring(0, Math.min(8, s.length)))
    Assertive.require(!includePaths.containsKey(i))
    Assertive.require(!includesByTexts.containsKey(t))
    includes[p] = s
    includePaths[i] = p
    includesByTexts[t] = i
  }
}