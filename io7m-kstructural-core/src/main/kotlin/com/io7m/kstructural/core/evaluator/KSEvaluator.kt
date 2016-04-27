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

package com.io7m.kstructural.core.evaluator

import com.io7m.junreachable.UnimplementedCodeException
import com.io7m.kstructural.core.KSBlock
import com.io7m.kstructural.core.KSDocument
import com.io7m.kstructural.core.KSID
import com.io7m.kstructural.core.KSInline
import com.io7m.kstructural.core.KSResult
import java.math.BigInteger
import java.util.Optional

object KSEvaluator : KSEvaluatorType {

  private data class Context(
    private var id_pool : BigInteger = BigInteger.ZERO)
  : KSEvaluationContextType {
    fun freshID() : BigInteger {
      val id = this.id_pool
      this.id_pool = this.id_pool.add(BigInteger.ONE)
      return id
    }
  }

  override fun evaluate(
    d : KSDocument<Unit>)
    : KSResult<KSDocument<KSEvaluation>, KSEvaluationError> {
    val c = Context()
    return evaluateDocument(c, d) flatMap { d -> checkIDs(c, d) }
  }

  private fun checkIDs(
    c : KSEvaluator.Context,
    d : KSDocument<KSEvaluation>)
    : KSResult<KSDocument<KSEvaluation>, KSEvaluationError> {
    return KSResult.succeed(d)
  }

  private fun evaluateDocument(
    c : Context,
    d : KSDocument<Unit>)
    : KSResult<KSDocument<KSEvaluation>, KSEvaluationError> =
    when (d) {
      is KSDocument.KSDocumentWithParts    -> evaluateDocumentWithParts(c, d)
      is KSDocument.KSDocumentWithSections -> evaluateDocumentWithSections(c, d)
    }

  private fun evaluateDocumentWithSections(
    c : Context,
    d : KSDocument.KSDocumentWithSections<Unit>)
    : KSResult<KSDocument<KSEvaluation>, KSEvaluationError> {

    if (d.content.isEmpty()) {
      return KSResult.fail(KSEvaluationError(
        d.position, "Documents must have at least one section or part"))
    }

    val id =
      c.freshID()
    val act_content =
      KSResult.mapIndexed({ e, i -> evaluateSection(c, e, i) }, d.content)
    val act_title =
      KSResult.mapIndexed({ e, i -> evaluateInlineText (c, e, i) }, d.title)

    return act_content flatMap { content ->
      act_title flatMap { title ->
        val eval = KSEvaluation(c, id, Optional.empty())
        val id_eval = KSEvaluation(c, c.freshID(), Optional.empty())
        val ksid = d.id.map { v -> KSID(v.position, v.value, id_eval) }
        val rd = KSDocument.KSDocumentWithSections(
          d.position, eval, ksid, title, content)
        KSResult.succeed<KSDocument<KSEvaluation>, KSEvaluationError>(rd)
      }
    }
  }

  private fun evaluateInlineText(
    c : Context,
    e : KSInline.KSInlineText<Unit>,
    i : Int)
    : KSResult<KSInline.KSInlineText<KSEvaluation>, KSEvaluationError> {
    // TODO: Generated method stub!
    throw UnimplementedCodeException()
  }

  private fun evaluateInline(
    c : Context,
    e : KSInline<Unit>,
    i : Int)
    : KSResult<KSInline<KSEvaluation>, KSEvaluationError> {
    // TODO: Generated method stub!
    throw UnimplementedCodeException()
  }

  private fun evaluateSection(
    c : Context,
    e : KSBlock.KSBlockSection<Unit>,
    i : Int)
    : KSResult<KSBlock.KSBlockSection<KSEvaluation>, KSEvaluationError> {
    // TODO: Generated method stub!
    throw UnimplementedCodeException()
  }


  private fun evaluateDocumentWithParts(
    c : Context,
    d : KSDocument.KSDocumentWithParts<Unit>)
    : KSResult<KSDocument<KSEvaluation>, KSEvaluationError> {
    // TODO: Generated method stub!
    throw UnimplementedCodeException()
  }

}
