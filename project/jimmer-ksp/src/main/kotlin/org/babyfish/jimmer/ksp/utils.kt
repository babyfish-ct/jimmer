package org.babyfish.jimmer.ksp

import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
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
    annotation(annotationType.qualifiedName!!)

fun KSAnnotated.annotation(qualifiedName: String): KSAnnotation? =
    if (this is KSPropertyDeclaration) {
        val selfAnno = annotations.firstOrNull {
            it.annotationType.resolve().declaration.fullName == qualifiedName
        }
        val getterAnno = getter?.annotation(qualifiedName)
        val returnAnno = getter?.returnType?.annotation(qualifiedName)
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
                    "'@${qualifiedName}' from different annotation targets: $targets"
            )
        }
        selfAnno ?: getterAnno ?: returnAnno
    } else {
        annotations.firstOrNull {
            it.annotationType.resolve().declaration.fullName == qualifiedName
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
fun <T> KSAnnotation.getListArgument(annoProp: KProperty1<out Annotation, Array<out T>>): List<T>? =
    arguments.firstOrNull { it.name?.asString() == annoProp.name }?.value as List<T>?

@Suppress("UNCHECKED_CAST")
fun KSAnnotation.getClassListArgument(annoProp: KProperty1<out Annotation, Array<out KClass<*>>>): List<KSClassDeclaration> =
    (arguments.firstOrNull { it.name?.asString() == annoProp.name }?.value as List<*>?)
        ?.map { (it as KSType).declaration } as List<KSClassDeclaration>?
        ?: emptyList()

@Suppress("UNCHECKED_CAST")
inline fun <reified E: Enum<E>> KSAnnotation.getEnumListArgument(annoProp: KProperty1<out Annotation, Array<out E>>): List<E> {
    val list = arguments.firstOrNull { it.name?.asString() == annoProp.name }?.value as List<Any>? ?: return emptyList()
    return list.map { value ->
        val name = value.toString()
        val lastIndex = name.lastIndexOf('.')
        enumValueOf<E>(if (lastIndex == -1) name else name.substring(lastIndex + 1))
    }
}

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