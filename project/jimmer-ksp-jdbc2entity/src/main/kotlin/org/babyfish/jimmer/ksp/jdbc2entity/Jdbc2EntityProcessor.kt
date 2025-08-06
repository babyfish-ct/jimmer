package org.babyfish.jimmer.ksp.jdbc2entity
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import org.babyfish.jimmer.ksp.context.SettingContext
import org.babyfish.jimmer.ksp.jdbc2entity.entity.JdbcTableMetadata
import org.babyfish.jimmer.ksp.util.JdbcMetadataExtractor
import org.babyfish.jimmer.ksp.util.toBigCamelCase

/**
 * JDBC 转 Jimmer 实体处理器
 *
 * 从数据库表结构生成 Jimmer 实体类
 * 支持：
 * - 自动检测表结构
 * - 生成实体类和属性
 * - 处理外键关系
 * - 添加适当的注解
 *
 * 配置参数：
 * - jdbc.driver: JDBC 驱动类名
 * - jdbc.url: 数据库连接 URL
 * - jdbc.username: 数据库用户名
 * - jdbc.password: 数据库密码
 * - entity.package: 生成实体的包名
 * - entity.tables: 要生成实体的表名（逗号分隔，为空则生成所有表）
 */
class Jdbc2EntityProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    var jdbcTables: List<JdbcTableMetadata> = emptyList()
    val entityCodeGenerator: JimmerEntityCodeGenerator = JimmerEntityCodeGenerator(codeGenerator, logger)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // 初始化设置上下文
        SettingContext.initialize(options)
        jdbcTables = JdbcMetadataExtractor.initAndGetJdbcMetaDataTables(options)

        return emptyList()
    }

    override fun finish() {
        // 生成实体类
        generateEntities(jdbcTables)
    }


    /**
     * 生成实体类
     */
    private fun generateEntities(tables: List<JdbcTableMetadata>) {

        tables.forEach { table ->
            try {
                entityCodeGenerator.generateEntityWithIO(table)
                val entityClassName = table.tableName.toBigCamelCase()
                logger.warn("成功生成实体: $entityClassName")
            } catch (e: Exception) {
                logger.error("生成实体失败: ${table.tableName}, 错误: ${e.message}")
                e.printStackTrace()
            }
        }
    }

}


