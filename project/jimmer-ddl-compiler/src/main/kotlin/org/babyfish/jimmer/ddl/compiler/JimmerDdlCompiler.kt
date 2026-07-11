package org.babyfish.jimmer.ddl.compiler

import site.addzero.ddlgenerator.core.dialect.AutoDdlDialects
import site.addzero.ddlgenerator.core.diff.AddComment
import site.addzero.ddlgenerator.core.diff.AddForeignKey
import site.addzero.ddlgenerator.core.diff.AlterColumn
import site.addzero.ddlgenerator.core.diff.AutoDdlOperation
import site.addzero.ddlgenerator.core.diff.CreateIndex
import site.addzero.ddlgenerator.core.diff.CreateSequence
import site.addzero.ddlgenerator.core.diff.CreateTable
import site.addzero.ddlgenerator.core.diff.SchemaDiffPlanner
import site.addzero.ddlgenerator.core.model.AutoDdlColumn
import site.addzero.ddlgenerator.core.model.AutoDdlComment
import site.addzero.ddlgenerator.core.model.AutoDdlCommentTargetType
import site.addzero.ddlgenerator.core.model.AutoDdlLogicalType
import site.addzero.ddlgenerator.core.model.AutoDdlSchema
import site.addzero.ddlgenerator.core.model.AutoDdlTable
import site.addzero.ddlgenerator.core.options.AutoDdlDiffOptions
import site.addzero.ddlgenerator.lsi.LsiAutoDdlSchemaAdapter
import site.addzero.lsi.clazz.LsiClass
import site.addzero.lsi.field.LsiField
import site.addzero.util.db.DatabaseType

object JimmerDdlCompiler {
    fun compile(
        classes: Collection<LsiClass>,
        settings: JimmerDdlCompilerSettings,
        relationTargetClasses: Collection<LsiClass> = classes,
    ): JimmerDdlCompilerResult {
        if (!settings.enabled) {
            return JimmerDdlCompilerResult.empty(settings)
        }

        val entities = classes.toJimmerDdlLsiClasses()
            .filter { it.isJimmerEntity() }
            .filter { settings.includesClass(it.qualifiedName) }
            .distinctBy { it.qualifiedName ?: it.simpleName.orEmpty() }
            .sortedBy { it.qualifiedName ?: it.simpleName.orEmpty() }
        if (entities.isEmpty()) {
            return JimmerDdlCompilerResult.empty(settings)
        }
        val relationTargetEntities = relationTargetClasses.toJimmerDdlLsiClasses()
            .filter { it.isJimmerEntity() }
            .distinctBy { it.qualifiedName ?: it.simpleName.orEmpty() }
            .sortedBy { it.qualifiedName ?: it.simpleName.orEmpty() }

        val schema = buildSchema(
            entities = entities,
            relationTargetEntities = relationTargetEntities,
            settings = settings,
        )
        val changePlan = JimmerDdlEntityTableSnapshot.planSchemaChanges(
            entities = entities,
            schema = schema,
            settings = settings,
        )
        if (!settings.compareDatabase && !changePlan.hasChanges) {
            return JimmerDdlCompilerResult.empty(
                settings = settings,
                entities = entities,
                schema = schema,
            )
        }

        val plan = generateDdl(
            schema = schema,
            changePlan = changePlan,
            settings = settings,
        )
        val sql = plan.statements.joinToString(separator = "\n")
            .trim()
            .let { content ->
                if (content.isBlank()) {
                    content
                } else {
                    content + "\n"
                }
            }
        return JimmerDdlCompilerResult(
            settings = settings,
            entities = entities,
            schema = schema,
            snapshotSchema = plan.snapshotSchema,
            statements = plan.statements,
            sql = sql,
            warnings = plan.warnings,
        )
    }

    private fun generateDdl(
        schema: AutoDdlSchema,
        changePlan: JimmerDdlSchemaChangePlan,
        settings: JimmerDdlCompilerSettings,
    ): JimmerDdlCompilePlan {
        val renameOperations = changePlan.renameOperations
        val operationPlan = if (settings.compareDatabase && settings.jdbc.url.isNotBlank()) {
            generateComparedOperations(
                schema = schema,
                settings = settings,
                renamedTables = renameOperations.associate { operation ->
                    operation.newTableName.lowercase() to operation.oldTableName
                },
            )
        } else {
            buildOfflineIncrementalOperationPlan(
                schema = schema,
                changePlan = changePlan,
                settings = settings,
            )
        }
        val renamedTableNames = renameOperations.map { operation -> operation.newTableName.lowercase() }.toSet()
        val operations = operationPlan.operations.filterNot { operation ->
            operation is CreateTable && operation.table.name.lowercase() in renamedTableNames
        }
        val statements = buildRenameTableStatements(renameOperations, settings) +
            AutoDdlDialects.require(settings.databaseType).render(operations) +
            buildPostgreSqlNullabilityRepairStatements(schema, settings)
        return JimmerDdlCompilePlan(
            statements = statements,
            snapshotSchema = operationPlan.snapshotSchema ?: schema,
            warnings = operationPlan.warnings,
        )
    }

    private fun buildOfflineIncrementalOperationPlan(
        schema: AutoDdlSchema,
        changePlan: JimmerDdlSchemaChangePlan,
        settings: JimmerDdlCompilerSettings,
    ): JimmerDdlOperationPlan {
        val diffSchema = schema.filterTables(changePlan.changedTableNames)
        val operations = buildOfflineIncrementalOperations(
            schema = diffSchema,
            changePlan = changePlan,
            settings = settings,
        )
        return JimmerDdlOperationPlan(
            operations = operations,
            snapshotSchema = schema,
        )
    }

    private fun generateComparedOperations(
        schema: AutoDdlSchema,
        settings: JimmerDdlCompilerSettings,
        renamedTables: Map<String, String>,
    ): JimmerDdlOperationPlan {
        return runCatching {
            val actualSchema = JimmerDdlDatabaseSchemaReader.read(
                settings = settings,
                desiredSchema = schema,
                renamedTables = renamedTables,
            )
            val operations = SchemaDiffPlanner.plan(
                schema,
                actualSchema,
                AutoDdlDiffOptions(settings.options, true, emptyList(), emptyList())
            )
            JimmerDdlOperationPlan(operations = operations)
        }.getOrElse { cause ->
            JimmerDdlOperationPlan(
                operations = buildOfflineOperations(schema, settings),
                warnings = listOf("Jimmer DDL database comparison failed; fallback to offline DDL generation: ${cause.message ?: cause::class.qualifiedName}"),
            )
        }
    }

    private fun buildSchema(
        entities: List<LsiClass>,
        relationTargetEntities: List<LsiClass>,
        settings: JimmerDdlCompilerSettings,
    ): AutoDdlSchema {
        val baseSchema = LsiAutoDdlSchemaAdapter.from(entities)
        val junctionTables = if (settings.includeManyToManyTables) {
            scanManyToManyTables(entities, relationTargetEntities)
        } else {
            emptyList()
        }
        return AutoDdlSchema(
            (baseSchema.tables + junctionTables).distinctBy { table -> table.name.lowercase() },
            baseSchema.sequences,
        )
    }

    private fun scanManyToManyTables(
        entities: List<LsiClass>,
        relationTargetEntities: List<LsiClass>,
    ): List<AutoDdlTable> {
        val relationTargetByQualifiedName = relationTargetEntities
            .mapNotNull { entity -> entity.qualifiedName?.takeIf { it.isNotBlank() }?.let { it to entity } }
            .toMap()
        val relationTargetBySimpleName = relationTargetEntities
            .mapNotNull { entity -> entity.simpleName?.takeIf { it.isNotBlank() }?.let { it to entity } }
            .toMap()
        return entities.flatMap { entity ->
            val ownerIdField = entity.findIdField() ?: return@flatMap emptyList()
            entity.allDdlFields()
                .filter { field -> field.hasAnnotation("org.babyfish.jimmer.sql.ManyToMany", "ManyToMany") }
                .mapNotNull { field ->
                    val targetType = field.type?.typeParameters?.firstOrNull() ?: field.type
                    val targetClass = targetType?.lsiClass ?: field.fieldTypeClass
                    ?: targetType?.qualifiedName?.let(relationTargetByQualifiedName::get)
                    ?: targetType?.simpleName?.let(relationTargetBySimpleName::get)
                    ?: return@mapNotNull null
                    val targetIdField = targetClass.findIdField() ?: return@mapNotNull null
                    val fieldName = field.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val ownerName = entity.simpleName.orEmpty().toSnakeCase()
                    val targetName = targetClass.simpleName.orEmpty().toSnakeCase()
                    val leftColumnName = "${ownerName}_id"
                    val rightColumnName = "${targetName}_id"
                    AutoDdlTable(
                        "${ownerName}_${fieldName.toSnakeCase()}_mapping",
                        listOf(
                            AutoDdlColumn(leftColumnName, ownerIdField.toLogicalType(), false, null, null, null, null, null, false, false, null, null),
                            AutoDdlColumn(rightColumnName, targetIdField.toLogicalType(), false, null, null, null, null, null, false, false, null, null),
                        ),
                        emptyList(),
                        emptyList(),
                        null,
                        null,
                    )
                }
        }
    }

    private fun LsiClass.allDdlFields(): List<LsiField> {
        val inherited = (superClasses + interfaces).flatMap { parent -> parent.allDdlFields() }
        return inherited + fields
    }

    private fun LsiClass.findIdField(): LsiField? {
        return allDdlFields().firstOrNull { field -> field.hasAnnotation("org.babyfish.jimmer.sql.Id", "Id") }
    }

    private fun LsiField.hasAnnotation(
        qualifiedName: String,
        simpleName: String,
    ): Boolean {
        return annotations.any { annotation -> annotation.qualifiedName == qualifiedName || annotation.simpleName == simpleName }
    }

    private fun LsiField.toLogicalType(): AutoDdlLogicalType {
        return when (typeName ?: type?.simpleName) {
            "Byte" -> AutoDdlLogicalType.INT8
            "Short" -> AutoDdlLogicalType.INT16
            "Int", "Integer" -> AutoDdlLogicalType.INT32
            "Long" -> AutoDdlLogicalType.INT64
            "Float" -> AutoDdlLogicalType.FLOAT32
            "Double" -> AutoDdlLogicalType.FLOAT64
            "Boolean" -> AutoDdlLogicalType.BOOLEAN
            else -> AutoDdlLogicalType.STRING
        }
    }

    private fun LsiClass.guessJimmerTableName(): String {
        val tableName = annotations.firstNotNullOfOrNull { annotation ->
            if (annotation.qualifiedName != "org.babyfish.jimmer.sql.Table" && annotation.simpleName != "Table") {
                return@firstNotNullOfOrNull null
            }
            annotation.getAttribute("name")?.toString()?.takeIf { it.isNotBlank() }
        }
        return tableName ?: simpleName.orEmpty().toSnakeCase()
    }

    private fun buildOfflineOperations(
        schema: AutoDdlSchema,
        settings: JimmerDdlCompilerSettings,
    ): List<AutoDdlOperation> {
        return buildList {
            if (settings.options.includeSequences) {
                schema.sequences.forEach { sequence ->
                    add(CreateSequence(sequence))
                }
            }

            schema.tables.forEach { table ->
                add(CreateTable(table))
            }

            if (settings.options.includeIndexes) {
                schema.tables.forEach { table ->
                    table.indexes.forEach { index ->
                        add(CreateIndex(table.name, index))
                    }
                }
            }

            if (settings.options.includeForeignKeys) {
                schema.tables.forEach { table ->
                    table.foreignKeys.forEach { foreignKey ->
                        add(AddForeignKey(table.name, foreignKey))
                    }
                }
            }

            if (settings.options.includeComments) {
                schema.tables.forEach { table ->
                    val tableComment = table.comment
                    if (!tableComment.isNullOrBlank()) {
                        add(
                            AddComment(
                                AutoDdlComment(
                                    AutoDdlCommentTargetType.TABLE,
                                    tableComment,
                                    table.name,
                                    null,
                                    null,
                                )
                            )
                        )
                    }
                    table.columns
                        .filter { column -> !column.comment.isNullOrBlank() }
                        .forEach { column ->
                            add(
                                AddComment(
                                    AutoDdlComment(
                                        AutoDdlCommentTargetType.COLUMN,
                                        column.comment.orEmpty(),
                                        table.name,
                                        column.name,
                                        null,
                                    )
                                )
                            )
                        }
                }
            }
        }
    }

    private fun buildOfflineIncrementalOperations(
        schema: AutoDdlSchema,
        changePlan: JimmerDdlSchemaChangePlan,
        settings: JimmerDdlCompilerSettings,
    ): List<AutoDdlOperation> {
        val previousSchema = changePlan.previous.toSchemaFor(
            schema = schema,
            renameOperations = changePlan.renameOperations,
        )
        if (previousSchema.tables.isEmpty() && previousSchema.sequences.isEmpty()) {
            return buildOfflineOperations(schema, settings)
        }
        return SchemaDiffPlanner.plan(
            schema,
            previousSchema,
            AutoDdlDiffOptions(settings.options, true, emptyList(), emptyList())
        )
    }

    private fun buildRenameTableStatements(
        renameOperations: List<RenameTable>,
        settings: JimmerDdlCompilerSettings,
    ): List<String> {
        return renameOperations.map { operation ->
            when (settings.databaseType) {
                DatabaseType.SQLSERVER -> "EXEC sp_rename '${operation.oldTableName}', '${operation.newTableName}';"
                else -> "ALTER TABLE ${quoteIdentifier(operation.oldTableName, settings.databaseType)} RENAME TO ${quoteIdentifier(operation.newTableName, settings.databaseType)};"
            }
        }
    }

    private fun buildPostgreSqlNullabilityRepairStatements(
        schema: AutoDdlSchema,
        settings: JimmerDdlCompilerSettings,
    ): List<String> {
        if (settings.databaseType != DatabaseType.POSTGRESQL) {
            return emptyList()
        }
        return schema.tables.flatMap { table ->
            table.columns
                .filter { column -> column.nullable }
                .map { column ->
                    "ALTER TABLE ${quoteIdentifier(table.name, settings.databaseType)} ALTER COLUMN ${quoteIdentifier(column.name, settings.databaseType)} DROP NOT NULL;"
                }
        }
    }

    private fun quoteIdentifier(
        name: String,
        databaseType: DatabaseType,
    ): String {
        val escaped = name.replace("\"", "\"\"")
        return when (databaseType) {
            DatabaseType.MYSQL, DatabaseType.TIDB, DatabaseType.OCEANBASE, DatabaseType.POLARDB -> "`$name`"
            DatabaseType.SQLSERVER -> "[$name]"
            else -> "\"$escaped\""
        }
    }

    private fun AutoDdlSchema.filterTables(tableNames: Set<String>): AutoDdlSchema {
        if (tableNames.isEmpty()) {
            return AutoDdlSchema(emptyList(), sequences)
        }
        val normalizedTableNames = tableNames.map { tableName -> tableName.lowercase() }.toSet()
        return AutoDdlSchema(
            tables.filter { table -> table.name.lowercase() in normalizedTableNames },
            sequences,
        )
    }

    private fun JimmerDdlSnapshot.toSchemaFor(
        schema: AutoDdlSchema,
        renameOperations: List<RenameTable>,
    ): AutoDdlSchema {
        val oldTableNameByNewName = renameOperations.associate { operation ->
            operation.newTableName.lowercase() to operation.oldTableName.lowercase()
        }
        val tables = schema.tables.mapNotNull { table ->
            val lookupName = oldTableNameByNewName[table.name.lowercase()] ?: table.name.lowercase()
            tableSchemas[lookupName]?.let { previous ->
                previous.copy(table.name, previous.columns, previous.foreignKeys, previous.indexes, previous.comment, previous.junction)
            }
        }
        return AutoDdlSchema(tables, schema.sequences)
    }
}

data class RenameTable(
    val oldTableName: String,
    val newTableName: String,
)

private fun LsiClass.isJimmerEntity(): Boolean {
    return annotations.any { annotation -> annotation.qualifiedName == "org.babyfish.jimmer.sql.Entity" || annotation.simpleName == "Entity" }
}

private data class JimmerDdlOperationPlan(
    val operations: List<AutoDdlOperation>,
    val snapshotSchema: AutoDdlSchema? = null,
    val warnings: List<String> = emptyList(),
)

private data class JimmerDdlCompilePlan(
    val statements: List<String>,
    val snapshotSchema: AutoDdlSchema,
    val warnings: List<String> = emptyList(),
)

private data class JimmerDdlColumnKey(
    val tableName: String,
    val columnName: String,
)

data class JimmerDdlCompilerResult(
    val settings: JimmerDdlCompilerSettings,
    val entities: List<LsiClass>,
    val schema: AutoDdlSchema,
    val snapshotSchema: AutoDdlSchema,
    val statements: List<String>,
    val sql: String,
    val warnings: List<String> = emptyList(),
) {
    val isEmpty
        get() = entities.isEmpty() || sql.isBlank()

    companion object {
        fun empty(
            settings: JimmerDdlCompilerSettings,
            entities: List<LsiClass> = emptyList(),
            schema: AutoDdlSchema = AutoDdlSchema(emptyList(), emptyList()),
        ): JimmerDdlCompilerResult {
            return JimmerDdlCompilerResult(
                settings = settings,
                entities = entities,
                schema = schema,
                snapshotSchema = schema,
                statements = emptyList(),
                sql = "",
                warnings = emptyList(),
            )
        }
    }
}

private fun String.toSnakeCase(): String {
    if (isBlank()) {
        return this
    }
    val builder = StringBuilder()
    for (index in indices) {
        val char = this[index]
        if (char in 'A'..'Z') {
            if (index > 0 && builder.lastOrNull() != '_') {
                builder.append('_')
            }
            builder.append(char.lowercaseChar())
        } else {
            builder.append(char)
        }
    }
    return builder.toString()
}
