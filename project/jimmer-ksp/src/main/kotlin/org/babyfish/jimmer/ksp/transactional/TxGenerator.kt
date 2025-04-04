package org.babyfish.jimmer.ksp.transactional

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import org.babyfish.jimmer.ksp.*
import org.babyfish.jimmer.ksp.immutable.generator.PROPAGATION_CLASS_NAME
import org.babyfish.jimmer.ksp.util.fastResolve
import org.babyfish.jimmer.ksp.util.suppressAllAnnotation
import java.io.OutputStreamWriter

class TxGenerator(
    private val codeGenerator: CodeGenerator,
    private val ctx: Context,
    private val declaration: KSClassDeclaration
) {
    private val simpleName = declaration.simpleName.asString() + "Tx"

    private val sqlClientName: String =
        determineSqlClientName()

    private val classTx = declaration.annotation(TX)

    fun generate(allFiles: List<KSFile>) {
        codeGenerator.createNewFile(
            Dependencies(false, *allFiles.toTypedArray()),
            declaration.packageName.asString(),
            simpleName
        ).use {
            val fileSpec = FileSpec
                .builder(
                    declaration.packageName.asString(),
                    simpleName
                ).apply {
                    indent("    ")
                    addAnnotation(suppressAllAnnotation())
                    addType()
                }.build()
            val writer = OutputStreamWriter(it, Charsets.UTF_8)
            fileSpec.writeTo(writer)
            writer.flush()
        }
    }

    private fun determineSqlClientName(): String {
        val sqlClientType = ctx.resolver.getClassDeclarationByName("org.babyfish.jimmer.sql.kt.KSqlClient")!!.asStarProjectedType()
        val props = declaration.getDeclaredProperties()
            .filter { sqlClientType.isAssignableFrom(it.type.fastResolve()) }
            .toList()
        if (props.isEmpty()) {
            throw MetaException(
                declaration,
                "The class uses @Tx must have a non-static properties whose type is KSqlClient"
            )
        }
        if (props.size > 1) {
            throw MetaException(
                declaration,
                "The class uses @Tx cannot multiple non-static sqlClient properties"
            )
        }
        val prop = props[0]
        if (prop.isPrivate()) {
            throw MetaException(
                prop,
                "The sqlClient field of the class uses @Tx cannot be private, protected or internal is recommended"
            )
        }
        return prop.name
    }

    private fun FileSpec.Builder.addType() {
        addType(
            TypeSpec.classBuilder(simpleName)
                .apply {
                    if (declaration.isInternal()) {
                        addModifiers(KModifier.INTERNAL)
                    }
                    if (declaration.isAbstract()) {
                        addModifiers(KModifier.ABSTRACT)
                    }
                    for (anno in declaration.annotations) {
                        val fullName = anno.fullName
                        if (fullName != TX  && fullName != TARGET_ANNOTATION) {
                            addAnnotation(anno.toAnnotationSpec())
                        }
                    }
                    val targetAnnotation = declaration.annotation(TARGET_ANNOTATION)
                    if (targetAnnotation != null) {
                        val annoDeclaration = targetAnnotation.getClassArgument("value")
                        addAnnotation(annoDeclaration!!.toClassName())
                    }
                    superclass(declaration.toClassName())
                    declaration.primaryConstructor?.let {
                        for (parameter in it.parameters) {
                            addSuperclassConstructorParameter(parameter.name!!.asString())
                        }
                    }
                    addConstructors()
                    addFunctions()
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addConstructors() {
        val primaryConstructor = declaration.primaryConstructor?.takeIf { !it.isPrivate() }
        if (primaryConstructor !== null) {
            primaryConstructor(
                FunSpec.constructorBuilder().apply {
                    setConstructorProperties(primaryConstructor, true)
                }.build()
            )
        } else {
            for (constructor in declaration.getConstructors()) {
                if (!constructor.isPrivate()) {
                    addFunction(
                        FunSpec.constructorBuilder().apply {
                            setConstructorProperties(constructor, false)
                        }.build()
                    )
                }
            }
        }
    }

    private fun FunSpec.Builder.setConstructorProperties(
        constructor: KSFunctionDeclaration,
        primary: Boolean
    ) {
        if (constructor.isProtected()) {
            addModifiers(KModifier.PROTECTED)
        }
        if (constructor.isInternal()) {
            addModifiers(KModifier.INTERNAL)
        }
        for (anno in constructor.annotations) {
            addAnnotation(anno.toAnnotationSpec())
        }
        for (parameter in constructor.parameters) {
            addParameter(
                ParameterSpec
                    .builder(parameter.name!!.asString(), parameter.type.toTypeName())
                    .build()
            )
        }
        if (!primary) {
            callSuperConstructor(*constructor.parameters.map { it.name!!.asString() }.toTypedArray())
        }
    }

    private fun TypeSpec.Builder.addFunctions() {
        for (function in declaration.getDeclaredFunctions()) {
            val tx = function.annotation(TX)
            if (tx != null && !function.isOpen()) {
                throw MetaException(
                    function,
                    "Only open method cannot be decorated by @Tx"
                )
            }
            val finalTx = when {
                tx !== null -> tx
                classTx === null || !function.isPublic() || function.isConstructor() -> continue
                else -> {
                    if (!function.isOpen()) {
                        throw MetaException(
                            function,
                            "The public method inherits the class-level @Tx must be open"
                        );
                    }
                    classTx
                }
            }
            addFunction(function, finalTx)
        }
    }

    private fun TypeSpec.Builder.addFunction(function: KSFunctionDeclaration, tx: KSAnnotation) {
        val propagation = tx.get<Any>("value").toString().let {
            val index = it.lastIndexOf(".")
            if (index == -1) {
                it
            } else {
                it.substring(index + 1)
            }
        }
        addFunction(
            FunSpec.builder(function.simpleName.asString())
                .apply {
                    addModifiers(KModifier.OVERRIDE)
                    if (function.isProtected()) {
                        addModifiers(KModifier.PROTECTED)
                    } else if (function.isInternal()) {
                        addModifiers(KModifier.INTERNAL)
                    }
                    for (anno in function.annotations) {
                        if (anno.fullName != TX) {
                            addAnnotation(anno.toAnnotationSpec())
                        }
                    }
                    for (parameter in function.parameters) {
                        addParameter(
                            ParameterSpec
                                .builder(parameter.name!!.asString(), parameter.type.toTypeName())
                                .build()
                        )
                    }
                    function.returnType?.let {
                        returns(it.toTypeName())
                    }
                    addCode(
                        CodeBlock.builder().apply {
                            beginControlFlow(
                                "return this.%L.transaction(%T.%L)",
                                sqlClientName,
                                PROPAGATION_CLASS_NAME,
                                propagation
                            )
                            addStatement(
                                "super.%L(%L)",
                                function.simpleName.asString(),
                                function.parameters.map { it.name!!.asString() }.joinToString { ", " }
                            )
                            endControlFlow()
                        }.build()
                    )
                }
                .build()
        )
    }
}