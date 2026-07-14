package org.babyfish.jimmer.ddl.compiler

import site.addzero.lsi.anno.LsiAnnotation
import site.addzero.lsi.clazz.LsiClass
import site.addzero.lsi.field.LsiField
import site.addzero.lsi.method.LsiMethod
import site.addzero.lsi.type.LsiType

fun Collection<LsiClass>.toJimmerDdlLsiClasses(): List<LsiClass> {
    val cache = linkedMapOf<String, LsiClass>()
    return filter { it.isJimmerEntityType() }
        .map { it.toJimmerDdlLsiClass(cache) }
        .distinctBy { it.qualifiedName ?: it.simpleName.orEmpty() }
}

private fun LsiClass.toJimmerDdlLsiClass(cache: MutableMap<String, LsiClass>): LsiClass {
    val key = qualifiedName ?: simpleName.orEmpty()
    cache[key]?.let { return it }

    val convertedSuperClasses = (superClasses + interfaces)
        .filter { it.isJimmerType() }
        .map { it.toJimmerDdlLsiClass(cache) }
    val fieldCandidates = fields.ifEmpty {
        methods.mapNotNull { it.toMethodBackedField(cache) }
    }
    val converted = JimmerDdlLsiClass(
        delegate = this,
        ddlFields = fieldCandidates,
        ddlSuperClasses = convertedSuperClasses,
    )
    cache[key] = converted
    return converted
}

private class JimmerDdlLsiClass(
    private val delegate: LsiClass,
    private val ddlFields: List<LsiField>,
    private val ddlSuperClasses: List<LsiClass>,
) : LsiClass {
    override val simpleName get() = delegate.simpleName
    override val qualifiedName get() = delegate.qualifiedName
    override val comment get() = delegate.comment
    override val fields get() = ddlFields
    override val annotations get() = delegate.annotations
    override val isInterface get() = delegate.isInterface
    override val isEnum get() = delegate.isEnum
    override val isCollectionType get() = delegate.isCollectionType
    override val isPojo get() = delegate.isPojo
    override val superClasses get() = ddlSuperClasses
    override val interfaces get() = delegate.interfaces
    override val methods get() = delegate.methods
    override val fileName get() = delegate.fileName
    override val isObject get() = delegate.isObject
    override val isCompanionObject get() = delegate.isCompanionObject
}

private fun LsiMethod.toMethodBackedField(cache: MutableMap<String, LsiClass>): LsiField? {
    if (isStatic || parameters.isNotEmpty()) {
        return null
    }
    val propertyName = name?.toJimmerPropertyName() ?: return null
    if (propertyName.isBlank() || propertyName == "class") {
        return null
    }
    return MethodBackedLsiField(propertyName, this, cache)
}

private class MethodBackedLsiField(
    private val propertyName: String,
    private val method: LsiMethod,
    private val cache: MutableMap<String, LsiClass>,
) : LsiField {
    override val name get() = propertyName
    override val type get() = method.returnType
    override val typeName get() = method.returnTypeName
    override val comment get() = method.comment
    override val annotations get() = method.annotations
    override val isStatic get() = method.isStatic
    override val isConstant get() = false
    override val isEnum get() = method.returnType?.lsiClass?.isEnum ?: false
    override val isVar get() = false
    override val isLateInit get() = false
    override val isCollectionType get() = method.returnType?.isCollectionType ?: false
    override val defaultValue: String? get() = null
    override val columnName: String? get() = annotations.columnNameFromAnnotations() ?: propertyName.toSnakeCase()
    override val declaringClass get() = method.declaringClass
    override val fieldTypeClass get() = method.returnType?.lsiClass?.toJimmerDdlLsiClass(cache)
    override val isNestedObject get() = !isCollectionType && fieldTypeClass?.isPojo == true
    override val children get() = if (isNestedObject) fieldTypeClass?.fields.orEmpty() else emptyList()
}

private fun String.toJimmerPropertyName(): String {
    if (startsWith("get") && length > 3) {
        return substring(3).replaceFirstChar(Char::lowercase)
    }
    if (startsWith("is") && length > 2) {
        return substring(2).replaceFirstChar(Char::lowercase)
    }
    return this
}

private fun List<LsiAnnotation>.columnNameFromAnnotations(): String? {
    return firstNotNullOfOrNull { annotation ->
        if (!annotation.simpleName.equals("Column", ignoreCase = true)) {
            return@firstNotNullOfOrNull null
        }
        annotation.getAttribute("name")
            ?.toString()
            ?.takeIf { it.isNotBlank() }
    }
}

private fun String.toSnakeCase(): String {
    if (isBlank()) {
        return this
    }
    val builder = StringBuilder()
    for (index in indices) {
        val char = this[index]
        if (char in 'A'..'Z') {
            if (index > 0 && builder.lastOrNull() != '_') {
                builder.append('_')
            }
            builder.append(char.lowercaseChar())
        } else {
            builder.append(char)
        }
    }
    return builder.toString()
}


private fun LsiClass.isJimmerEntityType(): Boolean {
    return annotations.any { annotation -> annotation.qualifiedName == "org.babyfish.jimmer.sql.Entity" || annotation.simpleName == "Entity" }
}

private fun LsiClass.isJimmerType(): Boolean {
    return annotations.any { annotation ->
        annotation.qualifiedName in JIMMER_TYPE_ANNOTATIONS || annotation.simpleName in JIMMER_TYPE_SIMPLE_NAMES
    }
}

private val JIMMER_TYPE_ANNOTATIONS = setOf(
    "org.babyfish.jimmer.sql.Entity",
    "org.babyfish.jimmer.sql.MappedSuperclass",
    "org.babyfish.jimmer.sql.Embeddable",
    "org.babyfish.jimmer.Immutable",
)

private val JIMMER_TYPE_SIMPLE_NAMES = setOf(
    "Entity",
    "MappedSuperclass",
    "Embeddable",
    "Immutable",
)
