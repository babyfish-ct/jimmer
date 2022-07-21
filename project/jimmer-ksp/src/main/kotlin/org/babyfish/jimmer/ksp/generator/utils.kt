package org.babyfish.jimmer.ksp.generator

import com.squareup.kotlinpoet.CodeBlock
import org.babyfish.jimmer.ksp.meta.ImmutableProp
import kotlin.reflect.KClass

internal fun CodeBlock.Builder.addElseBranchForProp(argType: KClass<*>) {
    addStatement("""else -> throw IllegalArgumentException("Illegal property ${
        if (argType == Int::class) "id" else "name"
    }: ${
        "\$prop"
    }")""")
}

internal fun regexpPatternFieldName(prop: ImmutableProp, index: Int): String =
    "__" + prop.name + "_pattern" + if (index == 0) "" else "_$index"