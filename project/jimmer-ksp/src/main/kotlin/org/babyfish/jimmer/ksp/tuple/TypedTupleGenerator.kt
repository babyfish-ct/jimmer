package org.babyfish.jimmer.ksp.tuple

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import org.babyfish.jimmer.Input
import org.babyfish.jimmer.Specification
import org.babyfish.jimmer.View
import org.babyfish.jimmer.impl.util.StringUtil
import org.babyfish.jimmer.ksp.Context
import org.babyfish.jimmer.ksp.MetaException
import org.babyfish.jimmer.ksp.immutable.generator.*
import org.babyfish.jimmer.ksp.name
import java.io.OutputStreamWriter

class TypedTupleGenerator(
    val ctx: Context,
    val declaration: KSClassDeclaration
) {
    private val props = declaration.getDeclaredProperties().toList()

    private val dtoSuperTypes by lazy {
        DTO_SUPER_TYPES.mapNotNull {
            ctx.resolver
                .getClassDeclarationByName(it)
                ?.asStarProjectedType()
        }
    }

    private val tableClassName = ClassName(
        declaration.packageName.asString(),
        "${declaration.simpleName.asString()}Table"
    )

    private val nullableTableClassName = tableClassName.nestedClass("Nullable")

    private val isBaseTableProjection = props.none {
        isDto(it.type.resolve())
    }

    init {
        if (props.isEmpty()) {
            throw MetaException(
                declaration,
                "There is properties"
            )
        }
    }

    fun generate() {
        ctx.environment.codeGenerator.createNewFile(
            Dependencies(false, declaration.containingFile!!),
            declaration.packageName.asString(),
            "${declaration.simpleName.asString()}Mapper"
        ).use {
            val fileSpec = FileSpec
                .builder(
                    declaration.packageName.asString(),
                    "${declaration.simpleName.asString()}Mapper"
                ).apply {
                    indent("    ")
                    addType(
                        TypeSpec
                            .classBuilder("${declaration.simpleName.asString()}Mapper")
                            .addSuperinterface(
                                TUPLE_MAPPER_CLASS_NAME.parameterizedBy(
                                    declaration.toClassName()
                                )
                            )
                            .apply {
                                addMembers()
                            }
                            .build()
                    )
                    if (isBaseTableProjection) {
                        addType(baseTableType())
                    }
                }.build()
            val writer = OutputStreamWriter(it, Charsets.UTF_8)
            fileSpec.writeTo(writer)
            writer.flush()
        }
    }

    private fun TypeSpec.Builder.addMembers() {
        if (isBaseTableProjection) {
            addSuperinterface(
                K_BASE_TABLE_PROJECTION.parameterizedBy(
                    tableClassName,
                    nullableTableClassName
                )
            )
        }
        addConstructor()
        addProp()
        addGetSelections()
        addGetBaseTableFactory()
        addGetSelectionLayout()
        addCreateTuple()
        for (i in 1 until props.size) {
            addBuilderClass(i)
        }
        addCompanion()
    }

    private fun TypeSpec.Builder.addConstructor() {
        primaryConstructor(
            FunSpec.constructorBuilder()
                .addModifiers(KModifier.PRIVATE)
                .addParameter(
                    "selections",
                    SELECTIONS_FIELD_TYPE
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addProp() {
        addProperty(
            PropertySpec
                .builder(
                    "selections",
                    SELECTIONS_FIELD_TYPE
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer("selections")
                .build()
        )
    }

    private fun TypeSpec.Builder.addGetSelections() {
        addFunction(
            FunSpec
                .builder("getSelections")
                .addModifiers(KModifier.OVERRIDE)
                .addAnnotation(
                    AnnotationSpec
                        .builder(Suppress::class)
                        .addMember("%S", "UNCHECKED_CAST")
                        .build()
                )
                .returns(
                    LIST.parameterizedBy(
                        SELECTION_CLASS_NAME.parameterizedBy(STAR)
                    )
                )
                .addStatement(
                    "return %T.unmodifiableList(selections.asList()) as List<%T>",
                    COLLECTIONS_CLASS_NAME,
                    SELECTION_CLASS_NAME.parameterizedBy(STAR)
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addGetBaseTableFactory() {
        if (!isBaseTableProjection) {
            return
        }
        addFunction(
            FunSpec
                .builder("getBaseTableFactory")
                .addModifiers(KModifier.OVERRIDE)
                .returns(
                    BASE_TABLE_FACTORY.parameterizedBy(
                        tableClassName,
                        nullableTableClassName
                    )
                )
                .addStatement("return %T.FACTORY", tableClassName)
                .build()
        )
    }

    private fun TypeSpec.Builder.addGetSelectionLayout() {
        if (!isBaseTableProjection) {
            return
        }
        addFunction(
            FunSpec
                .builder("getSelectionLayout")
                .addModifiers(KModifier.OVERRIDE)
                .returns(BASE_TABLE_SELECTION_LAYOUT)
                .addStatement("return %T.SELECTION_LAYOUT", tableClassName)
                .build()
        )
    }

    private fun baseTableType(): TypeSpec {
        val factoryTypeName = BASE_TABLE_FACTORY.parameterizedBy(
            tableClassName,
            nullableTableClassName
        )
        return TypeSpec
            .classBuilder(tableClassName)
            .primaryConstructor(baseTableConstructor())
            .superclass(ABSTRACT_K_BASE_TABLE)
            .addSuperclassConstructorParameter("baseTable")
            .addSuperinterface(
                K_NON_NULL_BASE_TABLE.parameterizedBy(nullableTableClassName)
            )
            .apply {
                props.forEachIndexed { index, prop ->
                    addProperty(baseTableProperty(prop, index, false))
                }
                addWeakJoinFunctions(tableClassName)
                addType(nullableBaseTableType())
                addType(
                    TypeSpec
                        .companionObjectBuilder()
                        .addProperty(
                            PropertySpec
                                .builder("FACTORY", factoryTypeName)
                                .addModifiers(KModifier.INTERNAL)
                                .initializer("%L", baseTableFactory())
                                .build()
                        )
                        .addProperty(
                            PropertySpec
                                .builder("SELECTION_LAYOUT", BASE_TABLE_SELECTION_LAYOUT)
                                .addModifiers(KModifier.INTERNAL)
                                .initializer("%L", baseTableSelectionLayout())
                                .build()
                        )
                        .build()
                )
            }
            .build()
    }

    private fun nullableBaseTableType(): TypeSpec =
        TypeSpec
            .classBuilder("Nullable")
            .primaryConstructor(baseTableConstructor())
            .superclass(ABSTRACT_K_BASE_TABLE)
            .addSuperclassConstructorParameter("baseTable")
            .addSuperinterface(K_NULLABLE_BASE_TABLE)
            .apply {
                props.forEachIndexed { index, prop ->
                    addProperty(baseTableProperty(prop, index, true))
                }
                addWeakJoinFunctions(nullableTableClassName)
            }
            .build()

    private fun TypeSpec.Builder.addWeakJoinFunctions(sourceTypeName: TypeName) {
        addFunction(weakJoinFunction(sourceTypeName, false, false))
        addFunction(weakJoinFunction(sourceTypeName, true, false))
        addFunction(weakJoinFunction(sourceTypeName, false, true))
        addFunction(weakJoinFunction(sourceTypeName, true, true))
    }

    private fun weakJoinFunction(
        sourceTypeName: TypeName,
        byType: Boolean,
        outer: Boolean
    ): FunSpec {
        val nullableTargetType = TypeVariableName("TNT", K_NULLABLE_BASE_TABLE)
        val targetType = TypeVariableName(
            "TT",
            K_NON_NULL_BASE_TABLE.parameterizedBy(
                if (outer) nullableTargetType else STAR
            )
        )
        return FunSpec
            .builder(if (outer) "weakOuterJoin" else "weakJoin")
            .apply {
                if (outer) {
                    addTypeVariable(nullableTargetType)
                }
                addTypeVariable(targetType)
                addParameter(
                    "targetSymbol",
                    K_BASE_TABLE_SYMBOL.parameterizedBy(targetType)
                )
                if (byType) {
                    addParameter(
                        "weakJoinType",
                        K_CLASS.parameterizedBy(
                            WildcardTypeName.producerOf(
                                K_PROPS_WEAK_JOIN.parameterizedBy(
                                    sourceTypeName,
                                    targetType
                                )
                            )
                        )
                    )
                } else {
                    addParameter(
                        "weakJoinLambda",
                        K_PROPS_WEAK_JOIN_FUN.parameterizedBy(
                            sourceTypeName,
                            targetType
                        )
                    )
                }
                returns(if (outer) nullableTargetType else targetType)
                addStatement(
                    "return %L(targetSymbol, %L)",
                    if (outer) "weakOuterJoinImpl" else "weakJoinImpl",
                    if (byType) "weakJoinType" else "weakJoinLambda"
                )
            }
            .build()
    }

    private fun baseTableConstructor(): FunSpec =
        FunSpec
            .constructorBuilder()
            .addModifiers(KModifier.INTERNAL)
            .addParameter("baseTable", BASE_TABLE)
            .build()

    private fun baseTableFactory(): CodeBlock =
        CodeBlock.builder()
            .add("%T.of(\n", BASE_TABLE_FACTORY)
            .indent()
            .add("{ %T(it) },\n", tableClassName)
            .add("{ %T(it) }\n", nullableTableClassName)
            .unindent()
            .add(")")
            .build()

    private fun baseTableSelectionLayout(): CodeBlock =
        CodeBlock.builder().apply {
            add("%T.of(\n", BASE_TABLE_SELECTION_LAYOUT)
            indent()
            props.forEachIndexed { index, prop ->
                if (index != 0) {
                    add(",\n")
                }
                add(
                    "%T.%L",
                    BASE_TABLE_SELECTION_KIND,
                    selectionKindConstant(prop)
                )
            }
            unindent()
            add("\n)")
        }.build()

    private fun baseTableProperty(
        prop: KSPropertyDeclaration,
        index: Int,
        outerNullable: Boolean
    ): PropertySpec =
        PropertySpec
            .builder(prop.name, baseTableSelectionTypeName(prop, outerNullable))
            .getter(
                FunSpec
                    .getterBuilder()
                    .addStatement("return selection(%L, %L)", index, outerNullable)
                    .build()
            )
            .build()

    private fun baseTableSelectionTypeName(
        prop: KSPropertyDeclaration,
        outerNullable: Boolean
    ): TypeName {
        val type = prop.type.resolve()
        val valueTypeName = prop.type.toTypeName().copy(nullable = false)
        val nullable = outerNullable || type.isMarkedNullable
        return if (isEntity(type)) {
            (if (nullable) K_NULLABLE_TABLE_CLASS_NAME else K_NON_NULL_TABLE_CLASS_NAME)
                .parameterizedBy(valueTypeName)
        } else {
            (if (nullable) K_NULLABLE_EXPRESSION else K_NONNULL_EXPRESSION)
                .parameterizedBy(valueTypeName)
        }
    }

    private fun selectionKindConstant(prop: KSPropertyDeclaration): String {
        val type = prop.type.resolve()
        return when {
            isEntity(type) && type.isMarkedNullable ->
                "NULLABLE_TABLE"

            isEntity(type) ->
                "NON_NULL_TABLE"

            type.isMarkedNullable ->
                "NULLABLE_EXPRESSION"

            else ->
                "NON_NULL_EXPRESSION"
        }
    }

    private fun isEntity(type: KSType): Boolean {
        val typeDeclaration = type.declaration as? KSClassDeclaration ?: return false
        return ctx.typeAnnotationOf(typeDeclaration) != null &&
                ctx.typeOf(typeDeclaration).isEntity
    }

    private fun isDto(type: KSType): Boolean {
        val typeDeclaration = type.declaration as? KSClassDeclaration ?: return false
        val starType = typeDeclaration.asStarProjectedType()
        return dtoSuperTypes.any {
            it.isAssignableFrom(starType)
        }
    }

    private fun TypeSpec.Builder.addCreateTuple() {
        addFunction(
            FunSpec
                .builder("createTuple")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("args", ARRAY.parameterizedBy(ANY.copy(nullable = true)))
                .returns(declaration.toClassName())
                .addCode(
                    CodeBlock.builder().apply {
                        add("return %T(\n", declaration.toClassName())
                        indent()
                        for (i in props.indices) {
                            if (i != 0) {
                                add(",\n")
                            }
                            add(
                                "%L = args[%L] as %T",
                                props[i].name,
                                i.toString(),
                                props[i].type.toTypeName()
                            )
                        }
                        unindent()
                        add("\n)\n")
                    }.build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addBuilderClass(index: Int) {
        val prop = props[index]
        addType(
            TypeSpec
                .classBuilder(StringUtil.typeName(prop.name, "Builder"))
                .primaryConstructor(
                    FunSpec
                        .constructorBuilder()
                        .addModifiers(KModifier.INTERNAL)
                        .addParameter("selections", SELECTIONS_FIELD_TYPE)
                        .build()
                )
                .addProperty(
                    PropertySpec
                        .builder("selections", SELECTIONS_FIELD_TYPE)
                        .addModifiers(KModifier.PRIVATE)
                        .initializer("selections")
                        .build()
                )
                .addFunction(
                    FunSpec
                        .builder(prop.name)
                        .addParameter(
                            "selection",
                            SELECTION_CLASS_NAME.parameterizedBy(
                                prop.type.toTypeName()
                            )
                        )
                        .returns(buildReturnTypeName(index))
                        .addStatement("selections[%L] = selection", index.toString())
                        .addStatement("return %T(selections)", buildReturnTypeName(index))
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addCompanion() {
        addType(
            TypeSpec
                .companionObjectBuilder()
                .addFunction(
                    FunSpec
                        .builder(props[0].name)
                        .addParameter(
                            "selection",
                            SELECTION_CLASS_NAME.parameterizedBy(
                                props[0].type.toTypeName()
                            )
                        )
                        .returns(buildReturnTypeName(0))
                        .addStatement(
                            "val selections = arrayOfNulls<%T>(%L)",
                            SELECTION_CLASS_NAME.parameterizedBy(STAR),
                            props.size
                        )
                        .addStatement("selections[0] = selection")
                        .addStatement("return %T(selections)", buildReturnTypeName(0))
                        .build()
                )
                .build()
        )
    }

    private fun buildReturnTypeName(index: Int): ClassName {
        if (index + 1 < props.size) {
            return ClassName(
                declaration.packageName.asString(),
                StringUtil.typeName(declaration.simpleName.asString(), "Mapper"),
                StringUtil.typeName(props[index + 1].name, "Builder")
            )
        }
        return ClassName(
            declaration.packageName.asString(),
            StringUtil.typeName(declaration.simpleName.asString(), "Mapper")
        )
    }

    companion object {
        private val ABSTRACT_K_BASE_TABLE = ClassName(
            "org.babyfish.jimmer.sql.kt.ast.table.impl",
            "AbstractKBaseTable"
        )

        private val BASE_TABLE = ClassName(
            "org.babyfish.jimmer.sql.ast.table",
            "BaseTable"
        )

        private val BASE_TABLE_FACTORY = ClassName(
            "org.babyfish.jimmer.sql.ast.table.spi",
            "BaseTableFactory"
        )

        private val BASE_TABLE_SELECTION_KIND = ClassName(
            "org.babyfish.jimmer.sql.ast.table.spi",
            "BaseTableSelectionKind"
        )

        private val BASE_TABLE_SELECTION_LAYOUT = ClassName(
            "org.babyfish.jimmer.sql.ast.table.spi",
            "BaseTableSelectionLayout"
        )

        private val K_BASE_TABLE_PROJECTION = ClassName(
            "org.babyfish.jimmer.sql.kt.ast.query",
            "KBaseTableProjection"
        )

        private val K_NON_NULL_BASE_TABLE = ClassName(
            "org.babyfish.jimmer.sql.kt.ast.table",
            "KNonNullBaseTable"
        )

        private val K_NULLABLE_BASE_TABLE = ClassName(
            "org.babyfish.jimmer.sql.kt.ast.table",
            "KNullableBaseTable"
        )

        private val K_BASE_TABLE_SYMBOL = ClassName(
            "org.babyfish.jimmer.sql.kt.ast.table",
            "KBaseTableSymbol"
        )

        private val K_PROPS_WEAK_JOIN = ClassName(
            "org.babyfish.jimmer.sql.kt.ast.table",
            "KPropsWeakJoin"
        )

        private val K_PROPS_WEAK_JOIN_FUN = ClassName(
            "org.babyfish.jimmer.sql.kt.ast.table",
            "KPropsWeakJoinFun"
        )

        private val K_CLASS = ClassName("kotlin.reflect", "KClass")

        private val K_NULLABLE_EXPRESSION = ClassName(
            "org.babyfish.jimmer.sql.kt.ast.expression",
            "KNullableExpression"
        )

        private val DTO_SUPER_TYPES = listOf(
            View::class.qualifiedName!!,
            Input::class.qualifiedName!!,
            Specification::class.qualifiedName!!
        )

        private val SELECTIONS_FIELD_TYPE =
            ARRAY.parameterizedBy(
                SELECTION_CLASS_NAME.parameterizedBy(
                    STAR
                ).copy(nullable = true)
            )
    }
}
