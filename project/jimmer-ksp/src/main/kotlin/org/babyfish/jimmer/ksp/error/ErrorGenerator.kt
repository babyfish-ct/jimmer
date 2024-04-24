package org.babyfish.jimmer.ksp.error

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import org.babyfish.jimmer.error.CodeBasedException
import org.babyfish.jimmer.error.CodeBasedRuntimeException
import org.babyfish.jimmer.error.ErrorFamily
import org.babyfish.jimmer.error.ErrorField
import org.babyfish.jimmer.impl.util.StringUtil
import org.babyfish.jimmer.ksp.*
import org.babyfish.jimmer.ksp.immutable.generator.CLIENT_EXCEPTION_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.JVM_STATIC_CLASS_NAME
import org.babyfish.jimmer.ksp.util.generatedAnnotation
import java.io.OutputStreamWriter
import kotlin.reflect.KClass

class ErrorGenerator(
    private val declaration: KSClassDeclaration,
    private val checkedException: Boolean,
    private val codeGenerator: CodeGenerator
) {

    private val enumClassName: ClassName = declaration.className()

    private val declaredFieldsCache = mutableMapOf<KSDeclaration, Map<String, TypeName>>()

    private val fieldsCache = mutableMapOf<KSClassDeclaration, Map<String, TypeName>>()

    private val family: String =
        declaration.annotation(ErrorFamily::class)?.get(ErrorFamily::value)?.takeIf { it.isNotEmpty() }
            ?: declaration.longSimpleName.let {
                when {
                    it.endsWith("_ErrorCode") -> it.substring(0, it.length - 10)
                    it.endsWith("ErrorCode") -> it.substring(0, it.length - 9)
                    it.endsWith("_Error") -> it.substring(0, it.length - 6)
                    it.endsWith("Error") -> it.substring(0, it.length - 5)
                    else -> it
                }
            }.let {
                StringUtil.snake(it, StringUtil.SnakeCase.UPPER)
            }

    private val exceptionSimpleName: String =
        declaration.longSimpleName.let {
            when {
                it.endsWith("_ErrorCode") -> it.substring(0, it.length - 10)
                it.endsWith("ErrorCode") -> it.substring(0, it.length - 9)
                it.endsWith("_Error") -> it.substring(0, it.length - 6)
                it.endsWith("Error") -> it.substring(0, it.length - 5)
                else -> it
            }
        } + "Exception"

    private val exceptionClassName = ClassName(
        declaration.packageName.asString(),
        exceptionSimpleName
    )

    fun generate(allFiles: List<KSFile>) {
        val superType: KClass<*> = if (checkedException) {
            CodeBasedException::class
        } else {
            CodeBasedRuntimeException::class
        }
        codeGenerator.createNewFile(
            Dependencies(false, *allFiles.toTypedArray()),
            declaration.packageName.asString(),
            exceptionSimpleName
        ).use { out ->
            val fileSpec = FileSpec
                .builder(
                    declaration.packageName.asString(),
                    exceptionSimpleName
                ).apply {
                    indent("    ")
                    addType(
                        TypeSpec
                            .classBuilder(exceptionSimpleName)
                            .superclass(superType)
                            .addModifiers(KModifier.ABSTRACT)
                            .addSuperclassConstructorParameter("message, cause")
                            .addAnnotation(generatedAnnotation(enumClassName))
                            .addAnnotation(
                                AnnotationSpec
                                    .builder(CLIENT_EXCEPTION_CLASS_NAME)
                                    .addMember("family = %S", family)
                                    .apply {
                                        val constants = declaration
                                            .declarations
                                            .filterIsInstance<KSClassDeclaration>()
                                            .filter { it.classKind == ClassKind.ENUM_ENTRY }
                                        addMember(
                                            "subTypes = [${constants.joinToString { "%T::class" }}]",
                                            *constants.map {
                                                exceptionClassName.nestedClass(
                                                    ktName(it, true)
                                                )
                                            }.toList().toTypedArray()
                                        )
                                    }
                                    .build()
                            )
                            .apply {
                                addMembers()
                            }
                            .build()
                    )
                }.build()
            val writer = OutputStreamWriter(out, Charsets.UTF_8)
            fileSpec.writeTo(writer)
            writer.flush()
        }
    }

    private fun TypeSpec.Builder.addMembers() {

        addInit(declaration)
        addGetEnum(declaration)
        addFields(declaration)

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

        for (item in declaration.declarations
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.classKind == ClassKind.ENUM_ENTRY}
        ) {
            addType(
                TypeSpec
                    .classBuilder(ktName(item, true))
                    .superclass(exceptionClassName)
                    .addAnnotation(
                        AnnotationSpec
                            .builder(CLIENT_EXCEPTION_CLASS_NAME)
                            .addMember("family = %S", family)
                            .addMember(
                                "code = %S",
                                StringUtil.snake(item.simpleName.asString(), StringUtil.SnakeCase.UPPER)
                            )
                            .build()
                    )
                    .apply {
                        val shared = fieldsOf(item.parentDeclaration as KSClassDeclaration)
                        if (shared.isEmpty()) {
                            addSuperclassConstructorParameter("message, cause")
                        } else {
                            addSuperclassConstructorParameter(
                                "message, cause, " + shared.keys.joinToString()
                            )
                        }
                        addInit(item)
                        addGetEnum(item)
                        addFields(item)
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
                .addParameter(
                    ParameterSpec
                        .builder("message", STRING.copy(nullable = true))
                        .defaultValue("null")
                        .build()
                )
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
                .returns(exceptionClassName.nestedClass(ktName(item, true)))
                .apply {
                    addCode(
                        CodeBlock
                            .builder()
                            .apply {
                                add("return %L(\n", ktName(item, true))
                                indent()
                                add("message,\ncause")
                                for ((name, _) in fields) {
                                    add(",\n").add(name)
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

    private fun TypeSpec.Builder.addInit(declaration: KSClassDeclaration) {
        for ((name, typeName) in declaredFieldsOf(declaration)) {
            addProperty(
                PropertySpec
                    .builder(name, typeName)
                    .initializer(name)
                    .build()
            )
        }
        primaryConstructor(
            FunSpec
                .constructorBuilder()
                .addParameter(
                    ParameterSpec
                        .builder("message", STRING.copy(nullable = true))
                        .defaultValue("null")
                        .build()
                )
                .addParameter(
                    ParameterSpec
                        .builder("cause", THROWABLE.copy(nullable = true))
                        .defaultValue("null")
                        .build()
                )
                .apply {
                    for ((name, typeName) in fieldsOf(declaration)) {
                        addParameter(
                            ParameterSpec
                                .builder(name, typeName)
                                .apply {
                                    if (typeName.isNullable) {
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

    private fun TypeSpec.Builder.addGetEnum(declaration: KSClassDeclaration) {
        addProperty(
            PropertySpec
                .builder(
                    StringUtil.identifier(this@ErrorGenerator.declaration.simpleName.asString()),
                    enumClassName
                )
                .apply {
                    if (declaration.classKind == ClassKind.ENUM_CLASS) {
                        addModifiers(KModifier.ABSTRACT)
                    } else {
                        addModifiers(KModifier.OVERRIDE)
                    }
                }
                .addAnnotation(
                    AnnotationSpec
                        .builder(JsonIgnore::class.asTypeName())
                        .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                        .build()
                )
                .apply {
                    if (declaration.classKind == ClassKind.ENUM_ENTRY) {
                        getter(
                            FunSpec
                                .getterBuilder()
                                .addStatement("return %T.%N", enumClassName, declaration.simpleName.asString())
                                .build()
                        )
                    }
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addFields(declaration: KSClassDeclaration) {
        val fields = fieldsOf(declaration)
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
                                        var addComma = false
                                        for (name in fields.keys) {
                                            if (addComma) {
                                                add(",\n")
                                            } else {
                                                addComma = true
                                            }
                                            add("%S to %N", name, name)
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

    private fun fieldsOf(declaration: KSClassDeclaration): Map<String, TypeName> {
        val cached = fieldsCache[declaration]
        if (cached !== null) {
            return cached
        }
        return if (declaration.classKind == ClassKind.ENUM_CLASS) {
            declaredFieldsOf(declaration)
        } else {
            val shared = declaredFieldsOf(declaration.parentDeclaration as KSClassDeclaration)
            if (shared.isEmpty()) {
                declaredFieldsOf(declaration)
            } else {
                val merged = shared.toMutableMap()
                for ((name, typeName) in declaredFieldsOf(declaration)) {
                    merged.put(name, typeName)?.let {
                        throw MetaException(
                            declaration,
                            "The field \"${name}\" has already been defined in enum \"" +
                                declaration.parentDeclaration!!.fullName +
                                "\""
                        )
                    }
                }
                merged
            }
        }.also {
            fieldsCache[declaration] = it
        }
    }

    private fun declaredFieldsOf(declaration: KSClassDeclaration): Map<String, TypeName> {
        val cached = declaredFieldsCache[declaration]
        if (cached !== null) {
            return cached
        }
        return declaration.annotations(ErrorField::class).map { anno ->
            anno[ErrorField::name]!!.also {
                if (it == "family" || it == "code") {
                    throw MetaException(
                        declaration,
                        null,
                        "The enum constant \"" +
                            declaration.parentDeclaration?.qualifiedName?.asString() +
                            '.' +
                            declaration.simpleName.asString() +
                            "\" is illegal, it cannot be decorated by \"@" +
                            ErrorFamily::class.java.name +
                            "\" with the name \"family\" or \"code\""
                    )
                }
            } to
                anno.getClassArgument(ErrorField::type)!!.toClassName()
                    .let {
                        if (anno[ErrorField::list] == true) {
                            LIST.parameterizedBy(it)
                        } else {
                            it
                        }
                    }
                    .copy(nullable = anno[ErrorField::nullable] == true)
        }.let {
            val map = mutableMapOf<String, TypeName>()
            for ((name, typeName) in it) {
                map.put(name, typeName)?.let {
                    throw MetaException(
                        declaration,
                        "Duplicate field \"$name\""
                    )
                }
            }
            declaredFieldsCache[declaration] = map
            map
        }
    }

    companion object {

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
    }
}