package org.babyfish.jimmer.ksp.generator

import com.squareup.kotlinpoet.CodeBlock
import org.babyfish.jimmer.ksp.meta.ImmutableProp
import org.babyfish.jimmer.ksp.meta.ImmutableType
import org.babyfish.jimmer.meta.PropId
import kotlin.reflect.KClass

class CaseAppender(
    private val builder: CodeBlock.Builder,
    private val type: ImmutableType,
    private val argType: KClass<*>
) {

    fun addCase(prop: ImmutableProp) {
        if (argType == PropId::class) {
            builder.add("%L ->\n\t", prop.slotName)
        } else {
            builder.add("%S ->\n\t", prop.name)
        }
    }

    fun addIllegalCase() {
        if (argType == PropId::class) {
            builder.add("-1 ->\n\t")
        }
    }
}