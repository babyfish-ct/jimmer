package org.babyfish.jimmer.ksp.jdbc2entity.entity

/**
 * 列元数据数据类
 */
data class JdbcColumnMetadata(
    val tableName: String,
    val columnName: String,
    val jdbcType: Int,
    val columnType: String,
    val columnLength: Int,
    val nullable: Boolean,
    val nullableFlag: String,
    val remarks: String,
    val defaultValue: String?,
    var isPrimaryKey: Boolean
)

/**
 * 表元数据数据类
 */
data class JdbcTableMetadata(
    val tableName: String,
    val schema: String,
    val tableType: String,
    val remarks: String,
    val columns: List<JdbcColumnMetadata>
)
