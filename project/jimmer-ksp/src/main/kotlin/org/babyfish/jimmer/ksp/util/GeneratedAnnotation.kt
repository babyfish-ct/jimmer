package org.babyfish.jimmer.ksp.util

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import org.babyfish.jimmer.dto.compiler.DtoFile
import org.babyfish.jimmer.ksp.immutable.generator.GENERATED_BY_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableType

internal fun generatedAnnotation(): AnnotationSpec =
    AnnotationSpec.builder(GENERATED_BY_CLASS_NAME)
        .build()

internal fun generatedAnnotation(className: ClassName): AnnotationSpec =
    AnnotationSpec.builder(GENERATED_BY_CLASS_NAME)
        .addMember("type = %T::class", className)
        .build()

internal fun generatedAnnotation(type: ImmutableType): AnnotationSpec =
    generatedAnnotation(type.className)

fun generatedAnnotation(dtoFile: DtoFile, mutable: Boolean): AnnotationSpec =
    AnnotationSpec
        .builder(GENERATED_BY_CLASS_NAME)
        .addMember(
            "file = %S, prompt = %S",
            dtoFile.path,
            if (mutable) {
                "The current DTO type is mutable. If you need to make it immutable, " +
                        "please remove the ksp argument `jimmer.dto.mutable`"
            } else {
                "The current DTO type is immutable. If you need to make it mutable, " +
                        "please set the ksp argument `jimmer.dto.mutable` to the string \"text\""
            }
        )
        .build()