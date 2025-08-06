package org.babyfish.jimmer.ksp.util

import org.babyfish.jimmer.ksp.jdbc2entity.entity.JdbcColumnMetadata
import org.babyfish.jimmer.ksp.jdbc2entity.entity.JdbcTableMetadata
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*


/**
 * JDBC元数据抽取工具类（单例静态工具类）
 *
 * 功能：独立的数据库元数据抽取工具，负责连接数据库并提取表结构信息
 * 与KSP处理器解耦，可以独立使用
 * 采用单例模式，所有方法均为静态方法
 */
object JdbcMetadataExtractor {

    /**
     * JDBC连接配置
     */
    data class JdbcConfig(
        val jdbcUrl: String = "jdbc:postgresql://localhost:5432/postgres",
        val jdbcUsername: String = "postgres",
        val jdbcPassword: String = "postgres",
        val jdbcSchema: String = "public",
        val jdbcDriver: String = "org.postgresql.Driver",
        val includeTables: List<String>? = null,
        val excludeTables: List<String>? = null,
        val excludeColumns: List<String> = DEFAULT_EXCLUDE_COLUMNS
    )

    val DEFAULT_EXCLUDE_COLUMNS = listOf(
        "create_time",
        "update_time",
        "create_by",
        "update_by",
        "created_at",
        "updated_at",
        "created_by",
        "updated_by",
        "deleted",
        "deleted_at",
        "is_deleted",
        "tenant_id",
        "delflag"
    )

    /**
     * 从KSP选项创建配置
     */
    fun fromKspOptions(options: Map<String, String>): JdbcConfig {
        return JdbcConfig(
            jdbcUrl = options["jdbcUrl"] ?: "jdbc:postgresql://localhost:5432/postgres",
            jdbcUsername = options["jdbcUsername"] ?: "postgres",
            jdbcPassword = options["jdbcPassword"] ?: "postgres",
            jdbcSchema = options["jdbcSchema"] ?: "public",
            jdbcDriver = options["jdbcDriver"] ?: "org.postgresql.Driver",
            includeTables = options["includeTables"]?.split(",")?.map { it.trim() },
            excludeTables = options["excludeTables"]?.split(",")?.map { it.trim() },
            excludeColumns = options["excludeColumns"]?.split(",")?.map { it.trim() }
                ?: DEFAULT_EXCLUDE_COLUMNS
        )
    }


    fun initAndGetJdbcMetaDataTables(kspOpntions: Map<String, String>): List<JdbcTableMetadata> {
        println("开始JDBC元数据处理...")

        // 从KSP选项创建配置
        val config = fromKspOptions(kspOpntions)

        // 测试数据库连接
        if (!testConnection(config)) {
            println("数据库连接失败，请检查配置")
            return emptyList()
        }

        // 尝试从数据库提取元数据
        val tables = try {
            extractDatabaseMetadata(config)
        } catch (e: Exception) {
            println("⚠️ 无法连接到数据库或提取元数据: ${e.message}")
            println("跳过JDBC元数据生成过程")
            when (e) {
                is ClassNotFoundException -> println("找不到JDBC驱动")
                is SQLException -> println("SQL错误: ${e.message}, 错误代码: ${e.errorCode}, SQL状态: ${e.sqlState}")
                else -> e.printStackTrace()
            }
            // 返回空列表表示没有表可处理
            return emptyList()
        }

        if (tables.isEmpty()) {
            println("没有找到符合条件的表, 跳过生成过程")
            return emptyList()
        }
        return tables
    }


    /**
     * 从数据库中提取元数据
     * @param config JDBC配置
     * @throws java.lang.ClassNotFoundException 如果找不到JDBC驱动
     * @throws java.sql.SQLException 如果数据库连接或查询失败
     */
    fun extractDatabaseMetadata(config: JdbcConfig): List<JdbcTableMetadata> {
        val tables = mutableListOf<JdbcTableMetadata>()
        var connection: Connection? = null

        try {
            // 加载驱动
            Class.forName(config.jdbcDriver)

            // 建立连接
            connection = createConnection(config)
            println("数据库连接成功")

            val metaData = connection.metaData

            // 获取所有表
            val tablesResultSet = metaData.getTables(
                connection.catalog,
                config.jdbcSchema,
                "%",
                arrayOf("TABLE")
            )

            while (tablesResultSet.next()) {
                val tableName = tablesResultSet.getString("TABLE_NAME")

                // 应用表过滤
                if (shouldIncludeTable(tableName, config)) {
                    val tableType = tablesResultSet.getString("TABLE_TYPE")
                    val remarks = tablesResultSet.getString("REMARKS") ?: ""

                    // 获取表的列信息
                    val columns = getColumnsForTable(metaData, tableName, config)

                    // 获取表的主键信息
                    val primaryKeys = getPrimaryKeysForTable(metaData, tableName, config)

                    // 标记主键列
                    columns.forEach { column ->
                        column.isPrimaryKey = primaryKeys.contains(column.columnName)
                    }

                    // 创建表元数据
                    val tableMetadata = JdbcTableMetadata(
                        tableName = tableName,
                        schema = config.jdbcSchema,
                        tableType = tableType,
                        remarks = remarks,
                        columns = columns
                    )

                    tables.add(tableMetadata)
                }
            }

            println("成功从数据库读取了 ${tables.size} 个表的元数据")
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                println("关闭数据库连接时发生错误: ${e.message}")
            }
        }

        return tables
    }

    /**
     * 创建数据库连接
     */
    private fun createConnection(config: JdbcConfig): Connection {
        val props = Properties()
        props.setProperty("user", config.jdbcUsername)
        props.setProperty("password", config.jdbcPassword)

        // 设置连接超时 (5秒)
        props.setProperty("connectTimeout", "5")

        println("正在连接数据库: ${config.jdbcUrl}")
        return DriverManager.getConnection(config.jdbcUrl, props)
    }

    /**
     * 获取表的所有列信息
     */
    private fun getColumnsForTable(
        metaData: DatabaseMetaData,
        tableName: String,
        config: JdbcConfig
    ): MutableList<JdbcColumnMetadata> {
        val columns = mutableListOf<JdbcColumnMetadata>()
        val columnsResultSet = metaData.getColumns(
            null,
            config.jdbcSchema,
            tableName,
            "%"
        )

        while (columnsResultSet.next()) {
            val columnName = columnsResultSet.getString("COLUMN_NAME")

            // 跳过排除的列
            if (shouldExcludeColumn(columnName, config)) {
                println("排除表 $tableName 中的列 $columnName")
                continue
            }

            val dataType = columnsResultSet.getInt("DATA_TYPE")
            val typeName = columnsResultSet.getString("TYPE_NAME")
            val columnSize = columnsResultSet.getInt("COLUMN_SIZE")
            val nullableFlag = columnsResultSet.getString("IS_NULLABLE")
            val nullable = nullableFlag.equals("YES", ignoreCase = true)
            val remarks = columnsResultSet.getString("REMARKS") ?: ""
            val defaultValue = columnsResultSet.getString("COLUMN_DEF")

            val columnMetadata = JdbcColumnMetadata(
                tableName = tableName,
                columnName = columnName,
                jdbcType = dataType,
                columnType = typeName,
                columnLength = columnSize,
                nullable = nullable,
                nullableFlag = nullableFlag,
                remarks = remarks,
                defaultValue = defaultValue,
                isPrimaryKey = false  // 稍后会更新此字段
            )

            columns.add(columnMetadata)
        }

        return columns
    }

    /**
     * 判断是否应该排除某列
     */
    private fun shouldExcludeColumn(columnName: String, config: JdbcConfig): Boolean {
        // 检查列名是否在排除列表中 (不区分大小写)
        return config.excludeColumns.any {
            it.equals(columnName, ignoreCase = true)
        }
    }

    /**
     * 获取表的主键列
     */
    private fun getPrimaryKeysForTable(metaData: DatabaseMetaData, tableName: String, config: JdbcConfig): Set<String> {
        val primaryKeys = mutableSetOf<String>()
        val pkResultSet = metaData.getPrimaryKeys(null, config.jdbcSchema, tableName)

        while (pkResultSet.next()) {
            val columnName = pkResultSet.getString("COLUMN_NAME")
            primaryKeys.add(columnName)
        }

        return primaryKeys
    }


    /**
     * 判断是否应该包含某个表（支持通配符 *）
     * @param tableName 当前表名
     * @param config 数据源配置
     * @return 如果表名符合包含/排除规则则返回 true
     */
    private fun shouldIncludeTable(tableName: String, config: JdbcConfig):
            Boolean {
        // 1. 如果指定了包含列表，则检查是否匹配任一包含规则
        config.includeTables?.let { includeRules ->
            if (includeRules.isNotEmpty()) {
                return includeRules.any { rule -> matchesWildcard(tableName, rule) }
            }
        }

        // 2. 如果指定了排除列表，则检查是否匹配任一排除规则
        config.excludeTables?.let { excludeRules ->
            if (excludeRules.isNotEmpty()) {
                return !excludeRules.any { rule -> matchesWildcard(tableName, rule) }
            }
        }

        // 3. 默认包含所有表
        return true
    }


    /**
     * 通配符匹配（支持 *）
     * @param input 待匹配的字符串（如表名）
     * @param pattern 通配符规则（如 "user_*", "*_mapping"）
     */
    private fun matchesWildcard(input: String, pattern: String): Boolean {
        val regex = pattern
            .replace(".", "\\.")  // 转义点号
            .replace("*", ".*")   // 将 * 转换为正则的 .*
            .let { "^$it$" }      // 完全匹配

        return Regex(regex, RegexOption.IGNORE_CASE).matches(input)
    }


    /**
     * 测试数据库连接
     * @param config JDBC配置
     */
    private fun testConnection(config: JdbcConfig): Boolean {
        return try {
            Class.forName(config.jdbcDriver)
            val connection = createConnection(config)
            connection.close()
            println("数据库连接测试成功")
            true
        } catch (e: Exception) {
            println("数据库连接测试失败: ${e.message}")
            when (e) {
                is ClassNotFoundException -> println("找不到JDBC驱动: ${config.jdbcDriver}")
                is SQLException -> println("SQL错误: ${e.message}, 错误代码: ${e.errorCode}, SQL状态: ${e.sqlState}")
                else -> e.printStackTrace()
            }
            false
        }
    }
}
