package org.babyfish.jimmer.ksp.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import org.babyfish.jimmer.error.CodeBasedException
import org.babyfish.jimmer.error.ErrorField
import org.babyfish.jimmer.ksp.annotations
import org.babyfish.jimmer.ksp.className
import org.babyfish.jimmer.ksp.get
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
        addClassFields()
        addConstructor()
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
    }

    private fun TypeSpec.Builder.addClassFields() {
        addProperty(
            PropertySpec
                .builder("code", enumClassName, KModifier.OVERRIDE)
                .initializer("code")
                .build()
        )
        addProperty(
            PropertySpec
                .builder("fields", MAP.parameterizedBy(STRING, ANY.copy(nullable = true)), KModifier.OVERRIDE)
                .initializer("fields")
                .build()
        )
    }

    private fun TypeSpec.Builder.addConstructor() {
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
                .addParameter("code", enumClassName)
                .addParameter(
                    "fields",
                    MAP.parameterizedBy(STRING, ANY.copy(nullable = true))
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addItem(item: KSClassDeclaration) {
        val fields = item.annotations(ErrorField::class).map { anno ->
            anno.get<String>("name")!! to
                anno.get<KSType>("type")!!
                    .toClassName()
                    .let {
                        if (anno.get<Boolean>("list") == true) {
                            LIST.parameterizedBy(it)
                        } else {
                            it
                        }
                    }
                    .copy(nullable = anno.get<Boolean>("nullable") == true)
        }
        addFunction(
            FunSpec
                .builder(creatorName(item))
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
                                if (fields.isEmpty()) {
                                    addStatement(
                                        "return %T(message, cause, %T.%N, %M())",
                                        exceptionClassName,
                                        enumClassName,
                                        item.toString(),
                                        EMPTY_MAP
                                    )
                                } else {
                                    add("return %T(\n", exceptionClassName)
                                    indent()
                                    add("message,\n")
                                    add("cause,\n")
                                    add("%T.%N,\n", enumClassName, item.toString())
                                    add("%M(", MAP_OF)
                                    indent()
                                    var addComma = false
                                    for ((name, _) in fields) {
                                        if (addComma) {
                                            add(",")
                                        } else {
                                            addComma = true
                                        }
                                        add("\n%S %M %L", name, TO, name)
                                    }
                                    unindent()
                                    add("\n)\n")
                                    unindent()
                                    add(")\n")
                                }
                            }
                            .build()
                    )
                }
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

        private fun creatorName(item: KSClassDeclaration): String {
            val simpleName = item.toString()
            val size = simpleName.length
            var toUpper = false
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
    }
}