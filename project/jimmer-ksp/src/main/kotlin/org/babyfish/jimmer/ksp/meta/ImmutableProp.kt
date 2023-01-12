package org.babyfish.jimmer.ksp.meta

import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.ksp.*
import org.babyfish.jimmer.ksp.generator.*
import org.babyfish.jimmer.meta.ModelException
import org.babyfish.jimmer.meta.impl.PropDescriptor
import org.babyfish.jimmer.pojo.AutoScalarStrategy
import org.babyfish.jimmer.pojo.Static
import org.babyfish.jimmer.pojo.Statics
import org.babyfish.jimmer.sql.*
import kotlin.reflect.KClass

class ImmutableProp(
    private val ctx: Context,
    val declaringType: ImmutableType,
    val id: Int,
    private val propDeclaration: KSPropertyDeclaration
) {
    init {
        if (propDeclaration.isMutable) {
            throw MetaException("Illegal property '${this}', this property of immutable interface must be readonly")
        }
    }

    val name: String = propDeclaration.name

    private val resolvedType: KSType = propDeclaration.type.resolve()

    val isTransient: Boolean =
        annotation(Transient::class) !== null

    val isList: Boolean =
        (resolvedType.declaration as KSClassDeclaration).asStarProjectedType().let { starType ->
            when {
                ctx.mapType.isAssignableFrom(starType) ->
                    throw MetaException("Illegal property '$propDeclaration', cannot be map")
                ctx.collectionType.isAssignableFrom(starType) ->
                    if (!ctx.listType.isAssignableFrom(starType) ||
                        !resolvedType.isAssignableFrom(ctx.listType)) {
                        true
                    } else {
                        throw MetaException("Illegal property '$propDeclaration', collection property must be immutable list")
                    }
                else -> false
            }
        }

    val targetDeclaration: KSClassDeclaration =
        if (isList) {
            resolvedType.arguments[0].type!!.resolve()
        } else {
            resolvedType
        }.declaration.also {
            if (it.annotation(MappedSuperclass::class) !== null) {
                throw ModelException(
                    "Illegal property \"$this\", its target type \"$it\" is illegal, it cannot be type decorated by @MappedSuperclass"
                )
            }
        } as KSClassDeclaration

    val primaryAnnotationType: Class<out Annotation>?

    val isNullable: Boolean

    init {
        val descriptor = PropDescriptor
            .newBuilder(
                declaringType.toString(),
                declaringType.sqlAnnotationType?.java,
                this.toString(),
                targetDeclaration.fullName,
                targetDeclaration.annotation(Entity::class)?.let { Entity::class.java }
                    ?: targetDeclaration.annotation(MappedSuperclass::class)?.let { MappedSuperclass::class.java }
                    ?: targetDeclaration.annotation(Embeddable::class)?.let { Embeddable::class.java }
                    ?: targetDeclaration.annotation(Immutable::class)?.let { Immutable::class.java },
                isList,
                resolvedType.isMarkedNullable,
                null
            ) {
                MetaException(it)
            }.apply {
                for (annotation in propDeclaration.annotations) {
                    add(annotation.fullName)
                    if (PropDescriptor.MAPPED_BY_PROVIDER_NAMES.contains(annotation.fullName) &&
                        annotation.arguments.any {
                            it.name?.asString() == "mappedBy" && it.value != ""
                        }) {
                        hasMappedBy()
                    }
                }
            }
            .build()
        primaryAnnotationType = descriptor.type.annotationType
        isNullable = descriptor.isNullable
    }

    private val isAssociation: Boolean =
        (targetDeclaration.classKind === ClassKind.INTERFACE)
            ?.takeIf {
                it
            }
            ?.let {
                ctx.typeAnnotationOf(targetDeclaration)
            }
            ?.let {
                true
            } ?: false

    fun isAssociation(entityLevel: Boolean): Boolean =
        isAssociation && (!entityLevel || targetDeclaration.annotation(Entity::class) != null)

    fun targetTypeName(
        draft: Boolean = false,
        overrideNullable: Boolean? = null
    ): ClassName =
        targetDeclaration
            .className(overrideNullable ?: isNullable) {
                if (draft && isAssociation) {
                    "$it$DRAFT"
                } else {
                    it
                }
            }

    fun typeName(draft: Boolean = false, overrideNullable: Boolean? = null): TypeName =
        targetTypeName(draft, overrideNullable)
            .let {
                if (isList) {
                    if (draft) {
                        MUTABLE_LIST.parameterizedBy(it)
                    } else {
                        LIST.parameterizedBy(it)
                    }
                } else {
                    it
                }
            }

    val targetType: ImmutableType? by lazy {
        targetDeclaration
            .takeIf { isAssociation }
            ?.let { ctx.typeOf(it) }
    }

    val isReference = isAssociation && !isList

    val isScalarList = isList && !isAssociation

    val isId: Boolean =
        primaryAnnotationType == Id::class.java

    val isKey: Boolean =
        propDeclaration.annotations {
            it.fullName == KEY_FULL_NAME
        }.isNotEmpty()

    val isPrimitive: Boolean =
        if (!isList && !isNullable) {
            when (typeName()) {
                BOOLEAN, CHAR, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE -> true
                else -> false
            }
        } else {
            false
        }

    val valueFieldName: String = "__${name}Value"

    val loadedFieldName: String? =
        if (isNullable || isPrimitive) {
            "__${name}Loaded"
        } else {
            null
        }

    fun annotation(annotationType: KClass<out Annotation>): KSAnnotation? =
        propDeclaration.annotation(annotationType)

    fun annotations(annotationType: KClass<out Annotation>): List<KSAnnotation> =
        propDeclaration.annotations(annotationType)

    fun annotations(predicate: (KSAnnotation) -> Boolean): List<KSAnnotation> =
        propDeclaration.annotations(predicate)

    val validationMessages: Map<ClassName, String> =
       parseValidationMessages(propDeclaration)

    fun staticProp(alias: String): StaticProp? =
        staticPropMap[alias] ?: staticPropMap[""]

    private val staticPropMap: MutableMap<String, StaticProp> =
        mutableListOf<KSAnnotation>()
            .apply {
                this += annotations(Static::class)
                for (statics in annotations(Statics::class)) {
                    this += statics["value"] ?: emptyList<KSAnnotation>()
                }
            }
            .map {
                val staticProp = StaticProp(
                    immutableProp = this,
                    alias = it.get<String>("alias") ?: "",
                    name = it.get<String>("name")?.takeIf { n -> n.isNotEmpty() } ?: name,
                    isEnabled = it["enabled"] ?: true,
                    isOptional = it["optional"] ?: false,
                    isIdOnly = it["idOnly"] ?: false,
                    targetAlias = it["targetAlias"] ?: ""
                )
                if (isTransient) {
                    throw MetaException(
                        "Illegal property \"" +
                            this +
                            "\", it is decorated by both @Static and @Transient, " +
                            "this is not allowed"
                    )
                }
                if (staticProp.isOptional && isNullable) {
                    throw MetaException(
                        "Illegal property \"" +
                            this +
                            "\", it is decorated by the annotation @Static " +
                            "whose `optional` is true, it is not allowed for nullable property"
                    )
                }
                if (staticProp.isIdOnly && !isAssociation(true)) {
                    throw MetaException(
                        "Illegal property \"" +
                            this +
                            "\", it is decorated by the annotation @Static " +
                            "whose `idOnly` is true, it is not allowed " +
                            "for non-orm-association property"
                    )
                }
                staticProp
            }
            .let {
                val map = mutableMapOf<String, StaticProp>()
                for (prop in it) {
                    if (map.put(prop.alias, prop) !== null) {
                        throw MetaException(
                            "Illegal prop \"this\", " +
                                "it is decorated by @${Static::class.qualifiedName}," +
                                "the value \"${prop.alias}\" of annotation argument \"alias\"" +
                                " is illegal, the static type with the same alias has already been defined" +
                                "by another @${Static::class.qualifiedName}\""
                        )
                    }
                }
                map
            }

    internal fun resolve() {
        for (staticProp in staticPropMap.values) {
            if (declaringType.isEntity &&
                staticProp.alias.isNotEmpty() &&
                !declaringType.staticDeclarationMap.containsKey(staticProp.alias)
            ) {
                throw MetaException(
                    "Illegal property \"" +
                        this +
                        "\", it is decorated by the annotation @Static " +
                        "whose `alias` is \"" +
                        staticProp.alias +
                        "\", but the declaring entity \"" +
                        declaringType +
                        "\" does not have a static type whose alias is \"" +
                        staticProp.alias +
                        "\""
                )
            }
            if (isAssociation) {
                if (!staticProp.isIdOnly) {
                    val targetStaticType = targetType!!.staticDeclarationMap[staticProp.targetAlias]
                    if (targetStaticType != null) {
                        staticPropMap[staticProp.alias] = staticProp.copy(target = targetStaticType)
                    } else if (staticProp.targetAlias.isEmpty()) {
                        staticPropMap[staticProp.alias] = staticProp.copy(
                            target = StaticDeclaration(
                                targetType!!,
                                "",
                                "",
                                AutoScalarStrategy.ALL,
                                false
                            )
                        )
                    } else {
                        throw MetaException("Illegal property \"" +
                                this +
                                "\", it is decorated by the annotation @Static " +
                                "whose `targetAlias` is \"" +
                                staticProp.targetAlias +
                                "\", but the target entity \"" +
                                targetType +
                                "\" does not have a static type whose alias is \"" +
                                staticProp +
                                "\""
                        )
                    }
                }
            }
        }
    }

    override fun toString(): String =
        "${declaringType}.${propDeclaration.name}"
}