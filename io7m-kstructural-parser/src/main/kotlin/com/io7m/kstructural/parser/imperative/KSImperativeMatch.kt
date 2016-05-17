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

package com.io7m.kstructural.parser.imperative

import com.io7m.kstructural.core.KSMatcherType
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeEOF
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeInline

object KSImperativeMatch {

  fun <C, R, E : Throwable> match(
    c : C,
    e : KSImperative,
    onCommand : KSMatcherType<C, KSImperativeCommand, R, E>,
    onInline : KSMatcherType<C, KSImperativeInline, R, E>,
    onEOF : KSMatcherType<C, KSImperativeEOF, R, E>) : R =
    when (e) {
      is KSImperativeCommand -> onCommand.apply(c, e)
      is KSImperativeEOF     -> onEOF.apply(c, e)
      is KSImperativeInline  -> onInline.apply(c, e)
    }

}