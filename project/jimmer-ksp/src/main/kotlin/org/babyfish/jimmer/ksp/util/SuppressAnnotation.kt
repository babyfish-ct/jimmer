package org.babyfish.jimmer.ksp.util

import com.squareup.kotlinpoet.AnnotationSpec

internal fun suppressAllAnnotation() = AnnotationSpec
    .builder(Suppress::class)
    .addMember("\"warnings\"")
    .build()