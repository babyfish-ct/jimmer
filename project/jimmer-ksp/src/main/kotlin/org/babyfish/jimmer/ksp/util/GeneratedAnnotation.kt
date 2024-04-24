package org.babyfish.jimmer.ksp.util

import com.squareup.kotlinpoet.Annotatable
import com.squareup.kotlinpoet.AnnotationSpec
import org.babyfish.jimmer.ksp.immutable.generator.GENERATED_BY_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableType

internal fun <T: Annotatable.Builder<T>> T.addGeneratedAnnotation(): T = addAnnotation(
    AnnotationSpec.builder(GENERATED_BY_CLASS_NAME)
        .build()
)

internal fun <T: Annotatable.Builder<T>> T.addGeneratedAnnotation(type: ImmutableType): T = addAnnotation(
    AnnotationSpec.builder(GENERATED_BY_CLASS_NAME)
        .addMember("type = %T::class", type.className)
        .build()
)