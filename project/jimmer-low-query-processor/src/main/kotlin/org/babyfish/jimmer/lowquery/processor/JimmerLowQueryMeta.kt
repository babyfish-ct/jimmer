package org.babyfish.jimmer.lowquery.processor

internal enum class LowQueryOperator {
    EQ,
    NE,
    LIKE,
    STARTS_WITH,
    ENDS_WITH,
    GT,
    GE,
    LT,
    LE,
    IN,
    NOT_IN,
    BETWEEN,
    TIME_RANGE,
}

internal enum class LowQueryFetcher {
    ALL_SCALAR_FIELDS,
    ALL_TABLE_FIELDS,
    TABLE,
}

internal enum class LowQueryVisibility(
    val code: String,
) {
    PUBLIC("public"),
    INTERNAL("internal"),
    PRIVATE("private"),
}

internal enum class LowQueryOrderDirection {
    ASC,
    DESC,
}

internal enum class LowQueryTablePathKind {
    REFERENCE,
    COLLECTION,
}

internal enum class LowQueryExtraFetchFieldKind {
    SCALAR,
    ASSOCIATION,
}

internal data class LowQueryParamMeta(
    val propertyName: String,
    val parameterName: String,
    val typeName: String,
    val targetTypeName: String,
    val operator: LowQueryOperator,
    val nullable: Boolean,
    val tablePropertyName: String = propertyName,
    val loadedPropertyName: String = propertyName,
    val entityValuePath: List<String> = listOf(propertyName),
    val entityValuePathNullable: List<Boolean> = emptyList(),
    val sourceApplies: Boolean = false,
    val entityApplies: Boolean = true,
    val parameterAnnotations: List<String> = emptyList(),
    val endParameterName: String? = null,
    val endTypeName: String? = null,
    val expression: String? = null,
    val expressionImports: List<String> = emptyList(),
    val tablePath: List<String> = listOf(tablePropertyName),
    val tablePathKinds: List<LowQueryTablePathKind> = emptyList(),
    val tablePathImportPackages: List<String> = emptyList(),
)

internal data class LowQueryOrderMeta(
    val propertyName: String,
    val direction: LowQueryOrderDirection,
    val priority: Int,
)

internal data class LowQueryKeywordMeta(
    val propertyName: String,
    val tablePath: List<String> = listOf(propertyName),
    val tablePathKinds: List<LowQueryTablePathKind> = emptyList(),
    val tablePathImportPackages: List<String> = emptyList(),
)

internal data class LowQueryFindByFieldMeta(
    val propertyName: String,
    val parameterName: String,
    val typeName: String,
)

internal data class LowQueryFindByMeta(
    val fields: List<LowQueryFindByFieldMeta>,
    val listFunctionName: String,
    val oneFunctionName: String,
    val visibility: LowQueryVisibility,
    val fetcher: LowQueryFetcher,
    val extraFetchFields: List<LowQueryExtraFetchFieldMeta>,
)

internal data class LowQueryExtraFetchFieldMeta(
    val propertyName: String,
    val kind: LowQueryExtraFetchFieldKind,
)

internal data class LowQueryEntityMeta(
    val packageName: String,
    val simpleName: String,
    val qualifiedName: String,
    val functionName: String,
    val clientFunctionName: String,
    val visibility: LowQueryVisibility,
    val clientVisibility: LowQueryVisibility,
    val fetcher: LowQueryFetcher,
    val params: List<LowQueryParamMeta>,
    val orders: List<LowQueryOrderMeta>,
    val keywordProps: List<LowQueryKeywordMeta> = emptyList(),
    val hasIdProperty: Boolean = false,
    val findBys: List<LowQueryFindByMeta> = emptyList(),
)
