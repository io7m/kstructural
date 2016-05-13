package com.io7m.kstructural.parser

import com.io7m.kstructural.core.KSElement
import com.io7m.kstructural.core.evaluator.KSEvaluation

interface KSSerializerType {

  fun serialize(e : KSElement<KSEvaluation>) : KSExpression

}