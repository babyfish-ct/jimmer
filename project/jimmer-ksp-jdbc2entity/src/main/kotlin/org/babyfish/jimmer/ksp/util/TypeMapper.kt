package org.babyfish.jimmer.ksp.util

// 定义Kotlin标准库类型，这些类型通常不需要导入
val kotlinStdTypes = setOf(
    "String",
    "Int",
    "Long",
    "Double",
    "Float",
    "Boolean",
    "Short",
    "Byte",
    "Char",
    "List",
    "Set",
    "Map",
    "MutableList",
    "MutableSet",
    "MutableMap"
)


/**
 * 类型映射工具类
 */
object TypeMapper {

    fun defaultValueForType(type: String): String {
        return when (type) {
            "String" -> "\"\""
            "Int" -> "0"
            "Long" -> "0L"
            "Boolean" -> "false"
            "Double" -> "0.0"
            else -> "null"
        }
    }

    /**
     * 将数据库类型映射到Kotlin类型
     */
    fun mapToKotlinType(columnType: String, isKmp: Boolean = true): String {
        val localDateStr = if (isKmp) {
            "kotlinx.datetime.LocalDate"
        } else {
            "java.time.LocalDate"
        }

        val localTimeStr = if (isKmp) {
            "kotlinx.datetime.LocalTime"
        } else {
            "java.time.LocalTime"
        }

        val localDateTimeStr = if (isKmp) {
            "kotlinx.datetime.LocalDateTime"
        } else {
            "java.time.LocalDateTime"
        }



        return when {
            columnType.contains("char", ignoreCase = true) -> "String"
            columnType.contains("varchar", ignoreCase = true) -> "String"
            columnType.contains("text", ignoreCase = true) -> "String"
            columnType.contains("bigint", ignoreCase = true) -> "Long"
            columnType.contains("int", ignoreCase = true) -> "Long"
            columnType.contains("int8", ignoreCase = true) -> "Long"
            columnType.contains("smallint", ignoreCase = true) -> "Short"
            columnType.contains("float", ignoreCase = true) -> "Float"
            columnType.contains("double", ignoreCase = true) -> "Double"
            columnType.contains("real", ignoreCase = true) -> "Float"
            columnType.contains("bool", ignoreCase = true) -> "Boolean"
            columnType.contains("timestamp", ignoreCase = true) -> localDateTimeStr
            columnType.contains("date", ignoreCase = true) -> localDateStr
            columnType.contains("time", ignoreCase = true) -> localTimeStr
//            else -> columnType.toBigCamelCase()
            else -> "String"
        }
    }


    fun mapJdbcTypeToKotlinType4Form(columnType: String, nullable: Boolean): String {
        // 可根据实际数据库类型扩展
        return when (columnType.lowercase()) {
            "varchar", "text", "char", "uuid" -> if (nullable) "String?" else "String"
            "int", "integer", "serial" -> if (nullable) "Int?" else "Int"
            "bigint" -> if (nullable) "Long?" else "Long"
            "bool", "boolean" -> if (nullable) "Boolean?" else "Boolean"
            "float", "double", "real", "numeric", "decimal" -> if (nullable) "Double?" else "Double"
            "date", "timestamp", "timestamptz" -> if (nullable) "String?" else "String"
            else -> if (nullable) "String?" else "String"
        }
    }


}
