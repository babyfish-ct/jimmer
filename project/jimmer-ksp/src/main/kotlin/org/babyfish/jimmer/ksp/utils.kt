package org.babyfish.jimmer.ksp

import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.ClassName
import kotlin.reflect.KClass

val KSDeclaration.fullName
    get() = qualifiedName!!.asString()

val KSPropertyDeclaration.name
    get() = simpleName.getShortName()

fun KSAnnotated.firstAnnotation(annotationType: KClass<out Annotation>): KSAnnotation? =
    annotations.firstOrNull {
        it.annotationType.resolve().declaration.fullName == annotationType.qualifiedName
    }

fun KSClassDeclaration.className(nullable: Boolean = false, simpleNameTranslator: (String) -> String = {it}): ClassName =
    ClassName(
        packageName.asString(),
        simpleName.asString().let(simpleNameTranslator)
    ).let {
        if (nullable) {
            it.copy(nullable = true) as ClassName
        } else {
            it
        }
    }

fun KSClassDeclaration.nestedClassName(nullable: Boolean = false, simpleNameListTranslator: (String) -> List<String>): ClassName =
    ClassName(
        packageName.asString(),
        simpleName.asString().let(simpleNameListTranslator)
    ).let {
        if (nullable) {
            it.copy(nullable = true) as ClassName
        } else {
            it
        }
    }