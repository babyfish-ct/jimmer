package org.babyfish.jimmer.ddl.compiler

import site.addzero.ddlgenerator.core.options.AutoDdlOptions
import site.addzero.util.db.DatabaseType
import java.io.File
import java.util.Properties

private const val DEFAULT_OUTPUT_DIR = "build/generated/jimmer-ddl/main/resources/db/migration"
private const val DEFAULT_VERSION = "1001"
private const val DEFAULT_DESCRIPTION = "jimmer_auto_ddl_generated"
private const val DEFAULT_SPRING_PROFILE = "local"
private val DEFAULT_DATABASE_TYPE = DatabaseType.POSTGRESQL

enum class JimmerDdlOutputFormat {
    PLAIN,
    FLYWAY;

    companion object {
        fun parse(value: String): JimmerDdlOutputFormat {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: FLYWAY
        }
    }
}

data class JimmerDdlJdbcSettings(
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val schema: String? = null,
    val driverClassName: String? = null,
) {
    val configured
        get() = url.isNotBlank()
}

data class JimmerDdlCompilerSettings(
    val enabled: Boolean = true,
    val databaseType: DatabaseType = DEFAULT_DATABASE_TYPE,
    val outputFormat: JimmerDdlOutputFormat = JimmerDdlOutputFormat.FLYWAY,
    val outputDir: String = DEFAULT_OUTPUT_DIR,
    val version: String = DEFAULT_VERSION,
    val description: String = DEFAULT_DESCRIPTION,
    val includePackages: List<String> = emptyList(),
    val excludePackages: List<String> = emptyList(),
    val options: AutoDdlOptions = AutoDdlOptions(),
    val includeManyToManyTables: Boolean = true,
    val compareDatabase: Boolean = true,
    val nullabilityRepairOnly: Boolean = false,
    val sourceFingerprint: String? = null,
    val jdbc: JimmerDdlJdbcSettings = JimmerDdlJdbcSettings(),
) {
    fun includesClass(qualifiedName: String?): Boolean {
        if (qualifiedName.isNullOrBlank()) {
            return includePackages.isEmpty()
        }

        if (excludePackages.any { packagePrefix -> qualifiedName.isInPackagePrefix(packagePrefix) }) {
            return false
        }

        if (includePackages.isEmpty()) {
            return true
        }

        return includePackages.any { packagePrefix -> qualifiedName.isInPackagePrefix(packagePrefix) }
    }

    val outputFileName: String
        get() {
            val normalizedDescription = description
                .ifBlank { DEFAULT_DESCRIPTION }
                .replace(Regex("[^A-Za-z0-9_]+"), "_")
                .trim('_')
                .ifBlank { DEFAULT_DESCRIPTION }
            return when (outputFormat) {
                JimmerDdlOutputFormat.FLYWAY -> "V${version.ifBlank { DEFAULT_VERSION }}__${normalizedDescription}.sql"
                JimmerDdlOutputFormat.PLAIN -> "${normalizedDescription}.sql"
            }
        }

    companion object {
        fun allFromOptions(options: Map<String, String>): List<JimmerDdlCompilerSettings> {
            val profileNames = options.option("jimmerDdl.profiles", defaultValue = "").toValueList()
            if (profileNames.isEmpty()) {
                return listOf(fromOptions(options))
            }
            return profileNames.map { profileName ->
                fromProfileOptions(options, profileName)
            }
        }

        fun fromOptions(options: Map<String, String>): JimmerDdlCompilerSettings {
            val outputDir = options.option(
                newKey = "jimmerDdl.outputDir",
                defaultValue = DEFAULT_OUTPUT_DIR,
            )
            val springProfile = options.option("jimmerDdl.springProfile", defaultValue = DEFAULT_SPRING_PROFILE)
            val jdbc = resolveJdbcSettings(options = options, profileName = null, outputDir = outputDir, springProfile = springProfile)
            return JimmerDdlCompilerSettings(
                enabled = options.option("jimmerDdl.enabled", defaultValue = "true").toBooleanStrictOrNull() ?: true,
                databaseType = resolveDatabaseType(options = options, profileName = null, jdbc = jdbc),
                outputFormat = JimmerDdlOutputFormat.parse(options.option("jimmerDdl.outputFormat", defaultValue = "flyway")),
                outputDir = outputDir,
                version = options.option("jimmerDdl.version", defaultValue = DEFAULT_VERSION),
                description = options.option("jimmerDdl.description", defaultValue = DEFAULT_DESCRIPTION),
                includePackages = options.option("jimmerDdl.includePackages", defaultValue = "").toPackageFilters(),
                excludePackages = options.option("jimmerDdl.excludePackages", defaultValue = "").toPackageFilters(),
                options = AutoDdlOptions(
                    options.option("jimmerDdl.includeForeignKeys", defaultValue = "true").toBooleanStrictOrNull() ?: true,
                    options.option("jimmerDdl.includeIndexes", defaultValue = "true").toBooleanStrictOrNull() ?: true,
                    options.option("jimmerDdl.includeComments", defaultValue = "true").toBooleanStrictOrNull() ?: true,
                    options.option("jimmerDdl.includeSequences", defaultValue = "true").toBooleanStrictOrNull() ?: true,
                ),
                includeManyToManyTables = options.option("jimmerDdl.includeManyToManyTables", defaultValue = "true").toBooleanStrictOrNull() ?: true,
                compareDatabase = options.option("jimmerDdl.compareDatabase", defaultValue = "true").toBooleanStrictOrNull() ?: true,
                nullabilityRepairOnly = options.option("jimmerDdl.nullabilityRepairOnly", defaultValue = "false").toBooleanStrictOrNull() ?: false,
                sourceFingerprint = options.option("jimmerDdl.sourceFingerprint", defaultValue = "").takeIf { it.isNotBlank() },
                jdbc = jdbc,
            )
        }

        private fun fromProfileOptions(
            options: Map<String, String>,
            profileName: String,
        ): JimmerDdlCompilerSettings {
            val outputDir = options.profileOption(
                profileName = profileName,
                key = "outputDir",
                defaultValue = DEFAULT_OUTPUT_DIR,
            )
            val springProfile = options.profileOption(profileName, "springProfile", defaultValue = DEFAULT_SPRING_PROFILE)
            val jdbc = resolveJdbcSettings(options = options, profileName = profileName, outputDir = outputDir, springProfile = springProfile)
            return JimmerDdlCompilerSettings(
                enabled = options.profileOption(profileName, "enabled", defaultValue = "true").toBooleanStrictOrNull() ?: true,
                databaseType = resolveDatabaseType(options = options, profileName = profileName, jdbc = jdbc),
                outputFormat = JimmerDdlOutputFormat.parse(options.profileOption(profileName, "outputFormat", defaultValue = "flyway")),
                outputDir = outputDir,
                version = options.profileOption(profileName, "version", defaultValue = DEFAULT_VERSION),
                description = options.profileOption(profileName, "description", defaultValue = "${profileName}_generated"),
                includePackages = options.profileOption(profileName, "includePackages", defaultValue = "").toPackageFilters(),
                excludePackages = options.profileOption(profileName, "excludePackages", defaultValue = "").toPackageFilters(),
                options = AutoDdlOptions(
                    options.profileOption(profileName, "includeForeignKeys", defaultValue = "true").toBooleanStrictOrNull() ?: true,
                    options.profileOption(profileName, "includeIndexes", defaultValue = "true").toBooleanStrictOrNull() ?: true,
                    options.profileOption(profileName, "includeComments", defaultValue = "true").toBooleanStrictOrNull() ?: true,
                    options.profileOption(profileName, "includeSequences", defaultValue = "true").toBooleanStrictOrNull() ?: true,
                ),
                includeManyToManyTables = options.profileOption(profileName, "includeManyToManyTables", defaultValue = "true").toBooleanStrictOrNull() ?: true,
                compareDatabase = options.profileOption(profileName, "compareDatabase", defaultValue = "true").toBooleanStrictOrNull() ?: true,
                nullabilityRepairOnly = options.profileOption(profileName, "nullabilityRepairOnly", defaultValue = "false").toBooleanStrictOrNull() ?: false,
                sourceFingerprint = options.profileOption(profileName, "sourceFingerprint", defaultValue = "").takeIf { it.isNotBlank() }
                    ?: options.option("jimmerDdl.sourceFingerprint", defaultValue = "").takeIf { it.isNotBlank() },
                jdbc = jdbc,
            )
        }

        private fun resolveDatabaseType(
            options: Map<String, String>,
            profileName: String?,
            jdbc: JimmerDdlJdbcSettings,
        ): DatabaseType {
            val rawValue = if (profileName == null) {
                options.option(newKey = "jimmerDdl.databaseType", defaultValue = "auto")
            } else {
                options.profileOption(profileName, key = "databaseType", defaultValue = "auto")
            }
            parseExplicitDatabaseType(rawValue)?.let { return it }

            return jdbc.url.takeIf { it.isNotBlank() }?.let { DatabaseType.fromUrl(it) }
                ?: DEFAULT_DATABASE_TYPE
        }

        private fun resolveJdbcSettings(
            options: Map<String, String>,
            profileName: String?,
            outputDir: String,
            springProfile: String,
        ): JimmerDdlJdbcSettings {
            val explicitUrl = optionByProfile(options, profileName, "jdbcUrl", defaultValue = "")
            val explicitUsername = optionByProfile(options, profileName, "jdbcUsername", defaultValue = "")
            val explicitPassword = optionByProfile(options, profileName, "jdbcPassword", defaultValue = "")
            val explicitSchema = optionByProfile(options, profileName, "jdbcSchema", defaultValue = "")
            val explicitDriver = optionByProfile(options, profileName, "jdbcDriver", defaultValue = "")
            if (explicitUrl.isNotBlank()) {
                return JimmerDdlJdbcSettings(
                    url = explicitUrl,
                    username = explicitUsername,
                    password = explicitPassword,
                    schema = explicitSchema.takeIf { it.isNotBlank() },
                    driverClassName = explicitDriver.takeIf { it.isNotBlank() },
                )
            }

            val springResourcePath = optionByProfile(options, profileName, "springResourcePath", defaultValue = "")
            val discovered = springResourcePath
                .takeIf { it.isNotBlank() }
                ?.let { readJdbcSettingsFromPath(it, springProfile) }
                ?: readJdbcSettingsFromProjectResources(outputDir, springProfile)
                ?: JimmerDdlJdbcSettings()
            return discovered.copy(
                schema = explicitSchema.takeIf { it.isNotBlank() } ?: discovered.schema,
                driverClassName = explicitDriver.takeIf { it.isNotBlank() } ?: discovered.driverClassName,
            )
        }

        private fun optionByProfile(
            options: Map<String, String>,
            profileName: String?,
            key: String,
            defaultValue: String,
        ): String {
            if (profileName == null) {
                return options.option("jimmerDdl.$key", defaultValue)
            }
            return options.profileOption(profileName, key, defaultValue)
        }

        private fun readJdbcSettingsFromProjectResources(
            outputDir: String,
            springProfile: String,
        ): JimmerDdlJdbcSettings? {
            val absoluteOutputDir = File(outputDir).absoluteFile.path
            val projectDir = absoluteOutputDir.substringBefore("${File.separator}build${File.separator}", missingDelimiterValue = "")
                .takeIf { it.isNotBlank() }
                ?.let(::File)
                ?: return null
            return listOf(
                projectDir.resolve("src/main/resources"),
                projectDir.resolve("src/main/resources/config"),
            ).firstNotNullOfOrNull { path -> readJdbcSettingsFromPath(path, springProfile) }
        }

        private fun readJdbcSettingsFromPath(path: String, springProfile: String): JimmerDdlJdbcSettings? {
            return readJdbcSettingsFromPath(File(path), springProfile)
        }

        private fun readJdbcSettingsFromPath(file: File, springProfile: String): JimmerDdlJdbcSettings? {
            if (!file.exists()) {
                return null
            }
            val files = when {
                file.isFile -> listOf(file)
                file.isDirectory -> file.walkTopDown()
                    .maxDepth(5)
                    .filter { candidate -> candidate.isFile && candidate.isSpringConfigFile() }
                    .sortedWith(
                        compareBy<File> { candidate -> candidate.datasourceConfigPriority() }
                            .thenBy { candidate -> candidate.relativeToOrSelf(file).invariantSeparatorsPath }
                    )
                    .toList()
                else -> emptyList()
            }
            return files.firstNotNullOfOrNull { candidate ->
                candidate.readSpringJdbcSettings(springProfile)
            }
        }

        private fun File.readSpringJdbcSettings(springProfile: String): JimmerDdlJdbcSettings? {
            val props = if (extension.equals("properties", ignoreCase = true)) {
                parsePropertiesConfig()
            } else {
                parseYamlConfig(readText(), springProfile)
            }
            return props.toJdbcSettings()
        }

        private fun File.parsePropertiesConfig(): Map<String, String> {
            return inputStream().use { input ->
                Properties().apply { load(input) }
            }.entries.associate { (key, value) -> key.toString() to value.toString() }
        }

        private fun parseYamlConfig(text: String, springProfile: String): Map<String, String> {
            val defaults = linkedMapOf<String, String>()
            val profiles = linkedMapOf<String, String>()
            text.normalizeYamlProfileKey()
                .split(Regex("(?m)^---\\s*$"))
                .forEach { document ->
                    val props = parseYamlDocument(document)
                    if (props.isEmpty()) {
                        return@forEach
                    }
                    val profile = props["spring.config.activate.on-profile"]
                    if (profile == null) {
                        defaults.putAll(props)
                    } else if (profile.split(',', ';').map { it.trim() }.any { it.equals(springProfile, ignoreCase = true) }) {
                        profiles.putAll(props)
                    }
                }
            return linkedMapOf<String, String>().apply {
                putAll(defaults)
                putAll(profiles)
            }
        }

        private fun parseYamlDocument(document: String): Map<String, String> {
            val props = linkedMapOf<String, String>()
            val pathStack = mutableListOf<Pair<Int, String>>()
            document.lineSequence().forEach { rawLine ->
                val line = rawLine.substringBeforeComment().takeIf { it.isNotBlank() } ?: return@forEach
                val indent = line.indexOfFirst { !it.isWhitespace() }.takeIf { it >= 0 } ?: 0
                val trimmed = line.trim()
                if (trimmed.startsWith("-") || !trimmed.contains(":")) {
                    return@forEach
                }
                val key = trimmed.substringBefore(':').trim().normalizeConfigKey()
                val rawValue = trimmed.substringAfter(':', missingDelimiterValue = "").trim()
                while (pathStack.isNotEmpty() && pathStack.last().first >= indent) {
                    pathStack.removeAt(pathStack.lastIndex)
                }
                pathStack += indent to key
                if (rawValue.isNotBlank()) {
                    val fullKey = pathStack.joinToString(".") { it.second }
                    props[fullKey] = rawValue.unquoteConfigValue()
                }
            }
            return props
        }

        private fun Map<String, String>.toJdbcSettings(): JimmerDdlJdbcSettings? {
            val dynamicPrefix = "spring.datasource.dynamic"
            val primary = this["$dynamicPrefix.primary"]?.takeIf { it.isNotBlank() } ?: "master"
            val prefixes = listOf(
                "$dynamicPrefix.datasource.$primary",
                "$dynamicPrefix.datasource.master",
                "spring.datasource",
            )
            val urlPrefix = prefixes.firstOrNull { prefix -> !this["$prefix.url"].isNullOrBlank() } ?: return null
            val url = this["$urlPrefix.url"] ?: return null
            val username = this["$urlPrefix.username"].orEmpty()
            val password = this["$urlPrefix.password"].orEmpty()
            val driver = this["$urlPrefix.driver-class-name"] ?: this["$urlPrefix.driverClassName"]
            return JimmerDdlJdbcSettings(
                url = url,
                username = username,
                password = password,
                driverClassName = driver,
            )
        }

        private fun File.isSpringConfigFile(): Boolean {
            val name = name.lowercase()
            return name.endsWith(".yml") || name.endsWith(".yaml") || name.endsWith(".properties")
        }

        private fun File.datasourceConfigPriority(): Int {
            val normalizedName = nameWithoutExtension.lowercase()
            return when {
                "datasource" in normalizedName -> 0
                "database" in normalizedName -> 1
                normalizedName == "db" || normalizedName.endsWith("-db") || normalizedName.endsWith("_db") -> 2
                normalizedName.startsWith("application") -> 3
                else -> 4
            }
        }

        private fun parseExplicitDatabaseType(rawValue: String): DatabaseType? {
            val normalized = rawValue.trim().lowercase()
            if (normalized.isBlank() || normalized == "auto" || normalized == "datasource") {
                return null
            }
            if (normalized.startsWith("jdbc:")) {
                return DatabaseType.fromUrl(rawValue)
            }
            val alias = when (normalized) {
                "pg", "postgres" -> "postgresql"
                "mssql" -> "sqlserver"
                "dameng" -> "dm"
                else -> normalized
            }
            return DatabaseType.fromCode(alias)
                ?: DatabaseType.fromName(alias)
        }

        private fun String.toPackageFilters(): List<String> {
            return toValueList()
        }

        private fun String.toValueList(): List<String> {
            return split(',', ';')
                .map { it.trim().trimEnd('.') }
                .filter { it.isNotBlank() }
                .distinct()
        }

        private fun Map<String, String>.option(
            newKey: String,
            defaultValue: String,
        ): String {
            val newValue = this[newKey]
            if (!newValue.isNullOrBlank()) {
                return newValue
            }
            return defaultValue
        }

        private fun Map<String, String>.profileOption(
            profileName: String,
            key: String,
            defaultValue: String,
        ): String {
            val profileValue = this["jimmerDdl.profile.$profileName.$key"]
            if (!profileValue.isNullOrBlank()) {
                return profileValue
            }
            return option(
                newKey = "jimmerDdl.$key",
                defaultValue = defaultValue,
            )
        }

        private fun String.normalizeYamlProfileKey(): String {
            return replace(Regex("(?m)^spring\\.\\s+config\\.\\s+activate\\.\\s+on-profile\\s*:")) {
                "spring.config.activate.on-profile:"
            }
        }

        private fun String.substringBeforeComment(): String {
            val trimmed = trimStart()
            if (trimmed.startsWith("#")) {
                return ""
            }
            val marker = indexOf(" #")
            if (marker >= 0) {
                return substring(0, marker).trimEnd()
            }
            return this
        }

        private fun String.normalizeConfigKey(): String {
            return replace(Regex("\\s*\\.\\s*"), ".")
        }

        private fun String.unquoteConfigValue(): String {
            return trim()
                .removeSurrounding("\"")
                .removeSurrounding("'")
        }
    }
}

private fun String.isInPackagePrefix(packagePrefix: String): Boolean {
    return this == packagePrefix || startsWith("$packagePrefix.")
}
