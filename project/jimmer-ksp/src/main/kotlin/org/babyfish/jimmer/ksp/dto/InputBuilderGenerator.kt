package org.babyfish.jimmer.ksp.dto

import com.google.devtools.ksp.getClassDeclarationByName
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import org.babyfish.jimmer.dto.compiler.AbstractProp
import org.babyfish.jimmer.dto.compiler.DtoModifier
import org.babyfish.jimmer.dto.compiler.DtoProp
import org.babyfish.jimmer.dto.compiler.DtoType
import org.babyfish.jimmer.ksp.annotation
import org.babyfish.jimmer.ksp.get
import org.babyfish.jimmer.ksp.fullName
import org.babyfish.jimmer.ksp.immutable.generator.INPUT_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableProp
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableType
import org.babyfish.jimmer.ksp.util.generatedAnnotation

class InputBuilderGenerator(
    private val parentGenerator: DtoGenerator
) {

    private val dtoType: DtoType<ImmutableType, ImmutableProp> = parentGenerator.dtoType

    fun generate() {
        parentGenerator.typeBuilder.addType(
            TypeSpec
                .classBuilder("Builder")
                .apply {
                    addAnnotation(generatedAnnotation())
                    addMembers()
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addMembers() {
        for (prop in dtoType.props) {
            addField(prop)
            addStateField(prop)
        }
        for (prop in dtoType.props) {
            addSetter(prop)
        }
        addBuild()
    }

    private fun TypeSpec.Builder.addField(prop: AbstractProp) {
        val isFieldNullable = isFieldNullable(prop)
        val typeName = parentGenerator.propTypeName(prop).copy(nullable = isFieldNullable)
        addProperty(
            PropertySpec
                .builder(prop.name, typeName)
                .addModifiers(KModifier.PRIVATE)
                .mutable(true)
                .initializer(
                    if (isFieldNullable) {
                        "null"
                    } else {
                        "false"
                    }
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addStateField(prop: AbstractProp) {
        parentGenerator.statePropName(prop, true)?.let {
            addProperty(
                PropertySpec
                    .builder(it, BOOLEAN, KModifier.PRIVATE)
                    .mutable(true)
                    .initializer("false")
                    .build()
            )
        }
    }

    private fun TypeSpec.Builder.addSetter(prop: AbstractProp) {
        val typeName = parentGenerator.propTypeName(prop)
        addFunction(
            FunSpec
                .builder(prop.name)
                .addParameter(prop.name, typeName)
                .returns(parentGenerator.getDtoClassName("Builder"))
                .apply {
                    addPropertyAnnotations(prop)
                }
                .addStatement("this.%L = %L", prop.name, prop.name)
                .apply {
                    parentGenerator.statePropName(prop, true)?.let {
                        addStatement("this.%L = true", it)
                    }
                }
                .addStatement("return this")
                .build()
        )
    }

    private fun TypeSpec.Builder.addBuild() {
        addFunction(
            FunSpec
                .builder("build")
                .returns(parentGenerator.getDtoClassName())
                .addCode(
                    CodeBlock
                        .builder()
                        .add("return %T(\n", parentGenerator.getDtoClassName())
                        .indent()
                        .apply {
                            for (prop in dtoType.props) {
                                val builderStatePropName = parentGenerator.statePropName(prop, true)
                                val dtoStatePropName = parentGenerator.statePropName(prop, false)
                                if (builderStatePropName === null) {
                                    addArg(prop)
                                    add(",\n")
                                } else {
                                    val inputModifier = prop.inputModifier
                                    add("// %L\n", inputModifier)
                                    if (inputModifier == DtoModifier.FIXED) {
                                        beginControlFlow("if (!%L)", builderStatePropName)
                                        add(
                                            "throw %T.unknownNullableProperty(%T::class.java, %S)",
                                            INPUT_CLASS_NAME,
                                            parentGenerator.getDtoClassName(),
                                            prop.name
                                        )
                                        nextControlFlow("else")
                                        addArg(prop)
                                        endControlFlow()
                                        add(",\n")
                                    } else {
                                        addArg(prop)
                                        add(",\n")
                                    }
                                    if (dtoStatePropName !== null) {
                                        add("%L,\n", builderStatePropName)
                                    }
                                }
                            }
                        }
                        .unindent()
                        .add(")\n")
                        .build()
                )
                .build()
        )
    }

    private fun CodeBlock.Builder.addArg(prop: AbstractProp) {
        add(prop.name)
        if (!prop.isNullable) {
            add(
                " ?: throw %T.unknownNonNullProperty(%T::class.java, %S)",
                INPUT_CLASS_NAME,
                parentGenerator.getDtoClassName(),
                prop.name
            )
        }
    }

    private fun acceptsFunction(name: String): Boolean {
        val annotation = parentGenerator.ctx.resolver.getClassDeclarationByName(name) ?: return false
        annotation.annotation(kotlin.annotation.Target::class)?.let { target ->
            return target.get<List<Any>>("allowedTargets").orEmpty().any { it.toString().endsWith("FUNCTION") }
        }
        annotation.annotation(java.lang.annotation.Target::class)?.let { target ->
            return target.get<List<Any>>("value").orEmpty().any { it.toString().endsWith("METHOD") }
        }
        return true
    }

    private fun FunSpec.Builder.addPropertyAnnotations(prop: AbstractProp) {
        val typeNames = mutableSetOf<String>()
        for (anno in prop.annotations) {
            if (acceptsFunction(anno.qualifiedName) && typeNames.add(anno.qualifiedName)) {
                addAnnotation(DtoGenerator.annotationOf(anno, parentGenerator.ctx.resolver))
            }
        }
        val base = prop.basePropOrNull as? ImmutableProp ?: return
        for (anno in base.annotations { DtoGenerator.isCopyableAnnotation(it, prop.annotations) }) {
            if (acceptsFunction(anno.fullName) && typeNames.add(anno.fullName)) {
                addAnnotation(anno.toAnnotationSpec().toBuilder().useSiteTarget(null).build())
            }
        }
    }

    private fun isFieldNullable(prop: AbstractProp): Boolean =
        prop.funcName != "null" && prop.funcName != "notNull"
}
