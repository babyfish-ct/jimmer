package org.babyfish.jimmer.ddl.compiler

import site.addzero.ddlgenerator.core.model.AutoDdlColumn
import site.addzero.ddlgenerator.core.model.AutoDdlForeignKey
import site.addzero.ddlgenerator.core.model.AutoDdlIndex
import site.addzero.ddlgenerator.core.model.AutoDdlIndexType
import site.addzero.ddlgenerator.core.model.AutoDdlJunction
import site.addzero.ddlgenerator.core.model.AutoDdlLogicalType
import site.addzero.ddlgenerator.core.model.AutoDdlSchema
import site.addzero.ddlgenerator.core.model.AutoDdlTable
import site.addzero.lsi.clazz.LsiClass
import java.io.File
import java.security.MessageDigest
import java.util.Base64
import java.util.Properties

private const val SOURCE_FINGERPRINT_KEY = "__sourceFingerprint"
private const val TABLE_NAME_KEY = "tableName"
private const val TABLE_HASH_KEY = "tableHash"
private const val TABLE_SCHEMA_KEY = "tableSchema"
private const val ENTITY_TABLE_PREFIX = "entity."
private val SAFE_SNAPSHOT_FILE_NAME = Regex("[a-z0-9][a-z0-9._-]*")

data class JimmerDdlSnapshot(
    val entityTables: Map<String, String> = emptyMap(),
    val tableHashes: Map<String, String> = emptyMap(),
    val tableSchemas: Map<String, AutoDdlTable> = emptyMap(),
) {
    val isEmpty
        get() = entityTables.isEmpty() && tableHashes.isEmpty()
}

data class JimmerDdlSchemaChangePlan(
    val previous: JimmerDdlSnapshot,
    val currentEntityTables: Map<String, String>,
    val currentTableHashes: Map<String, String>,
    val changedTableNames: Set<String>,
    val renameOperations: List<RenameTable>,
) {
    val hasChanges
        get() = changedTableNames.isNotEmpty() || renameOperations.isNotEmpty()
}

object JimmerDdlEntityTableSnapshot {
    fun planSchemaChanges(
        entities: List<LsiClass>,
        schema: AutoDdlSchema,
        settings: JimmerDdlCompilerSettings,
    ): JimmerDdlSchemaChangePlan {
        val previous = readSnapshot(settings)
        val currentEntityTables = entities.toSnapshot()
        val currentTableHashes = schema.toTableHashes()
        val renameOperations = planRenameTables(
            previous = previous,
            current = currentEntityTables,
            schema = schema,
        )
        val renamedNewTables = renameOperations.map { operation -> operation.newTableName.lowercase() }.toSet()
        val changedTableNames = currentTableHashes
            .filter { (tableName, currentHash) ->
                previous.isEmpty ||
                    previous.tableHashes.isEmpty() ||
                    previous.tableHashes[tableName] != currentHash
            }
            .keys + renamedNewTables
        return JimmerDdlSchemaChangePlan(
            previous = previous,
            currentEntityTables = currentEntityTables,
            currentTableHashes = currentTableHashes,
            changedTableNames = changedTableNames,
            renameOperations = renameOperations,
        )
    }

    fun planRenameTables(
        entities: List<LsiClass>,
        schema: AutoDdlSchema,
        settings: JimmerDdlCompilerSettings,
    ): List<RenameTable> {
        val previous = readSnapshot(settings)
        val current = entities.toSnapshot()
        return planRenameTables(
            previous = previous,
            current = current,
            schema = schema,
        )
    }

    fun readSnapshot(settings: JimmerDdlCompilerSettings): JimmerDdlSnapshot {
        val snapshotDirectory = JimmerDdlCompilerFiles.resolveSnapshotDirectory(settings) ?: return JimmerDdlSnapshot()
        return snapshotDirectory.readSnapshot()
    }

    private fun planRenameTables(
        previous: JimmerDdlSnapshot,
        current: Map<String, String>,
        schema: AutoDdlSchema,
    ): List<RenameTable> {
        if (previous.entityTables.isEmpty()) {
            return emptyList()
        }

        val existingDesiredTables = schema.tables.map { table -> table.name.lowercase() }.toSet()
        return current.mapNotNull { entry ->
            val oldTableName = previous.entityTables[entry.key]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val newTableName = entry.value.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            if (oldTableName.equals(newTableName, ignoreCase = true)) {
                return@mapNotNull null
            }
            if (newTableName.lowercase() !in existingDesiredTables) {
                return@mapNotNull null
            }
            RenameTable(
                oldTableName = oldTableName,
                newTableName = newTableName,
            )
        }.distinctBy { operation ->
            "${operation.oldTableName.lowercase()}->${operation.newTableName.lowercase()}"
        }
    }

    fun writeSnapshot(
        entities: List<LsiClass>,
        schema: AutoDdlSchema,
        settings: JimmerDdlCompilerSettings,
    ) {
        val snapshotDirectory = JimmerDdlCompilerFiles.resolveSnapshotDirectory(settings) ?: return
        writeSnapshot(
            snapshotDirectory = snapshotDirectory,
            entities = entities,
            schema = schema,
        )
    }

    fun writeGeneratedSnapshot(
        entities: List<LsiClass>,
        schema: AutoDdlSchema,
        settings: JimmerDdlCompilerSettings,
    ) {
        val snapshotDirectory = JimmerDdlCompilerFiles.resolveGeneratedSnapshotDirectory(settings)
        writeSnapshot(
            snapshotDirectory = snapshotDirectory,
            entities = entities,
            schema = schema,
        )
        writeGeneratedSourceFingerprint(settings)
    }

    private fun writeSnapshot(
        snapshotDirectory: File,
        entities: List<LsiClass>,
        schema: AutoDdlSchema,
    ) {
        val entityNamesByTable = entities.toSnapshot()
            .entries
            .groupBy(
                keySelector = { entry -> entry.value.lowercase() },
                valueTransform = { entry -> entry.key },
            )
        snapshotDirectory.mkdirs()
        val snapshotFiles = schema.tables.associateWith { table ->
            snapshotDirectory.resolve(table.name.toSnapshotFileName())
        }
        val expectedFileNames = snapshotFiles.values.mapTo(mutableSetOf()) { file -> file.name }
        snapshotDirectory.listFiles { file -> file.isFile && file.extension == "properties" }
            .orEmpty()
            .filterNot { file -> file.name in expectedFileNames }
            .forEach { file ->
                check(file.delete()) { "Cannot delete stale Jimmer DDL snapshot lockfile: ${file.absolutePath}" }
            }
        snapshotFiles.entries
            .sortedBy { (table) -> table.name.lowercase() }
            .forEach { (table, snapshotFile) ->
                val tableName = table.name.lowercase()
                val canonicalSchema = table.toCanonicalText()
                val content = buildString {
                    appendLine("# Jimmer DDL structural snapshot. Do not edit manually.")
                    appendLine("$TABLE_NAME_KEY=${table.name.escapeProperty()}")
                    appendLine("$TABLE_HASH_KEY=${canonicalSchema.sha256()}")
                    appendLine("$TABLE_SCHEMA_KEY=${canonicalSchema.encodeBase64().escapeProperty()}")
                    entityNamesByTable[tableName]
                        .orEmpty()
                        .sorted()
                        .forEach { qualifiedName ->
                            appendLine("$ENTITY_TABLE_PREFIX${qualifiedName.escapeProperty()}=${table.name.escapeProperty()}")
                        }
                }
                snapshotFile.writeTextIfChanged(content)
            }
    }

    private fun writeGeneratedSourceFingerprint(settings: JimmerDdlCompilerSettings) {
        val fingerprintFile = JimmerDdlCompilerFiles.resolveBuildSourceFingerprintFile(settings) ?: return
        val sourceFingerprint = settings.sourceFingerprint?.takeIf { it.isNotBlank() }
        if (sourceFingerprint == null) {
            if (fingerprintFile.exists()) {
                check(fingerprintFile.delete()) { "Cannot delete stale Jimmer DDL source fingerprint: ${fingerprintFile.absolutePath}" }
            }
            return
        }
        val content = buildString {
            appendLine("# Build-local Jimmer DDL source fingerprint. Do not commit this file.")
            appendLine("$SOURCE_FINGERPRINT_KEY=${sourceFingerprint.escapeProperty()}")
        }
        fingerprintFile.writeTextIfChanged(content)
    }

    private fun List<LsiClass>.toSnapshot(): Map<String, String> {
        return mapNotNull { entity ->
            val qualifiedName = entity.qualifiedName?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val tableName = entity.guessJimmerTableName().takeIf { it.isNotBlank() } ?: return@mapNotNull null
            qualifiedName to tableName
        }.toMap()
    }

    private fun File.readSnapshot(): JimmerDdlSnapshot {
        if (!isDirectory) {
            return JimmerDdlSnapshot()
        }
        val entityTables = linkedMapOf<String, String>()
        val tableHashes = linkedMapOf<String, String>()
        val tableSchemas = linkedMapOf<String, AutoDdlTable>()
        listFiles { file -> file.isFile && file.extension == "properties" }
            .orEmpty()
            .sortedBy { file -> file.name }
            .forEach { snapshotFile ->
                val props = snapshotFile.reader(Charsets.UTF_8).use { input ->
                    Properties().apply { load(input) }
                }
                val tableName = props.getProperty(TABLE_NAME_KEY)
                    ?.takeIf { it.isNotBlank() }
                    ?.lowercase()
                    ?: return@forEach
                props.getProperty(TABLE_HASH_KEY)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { hash -> tableHashes[tableName] = hash }
                props.getProperty(TABLE_SCHEMA_KEY)
                    ?.decodeTableSchema(tableName)
                    ?.let { table -> tableSchemas[tableName] = table }
                props.entries.forEach { (key, value) ->
                    val name = key.toString()
                    if (name.startsWith(ENTITY_TABLE_PREFIX)) {
                        val qualifiedName = name.removePrefix(ENTITY_TABLE_PREFIX)
                        value.toString().takeIf { it.isNotBlank() }?.let { mappedTableName ->
                            entityTables[qualifiedName] = mappedTableName
                        }
                    }
                }
            }
        return JimmerDdlSnapshot(
            entityTables = entityTables,
            tableHashes = tableHashes,
            tableSchemas = tableSchemas,
        )
    }

    private fun String.toSnapshotFileName(): String {
        val normalized = lowercase()
        val baseName = if (normalized.length <= 180 && SAFE_SNAPSHOT_FILE_NAME.matches(normalized)) {
            normalized
        } else {
            normalized.sha256()
        }
        return "$baseName.properties"
    }

    private fun File.writeTextIfChanged(content: String) {
        if (isFile && readText() == content) {
            return
        }
        parentFile.mkdirs()
        writeText(content)
    }

    private fun AutoDdlSchema.toTableHashes(): Map<String, String> {
        return tables.associate { table ->
            table.name.lowercase() to table.toCanonicalText().sha256()
        }
    }

    private fun AutoDdlTable.toCanonicalText(): String {
        return buildString {
            appendEncodedLine("table", name, comment.orEmpty())
            junction?.let { junction ->
                appendEncodedLine(
                    "junction",
                    junction.leftTableName,
                    junction.leftColumnName,
                    junction.rightTableName,
                    junction.rightColumnName,
                )
            }
            columns.sortedBy { column -> column.name.lowercase() }.forEach { column ->
                appendLine("column=${column.toCanonicalText()}")
            }
            indexes.sortedWith(compareBy<AutoDdlIndex> { index -> index.name.lowercase() }
                .thenBy { index -> index.columnNames.joinToString(",") { it.lowercase() } })
                .forEach { index ->
                    appendLine("index=${index.toCanonicalText()}")
                }
            foreignKeys.sortedWith(compareBy<AutoDdlForeignKey> { foreignKey -> foreignKey.name.lowercase() }
                .thenBy { foreignKey -> foreignKey.columnNames.joinToString(",") { it.lowercase() } })
                .forEach { foreignKey ->
                    appendLine("foreignKey=${foreignKey.toCanonicalText()}")
                }
        }
    }

    private fun AutoDdlColumn.toCanonicalText(): String {
        return listOf(
            name,
            logicalType.name,
            nullable.toString(),
            length.orEmptyPart(),
            precision.orEmptyPart(),
            scale.orEmptyPart(),
            defaultValue.orEmpty(),
            comment.orEmpty(),
            primaryKey.toString(),
            autoIncrement.toString(),
            sequenceName.orEmpty(),
            nativeTypeHint.orEmpty(),
        ).encodeParts()
    }

    private fun AutoDdlIndex.toCanonicalText(): String {
        return listOf(
            name,
            type.name,
            columnNames.joinToString(","),
        ).encodeParts()
    }

    private fun AutoDdlForeignKey.toCanonicalText(): String {
        return listOf(
            name,
            columnNames.joinToString(","),
            referencedTableName,
            referencedColumnNames.joinToString(","),
            onDelete.orEmpty(),
            onUpdate.orEmpty(),
        ).encodeParts()
    }

    private fun StringBuilder.appendEncodedLine(
        key: String,
        vararg values: String,
    ) {
        appendLine("$key=${values.toList().encodeParts()}")
    }

    private fun String.decodeTableSchema(defaultTableName: String): AutoDdlTable? {
        val text = runCatching { decodeBase64() }.getOrNull() ?: return null
        var tableName = defaultTableName
        var tableComment: String? = null
        var junction: AutoDdlJunction? = null
        val columns = mutableListOf<AutoDdlColumn>()
        val indexes = mutableListOf<AutoDdlIndex>()
        val foreignKeys = mutableListOf<AutoDdlForeignKey>()
        text.lineSequence()
            .filter { line -> line.isNotBlank() && "=" in line }
            .forEach { line ->
                val key = line.substringBefore("=")
                val parts = line.substringAfter("=").decodeParts()
                when (key) {
                    "table" -> {
                        tableName = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: tableName
                        tableComment = parts.getOrNull(1)?.takeIf { it.isNotBlank() }
                    }
                    "junction" -> {
                        if (parts.size >= 4) {
                            junction = AutoDdlJunction(
                                parts[0],
                                parts[2],
                                parts[1],
                                parts[3],
                            )
                        }
                    }
                    "column" -> parts.toColumn()?.let(columns::add)
                    "index" -> parts.toIndex()?.let(indexes::add)
                    "foreignKey" -> parts.toForeignKey()?.let(foreignKeys::add)
                }
            }
        if (columns.isEmpty()) {
            return null
        }
        return AutoDdlTable(
            tableName,
            columns,
            foreignKeys,
            indexes,
            tableComment,
            junction,
        )
    }

    private fun List<String>.toColumn(): AutoDdlColumn? {
        if (size < 12) {
            return null
        }
        val logicalType = runCatching { AutoDdlLogicalType.valueOf(this[1]) }.getOrNull() ?: return null
        return AutoDdlColumn(
            this[0],
            logicalType,
            this[2].toBooleanStrictOrNull() ?: true,
            this[3].toIntOrNull(),
            this[4].toIntOrNull(),
            this[5].toIntOrNull(),
            this[6].takeIf { it.isNotBlank() },
            this[7].takeIf { it.isNotBlank() },
            this[8].toBooleanStrictOrNull() ?: false,
            this[9].toBooleanStrictOrNull() ?: false,
            this[10].takeIf { it.isNotBlank() },
            this[11].takeIf { it.isNotBlank() },
        )
    }

    private fun List<String>.toIndex(): AutoDdlIndex? {
        if (size < 3) {
            return null
        }
        val indexType = runCatching { AutoDdlIndexType.valueOf(this[1]) }.getOrNull() ?: return null
        return AutoDdlIndex(
            this[0],
            this[2].split(',').filter { it.isNotBlank() },
            indexType,
        )
    }

    private fun List<String>.toForeignKey(): AutoDdlForeignKey? {
        if (size < 6) {
            return null
        }
        return AutoDdlForeignKey(
            this[0],
            this[1].split(',').filter { it.isNotBlank() },
            this[2],
            this[3].split(',').filter { it.isNotBlank() },
            this[4].takeIf { it.isNotBlank() },
            this[5].takeIf { it.isNotBlank() },
        )
    }

    private fun List<String>.encodeParts(): String {
        return joinToString("|") { value -> value.encodeBase64() }
    }

    private fun String.decodeParts(): List<String> {
        return split('|').map { value -> value.decodeBase64() }
    }

    private fun String.encodeBase64(): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray())
    }

    private fun String.decodeBase64(): String {
        return String(Base64.getUrlDecoder().decode(this))
    }

    private fun Int?.orEmptyPart(): String {
        return this?.toString().orEmpty()
    }

    private fun String.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(toByteArray())
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun String.escapeProperty(): String {
        return replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("=", "\\=")
            .replace(":", "\\:")
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
