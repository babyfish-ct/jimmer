package org.babyfish.jimmer.ksp.immutable.meta

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import org.babyfish.jimmer.Formula
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.dto.compiler.spi.BaseType
import org.babyfish.jimmer.ksp.*
import org.babyfish.jimmer.ksp.immutable.generator.DRAFT
import org.babyfish.jimmer.ksp.immutable.generator.FETCHER_DSL
import org.babyfish.jimmer.ksp.immutable.generator.PROPS
import org.babyfish.jimmer.ksp.immutable.generator.parseValidationMessages
import org.babyfish.jimmer.ksp.util.fastResolve
import org.babyfish.jimmer.sql.*
import kotlin.reflect.KClass

class ImmutableType(
    ctx: Context,
    val classDeclaration: KSClassDeclaration
) : BaseType {
    private val immutableAnnoTypeName: String =
        listOf(
            classDeclaration.annotation(Entity::class),
            classDeclaration.annotation(MappedSuperclass::class),
            classDeclaration.annotation(Embeddable::class),
            classDeclaration.annotation(Immutable::class)
        ).filterNotNull().map {
            it.annotationType.fastResolve().declaration.fullName
        }.also {
            if (it.size > 1) {
                throw MetaException(
                    classDeclaration,
                    "Conflict annotations: $it"
                )
            }
        }.first()

    override val isEntity: Boolean = immutableAnnoTypeName == Entity::class.qualifiedName

    val isMappedSuperclass: Boolean = immutableAnnoTypeName == MappedSuperclass::class.qualifiedName

    val isEmbeddable: Boolean = immutableAnnoTypeName == Embeddable::class.qualifiedName

    val isImmutable: Boolean = immutableAnnoTypeName == Immutable::class.qualifiedName

    val simpleName: String = classDeclaration.simpleName.asString()

    val className: ClassName = classDeclaration.className()

    val propsClassName: ClassName = classDeclaration.className { "$it$PROPS" }

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

    override val name: String
        get() = classDeclaration.simpleName.asString()

    override val packageName: String
        get() = classDeclaration.packageName.asString()

    override val qualifiedName: String
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
            .map { it.fastResolve().declaration }
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

    val inheritanceRoot: ImmutableType?

    val inheritanceStrategy: InheritanceType?

    val discriminatorColumnName: String?

    val discriminatorValue: String?

    val declaredProperties: Map<String, ImmutableProp>

    val redefinedProps: Map<String, ImmutableProp>

    init {
        val inheritance = classDeclaration.annotation(Inheritance::class)
        val discriminatorColumn = classDeclaration.annotation(DiscriminatorColumn::class)
        val discriminatorValue = classDeclaration.annotation(DiscriminatorValue::class)
        if (!isEntity) {
            if (inheritance !== null) {
                throw MetaException(
                    classDeclaration,
                    "@${Inheritance::class.java.name} can only be declared by entity type"
                )
            }
            if (discriminatorColumn !== null) {
                throw MetaException(
                    classDeclaration,
                    "@${DiscriminatorColumn::class.java.name} can only be declared by entity type"
                )
            }
            if (discriminatorValue !== null) {
                throw MetaException(
                    classDeclaration,
                    "@${DiscriminatorValue::class.java.name} can only be declared by entity type"
                )
            }
            inheritanceRoot = null
            inheritanceStrategy = null
            discriminatorColumnName = null
            this.discriminatorValue = null
        } else if (primarySuperType?.isEntity == true) {
            if (inheritance !== null) {
                throw MetaException(
                    classDeclaration,
                    "@${Inheritance::class.java.name} can only be declared by inheritance root type"
                )
            }
            if (discriminatorColumn !== null) {
                throw MetaException(
                    classDeclaration,
                    "@${DiscriminatorColumn::class.java.name} can only be declared by inheritance root type"
                )
            }
            val root = primarySuperType.inheritanceRoot
                ?: throw MetaException(
                    classDeclaration,
                    "it cannot inherit entity type \"$primarySuperType\" because the super type is not an inheritance root"
                )
            inheritanceRoot = root
            inheritanceStrategy = null
            discriminatorColumnName = null
            this.discriminatorValue =
                discriminatorValue?.get(DiscriminatorValue::value) ?: classDeclaration.simpleName.asString()
        } else if (inheritance !== null) {
            inheritanceRoot = this
            val strategy = when (val value: Any? = inheritance["strategy"]) {
                null -> InheritanceType.SINGLE_TABLE
                is InheritanceType -> value
                is KSClassDeclaration -> InheritanceType.valueOf(value.simpleName.asString())
                else -> throw MetaException(
                    classDeclaration,
                    "Illegal value of @${Inheritance::class.java.name}.strategy: $value"
                )
            }
            val joinedTableDeleteMode = when (val value: Any? = inheritance["joinedTableDeleteMode"]) {
                null -> JoinedTableDeleteMode.EXPLICIT
                is JoinedTableDeleteMode -> value
                is KSClassDeclaration -> JoinedTableDeleteMode.valueOf(value.simpleName.asString())
                else -> throw MetaException(
                    classDeclaration,
                    "Illegal value of @${Inheritance::class.java.name}.joinedTableDeleteMode: $value"
                )
            }
            if (strategy != InheritanceType.JOINED && joinedTableDeleteMode != JoinedTableDeleteMode.EXPLICIT) {
                throw MetaException(
                    classDeclaration,
                    "the `joinedTableDeleteMode` of @${Inheritance::class.java.name} " +
                            "can only be \"${JoinedTableDeleteMode.DB_CASCADE}\" when the inheritance strategy is " +
                            "\"${InheritanceType.JOINED}\""
                )
            }
            inheritanceStrategy = strategy
            discriminatorColumnName =
                discriminatorColumn?.get(DiscriminatorColumn::name)
                    ?: "DTYPE"
            this.discriminatorValue =
                discriminatorValue?.get(DiscriminatorValue::value) ?: classDeclaration.simpleName.asString()
        } else {
            if (discriminatorColumn !== null || discriminatorValue !== null) {
                throw MetaException(
                    classDeclaration,
                    "discriminator annotations can only be used by inheritance types"
                )
            }
            inheritanceRoot = null
            inheritanceStrategy = null
            discriminatorColumnName = null
            this.discriminatorValue = null
        }

        val superPropMap = superTypes
            .flatMap { it.properties.values }
            .groupBy { it.name }
            .toList()
            .associateBy({ it.first }) {
                if (it.second.size > 1) {
                    val prop1 = it.second[0]
                    val prop2 = it.second[1]
                    if (prop1.propDeclaration.type.fastResolve() != prop2.propDeclaration.type.fastResolve()) {
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
            val formula = propDeclaration.annotation(Formula::class)
            if (isEmbeddable && formula !== null && (formula[Formula::sql] ?: "").isNotEmpty()) {
                throw MetaException(
                    propDeclaration,
                    "The sql based formula property cannot be declared in embeddable type"
                )
            }
            if (propDeclaration.isAbstract()) {
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
                    if (formula !== null) {
                        formula[Formula::sql]?.takeIf { it.isNotEmpty() }?.let {
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
                .associateBy({ it.name }) {
                    ImmutableProp(ctx, this, propIdSequence++, it)
                } +
                    classDeclaration
                        .getDeclaredProperties()
                        .filter { it.annotation(Id::class) == null }
                        .associateBy({ it.name }) {
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

    private val idPropNameMap: Map<String, String> by lazy {
        mutableMapOf<String, String>().also { map ->
            for (prop in properties.values) {
                val baseProp = prop.idViewBaseProp
                if (baseProp !== null) {
                    map[baseProp.name] = prop.name
                }
            }
            for (prop in properties.values) {
                if (prop.isReverse) {
                    continue
                }
                if (prop.annotation(OneToOne::class) === null && prop.annotation(ManyToOne::class) === null) {
                    continue
                }
                if (map.containsKey(prop.name)) {
                    continue
                }
                val expectedPropName = "${prop.name}Id"
                val expectedProp = properties[expectedPropName]
                if (expectedProp != null) {
                    if (isExplicitMappedIdNameConflictAllowed(prop, expectedProp)) {
                        continue
                    }
                    throw MetaException(
                        expectedProp.propDeclaration,
                        "It looks like @IdView of association \"${prop}\", please add the @IdView annotation"
                    )
                }
                map[prop.name] = expectedPropName
            }
        }
    }

    private fun isExplicitMappedIdNameConflictAllowed(
        associationProp: ImmutableProp,
        expectedProp: ImmutableProp
    ): Boolean {
        val mapsId = associationProp.annotation(MapsId::class)
        val ownerIdProp = idProp
        return mapsId != null &&
                (mapsId[MapsId::value] ?: "").isEmpty() &&
                !associationProp.isReverse &&
                !associationProp.isTransient &&
                ownerIdProp != null &&
                expectedProp.isId &&
                expectedProp.name == ownerIdProp.name
    }

    fun getIdPropName(prop: String): String? =
        idPropNameMap[prop]

    val propsOrderById: List<ImmutableProp> by lazy {
        properties.values.sortedBy { it.id }
    }

    val idProp: ImmutableProp? by lazy {
        val idProps = declaredProperties.values.filter { it.declaringType === this && it.isId }
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

    internal fun resolve(ctx: Context, step: Int) {
        for (prop in declaredProperties.values) {
            prop.resolve(ctx, step)
        }
        for (prop in redefinedProps.values) {
            prop.resolve(ctx, step)
        }
    }

    companion object {

        @JvmStatic
        private val SQL_ANNOTATION_TYPES =
            setOf(Entity::class, MappedSuperclass::class, Embeddable::class)

        @JvmStatic
        private val FORMULA_CLASS_NAME = Formula::class.qualifiedName
    }
}
