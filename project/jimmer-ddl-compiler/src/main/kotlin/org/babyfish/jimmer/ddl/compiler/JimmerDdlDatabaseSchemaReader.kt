package org.babyfish.jimmer.ddl.compiler

import site.addzero.ddlgenerator.core.model.AutoDdlColumn
import site.addzero.ddlgenerator.core.model.AutoDdlLogicalType
import site.addzero.ddlgenerator.core.model.AutoDdlSchema
import site.addzero.ddlgenerator.core.model.AutoDdlTable
import site.addzero.util.db.DatabaseType
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Types
import java.util.Properties

object JimmerDdlDatabaseSchemaReader {
    fun read(
        settings: JimmerDdlCompilerSettings,
        desiredSchema: AutoDdlSchema,
        renamedTables: Map<String, String> = emptyMap(),
    ): AutoDdlSchema {
        val jdbc = settings.jdbc
        val driverClassName = jdbc.driverClassName ?: DatabaseType.getDriverClassName(jdbc.url)
        if (!driverClassName.isNullOrBlank()) {
            Class.forName(driverClassName)
        }

        val properties = Properties().apply {
            if (jdbc.username.isNotBlank()) {
                setProperty("user", jdbc.username)
            }
            if (jdbc.password.isNotBlank()) {
                setProperty("password", jdbc.password)
            }
            setProperty("connectTimeout", "5")
            setProperty("loginTimeout", "5")
        }
        DriverManager.getConnection(jdbc.url, properties).use { connection ->
            val schemaName = jdbc.schema ?: runCatching { connection.schema }.getOrNull()
            return readSchema(connection, schemaName, desiredSchema, renamedTables)
        }
    }

    private fun readSchema(
        connection: Connection,
        schemaName: String?,
        desiredSchema: AutoDdlSchema,
        renamedTables: Map<String, String>,
    ): AutoDdlSchema {
        val actualTables = desiredSchema.tables.mapNotNull { desiredTable ->
            readTable(connection, schemaName, desiredTable, renamedTables)
        }
        return AutoDdlSchema(actualTables, emptyList())
    }

    private fun readTable(
        connection: Connection,
        schemaName: String?,
        desiredTable: AutoDdlTable,
        renamedTables: Map<String, String>,
    ): AutoDdlTable? {
        val metaData = connection.metaData
        val tableName = findExistingTableName(metaData, connection.catalog, schemaName, desiredTable.name)
            ?: renamedTables[desiredTable.name.lowercase()]?.let { oldTableName ->
                findExistingTableName(metaData, connection.catalog, schemaName, oldTableName)
            }
            ?: return null
        val primaryKeys = readPrimaryKeys(metaData, connection.catalog, schemaName, tableName)
        val desiredColumns = desiredTable.columns.associateBy { column -> column.name.lowercase() }
        val columns = mutableListOf<AutoDdlColumn>()
        metaData.findColumns(connection.catalog, schemaName, tableName).use { resultSet ->
            while (resultSet.next()) {
                val columnName = resultSet.getString("COLUMN_NAME") ?: continue
                val desiredColumn = desiredColumns[columnName.lowercase()] ?: continue
                val jdbcType = resultSet.getInt("DATA_TYPE")
                val length = resultSet.getIntOrNull("COLUMN_SIZE")
                val scale = resultSet.getIntOrNull("DECIMAL_DIGITS")
                val nullable = resultSet.getInt("NULLABLE") == DatabaseMetaData.columnNullable
                columns += desiredColumn.copy(
                    columnName,
                    jdbcType.toLogicalType(),
                    nullable,
                    length,
                    desiredColumn.precision,
                    scale,
                    resultSet.getStringOrNull("COLUMN_DEF"),
                    desiredColumn.comment,
                    primaryKeys.any { key -> key.equals(columnName, ignoreCase = true) },
                    desiredColumn.autoIncrement,
                    desiredColumn.sequenceName,
                    resultSet.getStringOrNull("TYPE_NAME"),
                )
            }
        }
        return AutoDdlTable(
            desiredTable.name,
            columns,
            emptyList(),
            emptyList(),
            null,
            null,
        )
    }

    private fun findExistingTableName(
        metaData: DatabaseMetaData,
        catalog: String?,
        schemaName: String?,
        tableName: String,
    ): String? {
        val candidates = listOf(tableName, tableName.lowercase(), tableName.uppercase()).distinct()
        candidates.forEach { candidate ->
            metaData.getTables(catalog, schemaName, candidate, arrayOf("TABLE")).use { resultSet ->
                if (resultSet.next()) {
                    return resultSet.getString("TABLE_NAME") ?: candidate
                }
            }
        }
        return null
    }

    private fun DatabaseMetaData.findColumns(
        catalog: String?,
        schemaName: String?,
        tableName: String,
    ): ResultSet {
        return getColumns(catalog, schemaName, tableName, "%")
    }

    private fun readPrimaryKeys(
        metaData: DatabaseMetaData,
        catalog: String?,
        schemaName: String?,
        tableName: String,
    ): Set<String> {
        val primaryKeys = linkedSetOf<String>()
        metaData.getPrimaryKeys(catalog, schemaName, tableName).use { resultSet ->
            while (resultSet.next()) {
                resultSet.getString("COLUMN_NAME")?.let { columnName -> primaryKeys += columnName }
            }
        }
        return primaryKeys
    }

    private fun ResultSet.getIntOrNull(columnName: String): Int? {
        val value = getInt(columnName)
        return if (wasNull()) null else value
    }

    private fun ResultSet.getStringOrNull(columnName: String): String? {
        return runCatching { getString(columnName)?.takeIf { it.isNotBlank() } }.getOrNull()
    }

    private fun Int.toLogicalType(): AutoDdlLogicalType {
        return when (this) {
            Types.CHAR, Types.NCHAR -> AutoDdlLogicalType.CHAR
            Types.VARCHAR, Types.NVARCHAR -> AutoDdlLogicalType.STRING
            Types.LONGVARCHAR, Types.LONGNVARCHAR, Types.CLOB, Types.NCLOB -> AutoDdlLogicalType.TEXT
            Types.BOOLEAN, Types.BIT -> AutoDdlLogicalType.BOOLEAN
            Types.TINYINT -> AutoDdlLogicalType.INT8
            Types.SMALLINT -> AutoDdlLogicalType.INT16
            Types.INTEGER -> AutoDdlLogicalType.INT32
            Types.BIGINT -> AutoDdlLogicalType.INT64
            Types.NUMERIC, Types.DECIMAL -> AutoDdlLogicalType.DECIMAL
            Types.REAL -> AutoDdlLogicalType.FLOAT32
            Types.FLOAT, Types.DOUBLE -> AutoDdlLogicalType.FLOAT64
            Types.DATE -> AutoDdlLogicalType.DATE
            Types.TIME, Types.TIME_WITH_TIMEZONE -> AutoDdlLogicalType.TIME
            Types.TIMESTAMP -> AutoDdlLogicalType.DATETIME
            Types.TIMESTAMP_WITH_TIMEZONE -> AutoDdlLogicalType.DATETIME_TZ
            Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> AutoDdlLogicalType.BINARY
            Types.OTHER -> AutoDdlLogicalType.UNKNOWN
            else -> AutoDdlLogicalType.UNKNOWN
        }
    }
}
