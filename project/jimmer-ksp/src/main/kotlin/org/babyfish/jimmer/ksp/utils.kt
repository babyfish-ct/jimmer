package org.babyfish.jimmer.ksp

import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import org.babyfish.jimmer.ksp.meta.MetaException
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

val KSDeclaration.fullName: String
    get() = qualifiedName?.asString() ?: ""

val KSPropertyDeclaration.name: String
    get() = simpleName.getShortName()

fun KSAnnotated.annotations(predicate: (KSAnnotation) -> Boolean): List<KSAnnotation> {
    val resultList = mutableListOf<KSAnnotation>()
    resultList += annotations.filter(predicate)
    if (this is KSPropertyDeclaration) {
        resultList += getter?.annotations?.filter(predicate)?.toList() ?: emptyList()
        resultList += getter?.returnType?.annotations?.filter(predicate)?.toList() ?: emptyList()
    }
    return resultList
}

fun KSAnnotated.annotations(annotationType: KClass<out Annotation>): List<KSAnnotation> {
    return annotations { it.fullName == annotationType.qualifiedName }
}

fun KSAnnotated.annotation(annotationType: KClass<out Annotation>): KSAnnotation? =
    if (this is KSPropertyDeclaration) {
        val selfAnno = annotations.firstOrNull {
            it.annotationType.resolve().declaration.fullName == annotationType.qualifiedName
        }
        val getterAnno = getter?.annotation(annotationType)
        val returnAnno = getter?.returnType?.annotation(annotationType)
        val targets = mutableListOf<String>()
        if (selfAnno !== null) {
            targets.add("property")
        }
        if (getterAnno !== null) {
            targets.add("getter")
        }
        if (returnAnno !== null) {
            targets.add("return type")
        }
        if (targets.size > 1) {
            throw MetaException(
                this,
                "it is decorated by multiple annotations of type " +
                    "'@${annotationType.qualifiedName}' from different annotation targets: $targets"
            )
        }
        selfAnno ?: getterAnno ?: returnAnno
    } else {
        annotations.firstOrNull {
            it.annotationType.resolve().declaration.fullName == annotationType.qualifiedName
        }
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

val KSAnnotation.fullName: String
    get() = annotationType.resolve().declaration.fullName

@Suppress("UNCHECKED_CAST")
operator fun <T> KSAnnotation.get(annoProp: KProperty1<out Annotation, T>): T? =
    arguments.firstOrNull { it.name?.asString() == annoProp.name }?.value as T?

@Suppress("UNCHECKED_CAST")
operator fun <T> KSAnnotation.get(name: String): T? =
    arguments.firstOrNull { it.name?.asString() == name }?.value as T?

@Suppress("UNCHECKED_CAST")
fun KSAnnotation.getClassArgument(annoProp: KProperty1<out Annotation, KClass<*>>): KSClassDeclaration? =
    (arguments.firstOrNull { it.name?.asString() == annoProp.name }?.value as KSType?)?.declaration as KSClassDeclaration?

@Suppress("UNCHECKED_CAST")
fun <T> KSAnnotation.getListArgument(annoProp: KProperty1<out Annotation, Array<T>>): List<T>? =
    arguments.firstOrNull { it.name?.asString() == annoProp.name }?.value as List<T>?

fun TypeName.isBuiltInType(nullable: Boolean? = null): Boolean {
    if (this !is ClassName) {
        return false
    }
    if (nullable != null && isNullable != nullable) {
        return false
    }
    if (packageName != "kotlin") {
        return false
    }
    return when (simpleName) {
        "Boolean", "Char", "Byte", "Short", "Int", "Long", "Float", "Double" -> true
        else -> false
    }
}