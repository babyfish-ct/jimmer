package org.babyfish.jimmer.ksp.meta

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.ClassName
import org.babyfish.jimmer.ksp.*
import org.babyfish.jimmer.ksp.generator.DRAFT
import org.babyfish.jimmer.ksp.generator.FETCHER_DSL
import org.babyfish.jimmer.ksp.generator.parseValidationMessages
import org.babyfish.jimmer.pojo.*
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.MappedSuperclass
import java.util.regex.Pattern
import kotlin.reflect.KClass

class ImmutableType(
    ctx: Context,
    private val classDeclaration: KSClassDeclaration
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
                        "Illegal type \"$this\", it cannot be decorated by both " +
                            "@${annotationType!!.qualifiedName} and ${sqlAnnotationType.qualifiedName}"
                    )
                }
                annotationType = sqlAnnotationType
            }
        }
        annotationType
    }

    val isEntity: Boolean = classDeclaration.annotation(Entity::class) !== null

    val isMappedSuperclass: Boolean = classDeclaration.annotation(MappedSuperclass::class) != null

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
                        "Illegal immutable type '${classDeclaration.fullName}', " +
                            "it extends several super immutable types: ${it.map { sp -> sp.fullName }}"
                    )
                }
            }
            .firstOrNull()
            ?.let {
                ctx.typeOf(it)
            }

    val declaredProperties: Map<String, ImmutableProp>

    init {
        val superProps = superType?.properties
        val reorderedPropDeclarations = mutableListOf<KSPropertyDeclaration>()
        for (function in classDeclaration.getDeclaredFunctions()) {
            if (function.isAbstract) {
                throw MetaException("Illegal function '${classDeclaration.fullName}.${function}', only non-abstract function is acceptable")
            }
            for (anno in function.annotations) {
                if (anno.fullName.startsWith("org.babyfish.jimmer.")) {
                    throw MetaException(
                        "Non-abstract function '${function}' cannot be decorated by any jimmer annotations"
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
                            throw MetaException("'${propDeclaration}' overrides '$it', this is not allowed")
                        }
                        if (isId == (i == 0)) {
                            reorderedPropDeclarations += propDeclaration
                        }
                    } else {
                        for (anno in propDeclaration.annotations) {
                            if (anno.fullName.startsWith("org.babyfish.jimmer.")) {
                                throw MetaException(
                                    "'${propDeclaration}' is not abstract so that " +
                                        "it cannot be decorated by any jimmer annotations"
                                )
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
        properties.values.sortedBy { it -> it.id }
    }

    val idProp: ImmutableProp? = properties
        .values
        .filter { it.isId }
        .let {
            if (it.isEmpty() && isEntity) {
                throw MetaException("No id property is declared in '$classDeclaration'")
            }
            if (it.size > 1) {
                throw MetaException("Conflict id properties: $it")
            }
            it.firstOrNull()
        }

    val versionProp: ImmutableProp? = properties
        .values
        .filter { it.isId }
        .let {
            if (it.size > 1) {
                throw MetaException("Conflict version properties: $it")
            }
            it.firstOrNull()
        }
        ?.also {
            if (superType !== null && superType.isEntity && it.declaringType === this) {
                throw MetaException("Version property '$it' is not declared in super type")
            }
        }

    val validationMessages: Map<ClassName, String> =
        parseValidationMessages(classDeclaration)

    val declaredStaticDeclarationMap: Map<String, StaticDeclaration> =
        mutableListOf<KSAnnotation>()
            .apply {
                this += classDeclaration.annotations(StaticType::class)
                for (staticTypes in classDeclaration.annotations(StaticTypes::class)) {
                    this += staticTypes["value"] ?: emptyList()
                }
            }
            .map {
                StaticDeclaration(
                    immutableType = this,
                    alias = it.get<String>("alias")?.takeIf { it.isNotEmpty() } ?:
                        throw MetaException(
                            "Illegal type \"${classDeclaration.fullName}\", " +
                                "the `alias` of the annotation " +
                                "@${StaticType::class.qualifiedName} must be specified"
                        ),
                    topLevelName = (it["topLevelName"] ?: "")
                        .also { name ->
                              if (name.isNotEmpty()) {
                                  for (suffix in ILLEGAL_STATIC_SUFFIX) {
                                      if (name.endsWith(suffix)) {
                                          throw MetaException(
                                              "Illegal type \"${classDeclaration.fullName}\", " +
                                                  "it is decorated by @${StaticType::class.qualifiedName}," +
                                                  "the value \"${name}\" of annotation argument \"topLevelName\"" +
                                                  " is illegal, it cannot end \"$suffix\""
                                          )
                                      }
                                  }
                                  if (!STATIC_TYPE_PATTERN.matcher(name).matches()) {
                                      throw MetaException(
                                          "Illegal type \"${classDeclaration.fullName}\", " +
                                              "it is decorated by @${StaticType::class.qualifiedName}," +
                                              "the value \"${name}\" of annotation argument \"topLevelName\"" +
                                              " is illegal, it must match the regexp \"${STATIC_TYPE_PATTERN.pattern()}\""
                                      )
                                  }
                              }
                        },
                    allOptional = it["allOptional"] ?: false
                )
            }
            .let {
                val map = mutableMapOf<String, StaticDeclaration>()
                for (staticDeclaration in it) {
                    if (map.put(staticDeclaration.alias, staticDeclaration) !== null) {
                        throw MetaException(
                            "Illegal type \"${classDeclaration.fullName}\", " +
                                "it is decorated by @${StaticType::class.qualifiedName}," +
                                "the value \"${staticDeclaration.alias}\" of annotation argument \"alias\"" +
                                " is illegal, the static type with the same alias has already been defined" +
                                "by another @${StaticType::class.qualifiedName}\""
                        )
                    }
                }
                map
            }

    val staticDeclarationMap: Map<String, StaticDeclaration> =
        if (superType == null) {
            declaredStaticDeclarationMap
        } else {
            val staticMap = superType.staticDeclarationMap.toMutableMap()
            for (staticDeclaration in declaredStaticDeclarationMap.values) {
                if (staticMap.put(staticDeclaration.alias, staticDeclaration) !== null) {
                    throw MetaException(
                        "Illegal type \"${classDeclaration.fullName}\", " +
                            "it is decorated by @${StaticType::class.qualifiedName}," +
                            "the value \"${staticDeclaration.alias}\" of annotation argument \"alias\"" +
                            " is illegal, the static type with the same alias has already been defined" +
                            "in super types\""
                    )
                }
            }
            staticMap
        }

    fun autoScalarStrategy(alias: String): AutoScalarStrategy =
        autoScalarStrategyMap[alias]
            ?: alias.takeIf { it.isNotEmpty() }?.let { autoScalarStrategyMap[""] }
            ?: AutoScalarStrategy.ALL

    private val autoScalarStrategyMap: Map<String, AutoScalarStrategy> =
        mutableMapOf<String, AutoScalarStrategy>().apply {
            classDeclaration.annotation(AutoScalarRules::class)?.let {
                for (rule in it.get<List<KSAnnotation>>("value") ?: emptyList()) {
                    val alias: String = rule["alias"]!!
                    put(alias, autoScalarStrategy(rule["value"])!!)?.let {
                        conflictAutoScalarStrategy(alias)
                    }
                }
            }
            classDeclaration.annotation(AutoScalarRule::class)?.let {
                val alias: String = it["alias"]!!
                put(alias, autoScalarStrategy(it["value"])!!)?.let {
                    conflictAutoScalarStrategy(alias)
                }
            }
            classDeclaration.annotation(StaticTypes::class)?.let {
                for (rule in it.get<List<KSAnnotation>>("value") ?: emptyList()) {
                    val alias: String = rule["alias"]!!
                    put(alias, autoScalarStrategy(rule["autoScalarStrategy"])!!)?.let {
                        conflictAutoScalarStrategy(alias)
                    }
                }
            }
            classDeclaration.annotation(StaticType::class)?.let {
                val alias: String = it["alias"]!!
                put(alias, autoScalarStrategy(it["autoScalarStrategy"])!!)?.let {
                    conflictAutoScalarStrategy(alias)
                }
            }
        }

    private fun conflictAutoScalarStrategy(alias: String): Nothing =
        throw MetaException(
            "Illegal type \"${classDeclaration.fullName}\", " +
                "the auto scalar strategy of the alias \"${alias}\" cannot be configured multiple times"
        )

    internal fun resolve() {
        for (prop in declaredProperties.values) {
            prop.resolve()
        }
    }

    override fun toString(): String =
        classDeclaration.fullName

    companion object {

        @JvmStatic
        private val SQL_ANNOTATION_TYPES =
            setOf(Entity::class, MappedSuperclass::class, Embeddable::class)

        @JvmStatic
        private val ILLEGAL_STATIC_SUFFIX = arrayOf(
            "Draft", "Fetcher", "Props", "Table", "TableEx"
        )

        @JvmStatic
        private val STATIC_TYPE_PATTERN =
            Pattern.compile("[A-Za-z_$][A-Za-z_$0-9]*")

        @JvmStatic
        private fun autoScalarStrategy(ksType: KSType?): AutoScalarStrategy? =
            when (ksType?.declaration?.simpleName?.asString()) {
                "DECLARED" -> AutoScalarStrategy.DECLARED
                "NONE" -> AutoScalarStrategy.NONE
                "ALL" -> AutoScalarStrategy.ALL
                else -> null
            }
    }
}