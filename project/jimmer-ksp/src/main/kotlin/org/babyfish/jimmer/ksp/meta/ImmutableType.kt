package org.babyfish.jimmer.ksp.meta

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import org.babyfish.jimmer.Formula
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.ksp.*
import org.babyfish.jimmer.ksp.generator.DRAFT
import org.babyfish.jimmer.ksp.generator.FETCHER_DSL
import org.babyfish.jimmer.ksp.generator.parseValidationMessages
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.MappedSuperclass
import kotlin.reflect.KClass

class ImmutableType(
    ctx: Context,
    val classDeclaration: KSClassDeclaration
) {
    private val immutableAnnoTypeName: String =
        listOf(
            classDeclaration.annotation(Entity::class),
            classDeclaration.annotation(MappedSuperclass::class),
            classDeclaration.annotation(Embeddable::class),
            classDeclaration.annotation(Immutable::class)
        ).filterNotNull().map {
            it.annotationType.resolve().declaration.fullName
        }.also {
            if (it.size > 1) {
                throw MetaException(
                    classDeclaration,
                    "Conflict annotations: $it"
                )
            }
        }.first()

    val isEntity: Boolean = immutableAnnoTypeName == Entity::class.qualifiedName

    val isMappedSuperclass: Boolean = immutableAnnoTypeName == MappedSuperclass::class.qualifiedName

    val isEmbeddable: Boolean = immutableAnnoTypeName == Embeddable::class.qualifiedName

    val isImmutable: Boolean = immutableAnnoTypeName == Immutable::class.qualifiedName

    val simpleName: String = classDeclaration.simpleName.asString()

    val className: ClassName = classDeclaration.className()

    val draftClassName: ClassName = classDeclaration.className { "$it$DRAFT" }

    val fetcherDslClassName: ClassName = classDeclaration.className { "$it$FETCHER_DSL" }

    fun draftClassName(vararg nestedNames: String) =
        classDeclaration.nestedClassName {
            mutableListOf<String>().apply {
                add("$it$DRAFT")
                for (nestedName in nestedNames) {
                    add(nestedName)
                }
            }
        }

    val sqlAnnotationType: KClass<out Annotation>? = run {
        var annotationType: KClass<out Annotation>? = null
        for (sqlAnnotationType in SQL_ANNOTATION_TYPES) {
            classDeclaration.annotation(sqlAnnotationType)?.let {
                if (annotationType != null) {
                    throw MetaException(
                        classDeclaration,
                        "it cannot be decorated by both " +
                            "@${annotationType!!.qualifiedName} and ${sqlAnnotationType.qualifiedName}"
                    )
                }
                annotationType = sqlAnnotationType
            }
        }
        annotationType
    }

    val name: String
        get() = classDeclaration.simpleName.asString()

    val qualifiedName: String
        get() = classDeclaration.qualifiedName!!.asString()

    val isAcrossMicroServices: Boolean =
        classDeclaration.annotation(MappedSuperclass::class)?.get(MappedSuperclass::acrossMicroServices) ?: false

    val microServiceName: String =
        (
            classDeclaration.annotation(Entity::class)?.get(Entity::microServiceName)
                ?: classDeclaration.annotation(MappedSuperclass::class)?.get(MappedSuperclass::microServiceName)
                ?: ""
        ).also {
            if (it.isNotEmpty() && isAcrossMicroServices) {
                throw MetaException(
                    classDeclaration,
                    "the `acrossMicroServices` of its annotation \"@" +
                        MappedSuperclass::class.java.name +
                        "\" is true so that `microServiceName` cannot be specified"
                )
            }
        }

    val superTypes: List<ImmutableType> =
        classDeclaration
            .superTypes
            .map { it.resolve().declaration }
            .filterIsInstance<KSClassDeclaration>()
            .filter {
                it.classKind == ClassKind.INTERFACE &&
                    ctx.typeAnnotationOf(it) !== null
            }
            .toList()
            .map {
                ctx.typeOf(it)
            }.also {
                if (it.isEmpty()) {
                    return@also
                }
                when {
                    isImmutable -> if (it.size > 1) {
                        throw MetaException(
                            classDeclaration,
                            "simple immutable type does not support multiple inheritance"
                        )
                    }
                    isEmbeddable -> throw MetaException(
                        classDeclaration,
                        "embeddable type does not support inheritance"
                    )
                    isEntity -> for (superType in it) {
                        if (!superType.isEntity && !superType.isMappedSuperclass) {
                            throw MetaException(
                                classDeclaration,
                                "the super type \"$superType\" is neither entity nor mapped super class"
                            )
                        }
                    }
                    isMappedSuperclass -> for (superType in it) {
                        if (!superType.isMappedSuperclass) {
                            throw MetaException(
                                classDeclaration,
                                "the super type \"$superType\" is not mapped super class"
                            )
                        }
                    }
                }
                for (superType in it) {
                    if (!superType.isAcrossMicroServices && superType.microServiceName != microServiceName) {
                        throw MetaException(
                            classDeclaration,
                            "its micro service name is \"" +
                                microServiceName +
                                "\" but the micro service name of its super type \"" +
                                superType.qualifiedName +
                                "\" is \"" +
                                superType.microServiceName +
                                "\""
                        )
                    }
                }
            }

    val primarySuperType: ImmutableType? =
        superTypes
            .filter { !it.isMappedSuperclass }
            .also {
                if (it.size > 1) {
                    throw MetaException(
                        classDeclaration,
                        "two many primary(not mapped super class) super types: $it"
                    )
                }
            }
            .firstOrNull()

    val declaredProperties: Map<String, ImmutableProp>

    val redefinedProps: Map<String, ImmutableProp>

    init {
        val superPropMap = superTypes
            .flatMap { it.properties.values }
            .groupBy { it.name }
            .toList()
            .associateBy({it.first}) {
                if (it.second.size > 1) {
                    val prop1 = it.second[0]
                    val prop2 = it.second[1]
                    if (prop1.propDeclaration.type.resolve() != prop2.propDeclaration.type.resolve()) {
                        throw MetaException(
                            classDeclaration,
                            "There are two super properties with the same name: \"" +
                                prop1 +
                                "\" and \"" +
                                prop2 +
                                "\", but their return type are different"
                        )
                    }
                }
                it.second.first()
            }

        for (propDeclaration in classDeclaration.getDeclaredProperties()) {
            val superProp = superPropMap[propDeclaration.name]
            if (superProp != null) {
                throw MetaException(
                    propDeclaration,
                    "it overrides '$superProp', this is not allowed"
                )
            }
            if (propDeclaration.isAbstract()) {
                val formula = propDeclaration.annotation(Formula::class)
                if (formula !== null) {
                    val sql = formula[Formula::sql] ?: ""
                    if (sql.isEmpty()) {
                        throw MetaException(
                            propDeclaration,
                            "it is abstract and decorated by @" +
                                Formula::class.java.name +
                                ", abstract modifier means simple calculation property based on " +
                                "SQL expression so that the `sql` of that annotation must be specified"
                        )
                    }
                    val dependencies = formula.getListArgument(Formula::dependencies) ?: emptyList()
                    if (dependencies.isNotEmpty()) {
                        throw MetaException(
                            propDeclaration,
                            "it is abstract and decorated by @" +
                                Formula::class.java.name +
                                ", abstract modifier means simple calculation property based on " +
                                "SQL expression so that the `dependencies` of that annotation cannot be specified"
                        )
                    }
                }
            } else {
                for (anno in propDeclaration.annotations) {
                    if (anno.fullName.startsWith("org.babyfish.jimmer.") && anno.fullName != FORMULA_CLASS_NAME) {
                        throw MetaException(
                            propDeclaration,
                            "it is not abstract so that " +
                                "it cannot be decorated by " +
                                "any jimmer annotations except @" +
                                FORMULA_CLASS_NAME
                        )
                    }
                    val formula = propDeclaration.annotation(Formula::class)
                    if (formula !== null) {
                        formula[Formula::sql]?.takeIf { it.isNotEmpty() } ?.let {
                            throw MetaException(
                                propDeclaration,
                                "it is non-abstract and decorated by @" +
                                    Formula::class.java.name +
                                    ", non-abstract modifier means simple calculation property based on " +
                                    "kotlin expression so that the `sql` of that annotation cannot be specified"
                            )
                        }
                        val dependencies = formula.getListArgument(Formula::dependencies) ?: emptyList()
                        if (dependencies.isEmpty()) {
                            throw MetaException(
                                propDeclaration,
                                "it is non-abstract and decorated by @" +
                                    Formula::class.java.name +
                                    ", non-abstract modifier means simple calculation property based on " +
                                    "kotlin expression so that the `dependencies` of that annotation must be specified"
                            )
                        }
                    }
                }
            }
        }

        for (function in classDeclaration.getDeclaredFunctions()) {
            if (function.isAbstract) {
                throw MetaException(function, "only non-abstract function is acceptable")
            }
            for (anno in function.annotations) {
                if (anno.fullName.startsWith("org.babyfish.jimmer.")) {
                    throw MetaException(
                        classDeclaration,
                        "Non-abstract function cannot be decorated by any jimmer annotations"
                    )
                }
            }
        }

        var propIdSequence = primarySuperType?.properties?.size ?: 0
        redefinedProps = superPropMap.filterKeys {
            primarySuperType == null || !primarySuperType.properties.containsKey(it)
        }.mapValues {
            ImmutableProp(ctx, this, propIdSequence++, it.value.propDeclaration)
        }

        declaredProperties =
            classDeclaration
                .getDeclaredProperties()
                .filter { it.annotation(Id::class) != null }
                .associateBy({it.name}) {
                    ImmutableProp(ctx, this, propIdSequence++, it)
                } +
                classDeclaration
                    .getDeclaredProperties()
                    .filter { it.annotation(Id::class) == null }
                    .associateBy({it.name}) {
                        ImmutableProp(ctx, this, propIdSequence++, it)
                    }
    }

    val properties: Map<String, ImmutableProp> =
        if (superTypes.isEmpty()) {
            declaredProperties
        } else {
            val map = mutableMapOf<String, ImmutableProp>()
            for (superType in superTypes) {
                for ((name, prop) in superType.properties) {
                    if (prop.isId) {
                        map[name] = prop
                    }
                }
            }
            for ((name, prop) in redefinedProps) {
                if (prop.isId) {
                    map[name] = prop
                }
            }
            for ((name, prop) in declaredProperties) {
                if (prop.isId) {
                    map[name] = prop
                }
            }
            for (superType in superTypes) {
                for ((name, prop) in superType.properties) {
                    if (!prop.isId) {
                        map[name] = prop
                    }
                }
            }
            for ((name, prop) in redefinedProps) {
                if (!prop.isId) {
                    map[name] = prop
                }
            }
            for ((name, prop) in declaredProperties) {
                if (!prop.isId) {
                    map[name] = prop
                }
            }
            map
        }

    val propsOrderById: List<ImmutableProp> by lazy {
        properties.values.sortedBy { it.id }
    }

    val idProp: ImmutableProp? by lazy {
        val idProps = declaredProperties.values.filter { it.isId }
        if (idProps.size > 1) {
            throw MetaException(
                classDeclaration,
                "two many properties are decorated by \"@${Id::class.qualifiedName}\": " +
                    idProps
            )
        }
        val superIdProp = superTypes.firstOrNull { it.idProp !== null }?.idProp
        if (superIdProp != null && idProps.isNotEmpty()) {
            throw MetaException(
                classDeclaration,
                "it cannot declare id property " +
                    "because id property has been declared by super type"
            )
        }
        val prop = idProps.firstOrNull() ?: superIdProp
        if (prop == null && isEntity) {
            throw MetaException(
                classDeclaration,
                "it is decorated by \"@${Entity::class.qualifiedName}\" " +
                    "but there is no id property"
            )
        }
        prop
    }

    val validationMessages: Map<ClassName, String> =
        parseValidationMessages(classDeclaration)

    override fun toString(): String =
        classDeclaration.fullName

    internal fun resolve(ctx: Context, step: Int): Boolean {
        var hasNext = false
        for (prop in declaredProperties.values) {
            hasNext = hasNext or prop.resolve(ctx, step)
        }
        for (prop in redefinedProps.values) {
            hasNext = hasNext or prop.resolve(ctx, step)
        }
        return hasNext
    }

    companion object {

        @JvmStatic
        private val SQL_ANNOTATION_TYPES =
            setOf(Entity::class, MappedSuperclass::class, Embeddable::class)

        @JvmStatic
        private val FORMULA_CLASS_NAME = Formula::class.qualifiedName
    }
}