package org.babyfish.jimmer.ksp.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import org.babyfish.jimmer.error.CodeBasedException
import org.babyfish.jimmer.error.ErrorField
import org.babyfish.jimmer.ksp.annotations
import org.babyfish.jimmer.ksp.className
import org.babyfish.jimmer.ksp.get
import org.babyfish.jimmer.ksp.getClassArgument
import java.io.OutputStreamWriter

class ErrorGenerator(
    private val declaration: KSClassDeclaration,
    private val codeGenerator: CodeGenerator
) {

    private val enumClassName: ClassName = declaration.className()

    private val exceptionSimpleName: String =
        declaration.longSimpleName.let {
            when {
                it.endsWith("_ErrorCode") -> it.substring(0, it.length - 10)
                it.endsWith("ErrorCode") -> it.substring(0, it.length - 9)
                it.endsWith("_Error") -> it.substring(0, it.length - 6)
                it.endsWith("Error") -> it.substring(0, it.length - 5)
                else -> it
            } + "Exception"
        }

    private val exceptionClassName = ClassName(
        declaration.packageName.asString(),
        exceptionSimpleName
    )

    fun generate(allFiles: List<KSFile>) {
        codeGenerator.createNewFile(
            Dependencies(false, *allFiles.toTypedArray()),
            declaration.packageName.asString(),
            exceptionSimpleName
        ).use {
            val fileSpec = FileSpec
                .builder(
                    declaration.packageName.asString(),
                    exceptionSimpleName
                ).apply {
                    indent("    ")
                    addType(
                        TypeSpec
                            .classBuilder(exceptionSimpleName)
                            .superclass(CodeBasedException::class)
                            .addModifiers(KModifier.ABSTRACT)
                            .addSuperclassConstructorParameter("message, cause")
                            .apply {
                                addMembers()
                            }
                            .build()
                    )
                }.build()
            val writer = OutputStreamWriter(it, Charsets.UTF_8)
            fileSpec.writeTo(writer)
            writer.flush()
        }
    }

    private fun TypeSpec.Builder.addMembers() {

        primaryConstructor(
            FunSpec
                .constructorBuilder()
                .addModifiers(KModifier.PRIVATE)
                .addParameter("message", STRING)
                .addParameter(
                    ParameterSpec
                        .builder(
                            "cause",
                            THROWABLE.copy(nullable = true)
                        )
                        .defaultValue("null")
                        .build()
                )
                .build()
        )

        addProperty(
            PropertySpec
                .builder("code", enumClassName)
                .addModifiers(KModifier.ABSTRACT, KModifier.OVERRIDE)
                .build()
        )

        addType(
            TypeSpec
                .companionObjectBuilder()
                .apply {
                    for (item in declaration.declarations.filterIsInstance<KSClassDeclaration>()) {
                        addItem(item)
                    }
                }
                .build()
        )

        for (item in declaration.declarations.filterIsInstance<KSClassDeclaration>()) {
            addType(
                TypeSpec
                    .classBuilder(ktName(item, true))
                    .superclass(exceptionClassName)
                    .addSuperclassConstructorParameter("message, cause")
                    .apply {
                        val fields = fieldsOf(item)
                        addInit(fields)
                        addCode(item)
                        addFields(fields)
                    }
                    .build()
            )
        }
    }

    private fun TypeSpec.Builder.addItem(item: KSClassDeclaration) {
        val fields = fieldsOf(item)
        addFunction(
            FunSpec
                .builder(ktName(item, false))
                .addAnnotation(JVM_STATIC_CLASS_NAME)
                .addParameter("message", STRING)
                .addParameter(
                    ParameterSpec
                        .builder("cause", THROWABLE.copy(nullable = true))
                        .defaultValue("null")
                        .build()
                )
                .apply {
                    for ((name, type) in fields) {
                        if (type.isNullable) {
                            addParameter(
                                ParameterSpec
                                    .builder(name, type)
                                    .defaultValue("null")
                                    .build()
                            )
                        } else {
                            addParameter(name, type)
                        }
                    }
                }
                .returns(exceptionClassName)
                .apply {
                    addCode(
                        CodeBlock
                            .builder()
                            .apply {
                                add("return %L(\n", ktName(item, true))
                                indent()
                                add("message,\ncause")
                                for (field in fields) {
                                    add(",\n").add(field.first)
                                }
                                unindent()
                                add("\n)\n")
                            }
                            .build()
                    )
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addInit(fields: List<Pair<String, TypeName>>) {
        for (field in fields) {
            addProperty(
                PropertySpec
                    .builder(field.first, field.second)
                    .initializer(field.first)
                    .build()
            )
        }
        primaryConstructor(
            FunSpec
                .constructorBuilder()
                .addParameter("message", STRING)
                .addParameter(
                    ParameterSpec
                        .builder("cause", THROWABLE.copy(nullable = true))
                        .defaultValue("null")
                        .build()
                )
                .apply {
                    for (field in fields) {
                        addParameter(
                            ParameterSpec
                                .builder(field.first, field.second)
                                .apply {
                                    if (field.second.isNullable) {
                                        defaultValue("null")
                                    }
                                }
                                .build()
                        )
                    }
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addCode(item: KSClassDeclaration) {
        addProperty(
            PropertySpec
                .builder("code", enumClassName, KModifier.OVERRIDE)
                .getter(
                    FunSpec
                        .getterBuilder()
                        .addStatement("return %T.%N", enumClassName, item.simpleName.asString())
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addFields(fields: List<Pair<String, TypeName>>) {
        addProperty(
            PropertySpec
                .builder("fields", MAP.parameterizedBy(STRING, ANY.copy(nullable = true)), KModifier.OVERRIDE)
                .getter(
                    FunSpec
                        .getterBuilder()
                        .addCode(
                            CodeBlock
                                .builder()
                                .apply {
                                    if (fields.isEmpty()) {
                                        add("return emptyMap()")
                                    } else {
                                        add("return mapOf(\n")
                                        indent()
                                        for (i in fields.indices) {
                                            if (i != 0) {
                                                add(",\n")
                                            }
                                            add("%S to %N", fields[i].first, fields[i].first)
                                        }
                                        unindent()
                                        add("\n)\n")
                                    }
                                }
                                .build()
                        )
                        .build()
                )
                .build()
        )
    }

    companion object {

        private val EMPTY_MAP = MemberName("kotlin.collections", "emptyMap")

        private val MAP_OF = MemberName("kotlin.collections", "mapOf")

        private val TO = MemberName("kotlin", "to")

        private val KSClassDeclaration.longSimpleName: String
            get() = (parentDeclaration?.takeIf { it is KSClassDeclaration }?.let {
                (it as KSClassDeclaration).longSimpleName + "_"
            } ?: "") + simpleName.asString()

        private fun ktName(item: KSClassDeclaration, upperHead: Boolean): String {
            val simpleName = item.toString()
            val size = simpleName.length
            var toUpper = upperHead
            val builder = StringBuilder()
            for (i in 0 until size) {
                val c = simpleName[i]
                toUpper = if (c == '_') {
                    true
                } else {
                    if (toUpper) {
                        builder.append(c.uppercaseChar())
                    } else {
                        builder.append(c.lowercaseChar())
                    }
                    false
                }
            }
            return builder.toString()
        }

        private fun fieldsOf(item: KSClassDeclaration): List<Pair<String, TypeName>> =
            item.annotations(ErrorField::class).map { anno ->
                anno[ErrorField::name]!! to
                    anno.getClassArgument(ErrorField::type)!!.toClassName()
                        .let {
                            if (anno[ErrorField::list] == true) {
                                LIST.parameterizedBy(it)
                            } else {
                                it
                            }
                        }
                        .copy(nullable = anno[ErrorField::nullable] == true)
            }
    }
}