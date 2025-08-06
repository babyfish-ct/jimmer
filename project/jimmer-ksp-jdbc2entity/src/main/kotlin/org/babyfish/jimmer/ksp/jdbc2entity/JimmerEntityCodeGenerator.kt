package org.babyfish.jimmer.ksp.jdbc2entity

import org.babyfish.jimmer.ksp.context.SettingContext
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import org.babyfish.jimmer.ksp.jdbc2entity.entity.JdbcColumnMetadata
import org.babyfish.jimmer.ksp.jdbc2entity.entity.JdbcTableMetadata
import org.babyfish.jimmer.ksp.util.TypeMapper.mapToKotlinType
import org.babyfish.jimmer.ksp.util.toBigCamelCase
import org.babyfish.jimmer.ksp.util.toLowCamelCase
import java.io.File

/**
 * Jimmer 实体代码生成器
 */
class JimmerEntityCodeGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) {


    /**
     * 生成 Jimmer 实体类（使用 IO 直接写入文件）
     */
    fun generateEntityWithIO(table: JdbcTableMetadata) {
        val settings = SettingContext.settings

        val modelPackageName = settings.modelPackageName
        val className = getEntityClassName(table.tableName)
        val fileName = "$className.kt"
        val fileContent = generateEntityFileContent(table, modelPackageName)

        val modelOutputDir = settings.modelOutputDir


        val targetDir = File(modelOutputDir)

        // 确保目录存在
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        // 写入文件
        val targetFile = File(modelOutputDir, fileName)
        if (targetFile.exists()) {
            logger.warn("实体文件已存在，跳过生成: ${targetFile.absolutePath}")
            return
        }
        targetFile.writeText(fileContent)

        logger.info("生成实体文件: ${targetFile.absolutePath}")
    }

    /**
     * 生成实体文件内容
     * 使用模板字符串
     */
    private fun generateEntityFileContent(table: JdbcTableMetadata, packageName: String): String {
        val className = table.tableName.toBigCamelCase()
        val tableName = table.tableName
        val description = table.remarks.ifBlank { className }

        // 所有实体约定都继承 BaseEntity,后续用户可自行修改(已有实体不会覆盖)
        val baseInterface = " : BaseEntity"

        // 生成导入语句
        val imports = generateImports(table, true)

        // 生成属性（排除 id 字段，因为它在 BaseEntity 中已定义）
        val filteredColumns = table.columns.filter { !isIdField(it.columnName) }

        val properties = filteredColumns.joinToString("\n\n") { column ->
            generateProperty(column)
        }

        return """
            package $packageName

            $imports

            /**
             * $description
             *
             * 对应数据库表: $tableName
             */
            @Entity
            @Table(name = "$tableName")
            interface $className$baseInterface {

            $properties
            }
        """.trimIndent()
    }

    /**
     * 生成导入语句
     */
    private fun generateImports(table: JdbcTableMetadata, shouldExtendBaseEntity: Boolean = false): String {
        val imports = mutableSetOf<String>()

        // 基础 Jimmer 导入
        imports.add("import org.babyfish.jimmer.sql.*")

        // 始终导入 BaseEntity
        imports.add("import ${SettingContext.settings.baseEntityPkg}")


        // 根据列类型添加导入
        table.columns.forEach { column ->
            val baseType = mapToKotlinType(column.columnType)
            when {
                baseType.contains("LocalDate") -> imports.add("import java.time.LocalDate")
                baseType.contains("LocalTime") -> imports.add("import java.time.LocalTime")
                baseType.contains("LocalDateTime") -> imports.add("import java.time.LocalDateTime")
                baseType.contains("BigDecimal") -> imports.add("import java.math.BigDecimal")
                baseType.contains("UUID") -> imports.add("import java.util.UUID")
            }
        }

        return imports.sorted().joinToString("\n")
    }

    /**
     * 获取实体类名（驼峰命名）
     */
    private fun getEntityClassName(tableName: String): String {
        return tableName.toBigCamelCase()
    }

    /**
     * 生成属性
     */
    private fun generateProperty(column: JdbcColumnMetadata): String {
        val propertyName = column.columnName.toLowCamelCase()
        val kotlinType = getKotlinType(column)
        val description = column.remarks.ifBlank { propertyName }
        val columnName = column.columnName

        // 构建 KDoc 注释
        val kdocComment = "/**\n     * $description\n     */"

        // 构建注解
        val annotations = mutableListOf<String>()

        // 添加 Column 注解
        val columnAnnotation = """ @Column(name="$columnName") """.trimIndent()

        annotations.add(columnAnnotation)

        // 主键注解
        if (column.isPrimaryKey) {
            annotations.add("@Id")
            if (kotlinType.contains("Long") || kotlinType.contains("Int")) {
                annotations.add("@GeneratedValue(strategy = GenerationType.IDENTITY)")
            }
        }

        return """
            $kdocComment
            ${annotations.joinToString("\n")}
            val $propertyName: $kotlinType
        """.trimIndent()
    }

    /**
     * 获取 Kotlin 类型（分离类型映射和可空性处理）
     */
    private fun getKotlinType(column: JdbcColumnMetadata): String {
        // 先获取基础类型
        val baseType = mapToKotlinType(column.columnType, false)

        // 再处理可空性
        return if (column.nullable) {
            "$baseType?"
        } else {
            baseType
        }
    }

    /**
     * 判断列名是否为 id 字段
     * 因为 id 字段在 BaseEntity 中已定义，所以不需要在实体中重复定义
     */
    private fun isIdField(columnName: String): Boolean {
        val fieldName = columnName.toLowCamelCase()
        return fieldName == "id"
    }
}
