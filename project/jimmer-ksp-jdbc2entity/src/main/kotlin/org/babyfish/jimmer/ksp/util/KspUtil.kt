package org.babyfish.jimmer.ksp.util

import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.*
import java.io.File

val KSPropertyDeclaration.firstTypeArgumentKSClassDeclaration: KSClassDeclaration?
    get() {
        return try {
            // 正确的方式：通过 resolve() 获取类型参数
            val resolvedType = this.type.resolve()
            val firstTypeArgument = resolvedType.arguments.firstOrNull()
            val firstType: KSType? = firstTypeArgument?.type?.resolve()
            val firstClassDeclaration: KSClassDeclaration? = firstType?.declaration as? KSClassDeclaration
            firstClassDeclaration
        } catch (e: Exception) {
            println("获取泛型类型失败: ${this.simpleName.asString()}, 错误: ${e.message}")
            null
        }
    }


val KSPropertyDeclaration.name: String
    get() = this.simpleName.asString()
val KSPropertyDeclaration.resolveType: KSType
    get() = this.type.resolve()
val KSPropertyDeclaration.isRequired: Boolean
    get() = !this.resolveType.isMarkedNullable

val KSPropertyDeclaration.typeDecl: KSDeclaration  // 属性类型的声明
    get() = resolveType.declaration

val KSPropertyDeclaration.typeName: String  // 属性类型的简单名称
    get() = this.typeDecl.simpleName.asString()


// 生成默认值
val KSPropertyDeclaration.defaultValue: String
    get() = this.defaultValue()
// 判断是否需要导入对于form来说,递归可能这里会生成无用的iso



/**
 * 获取 KSType 的基础类型字符串（简化版本）
 * 只处理基本的类型解析，不进行复杂的类型映射
 */
fun KSType.getBasicQualifiedTypeString(): String {
    return try {
        val type = this
        val qualifiedName = type.declaration.qualifiedName?.asString()
        val simpleName = type.declaration.simpleName.asString()

        // 如果类型名包含错误标记，返回简单类型名
        if (simpleName.contains("<ERROR")) {
            return simpleName.replace("<ERROR", "").replace(">", "")
        }

        // 基础类型名称
        val baseType = qualifiedName ?: simpleName

        // 处理泛型参数
        val genericArgs = if (type.arguments.isNotEmpty()) {
            type.arguments.joinToString(", ") { arg ->
                arg.type?.resolve()?.getBasicQualifiedTypeString() ?: "*"
            }
        } else null

        // 处理可空性
        val nullableSuffix = if (type.nullability == Nullability.NULLABLE) "?" else ""

        when {
            genericArgs != null -> "$baseType<$genericArgs>$nullableSuffix"
            else -> "$baseType$nullableSuffix"
        }
    } catch (e: Exception) {
        // 异常时返回简单类型名
        this.declaration.simpleName.asString()
    }
}

/**
 * 检查是否是 Jimmer 实体
 */
private fun KSType.isJimmerEntity(): Boolean {
    return try {
        val declaration = this.declaration as? KSClassDeclaration
        declaration?.annotations?.any {
            it.shortName.asString() == "Entity" &&
                    it.annotationType.resolve().declaration.qualifiedName?.asString()?.contains("jimmer") == true
        } ?: false
    } catch (e: Exception) {
        false
    }
}


fun genCode(pathname: String, code: String, skipExistFile: Boolean = false) {
//        val file = FileUtil.file(pathname)
//        FileUtil.writeUtf8String(toJsonStr, file)

    val targetFile = File(pathname)
    targetFile.parentFile?.mkdirs()
    if (skipExistFile) {
        if (targetFile.exists()) {
            return
        }

    }
    targetFile.writeText(code)

}

// 扩展 KSPropertyDeclaration，判断属性是否为枚举类型
fun KSPropertyDeclaration.isEnumProperty(): Boolean {
    return this.type.resolve().declaration.let { decl ->
        (decl as? KSClassDeclaration)?.classKind == ClassKind.ENUM_CLASS
    }
}


/**
 * 获取属性的全限定类型字符串（包含泛型参数和可空性）
 * 简化版本，只进行基础类型解析
 */
fun KSPropertyDeclaration.getQualifiedTypeString(): String {
    return try {
        val type = this.type.resolve()
        type.getBasicQualifiedTypeString()
    } catch (e: Exception) {
        // 如果类型解析失败，尝试使用原始类型字符串
        val rawTypeString = this.type.toString()
        if (rawTypeString.contains("<ERROR") ||
            rawTypeString.any { !it.isLetterOrDigit() && it != '.' && it != '_' && it != '$' && it != '<' && it != '>' && it != '?' && it != ',' && it != ' ' }
        ) {
            "kotlin.Any"
        } else {
            rawTypeString
        }
    }
}


/**
 * 获取属性的简化类型字符串（不包含包名，但保留泛型）
 */
fun KSPropertyDeclaration.getSimpleTypeString(): String {
    val type = this.type.resolve()
    return buildString {
        // 基础类型名称
        append(type.declaration.simpleName.asString())

        // 处理泛型参数
        if (type.arguments.isNotEmpty()) {
            append("<")
            append(type.arguments.joinToString(", ") { arg ->
                arg.type?.resolve()?.let {
                    it.declaration.simpleName.asString()
                } ?: "*"
            })
            append(">")
        }

        // 处理可空性
        if (type.nullability == Nullability.NULLABLE) {
            append("?")
        }
    }
}


fun KSPropertyDeclaration.defaultValue(): String {
    val type = this.type.resolve()
    val typeDecl = type.declaration
    val fullTypeName = type.declaration.qualifiedName?.asString() ?: ""
    val typeName = typeDecl.simpleName.asString()
    val isNullable = type.isMarkedNullable
    return when {
        this.isEnumProperty() -> {
            if (isNullable) "null" else "${fullTypeName}.entries.first()"
        }

        isNullable -> "null"
        typeName == "String" -> "\"\""
        typeName == "Int" -> "0"
        typeName == "Long" -> "0L"
        typeName == "Double" -> "0.0"
        typeName == "Float" -> "0f"
        typeName == "Boolean" -> "false"
        typeName == "List" -> "emptyList()"
        typeName == "Set" -> "emptySet()"
        typeName == "Map" -> "emptyMap()"
        typeName == "LocalDateTime" -> "kotlin.time.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())"
        typeName == "LocalDate" -> "kotlin.time.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date"
        typeName == "LocalTime" -> "kotlin.time.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).time"
        else -> ""
    }
}


fun KSPropertyDeclaration.getAnno(annoShortName: String): KSAnnotation? {
    return this.annotations.find { it.shortName.asString() == annoShortName }
}

fun KSAnnotation?.getArg(argName: String): Any? {
    val value = this?.arguments?.firstOrNull { it.name?.asString() == argName }?.value
    return value

}


fun KSPropertyDeclaration.isCustomClassType(): Boolean {
    val type = this.type.resolve()
    val declaration = type.declaration

    // 情况1：声明是类（且不是基本类型）
    if (declaration is KSClassDeclaration) {
        val qualifiedName = declaration.qualifiedName?.asString()

        // 排除Kotlin/Java的基本类型
        return qualifiedName !in setOf(
            "kotlin.String",
            "kotlin.Int",
            "kotlin.Long",
            "kotlin.Boolean",
            "kotlin.Float",
            "kotlin.Double",
            "kotlin.Byte",
            "kotlin.Short",
            "kotlin.Char",
            "java.lang.String",
            "java.lang.Integer" // 其他基本类型...
        )
    }

    // 情况2：其他情况（如泛型、类型参数等）默认视为非基本类型
    return true
}


fun KSPropertyDeclaration.ktType(): String {
    val ktType = this.type.resolve().declaration.simpleName.asString()
    return ktType
}


fun KSAnnotation?.getArgFirstValue(): String? {
    return this?.arguments?.firstOrNull()?.value?.toString()

}


fun KSPropertyDeclaration.isNullable(): Boolean {
    return this.type.resolve().isMarkedNullable
}


fun KSPropertyDeclaration.isNullableFlag(): String {
    val nullable = this.isNullable()
    return if (nullable) {
        "NULL"

    } else {
        "NOT NULL"

    }
}


// 判断是否为Jimmer实体接口
fun isJimmerEntity(typeDecl: KSDeclaration): Boolean {
    return typeDecl.annotations.any { it.shortName.asString() == "Entity" } && (typeDecl as? KSClassDeclaration)?.classKind == ClassKind.INTERFACE
}

// 判断是否为枚举类
fun isEnum(typeDecl: KSDeclaration): Boolean {
    return (typeDecl as? KSClassDeclaration)?.classKind == ClassKind.ENUM_CLASS
}

fun KSPropertyDeclaration.isCollectionType(): Boolean {
    val type = this.type.resolve()
    val declaration = type.declaration

    // 获取类型的全限定名（如 "kotlin.collections.List"）
    val typeName = declaration.qualifiedName?.asString() ?: return false

    // 检查是否是常见集合类型
    return typeName in setOf(
        "kotlin.collections.List",
        "kotlin.collections.MutableList",
        "kotlin.collections.Set",
        "kotlin.collections.MutableSet",
        "kotlin.collections.Map",
        "kotlin.collections.MutableMap",
        "java.util.List",
        "java.util.ArrayList",
        "java.util.Set",
        "java.util.HashSet",
        "java.util.Map",
        "java.util.HashMap"
    )
}


/**
 * 猜测Jimmer实体的表名
 * 1. 优先读取@Table注解的name属性
 * 2. 没有则尝试从KDoc注释中提取@table标签
 * 3. 没有则用类名转下划线
 */
fun guessTableName(ktClass: KSClassDeclaration): String {
    // 1. 优先读取@Table注解
    val tableAnn = ktClass.annotations.firstOrNull {
        it.shortName.asString() == "Table" || it.annotationType.resolve().declaration.qualifiedName?.asString() == "org.babyfish.jimmer.sql.Table"
    }
    val tableNameFromAnn = tableAnn?.arguments?.firstOrNull { it.name?.asString() == "name" }?.value as? String
    if (!tableNameFromAnn.isNullOrBlank()) {
        return tableNameFromAnn
    }

    // 2. 尝试从KDoc注释中提取@table标签
    val doc = ktClass.docString
    if (!doc.isNullOrBlank()) {
        // 支持 @table 表名 或 @table:表名
        val regex = Regex("@table[:：]?\\s*([\\w_]+)")
        val match = regex.find(doc)
        if (match != null) {
            return match.groupValues[1]
        }
    }

    // 3. 默认用类名转下划线
    val asString = ktClass.simpleName.asString().toUnderLineCase()
    return asString
}


fun KSPropertyDeclaration.hasAnno(string: String): Boolean {
    return this.getAnno(string) != null
}

/**
 * 获取完整的类型字符串表达，包括泛型、注解、函数类型等
 * 用于 @ComposeAssist 等需要精确类型信息的场景
 */
fun KSType.getCompleteTypeString(): String {
    return buildString {
        // 处理函数类型的注解（如 @Composable）
        val annotations = this@getCompleteTypeString.annotations.toList()
        if (annotations.isNotEmpty()) {
            val annotationStrings = annotations.map { annotation ->
                val shortName = annotation.shortName.asString()
                val args = annotation.arguments
                if (args.isNotEmpty()) {
                    val argString = args.joinToString(", ") { arg ->
                        "${arg.name?.asString() ?: ""}=${arg.value}"
                    }
                    "[$shortName($argString)]"
                } else {
                    "[$shortName]"
                }
            }
            append(annotationStrings.joinToString(" "))
            append(" ")
        }

        // 获取基础类型名称
        val declaration = this@getCompleteTypeString.declaration
        val baseTypeName = declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()

        // 处理函数类型
        if (baseTypeName.startsWith("kotlin.Function")) {
            append(buildFunctionTypeString())
        } else {
            append(baseTypeName)

            // 处理泛型参数
            val typeArguments = this@getCompleteTypeString.arguments
            if (typeArguments.isNotEmpty()) {
                append("<")
                append(typeArguments.joinToString(", ") { arg ->
                    when (arg.variance) {
                        Variance.STAR -> "*"
                        Variance.CONTRAVARIANT -> "in ${arg.type?.resolve()?.getCompleteTypeString() ?: "*"}"
                        Variance.COVARIANT -> "out ${arg.type?.resolve()?.getCompleteTypeString() ?: "*"}"
                        else -> arg.type?.resolve()?.getCompleteTypeString() ?: "*"
                    }
                })
                append(">")
            }
        }

        // 处理可空性
        if (this@getCompleteTypeString.isMarkedNullable) {
            append("?")
        }
    }
}

/**
 * 构建函数类型字符串，如 (T) -> R, @Composable (T) -> Unit 等
 */
private fun KSType.buildFunctionTypeString(): String {
    val declaration = this.declaration
    val baseTypeName = declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()

    // 解析函数类型的参数数量
    val functionNumber = when {
        baseTypeName == "kotlin.Function0" -> 0
        baseTypeName.startsWith("kotlin.Function") -> {
            baseTypeName.removePrefix("kotlin.Function").toIntOrNull() ?: 0
        }

        else -> 0
    }

    val typeArguments = this.arguments

    return buildString {
        // 函数参数类型
        if (functionNumber > 0 && typeArguments.size > functionNumber) {
            append("(")
            val paramTypes = typeArguments.take(functionNumber).map { arg ->
                arg.type?.resolve()?.getCompleteTypeString() ?: "*"
            }
            append(paramTypes.joinToString(", "))
            append(")")
        } else if (functionNumber == 0) {
            append("()")
        }

        append(" -> ")

        // 返回类型
        val returnType = typeArguments.lastOrNull()?.type?.resolve()?.getCompleteTypeString() ?: "Unit"
        append(returnType)
    }
}

/**
 * 获取参数的完整类型字符串，包括参数注解
 */
fun KSValueParameter.getCompleteTypeString(): String {
    return buildString {
        // 处理参数注解
        val annotations = this@getCompleteTypeString.annotations.toList()
        if (annotations.isNotEmpty()) {
            val annotationStrings = annotations.map { annotation ->
                val shortName = annotation.shortName.asString()
                val args = annotation.arguments
                if (args.isNotEmpty()) {
                    val argString = args.joinToString(", ") { arg ->
                        val name = arg.name?.asString()
                        val value = arg.value
                        if (name != null) "$name=$value" else value.toString()
                    }
                    "@$shortName($argString)"
                } else {
                    "@$shortName"
                }
            }
            append(annotationStrings.joinToString(" "))
            append(" ")
        }

        // 获取类型字符串
        append(this@getCompleteTypeString.type.resolve().getCompleteTypeString())
    }
}

/**
 * 获取简化的类型字符串，移除包名但保留泛型和注解
 */
fun KSType.getSimplifiedTypeString(): String {
    return this.getCompleteTypeString()
        .replace("kotlin.collections.", "")
        .replace("kotlin.", "")
        .replace("androidx.compose.runtime.", "")
        .replace("androidx.compose.ui.", "")
        .replace("androidx.compose.foundation.", "")
        .replace("androidx.compose.material3.", "")
}

/**
 * 获取函数的完整签名字符串，包括泛型参数、参数注解等
 */
fun KSFunctionDeclaration.getCompleteSignature(): String {
    return buildString {
        // 函数注解
        val annotations = this@getCompleteSignature.annotations.toList()
        if (annotations.isNotEmpty()) {
            annotations.forEach { annotation ->
                append("@${annotation.shortName.asString()}")
                if (annotation.arguments.isNotEmpty()) {
                    append("(")
                    append(annotation.arguments.joinToString(", ") { arg ->
                        "${arg.name?.asString() ?: ""}=${arg.value}"
                    })
                    append(")")
                }
                append("\n")
            }
        }

        append("fun ")

        // 泛型参数
        val typeParameters = this@getCompleteSignature.typeParameters
        if (typeParameters.isNotEmpty()) {
            append("<")
            append(typeParameters.joinToString(", ") { typeParam ->
                val name = typeParam.name.asString()
                val bounds = typeParam.bounds.toList()
                if (bounds.isNotEmpty()) {
                    val boundsString = bounds.joinToString(" & ") { bound ->
                        bound.resolve().getCompleteTypeString()
                    }
                    "$name : $boundsString"
                } else {
                    name
                }
            })
            append("> ")
        }

        append(this@getCompleteSignature.simpleName.asString())
        append("(")

        // 参数列表
        val parameters = this@getCompleteSignature.parameters
        append(parameters.joinToString(",\n    ") { param ->
            val paramName = param.name?.asString() ?: ""
            val paramType = param.getCompleteTypeString()
            val defaultValue = if (param.hasDefault) " = ..." else ""
            "$paramName: $paramType$defaultValue"
        })

        append(")")

        // 返回类型
        val returnType = this@getCompleteSignature.returnType?.resolve()
        if (returnType != null && returnType.declaration.simpleName.asString() != "Unit") {
            append(": ${returnType.getCompleteTypeString()}")
        }
    }
}

/**
 * 布尔值加法操作符，用于权重计算
 * 将布尔值视为 0 和 1 进行加法运算
 */
operator fun Boolean.plus(other: Boolean): Int = this.toInt() + other.toInt()

/**
 * 布尔值转整数
 */
fun Boolean.toInt(): Int = if (this) 1 else 0

/**
 * 整数与布尔值加法操作符
 */
operator fun Int.plus(boolean: Boolean): Int = this + boolean.toInt()
