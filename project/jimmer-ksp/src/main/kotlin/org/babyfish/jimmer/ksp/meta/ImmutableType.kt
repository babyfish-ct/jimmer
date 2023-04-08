package org.babyfish.jimmer.ksp.meta

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.ClassName
import org.babyfish.jimmer.Formula
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

    val isEntity: Boolean = classDeclaration.annotation(Entity::class) !== null

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

    val superType: ImmutableType? =
        classDeclaration
            .superTypes
            .map { it.resolve().declaration }
            .filterIsInstance<KSClassDeclaration>()
            .filter {
                it.classKind == ClassKind.INTERFACE &&
                    ctx.typeAnnotationOf(it) !== null
            }
            .toList()
            .also { 
                if (it.size > 1) {
                    throw MetaException(
                        classDeclaration,
                        "it extends several super immutable types: ${it.map { sp -> sp.fullName }}"
                    )
                }
            }
            .firstOrNull()
            ?.let {
                ctx.typeOf(it)
            }?.also {
                if (!it.isAcrossMicroServices && it.microServiceName != microServiceName) {
                    throw MetaException(
                        classDeclaration,
                        "its micro service name is \"" +
                            microServiceName +
                            "\" but the micro service name of its super type \"" +
                            it.qualifiedName +
                            "\" is \"" +
                            it.microServiceName +
                            "\""
                    )
                }
            }

    val declaredProperties: Map<String, ImmutableProp>

    init {
        val superProps = superType?.properties
        val reorderedPropDeclarations = mutableListOf<KSPropertyDeclaration>()
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
        for (i in 0..1) {
            classDeclaration
                .getDeclaredProperties()
                .forEach { propDeclaration ->
                    if (propDeclaration.isAbstract()) {
                        val isId = propDeclaration.annotations(Id::class).isNotEmpty()
                        superProps?.get(propDeclaration.name)?.let {
                            throw MetaException(
                                propDeclaration,
                                "it overrides '$it', this is not allowed"
                            )
                        }
                        if (isId == (i == 0)) {
                            reorderedPropDeclarations += propDeclaration
                        }
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
                                reorderedPropDeclarations += propDeclaration
                            }
                        }
                    }
                }
        }
        val basePropId = (superProps?.size ?: 0) + 1
        declaredProperties = reorderedPropDeclarations
            .mapIndexed { index, propDeclaration ->
                ImmutableProp(ctx, this, basePropId + index, propDeclaration)
            }
            .associateBy { it.name }
    }

    val properties: Map<String, ImmutableProp> =
        if (superType == null) {
            declaredProperties
        } else {
            val map = mutableMapOf<String, ImmutableProp>()
            for ((name, prop) in superType.properties) {
                if (prop.isId) {
                    map[name] = prop
                }
            }
            for ((name, prop) in declaredProperties) {
                if (prop.isId) {
                    map[name] = prop
                }
            }
            for ((name, prop) in superType.properties) {
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
        val superIdProp = superType?.idProp
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