package org.babyfish.jimmer.ksp.generator

import com.squareup.kotlinpoet.CodeBlock
import kotlin.reflect.KClass

fun CodeBlock.Builder.addElseBranchForProp(argType: KClass<*>) {
    addStatement("""else -> throw IllegalArgumentException("Illegal property ${
        if (argType == Int::class) "id" else "name"
    }: ${
        "\$prop"
    }")""")
}