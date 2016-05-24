package com.io7m.kstructural.core

import com.io7m.kstructural.core.KSElement.KSBlock
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockFootnote
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockFormalItem
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockImport
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockParagraph
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockPart
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSubsection

object KSBlockMatch {

  fun <C, T, R, E : Throwable> match(
    c : C,
    e : KSBlock<T>,
    onDocument : KSMatcherType<C, KSBlockDocument<T>, R, E>,
    onSection : KSMatcherType<C, KSBlockSection<T>, R, E>,
    onSubsection : KSMatcherType<C, KSBlockSubsection<T>, R, E>,
    onParagraph : KSMatcherType<C, KSBlockParagraph<T>, R, E>,
    onFormalItem : KSMatcherType<C, KSBlockFormalItem<T>, R, E>,
    onFootnote : KSMatcherType<C, KSBlockFootnote<T>, R, E>,
    onPart : KSMatcherType<C, KSBlockPart<T>, R, E>,
    onImport : KSMatcherType<C, KSBlockImport<T>, R, E>) : R =
    when (e) {
      is KSBlockDocument   -> onDocument.apply(c, e)
      is KSBlockSection    -> onSection.apply(c, e)
      is KSBlockSubsection -> onSubsection.apply(c, e)
      is KSBlockParagraph  -> onParagraph.apply(c, e)
      is KSBlockFormalItem -> onFormalItem.apply(c, e)
      is KSBlockFootnote   -> onFootnote.apply(c, e)
      is KSBlockPart       -> onPart.apply(c, e)
      is KSBlockImport     -> onImport.apply(c, e)
    }

}