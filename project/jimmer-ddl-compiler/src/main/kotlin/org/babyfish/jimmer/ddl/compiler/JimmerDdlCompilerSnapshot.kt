package org.babyfish.jimmer.ddl.compiler

import site.addzero.lsi.anno.LsiAnnotation
import site.addzero.lsi.clazz.LsiClass
import site.addzero.lsi.field.LsiField
import site.addzero.lsi.method.LsiMethod
import site.addzero.lsi.method.LsiParameter
import site.addzero.lsi.type.LsiType

/**
 * KSP symbols cannot be safely resolved from `finish` after PSI changes.
 * Freeze the LSI view during the valid processing round, then compile DDL from
 * these immutable snapshots in `finish`.
 */
internal fun LsiClass.toStableJimmerDdlSnapshot(): LsiClass {
    return snapshot(cache = linkedMapOf(), shallow = false)
}

private fun LsiClass.snapshot(
    cache: MutableMap<String, LsiClass>,
    shallow: Boolean,
): LsiClass {
    val key = "${qualifiedName ?: simpleName.orEmpty()}#$shallow"
    cache[key]?.let { return it }

    val snapshot = SnapshotLsiClass(
        simpleName = simpleName,
        qualifiedName = qualifiedName,
        comment = comment,
        fields = fields.map { field -> field.snapshot(cache, snapshotFieldTypeClass = !shallow) },
        annotations = annotations.map { it.snapshot() },
        isInterface = isInterface,
        isEnum = isEnum,
        isCollectionType = isCollectionType,
        isPojo = isPojo,
        superClasses = superClasses.map { it.snapshot(cache, shallow = shallow) },
        interfaces = interfaces.map { it.snapshot(cache, shallow = shallow) },
        methods = if (fields.isEmpty()) methods.map { it.snapshot(cache) } else emptyList(),
        fileName = fileName,
        isObject = isObject,
        isCompanionObject = isCompanionObject,
    )
    cache[key] = snapshot
    return snapshot
}

private fun LsiField.snapshot(
    cache: MutableMap<String, LsiClass>,
    snapshotFieldTypeClass: Boolean,
): LsiField {
    val annotationSnapshots = annotations.map { it.snapshot() }
    val association = annotationSnapshots.any { annotation ->
        annotation.simpleName in setOf("ManyToOne", "OneToOne", "OneToMany", "ManyToMany")
    }
    return SnapshotLsiField(
        name = name,
        type = type?.snapshot(cache, snapshotLsiClass = snapshotFieldTypeClass && association),
        typeName = typeName,
        comment = comment,
        annotations = annotationSnapshots,
        isStatic = isStatic,
        isConstant = isConstant,
        isEnum = isEnum,
        isVar = isVar,
        isLateInit = isLateInit,
        isCollectionType = isCollectionType,
        defaultValue = defaultValue,
        columnName = columnName,
        declaringClass = null,
        fieldTypeClass = if (snapshotFieldTypeClass && association) fieldTypeClass?.snapshot(cache, shallow = true) else null,
        isNestedObject = false,
        children = emptyList(),
        isNullable = isNullable,
    )
}

private fun LsiMethod.snapshot(cache: MutableMap<String, LsiClass>): LsiMethod {
    return SnapshotLsiMethod(
        name = name,
        returnType = returnType?.snapshot(cache),
        returnTypeName = returnTypeName,
        comment = comment,
        annotations = annotations.map { it.snapshot() },
        isStatic = isStatic,
        isAbstract = isAbstract,
        parameters = parameters.map { it.snapshot(cache) },
        declaringClass = null,
    )
}

private fun LsiParameter.snapshot(cache: MutableMap<String, LsiClass>): LsiParameter {
    return SnapshotLsiParameter(
        name = name,
        type = type?.snapshot(cache),
        typeName = typeName,
        annotations = annotations.map { it.snapshot() },
        hasDefault = hasDefault,
    )
}

private fun LsiType.snapshot(
    cache: MutableMap<String, LsiClass>,
    snapshotLsiClass: Boolean = false,
): LsiType {
    return SnapshotLsiType(
        simpleName = simpleName,
        qualifiedName = qualifiedName,
        presentableText = presentableText,
        annotations = annotations.map { it.snapshot() },
        isCollectionType = isCollectionType,
        isNullable = isNullable,
        typeParameters = typeParameters.map { it.snapshot(cache, snapshotLsiClass = snapshotLsiClass) },
        isPrimitive = isPrimitive,
        componentType = componentType?.snapshot(cache, snapshotLsiClass = snapshotLsiClass),
        isArray = isArray,
        lsiClass = if (snapshotLsiClass && !isCollectionType) {
            lsiClass?.snapshot(cache, shallow = true)
        } else {
            null
        },
    )
}

private fun LsiAnnotation.snapshot(): LsiAnnotation {
    return SnapshotLsiAnnotation(
        qualifiedName = qualifiedName,
        simpleName = simpleName,
        attributes = attributes.mapValues { (_, value) -> value.freezeAnnotationAttribute() },
    )
}

private fun Any?.freezeAnnotationAttribute(): Any? {
    return when (this) {
        null -> null
        is String, is Number, is Boolean -> this
        is Collection<*> -> map { it.freezeAnnotationAttribute() }
        is Array<*> -> map { it.freezeAnnotationAttribute() }
        else -> toString()
    }
}

private data class SnapshotLsiAnnotation(
    override val qualifiedName: String?,
    override val simpleName: String?,
    override val attributes: Map<String, Any?>,
) : LsiAnnotation {
    override fun getAttribute(name: String): Any? = attributes[name]

    override fun hasAttribute(name: String): Boolean = attributes.containsKey(name)
}

private data class SnapshotLsiType(
    override val simpleName: String?,
    override val qualifiedName: String?,
    override val presentableText: String?,
    override val annotations: List<LsiAnnotation>,
    override val isCollectionType: Boolean,
    override val isNullable: Boolean,
    override val typeParameters: List<LsiType>,
    override val isPrimitive: Boolean,
    override val componentType: LsiType?,
    override val isArray: Boolean,
    override val lsiClass: LsiClass?,
) : LsiType

private data class SnapshotLsiField(
    override val name: String?,
    override val type: LsiType?,
    override val typeName: String?,
    override val comment: String?,
    override val annotations: List<LsiAnnotation>,
    override val isStatic: Boolean,
    override val isConstant: Boolean,
    override val isEnum: Boolean,
    override val isVar: Boolean,
    override val isLateInit: Boolean,
    override val isCollectionType: Boolean,
    override val defaultValue: String?,
    override val columnName: String?,
    override val declaringClass: LsiClass?,
    override val fieldTypeClass: LsiClass?,
    override val isNestedObject: Boolean,
    override val children: List<LsiField>,
    override val isNullable: Boolean,
) : LsiField

private data class SnapshotLsiClass(
    override val simpleName: String?,
    override val qualifiedName: String?,
    override val comment: String?,
    override val fields: List<LsiField>,
    override val annotations: List<LsiAnnotation>,
    override val isInterface: Boolean,
    override val isEnum: Boolean,
    override val isCollectionType: Boolean,
    override val isPojo: Boolean,
    override val superClasses: List<LsiClass>,
    override val interfaces: List<LsiClass>,
    override val methods: List<LsiMethod>,
    override val fileName: String?,
    override val isObject: Boolean,
    override val isCompanionObject: Boolean,
) : LsiClass

private data class SnapshotLsiMethod(
    override val name: String?,
    override val returnType: LsiType?,
    override val returnTypeName: String?,
    override val comment: String?,
    override val annotations: List<LsiAnnotation>,
    override val isStatic: Boolean,
    override val isAbstract: Boolean,
    override val parameters: List<LsiParameter>,
    override val declaringClass: LsiClass?,
) : LsiMethod

private data class SnapshotLsiParameter(
    override val name: String?,
    override val type: LsiType?,
    override val typeName: String?,
    override val annotations: List<LsiAnnotation>,
    override val hasDefault: Boolean,
) : LsiParameter
