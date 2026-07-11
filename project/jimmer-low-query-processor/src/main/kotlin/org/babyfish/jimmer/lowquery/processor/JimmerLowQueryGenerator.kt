package org.babyfish.jimmer.lowquery.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies

internal class JimmerLowQueryGenerator(
    private val codeGenerator: CodeGenerator,
) {
    fun generate(
        entities: Set<LowQueryEntityMeta>,
        generatedPackage: String?,
        springComponentAvailable: Boolean = false,
    ) {
        entities.groupBy { it.outputPackage(generatedPackage) }
            .forEach { (packageName, packageEntities) ->
                val fileName = packageName.substringAfterLast('.').replaceFirstChar(Char::uppercase) + "JimmerLowQueries"
                createFile(packageName, fileName).use { stream ->
                    val code = buildFile(packageName, packageEntities.sortedBy { it.qualifiedName }, springComponentAvailable)
                    stream.write(code.toByteArray())
                }
            }
    }

    private fun LowQueryEntityMeta.outputPackage(generatedPackage: String?): String {
        return generatedPackage?.takeIf { it.isNotBlank() } ?: "$packageName.generated.lowquery"
    }

    private fun createFile(packageName: String, fileName: String) =
        codeGenerator.createNewFile(Dependencies.ALL_FILES, packageName, fileName, "kt")

    private fun buildFile(
        packageName: String,
        entities: List<LowQueryEntityMeta>,
        springComponentAvailable: Boolean,
    ): String {
        val hasSourceParams = entities.any { entity ->
            entity.keywordProps.isNotEmpty() ||
                entity.params.any { param -> param.sourceApplies || param.isRangeParam() }
        }
        val hasKeywordParams = entities.any { entity -> entity.keywordProps.isNotEmpty() }
        return buildString {
            appendLine("@file:Suppress(\"unused\")")
            appendLine()
            appendLine("package $packageName")
            appendLine()
            appendLine("import org.babyfish.jimmer.ImmutableObjects")
            appendLine("import org.babyfish.jimmer.sql.ast.LikeMode")
            appendLine("import org.babyfish.jimmer.sql.kt.KSqlClient")
            appendLine("import org.babyfish.jimmer.sql.kt.ast.expression.asc")
            appendLine("import org.babyfish.jimmer.sql.kt.ast.expression.desc")
            appendLine("import org.babyfish.jimmer.sql.kt.ast.expression.eq")
            appendLine("import org.babyfish.jimmer.sql.kt.ast.expression.`eq?`")
            appendLine("import org.babyfish.jimmer.sql.kt.ast.expression.`ge?`")
            appendLine("import org.babyfish.jimmer.sql.kt.ast.expression.`gt?`")
            if (hasKeywordParams) {
                appendLine("import org.babyfish.jimmer.sql.kt.ast.expression.ilike")
            }
            appendLine("import org.babyfish.jimmer.sql.kt.ast.expression.`ilike?`")
            appendLine("import org.babyfish.jimmer.sql.kt.ast.expression.`le?`")
            appendLine("import org.babyfish.jimmer.sql.kt.ast.expression.`like?`")
            appendLine("import org.babyfish.jimmer.sql.kt.ast.expression.`lt?`")
            appendLine("import org.babyfish.jimmer.sql.kt.ast.expression.`ne?`")
            if (hasKeywordParams) {
                appendLine("import org.babyfish.jimmer.sql.kt.ast.expression.or")
            }
            appendLine("import org.babyfish.jimmer.sql.kt.ast.expression.`valueIn?`")
            appendLine("import org.babyfish.jimmer.sql.kt.ast.expression.`valueNotIn?`")
            appendLine("import org.babyfish.jimmer.sql.kt.ast.expression.`between?`")
            appendLine("import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery")
            appendLine("import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery")
            appendLine("import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable")
            if (springComponentAvailable) {
                appendLine("import org.springframework.stereotype.Component")
                if (hasSourceParams) {
                    appendLine("import org.babyfish.jimmer.lowquery.runtime.JimmerLowQueryParameterSource")
                }
                appendLine("import org.babyfish.jimmer.lowquery.runtime.JimmerLowQueryProvider")
                appendLine("import kotlin.reflect.KClass")
            }
            if (hasKeywordParams) {
            }
            entities.flatMap { entity ->
                listOf(entity.qualifiedName, "${entity.packageName}.fetchBy") +
                    entity.params.flatMap { param -> param.tableImportNames(entity.packageName) } +
                    entity.keywordProps.flatMap { keyword -> keyword.tableImportNames(entity.packageName) } +
                    entity.orderProperties().map { propertyName -> "${entity.packageName}.${propertyName.escapeIdentifier()}" } +
                    entity.findByProperties().map { propertyName -> "${entity.packageName}.${propertyName.escapeIdentifier()}" } +
                    entity.expressionImports()
            }
                .distinct()
                .sorted()
                .forEach { appendLine("import $it") }
            appendLine()
            entities.forEachIndexed { index, entity ->
                val sections = mutableListOf<String>()
                if (entity.hasLowQuerySurface()) {
                    sections += entity.buildFunction()
                    sections += entity.buildApplyParamsFunction()
                    sections += entity.buildApplyFunction()
                    sections += entity.buildClientFunction()
                    if (springComponentAvailable) {
                        sections += entity.buildSpringProvider()
                    }
                }
                if (entity.findBys.isNotEmpty()) {
                    sections += entity.buildFindByFunctions()
                }
                append(sections.joinToString("\n\n"))
                if (index != entities.lastIndex) {
                    appendLine()
                    appendLine()
                }
            }
        }
    }

    private fun LowQueryEntityMeta.buildFunction(): String {
        val parameters = renderQueryParameters()
        val parameterLines = parameters.joinToString(",\n") { parameter -> "    $parameter" }
        val orderByLine = buildOrderByLine("    ")
        val returnType = "KConfigurableRootQuery<KNonNullTable<$simpleName>, $simpleName>"
        return buildString {
            appendLine("@JvmName(\"${functionName}For${simpleName}LowQuery\")")
            if (parameters.isEmpty()) {
                appendLine("${visibility.code} fun KMutableRootQuery.ForEntity<$simpleName>.${functionName.escapeIdentifier()}(): $returnType {")
                appendLine("    ${applyParamsFunctionName()}()")
            } else {
                appendLine("${visibility.code} fun KMutableRootQuery.ForEntity<$simpleName>.${functionName.escapeIdentifier()}(")
                appendLine(parameterLines)
                appendLine("): $returnType {")
                appendLine("    ${applyParamsFunctionName()}(${buildApplyParamArguments()})")
            }
            if (orderByLine != null) {
                appendLine(orderByLine)
            }
            appendLine("    return ${buildSelectExpression()}")
            append("}")
        }
    }


    private fun LowQueryEntityMeta.buildApplyParamsFunction(): String {
        val parameters = renderQueryParameters()
        val parameterLines = parameters.joinToString(",\n") { parameter -> "    $parameter" }
        val whereLines = buildList {
            addAll(params.map { param -> param.buildWhereLine() })
            buildKeywordWhereBlock("    ", KEYWORD_PARAMETER_NAME)?.let(::add)
        }.joinToString("\n")
        return buildString {
            if (parameters.isEmpty()) {
                appendLine("public fun KMutableRootQuery.ForEntity<$simpleName>.${applyParamsFunctionName()}() {")
            } else {
                appendLine("public fun KMutableRootQuery.ForEntity<$simpleName>.${applyParamsFunctionName()}(")
                appendLine(parameterLines)
                appendLine(") {")
            }
            if (whereLines.isNotEmpty()) {
                appendLine(whereLines)
            }
            append("}")
        }
    }


    private fun LowQueryEntityMeta.buildClientFunction(): String {
        val annotation = "@JvmName(\"${clientFunctionName}For${simpleName}ByEntity\")"
        val extraParameterLines = renderEntityFunctionPlainParameters()
        return buildString {
            appendLine(annotation)
            appendLine("${clientVisibility.code} fun KSqlClient.${clientFunctionName.escapeIdentifier()}(")
            appendLine("    entity: $simpleName${if (extraParameterLines.isEmpty()) "" else ","}")
            extraParameterLines.forEachIndexed { index, parameter ->
                val suffix = if (index == extraParameterLines.lastIndex) "" else ","
                appendLine("    $parameter$suffix")
            }
            appendLine("): KConfigurableRootQuery<KNonNullTable<$simpleName>, $simpleName> {")
            appendLine("    return createQuery($simpleName::class) {")
            appendLine("        applyLowQuery(${buildApplyLowQueryArguments("entity")})")
            appendLine("        ${buildSelectExpression()}")
            appendLine("    }")
            append("}")
        }
    }

    private fun LowQueryEntityMeta.buildApplyFunction(): String {
        val whereLines = buildList {
            addAll(params.filter { param -> param.entityApplies }.map { param -> param.buildEntityWhereBlock() })
            if (needsStandaloneKeywordParameter()) {
                buildKeywordWhereBlock("        ", KEYWORD_PARAMETER_NAME)?.let(::add)
            }
        }.joinToString("\n")
        val orderByLine = buildOrderByLine("    ")
        val extraParameterLines = renderEntityFunctionPlainParameters()
        return buildString {
            appendLine("private fun KMutableRootQuery.ForEntity<$simpleName>.applyLowQuery(")
            appendLine("    entity: $simpleName${if (extraParameterLines.isEmpty()) "" else ","}")
            extraParameterLines.forEachIndexed { index, parameter ->
                val suffix = if (index == extraParameterLines.lastIndex) "" else ","
                appendLine("    $parameter$suffix")
            }
            appendLine(") {")
            if (whereLines.isNotEmpty()) {
                appendLine(whereLines)
            }
            if (orderByLine != null) {
                appendLine(orderByLine)
            }
            append("}")
        }
    }

    private fun LowQueryEntityMeta.buildSpringProvider(): String {
        val providerName = "${simpleName}JimmerLowQueryProvider"
        val beanName = "$qualifiedName.$providerName"
        val parameterMap = params
            .filterNot { param -> param.isRangeParam() }
            .joinToString(", ") { param ->
                "\"${param.propertyName.escapeString()}\" to \"${param.parameterName.escapeString()}\""
            }
        val sourceWhereLines = params
            .filter { param -> param.sourceApplies || param.isRangeParam() }
            .map { param -> param.buildSourceWhereBlock() }
            .plus(listOfNotNull(buildSourceKeywordWhereBlock()))
            .joinToString("\n")
        return buildString {
            appendLine("@Component(\"${beanName.escapeString()}\")")
            appendLine("public class $providerName : JimmerLowQueryProvider<$simpleName> {")
            appendLine("    override val entityType: KClass<$simpleName> = $simpleName::class")
            appendLine()
            appendLine("    override val parameterNames: Map<String, String> = mapOf($parameterMap)")
            appendLine()
            appendLine("    override val hasOrderBy: Boolean = ${buildOrderItems().isNotEmpty()}")
            appendLine()
            appendLine("    override fun apply(")
            appendLine("        query: KMutableRootQuery.ForEntity<$simpleName>,")
            appendLine("        entity: $simpleName")
            appendLine("    ) {")
            appendLine("        query.applyLowQuery(entity)")
            appendLine("    }")
            if (sourceWhereLines.isNotEmpty()) {
                appendLine()
                appendLine("    override fun apply(")
                appendLine("        query: KMutableRootQuery.ForEntity<$simpleName>,")
                appendLine("        source: JimmerLowQueryParameterSource")
                appendLine("    ) {")
                appendLine("        query.run {")
                appendLine(sourceWhereLines)
                appendLine("        }")
                appendLine("    }")
            }
            append("}")
        }
    }

    private fun LowQueryEntityMeta.buildSelectExpression(): String {
        return when (fetcher) {
            LowQueryFetcher.ALL_SCALAR_FIELDS -> "select(table.fetchBy { allScalarFields() })"
            LowQueryFetcher.ALL_TABLE_FIELDS -> "select(table.fetchBy { allTableFields() })"
            LowQueryFetcher.TABLE -> "select(table)"
        }
    }

    private fun LowQueryEntityMeta.buildOrderByLine(indent: String): String? {
        val orderItems = buildOrderItems()
        if (orderItems.isEmpty()) {
            return null
        }
        val expressions = orderItems.joinToString(", ") { order ->
            val functionName = when (order.direction) {
                LowQueryOrderDirection.ASC -> "asc"
                LowQueryOrderDirection.DESC -> "desc"
            }
            "table.${order.propertyName.escapeIdentifier()}.$functionName()"
        }
        return "${indent}orderBy($expressions)"
    }

    private fun LowQueryEntityMeta.buildOrderItems(): List<LowQueryOrderMeta> {
        if (orders.isNotEmpty()) {
            return orders
        }
        if (!hasIdProperty) {
            return emptyList()
        }
        return listOf(
            LowQueryOrderMeta(
                propertyName = "id",
                direction = LowQueryOrderDirection.DESC,
                priority = Int.MAX_VALUE,
            ),
        )
    }

    private fun LowQueryEntityMeta.orderProperties(): List<String> {
        return buildOrderItems().map { order -> order.propertyName }
    }

    private fun LowQueryEntityMeta.findByProperties(): List<String> {
        return findBys.flatMap { findBy ->
            findBy.fields.map { field -> field.propertyName } +
                findBy.extraFetchFields.map { field -> field.propertyName }
        }
    }

    private fun LowQueryEntityMeta.hasLowQuerySurface(): Boolean {
        return params.isNotEmpty() || orders.isNotEmpty() || keywordProps.isNotEmpty()
    }

    private fun LowQueryEntityMeta.applyParamsFunctionName(): String = "apply${simpleName}LowQueryParams"

    private fun LowQueryEntityMeta.buildFindByFunctions(): String {
        return findBys.joinToString("\n\n") { findBy ->
            buildString {
                append(findBy.buildListFunction(this@buildFindByFunctions))
                appendLine()
                appendLine()
                append(findBy.buildOneFunction(this@buildFindByFunctions))
            }
        }
    }

    private fun LowQueryFindByMeta.buildListFunction(entity: LowQueryEntityMeta): String {
        val parameterFields = fields.filterNot { field -> entity.hasFixedExpression(field.propertyName) }
        val fixedWhereLines = entity.fixedExpressionWhereLines("        ")
        return buildString {
            appendLine("@JvmName(\"${listFunctionName}For${entity.simpleName}${buildFindByJvmNameSuffix()}List\")")
            if (parameterFields.isEmpty()) {
                appendLine("${visibility.code} fun KSqlClient.${listFunctionName.escapeIdentifier()}(): List<${entity.simpleName}> {")
            } else {
                appendLine("${visibility.code} fun KSqlClient.${listFunctionName.escapeIdentifier()}(")
                parameterFields.forEach { field ->
                    appendLine("    ${field.parameterName.escapeIdentifier()}: ${field.typeName},")
                }
                appendLine("): List<${entity.simpleName}> {")
            }
            appendLine("    return executeQuery(${entity.simpleName}::class) {")
            parameterFields.forEach { field ->
                appendLine("        where(table.${field.propertyName.escapeIdentifier()} eq ${field.parameterName.escapeIdentifier()})")
            }
            fixedWhereLines.forEach { line ->
                appendLine(line)
            }
            buildOrderByLine("        ", entity)?.let { orderByLine ->
                appendLine(orderByLine)
            }
            append(buildSelectBlock("        "))
            appendLine("    }")
            append("}")
        }
    }

    private fun LowQueryFindByMeta.buildOneFunction(entity: LowQueryEntityMeta): String {
        val parameterFields = fields.filterNot { field -> entity.hasFixedExpression(field.propertyName) }
        val fixedWhereLines = entity.fixedExpressionWhereLines("        ")
        return buildString {
            appendLine("@JvmName(\"${oneFunctionName}For${entity.simpleName}${buildFindByJvmNameSuffix()}OneOrNull\")")
            if (parameterFields.isEmpty()) {
                appendLine("${visibility.code} fun KSqlClient.${oneFunctionName.escapeIdentifier()}(): ${entity.simpleName}? {")
            } else {
                appendLine("${visibility.code} fun KSqlClient.${oneFunctionName.escapeIdentifier()}(")
                parameterFields.forEach { field ->
                    appendLine("    ${field.parameterName.escapeIdentifier()}: ${field.typeName},")
                }
                appendLine("): ${entity.simpleName}? {")
            }
            appendLine("    return createQuery(${entity.simpleName}::class) {")
            parameterFields.forEach { field ->
                appendLine("        where(table.${field.propertyName.escapeIdentifier()} eq ${field.parameterName.escapeIdentifier()})")
            }
            fixedWhereLines.forEach { line ->
                appendLine(line)
            }
            buildOrderByLine("        ", entity)?.let { orderByLine ->
                appendLine(orderByLine)
            }
            append(buildSelectBlock("        "))
            appendLine("    }.fetchOneOrNull()")
            append("}")
        }
    }

    private fun LowQueryFindByMeta.buildFindBySuffix(): String =
        fields.joinToString("And") { field -> field.propertyName.replaceFirstChar(Char::uppercase) }

    private fun LowQueryFindByMeta.buildFindByJvmNameSuffix(): String {
        val suffix = buildFindBySuffix()
        if (suffix.isBlank()) {
            return ""
        }
        return "By$suffix"
    }

    private fun LowQueryEntityMeta.hasFixedExpression(propertyName: String): Boolean {
        return params.any { param ->
            param.propertyName == propertyName && !param.expression.isNullOrBlank()
        }
    }

    private fun LowQueryEntityMeta.fixedExpressionWhereLines(indent: String): List<String> {
        return params
            .filter { param -> !param.expression.isNullOrBlank() }
            .map { param ->
                "${indent}where(table.${param.propertyName.escapeIdentifier()} eq ${param.expression})"
            }
    }

    private fun LowQueryFindByMeta.buildOrderByLine(
        indent: String,
        entity: LowQueryEntityMeta,
    ): String? {
        val orderItems = entity.buildOrderItems()
        if (orderItems.isEmpty()) {
            return null
        }
        val expressions = orderItems.joinToString(", ") { order ->
            val functionName = when (order.direction) {
                LowQueryOrderDirection.ASC -> "asc"
                LowQueryOrderDirection.DESC -> "desc"
            }
            "table.${order.propertyName.escapeIdentifier()}.$functionName()"
        }
        return "${indent}orderBy($expressions)"
    }

    private fun LowQueryEntityMeta.expressionImports(): List<String> {
        return params
            .filter { param -> !param.expression.isNullOrBlank() }
            .flatMap { param -> param.expressionImports }
    }

    private fun LowQueryFindByMeta.buildSelectBlock(indent: String): String {
        val baseFetchLine = when (fetcher) {
            LowQueryFetcher.ALL_SCALAR_FIELDS -> "allScalarFields()"
            LowQueryFetcher.ALL_TABLE_FIELDS -> "allTableFields()"
            LowQueryFetcher.TABLE -> null
        }
        if (baseFetchLine == null) {
            return "${indent}select(table)\n"
        }
        if (extraFetchFields.isEmpty()) {
            return "${indent}select(table.fetchBy { $baseFetchLine })\n"
        }
        return buildString {
            appendLine("${indent}select(table.fetchBy {")
            appendLine("$indent    $baseFetchLine")
            extraFetchFields.forEach { field ->
                when (field.kind) {
                    LowQueryExtraFetchFieldKind.SCALAR -> appendLine("$indent    ${field.propertyName.escapeIdentifier()}()")
                    LowQueryExtraFetchFieldKind.ASSOCIATION -> appendLine("$indent    ${field.propertyName.escapeIdentifier()} { allScalarFields() }")
                }
            }
            appendLine("$indent})")
        }
    }

    private fun LowQueryParamMeta.buildWhereLine(): String {
        val parameter = parameterName.escapeIdentifier()
        val condition = buildTableConditionExpression { property ->
            when (operator) {
                LowQueryOperator.EQ -> "$property `eq?` $parameter"
                LowQueryOperator.NE -> "$property `ne?` $parameter"
                LowQueryOperator.LIKE -> "$property.`ilike?`($parameter, LikeMode.ANYWHERE)"
                LowQueryOperator.STARTS_WITH -> "$property.`like?`($parameter, LikeMode.START)"
                LowQueryOperator.ENDS_WITH -> "$property.`like?`($parameter, LikeMode.END)"
                LowQueryOperator.GT -> "$property `gt?` $parameter"
                LowQueryOperator.GE -> "$property `ge?` $parameter"
                LowQueryOperator.LT -> "$property `lt?` $parameter"
                LowQueryOperator.LE -> "$property `le?` $parameter"
                LowQueryOperator.IN -> "$property `valueIn?` $parameter"
                LowQueryOperator.NOT_IN -> "$property `valueNotIn?` $parameter"
                LowQueryOperator.BETWEEN -> "$property.`between?`($parameter, ${requiredEndParameterName().escapeIdentifier()})"
                LowQueryOperator.TIME_RANGE -> {
                    val start = "$parameter?.getOrNull(0)"
                    val end = "$parameter?.getOrNull(1)"
                    "$property.`between?`($start, $end)"
                }
            }
        }
        return "    where($condition)"
    }

    private fun LowQueryParamMeta.buildEntityWhereBlock(): String {
        val entityValue = entityValueExpression()
        val condition = buildTableConditionExpression { property ->
            when (operator) {
                LowQueryOperator.EQ -> "$property `eq?` $entityValue"
                LowQueryOperator.NE -> "$property `ne?` $entityValue"
                LowQueryOperator.LIKE -> "$property.`ilike?`($entityValue, LikeMode.ANYWHERE)"
                LowQueryOperator.STARTS_WITH -> "$property.`like?`($entityValue, LikeMode.START)"
                LowQueryOperator.ENDS_WITH -> "$property.`like?`($entityValue, LikeMode.END)"
                LowQueryOperator.GT -> "$property `gt?` $entityValue"
                LowQueryOperator.GE -> "$property `ge?` $entityValue"
                LowQueryOperator.LT -> "$property `lt?` $entityValue"
                LowQueryOperator.LE -> "$property `le?` $entityValue"
                LowQueryOperator.IN -> "$property `valueIn?` $entityValue"
                LowQueryOperator.NOT_IN -> "$property `valueNotIn?` $entityValue"
                LowQueryOperator.BETWEEN -> {
                    val parameter = parameterName.escapeIdentifier()
                    val endParameter = requiredEndParameterName().escapeIdentifier()
                    "$property.`between?`($parameter, $endParameter)"
                }
                LowQueryOperator.TIME_RANGE -> {
                    val parameter = parameterName.escapeIdentifier()
                    "$property.`between?`($parameter?.getOrNull(0), $parameter?.getOrNull(1))"
                }
            }
        }
        return buildString {
            if (operator == LowQueryOperator.BETWEEN || operator == LowQueryOperator.TIME_RANGE) {
                append("        where($condition)")
                return@buildString
            }
            appendLine("        if (ImmutableObjects.isLoaded(entity, \"${loadedPropertyName.escapeString()}\")) {")
            appendLine("            where($condition)")
            append("        }")
        }
    }

    private fun LowQueryParamMeta.buildSourceWhereBlock(): String {
        val parameter = parameterName.escapeIdentifier()
        return when (operator) {
            LowQueryOperator.EQ -> buildSingleSourceWhereBlock(parameter, buildTableConditionExpression { property -> "$property `eq?` $parameter" })
            LowQueryOperator.NE -> buildSingleSourceWhereBlock(parameter, buildTableConditionExpression { property -> "$property `ne?` $parameter" })
            LowQueryOperator.LIKE -> buildSingleSourceWhereBlock(
                parameter,
                buildTableConditionExpression { property -> "$property.`ilike?`($parameter, LikeMode.ANYWHERE)" },
            )
            LowQueryOperator.STARTS_WITH -> buildSingleSourceWhereBlock(
                parameter,
                buildTableConditionExpression { property -> "$property.`like?`($parameter, LikeMode.START)" },
            )
            LowQueryOperator.ENDS_WITH -> buildSingleSourceWhereBlock(
                parameter,
                buildTableConditionExpression { property -> "$property.`like?`($parameter, LikeMode.END)" },
            )
            LowQueryOperator.GT -> buildSingleSourceWhereBlock(parameter, buildTableConditionExpression { property -> "$property `gt?` $parameter" })
            LowQueryOperator.GE -> buildSingleSourceWhereBlock(parameter, buildTableConditionExpression { property -> "$property `ge?` $parameter" })
            LowQueryOperator.LT -> buildSingleSourceWhereBlock(parameter, buildTableConditionExpression { property -> "$property `lt?` $parameter" })
            LowQueryOperator.LE -> buildSingleSourceWhereBlock(parameter, buildTableConditionExpression { property -> "$property `le?` $parameter" })
            LowQueryOperator.IN -> buildCollectionSourceWhereBlock(parameter, buildTableConditionExpression { property -> "$property `valueIn?` $parameter" })
            LowQueryOperator.NOT_IN -> buildCollectionSourceWhereBlock(parameter, buildTableConditionExpression { property -> "$property `valueNotIn?` $parameter" })
            LowQueryOperator.BETWEEN -> {
                val endName = requiredEndParameterName().escapeIdentifier()
                val condition = buildTableConditionExpression { property ->
                    "$property.`between?`($parameter, $endName)"
                }
                """
            val $parameter = source.value("${parameterName.escapeString()}", $targetTypeName::class)
            val $endName = source.value("${requiredEndParameterName().escapeString()}", $targetTypeName::class)
            where($condition)""".trimIndent().prependIndent("            ")
            }
            LowQueryOperator.TIME_RANGE -> {
                val condition = buildTableConditionExpression { property ->
                    "$property.`between?`($parameter.getOrNull(0), $parameter.getOrNull(1))"
                }
                """
            val $parameter = source.values("${parameterName.escapeString()}", $targetTypeName::class)
            where($condition)""".trimIndent().prependIndent("            ")
            }
        }
    }

    private fun LowQueryEntityMeta.renderQueryParameters(): List<String> {
        val parameterLines = params.map { param -> param.renderParameter() }.toMutableList()
        if (needsStandaloneKeywordParameter()) {
            parameterLines += "$KEYWORD_PARAMETER_NAME: String? = null"
        }
        return parameterLines
    }

    private fun LowQueryEntityMeta.renderEntityFunctionPlainParameters(): List<String> {
        val parameterLines = params
            .filter { param -> param.isRangeParam() }
            .flatMap { param -> param.renderPlainParameters() }
            .toMutableList()
        if (needsStandaloneKeywordParameter()) {
            parameterLines += "$KEYWORD_PARAMETER_NAME: String? = null"
        }
        return parameterLines
    }

    private fun LowQueryEntityMeta.needsStandaloneKeywordParameter(): Boolean {
        if (keywordProps.isEmpty()) {
            return false
        }
        return params.none { param ->
            param.parameterName == KEYWORD_PARAMETER_NAME ||
                param.endParameterName == KEYWORD_PARAMETER_NAME
        }
    }

    private fun LowQueryEntityMeta.buildKeywordWhereBlock(
        indent: String,
        parameterName: String,
    ): String? {
        if (keywordProps.isEmpty()) {
            return null
        }
        val parameter = parameterName.escapeIdentifier()
        val trimmedName = "${parameterName}Trimmed".escapeIdentifier()
        val conditions = keywordProps.map { keyword ->
            keyword.buildTableConditionExpression { property ->
                "$property.`ilike?`($trimmedName, LikeMode.ANYWHERE)"
            }
        }
        return buildString {
            appendLine("${indent}val $trimmedName = $parameter?.trim()?.takeIf { it.isNotEmpty() }")
            appendLine("${indent}if ($trimmedName != null) {")
            if (conditions.size == 1) {
                appendLine("${indent}    where(${conditions.single()})")
            } else {
                appendLine("${indent}    where(")
                appendLine("${indent}        or(")
                conditions.forEachIndexed { index, condition ->
                    val suffix = if (index == conditions.lastIndex) "" else ","
                    appendLine("${indent}            $condition$suffix")
                }
                appendLine("${indent}        ),")
                appendLine("${indent}    )")
            }
            append("${indent}}")
        }
    }

    private fun LowQueryEntityMeta.buildSourceKeywordWhereBlock(): String? {
        if (keywordProps.isEmpty()) {
            return null
        }
        val keywordWhereBlock = buildKeywordWhereBlock("            ", KEYWORD_PARAMETER_NAME) ?: return null
        return buildString {
            appendLine("            val $KEYWORD_PARAMETER_NAME = source.value(\"$KEYWORD_PARAMETER_NAME\", String::class)")
            append(keywordWhereBlock)
        }
    }

    private fun LowQueryParamMeta.buildTableConditionExpression(
        terminalCondition: (String) -> String,
    ): String {
        if (tablePath.size <= 1) {
            return terminalCondition("table.${tablePath.firstOrNull().orEmpty().escapeIdentifier()}")
        }
        return buildTablePathExpression("table", 0, terminalCondition)
    }

    private fun LowQueryKeywordMeta.buildTableConditionExpression(
        terminalCondition: (String) -> String,
    ): String {
        if (tablePath.size <= 1) {
            return terminalCondition("table.${tablePath.firstOrNull().orEmpty().escapeIdentifier()}")
        }
        return buildTablePathExpression("table", 0, terminalCondition)
    }

    private fun LowQueryParamMeta.buildTablePathExpression(
        receiver: String,
        index: Int,
        terminalCondition: (String) -> String,
    ): String {
        val propertyName = tablePath.getOrNull(index).orEmpty().escapeIdentifier()
        if (index == tablePath.lastIndex) {
            return terminalCondition("$receiver.$propertyName")
        }
        return when (tablePathKinds.getOrNull(index) ?: LowQueryTablePathKind.REFERENCE) {
            LowQueryTablePathKind.REFERENCE -> buildTablePathExpression("$receiver.$propertyName", index + 1, terminalCondition)
            LowQueryTablePathKind.COLLECTION -> "$receiver.$propertyName { ${buildTablePathExpression("this", index + 1, terminalCondition)} }"
        }
    }

    private fun LowQueryKeywordMeta.buildTablePathExpression(
        receiver: String,
        index: Int,
        terminalCondition: (String) -> String,
    ): String {
        val propertyName = tablePath.getOrNull(index).orEmpty().escapeIdentifier()
        if (index == tablePath.lastIndex) {
            return terminalCondition("$receiver.$propertyName")
        }
        return when (tablePathKinds.getOrNull(index) ?: LowQueryTablePathKind.REFERENCE) {
            LowQueryTablePathKind.REFERENCE -> buildTablePathExpression("$receiver.$propertyName", index + 1, terminalCondition)
            LowQueryTablePathKind.COLLECTION -> "$receiver.$propertyName { ${buildTablePathExpression("this", index + 1, terminalCondition)} }"
        }
    }

    private fun LowQueryParamMeta.tableImportNames(entityPackageName: String): List<String> {
        if (tablePath.isEmpty()) {
            return emptyList()
        }
        return tablePath.distinct().mapIndexed { index, propertyName ->
            val importPackageName = tablePathImportPackages.getOrNull(index) ?: entityPackageName
            "$importPackageName.${propertyName.escapeIdentifier()}"
        }
    }

    private fun LowQueryKeywordMeta.tableImportNames(entityPackageName: String): List<String> {
        if (tablePath.isEmpty()) {
            return emptyList()
        }
        return tablePath.distinct().mapIndexed { index, propertyName ->
            val importPackageName = tablePathImportPackages.getOrNull(index) ?: entityPackageName
            "$importPackageName.${propertyName.escapeIdentifier()}"
        }
    }

    private fun LowQueryParamMeta.buildSingleSourceWhereBlock(
        localName: String,
        condition: String,
    ): String {
        return """
            val $localName = source.value("${parameterName.escapeString()}", $targetTypeName::class)
            where($condition)""".trimIndent().prependIndent("            ")
    }

    private fun LowQueryParamMeta.buildCollectionSourceWhereBlock(
        localName: String,
        condition: String,
    ): String {
        return """
            val $localName = source.values("${parameterName.escapeString()}", $targetTypeName::class)
            if ($localName.isNotEmpty()) {
                where($condition)
            }""".trimIndent().prependIndent("            ")
    }

    private fun LowQueryParamMeta.entityValueExpression(): String {
        if (entityValuePath.size <= 1) {
            val property = "entity.${entityValuePath.firstOrNull().orEmpty().escapeIdentifier()}"
            return when (operator) {
                LowQueryOperator.IN,
                LowQueryOperator.NOT_IN -> "listOfNotNull($property)"

                else -> property
            }
        }
        val property = entityValuePath.entityValueAccessExpression(entityValuePathNullable)
        return when (operator) {
            LowQueryOperator.IN,
            LowQueryOperator.NOT_IN -> "listOfNotNull($property)"

            else -> property
        }
    }

    private fun List<String>.entityValueAccessExpression(nullablePath: List<Boolean>): String {
        val path = this
        if (path.isEmpty()) {
            return "entity"
        }
        return buildString {
            append("entity.")
            append(path.first().escapeIdentifier())
            path.drop(1).forEachIndexed { index, part ->
                val previousIsNullable = nullablePath.getOrNull(index) ?: true
                if (previousIsNullable) {
                    append("?.")
                } else {
                    append(".")
                }
                append(part.orEmpty().escapeIdentifier())
            }
        }
    }

    private fun LowQueryParamMeta.renderParameter(): String {
        val annotations = renderParameterAnnotations()
        val defaultValue = if (nullable) " = null" else ""
        val firstParameter = "$annotations${parameterName.escapeIdentifier()}: $typeName$defaultValue"
        val endName = endParameterName ?: return firstParameter
        val endType = endTypeName ?: typeName
        return "$firstParameter,\n    ${endName.escapeIdentifier()}: $endType = null"
    }

    private fun LowQueryParamMeta.renderPlainParameters(): List<String> {
        val defaultValue = if (nullable) " = null" else ""
        val firstParameter = "${parameterName.escapeIdentifier()}: $typeName$defaultValue"
        val endName = endParameterName ?: return listOf(firstParameter)
        val endType = endTypeName ?: typeName
        return listOf(firstParameter, "${endName.escapeIdentifier()}: $endType = null")
    }

    private fun LowQueryParamMeta.renderParameterAnnotations(): String {
        if (parameterAnnotations.isEmpty()) {
            return ""
        }
        return parameterAnnotations.joinToString(separator = " ") + " "
    }

    private fun LowQueryParamMeta.requiredEndParameterName(): String =
        endParameterName ?: error("范围查询缺少结束参数名：$propertyName")

    private fun LowQueryParamMeta.isRangeParam(): Boolean =
        operator == LowQueryOperator.BETWEEN || operator == LowQueryOperator.TIME_RANGE

    private fun LowQueryEntityMeta.buildApplyParamArguments(): String {
        val arguments = params.flatMap { param ->
            val arguments = mutableListOf(
                "${param.parameterName.escapeIdentifier()} = ${param.parameterName.escapeIdentifier()}",
            )
            val endName = param.endParameterName
            if (endName != null) {
                arguments += "${endName.escapeIdentifier()} = ${endName.escapeIdentifier()}"
            }
            arguments
        }.toMutableList()
        if (needsStandaloneKeywordParameter()) {
            arguments += "$KEYWORD_PARAMETER_NAME = $KEYWORD_PARAMETER_NAME"
        }
        return arguments.joinToString(", ")
    }


    private fun LowQueryEntityMeta.buildApplyLowQueryArguments(entityArgument: String): String {
        val arguments = mutableListOf(entityArgument)
        params.filter { it.isRangeParam() }.forEach { param ->
            arguments += "${param.parameterName.escapeIdentifier()} = ${param.parameterName.escapeIdentifier()}"
            val endName = param.endParameterName
            if (endName != null) {
                arguments += "${endName.escapeIdentifier()} = ${endName.escapeIdentifier()}"
            }
        }
        if (needsStandaloneKeywordParameter()) {
            arguments += "$KEYWORD_PARAMETER_NAME = $KEYWORD_PARAMETER_NAME"
        }
        return arguments.joinToString(", ")
    }

    private fun String.escapeIdentifier(): String {
        if (this in KOTLIN_KEYWORDS) {
            return "`$this`"
        }
        return this
    }

    private fun String.escapeString(): String =
        replace("\\", "\\\\")
            .replace("\"", "\\\"")

    private companion object {
        private const val KEYWORD_PARAMETER_NAME = "keyword"

        private val KOTLIN_KEYWORDS = setOf(
            "as",
            "break",
            "class",
            "continue",
            "do",
            "else",
            "false",
            "for",
            "fun",
            "if",
            "in",
            "interface",
            "is",
            "null",
            "object",
            "package",
            "return",
            "super",
            "this",
            "throw",
            "true",
            "try",
            "typealias",
            "typeof",
            "val",
            "var",
            "when",
            "while",
        )
    }
}
