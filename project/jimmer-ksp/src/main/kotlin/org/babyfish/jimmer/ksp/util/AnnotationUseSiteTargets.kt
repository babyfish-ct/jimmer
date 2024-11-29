package org.babyfish.jimmer.ksp.util

import com.google.devtools.ksp.symbol.AnnotationUseSiteTarget
import com.squareup.kotlinpoet.AnnotationSpec

fun AnnotationUseSiteTarget.toPoetTarget(): AnnotationSpec.UseSiteTarget =
    when (this) {
        AnnotationUseSiteTarget.FIELD -> AnnotationSpec.UseSiteTarget.FIELD
        AnnotationUseSiteTarget.GET -> AnnotationSpec.UseSiteTarget.GET
        AnnotationUseSiteTarget.SET -> AnnotationSpec.UseSiteTarget.SET
        AnnotationUseSiteTarget.PROPERTY -> AnnotationSpec.UseSiteTarget.PROPERTY
        AnnotationUseSiteTarget.PARAM -> AnnotationSpec.UseSiteTarget.PARAM
        AnnotationUseSiteTarget.SETPARAM -> AnnotationSpec.UseSiteTarget.SETPARAM
        AnnotationUseSiteTarget.RECEIVER -> AnnotationSpec.UseSiteTarget.RECEIVER
        AnnotationUseSiteTarget.DELEGATE -> AnnotationSpec.UseSiteTarget.DELEGATE
        AnnotationUseSiteTarget.FILE -> AnnotationSpec.UseSiteTarget.FILE
    }