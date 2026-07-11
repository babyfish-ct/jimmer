package org.babyfish.jimmer.lowquery.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate

internal const val LOW_QUERY_ANNOTATION = "org.babyfish.jimmer.lowquery.annotation.JimmerLowQuery"
internal const val LOW_QUERY_PARAM_ANNOTATION = "org.babyfish.jimmer.lowquery.annotation.JimmerLowQueryParam"
private const val JIMMER_FIND_BY_ANNOTATION = "org.babyfish.jimmer.lowquery.annotation.JimmerFindBy"
private const val JIMMER_ENTITY_ANNOTATION = "org.babyfish.jimmer.sql.Entity"
private const val JIMMER_ID_ANNOTATION = "org.babyfish.jimmer.sql.Id"
private const val JIMMER_ID_VIEW_ANNOTATION = "org.babyfish.jimmer.sql.IdView"
private const val JIMMER_MAPPED_SUPERCLASS_ANNOTATION = "org.babyfish.jimmer.sql.MappedSuperclass"
private const val JIMMER_TRANSIENT_ANNOTATION = "org.babyfish.jimmer.sql.Transient"
private const val JIMMER_FORMULA_ANNOTATION = "org.babyfish.jimmer.Formula"
private const val ANNOTATION_PACKAGE = "org.babyfish.jimmer.lowquery.annotation"
private const val KEYWORD_ANNOTATION = "$ANNOTATION_PACKAGE.Keyword"
private const val ORDER_BY_ASC_ANNOTATION = "$ANNOTATION_PACKAGE.OrderByAsc"
private const val ORDER_BY_DESC_ANNOTATION = "$ANNOTATION_PACKAGE.OrderByDesc"
private const val TIME_RANGE_ANNOTATION = "$ANNOTATION_PACKAGE.TimeRange"
private const val CONSTANT_EQ_EXPRESSION_ANNOTATION = "expression"
private const val CONSTANT_EQ_IMPORTS_ANNOTATION = "imports"
private const val MAX_KEYWORD_ASSOCIATION_DEPTH = 2

internal data class LowQueryCollectResult(
    val entities: Set<LowQueryEntityMeta>,
    val deferred: List<KSAnnotated>,
    val hasErrors: Boolean,
)

internal class JimmerLowQueryCollector(
    private val resolver: Resolver,
    private val logger: KSPLogger,
) {
    private var hasErrors = false

    fun collect(): LowQueryCollectResult {
        val deferred = mutableListOf<KSAnnotated>()
        val entitySymbols = linkedSetOf<KSClassDeclaration>()
        resolver.getSymbolsWithAnnotation(LOW_QUERY_ANNOTATION)
            .forEach { symbol ->
                if (!symbol.validate()) {
                    deferred += symbol
                    return@forEach
                }
                val entity = symbol as? KSClassDeclaration
                if (entity == null) {
                    error("@JimmerLowQuery 只能标记 Jimmer 实体接口。", symbol)
                    return@forEach
                }
                entitySymbols += entity
            }
        collectEntitiesFromPropertyAnnotations(LOW_QUERY_FIELD_ANNOTATIONS, deferred, entitySymbols)
        collectEntitiesFromPropertyAnnotations(LOW_QUERY_ORDER_ANNOTATIONS, deferred, entitySymbols)
        collectEntitiesFromPropertyAnnotations(setOf(KEYWORD_ANNOTATION), deferred, entitySymbols)
        collectEntitiesFromPropertyAnnotations(setOf(JIMMER_FIND_BY_ANNOTATION), deferred, entitySymbols)
        val entities = entitySymbols
            .mapNotNull { entity -> collectEntity(entity) }
            .toSet()
        return LowQueryCollectResult(
            entities = entities,
            deferred = deferred,
            hasErrors = hasErrors,
        )
    }

    private fun collectEntitiesFromPropertyAnnotations(
        annotationNames: Set<String>,
        deferred: MutableList<KSAnnotated>,
        entitySymbols: MutableSet<KSClassDeclaration>,
    ) {
        annotationNames.forEach { annotationName ->
            resolver.getSymbolsWithAnnotation(annotationName)
                .forEach { symbol ->
                    if (!symbol.validate()) {
                        deferred += symbol
                        return@forEach
                    }
                    val classDeclaration = symbol as? KSClassDeclaration
                    if (classDeclaration != null) {
                        if (classDeclaration.isGeneratedSource()) {
                            return@forEach
                        }
                        val classAnnotationAllowed = annotationName == TIME_RANGE_ANNOTATION ||
                            annotationName == JIMMER_FIND_BY_ANNOTATION
                        if (!classAnnotationAllowed || !classDeclaration.hasAnnotation(JIMMER_ENTITY_ANNOTATION)) {
                            error("类级低代码查询注解只能标记 Jimmer 实体。", symbol)
                            return@forEach
                        }
                        entitySymbols += classDeclaration
                        return@forEach
                    }
                    val property = symbol as? KSPropertyDeclaration
                    if (property == null) {
                        error("低代码查询注解只能标记实体字段。", symbol)
                        return@forEach
                    }
                    val entity = property.parentDeclaration as? KSClassDeclaration
                    if (entity == null) {
                        error("低代码查询注解只能标记实体字段。", property)
                        return@forEach
                    }
                    if (!entity.hasAnnotation(JIMMER_ENTITY_ANNOTATION)) {
                        if (property.isGeneratedSource() || entity.hasAnnotation(JIMMER_MAPPED_SUPERCLASS_ANNOTATION)) {
                            return@forEach
                        }
                        error("低代码查询注解只能标记 Jimmer 实体字段。", property)
                        return@forEach
                    }
                    entitySymbols += entity
                }
        }
    }

    private fun collectEntity(entity: KSClassDeclaration): LowQueryEntityMeta? {
        if (!entity.hasAnnotation(JIMMER_ENTITY_ANNOTATION)) {
            error("@JimmerLowQuery 只能用于带 @Entity 的 Jimmer 实体。", entity)
            return null
        }
        val annotation = entity.findAnnotation(LOW_QUERY_ANNOTATION)
        val packageName = entity.packageName.asString()
        val simpleName = entity.simpleName.asString()
        val qualifiedName = entity.qualifiedName?.asString()
        if (qualifiedName.isNullOrBlank()) {
            error("无法解析实体 $simpleName 的全限定名。", entity)
            return null
        }
        val properties = entity.getAllProperties().toList()
        val lowQueryTriggered = entity.isLowQueryTriggered(properties)
        val fieldParams = properties
            .flatMap { property -> collectParams(property, properties, lowQueryTriggered) }
            .distinctBy { param -> param.parameterName }
        val params = fieldParams + collectClassParams(entity, properties, fieldParams)
        val orders = properties
            .mapNotNull { property -> collectOrder(property) }
            .sortedWith(compareBy<LowQueryOrderMeta> { it.priority }.thenBy { it.propertyName })
        val keywordProps = collectKeywordProps(entity, properties)
        val findBys = collectFindBys(entity, properties)
        if (params.isEmpty() && orders.isEmpty() && keywordProps.isEmpty() && findBys.isEmpty()) {
            error("$qualifiedName 至少需要一个 @Eq/@Like/@In、@Keyword、@JimmerLowQueryParam、@JimmerFindBy 或 @OrderByAsc/@OrderByDesc 字段。", entity)
            return null
        }
        return LowQueryEntityMeta(
            packageName = packageName,
            simpleName = simpleName,
            qualifiedName = qualifiedName,
            functionName = annotation?.stringValue("functionName")?.takeIf { it.isNotBlank() } ?: "query",
            clientFunctionName = annotation?.stringValue("clientFunctionName")?.takeIf { it.isNotBlank() }
                ?: "createLowQuery",
            visibility = annotation?.enumValue("visibility", LowQueryVisibility.PUBLIC) ?: LowQueryVisibility.PUBLIC,
            clientVisibility = annotation?.enumValue("clientVisibility", LowQueryVisibility.PUBLIC)
                ?: LowQueryVisibility.PUBLIC,
            fetcher = annotation?.enumValue("fetcher", LowQueryFetcher.ALL_SCALAR_FIELDS)
                ?: LowQueryFetcher.ALL_SCALAR_FIELDS,
            params = params,
            orders = orders,
            keywordProps = keywordProps,
            hasIdProperty = properties.any { property -> property.hasAnnotation(JIMMER_ID_ANNOTATION) },
            findBys = findBys,
        )
    }

    private fun collectParams(
        property: KSPropertyDeclaration,
        properties: List<KSPropertyDeclaration>,
        lowQueryTriggered: Boolean,
    ): List<LowQueryParamMeta> {
        val fieldAnnotations = property.findLowQueryFieldAnnotations()
        if (fieldAnnotations.size > 1) {
            error("字段 ${property.simpleName.asString()} 只能标记一个低代码查询 where 注解。", property)
            return emptyList()
        }
        if (property.isCalculatedProperty()) {
            if (fieldAnnotations.isNotEmpty()) {
                error("Jimmer @Transient/@Formula 计算字段不能参与低代码查询。", property)
            }
            return emptyList()
        }
        val annotation = fieldAnnotations.singleOrNull()
        if (annotation != null) {
            return listOfNotNull(collectAnnotatedParam(property, annotation))
        }
        if (!lowQueryTriggered) {
            return emptyList()
        }
        return collectDefaultParams(property, properties)
    }

    private fun collectFindBys(
        entity: KSClassDeclaration,
        properties: List<KSPropertyDeclaration>,
    ): List<LowQueryFindByMeta> {
        val byProperty = properties.mapNotNull { property ->
            val annotation = property.findAnnotation(JIMMER_FIND_BY_ANNOTATION) ?: return@mapNotNull null
            collectFindBy(entity, properties, property, annotation)
        }
        val byClass = entity.annotations.flatMap { annotation ->
            val qualifiedName = annotation.annotationType.resolve().declaration.qualifiedName?.asString()
            if (qualifiedName != JIMMER_FIND_BY_ANNOTATION) {
                return@flatMap emptyList()
            }
            collectClassFindBys(entity, properties, annotation)
        }
        return (byProperty + byClass)
            .distinctBy { findBy -> findBy.listFunctionName to findBy.oneFunctionName }
    }

    private fun collectClassFindBys(
        entity: KSClassDeclaration,
        properties: List<KSPropertyDeclaration>,
        annotation: KSAnnotation,
    ): List<LowQueryFindByMeta> {
        val fieldNames = annotation.stringArrayValue("fields")
        if (fieldNames.isEmpty()) {
            error("类级 @JimmerFindBy 必须通过 fields 指定实体字段。", entity)
            return emptyList()
        }
        val composite = annotation.booleanValue("composite") == true
        if (fieldNames.size > 1 && !composite && annotation.hasCustomFindByName()) {
            error("类级 @JimmerFindBy 一次指定多个字段并分别生成时不能覆盖 name、listFunctionName 或 oneFunctionName。", entity)
            return emptyList()
        }
        if (fieldNames.size > 1 && composite && !annotation.stringValue("name").isNullOrBlank()) {
            error("类级 @JimmerFindBy 生成组合查询时不能用 name 覆盖多个参数名。", entity)
            return emptyList()
        }
        val findByFields = collectFindByFields(entity, properties, fieldNames, annotation)
        if (findByFields.size != fieldNames.size) {
            return emptyList()
        }
        if (composite) {
            return listOfNotNull(collectFindBy(entity, properties, findByFields, annotation, entity))
        }
        return findByFields.mapNotNull { field -> collectFindBy(entity, properties, listOf(field), annotation, entity) }
    }

    private fun collectFindBy(
        entity: KSClassDeclaration,
        properties: List<KSPropertyDeclaration>,
        property: KSPropertyDeclaration,
        annotation: KSAnnotation,
    ): LowQueryFindByMeta? {
        val field = collectFindByField(property, annotation) ?: return null
        return collectFindBy(entity, properties, listOf(field), annotation, property)
    }

    private fun collectFindBy(
        entity: KSClassDeclaration,
        properties: List<KSPropertyDeclaration>,
        fields: List<LowQueryFindByFieldMeta>,
        annotation: KSAnnotation,
        symbol: KSAnnotated,
    ): LowQueryFindByMeta? {
        if (fields.isEmpty()) {
            return null
        }
        val extraFetchFieldNames = annotation.stringArrayValue("extraFetchFields")
        val fetcher = annotation.enumValue("fetcher", LowQueryFetcher.ALL_SCALAR_FIELDS)
        if (fetcher == LowQueryFetcher.TABLE && extraFetchFieldNames.isNotEmpty()) {
            error("@JimmerFindBy(fetcher = TABLE) 不能再配置 extraFetchFields。", symbol)
            return null
        }
        val extraFetchFields = collectExtraFetchFields(properties, extraFetchFieldNames, symbol)
        if (extraFetchFields.size != extraFetchFieldNames.size) {
            return null
        }
        val entitySimpleName = entity.simpleName.asString()
        val propertySuffix = fields.joinToString("And") { field -> field.propertyName.replaceFirstChar(Char::uppercase) }
        return LowQueryFindByMeta(
            fields = fields,
            listFunctionName = annotation.stringValue("listFunctionName")?.takeIf { it.isNotBlank() }
                ?: "find${entitySimpleName}By$propertySuffix",
            oneFunctionName = annotation.stringValue("oneFunctionName")?.takeIf { it.isNotBlank() }
                ?: "findOneOrNull${entitySimpleName}By$propertySuffix",
            visibility = annotation.enumValue("visibility", LowQueryVisibility.PUBLIC),
            fetcher = fetcher,
            extraFetchFields = extraFetchFields,
        )
    }

    private fun collectExtraFetchFields(
        properties: List<KSPropertyDeclaration>,
        fieldNames: List<String>,
        symbol: KSAnnotated,
    ): List<LowQueryExtraFetchFieldMeta> = fieldNames.mapNotNull { fieldName ->
        val property = properties.firstOrNull { property -> property.simpleName.asString() == fieldName }
        if (property == null) {
            error("@JimmerFindBy extraFetchFields 指定的字段不存在：$fieldName", symbol)
            return@mapNotNull null
        }
        val kind = if (property.type.resolve().isAssociationType()) {
            LowQueryExtraFetchFieldKind.ASSOCIATION
        } else {
            LowQueryExtraFetchFieldKind.SCALAR
        }
        LowQueryExtraFetchFieldMeta(
            propertyName = fieldName,
            kind = kind,
        )
    }

    private fun collectFindByFields(
        entity: KSClassDeclaration,
        properties: List<KSPropertyDeclaration>,
        fieldNames: List<String>,
        annotation: KSAnnotation,
    ): List<LowQueryFindByFieldMeta> = fieldNames.mapNotNull { fieldName ->
        val property = properties.firstOrNull { property -> property.simpleName.asString() == fieldName }
        if (property == null) {
            error("类级 @JimmerFindBy 指定的字段不存在：$fieldName", entity)
            return@mapNotNull null
        }
        collectFindByField(property, annotation)
    }

    private fun collectFindByField(
        property: KSPropertyDeclaration,
        annotation: KSAnnotation,
    ): LowQueryFindByFieldMeta? {
        val propertyName = property.simpleName.asString()
        if (property.isCalculatedProperty()) {
            error("Jimmer @Transient/@Formula 计算字段不能生成辅助查询方法。", property)
            return null
        }
        val type = property.type.resolve()
        if (!type.isScalarType()) {
            error("@JimmerFindBy 只能用于标量字段：$propertyName", property)
            return null
        }
        val parameterName = annotation.stringValue("name")?.takeIf { it.isNotBlank() } ?: propertyName
        return LowQueryFindByFieldMeta(
            propertyName = propertyName,
            parameterName = parameterName,
            typeName = type.renderType(forceNullable = false, preserveNullable = false),
        )
    }

    private fun collectAnnotatedParam(
        property: KSPropertyDeclaration,
        annotation: KSAnnotation,
    ): LowQueryParamMeta? {
        val propertyName = property.simpleName.asString()
        val operator = annotation.lowQueryOperator()
        val type = property.type.resolve()
        val targetFieldName = annotation.stringValue("targetField")?.takeIf { it.isNotBlank() }
        val nullable = type.isMarkedNullable ||
            annotation.booleanValue("nullable") != false ||
            operator == LowQueryOperator.BETWEEN ||
            operator == LowQueryOperator.TIME_RANGE
        val expression = if (operator == LowQueryOperator.EQ) {
            annotation.stringValue(CONSTANT_EQ_EXPRESSION_ANNOTATION)?.takeIf { it.isNotBlank() }
        } else {
            null
        }
        val expressionImports = if (expression == null) {
            emptyList()
        } else {
            annotation.stringArrayValue(CONSTANT_EQ_IMPORTS_ANNOTATION)
        }
        if (targetFieldName != null) {
            return collectAnnotatedParamWithTargetField(property, annotation, propertyName, operator, targetFieldName, nullable, expression, expressionImports)
        }
        val scalarTypeName = type.renderType(forceNullable = false, preserveNullable = false)
        val parameterName = annotation.parameterName(propertyName, operator)
        val endParameterName = annotation.endParameterName(propertyName, operator)
        return LowQueryParamMeta(
            propertyName = propertyName,
            parameterName = parameterName,
            typeName = type.renderParamType(operator, nullable),
            targetTypeName = scalarTypeName,
            operator = operator,
            nullable = nullable,
            sourceApplies = true,
            parameterAnnotations = property.renderParameterAnnotations(),
            endParameterName = endParameterName,
            endTypeName = if (endParameterName != null) {
                type.renderParamType(operator, nullable)
            } else {
                null
            },
            expression = expression,
            expressionImports = expressionImports,
        )
    }

    /**
     * 处理 targetField 穿透：将关联属性上的查询注解逐传到目标实体的标量字段。
     */
    private fun collectAnnotatedParamWithTargetField(
        property: KSPropertyDeclaration,
        annotation: KSAnnotation,
        propertyName: String,
        operator: LowQueryOperator,
        targetFieldName: String,
        nullable: Boolean,
        expression: String?,
        expressionImports: List<String>,
    ): LowQueryParamMeta? {
        val associationTarget = property.type.resolve().resolveAssociationTarget() ?: run {
            error("targetField 只能在关联属性（@ManyToOne/@OneToMany）上使用，${propertyName} 不是关联属性。", property)
            return null
        }
        val targetEntity = associationTarget.first
        val pathKind = associationTarget.second
        val targetProperties = targetEntity.getAllProperties().toList()
        val targetProperty = targetProperties.firstOrNull { prop -> prop.simpleName.asString() == targetFieldName }
        if (targetProperty == null) {
            error("targetField \"$targetFieldName\" 在实体 ${targetEntity.simpleName.asString()} 中不存在。", property)
            return null
        }
        if (targetProperty.isCalculatedProperty()) {
            error("targetField \"$targetFieldName\" 不能是 @Transient/@Formula 计算字段。", property)
            return null
        }
        val targetType = targetProperty.type.resolve()
        if (!targetType.isScalarType()) {
            error("targetField \"$targetFieldName\" 必须是标量字段，不能是关联属性。", property)
            return null
        }
        val scalarTypeName = targetType.renderType(forceNullable = false, preserveNullable = false)
        val parameterName = annotation.parameterName(targetFieldName, operator)
        val endParameterName = annotation.endParameterName(targetFieldName, operator)
        val parameterAnnotations = targetProperty.renderParameterAnnotations()
        val scalarTablePath = listOf(propertyName, targetFieldName)
        val scalarTablePathKinds = listOf(pathKind)
        val scalarTablePathImportPackages = listOf(
            property.packageName.asString(),
            targetEntity.packageName.asString(),
        )
        val associationTypeIsNullable = property.type.resolve().isMarkedNullable
        val targetFieldIsNullable = targetType.isMarkedNullable
        val loadedPropertyName = if (pathKind == LowQueryTablePathKind.REFERENCE) propertyName else targetFieldName
        val entityValuePath: List<String>
        val entityValuePathNullable: List<Boolean>
        val entityApplies: Boolean
        if (pathKind == LowQueryTablePathKind.REFERENCE) {
            entityValuePath = listOf(propertyName, targetFieldName)
            entityValuePathNullable = listOf(associationTypeIsNullable, targetFieldIsNullable)
            entityApplies = true
        } else {
            entityValuePath = listOf(targetFieldName)
            entityValuePathNullable = listOf(targetFieldIsNullable)
            entityApplies = false
        }
        return LowQueryParamMeta(
            propertyName = targetFieldName,
            parameterName = parameterName,
            typeName = targetType.renderParamType(operator, nullable),
            targetTypeName = scalarTypeName,
            operator = operator,
            nullable = nullable,
            tablePropertyName = targetFieldName,
            loadedPropertyName = loadedPropertyName,
            entityValuePath = entityValuePath,
            entityValuePathNullable = entityValuePathNullable,
            sourceApplies = true,
            entityApplies = entityApplies,
            parameterAnnotations = parameterAnnotations,
            endParameterName = endParameterName,
            endTypeName = if (endParameterName != null) {
                targetType.renderParamType(operator, nullable)
            } else {
                null
            },
            expression = expression,
            expressionImports = expressionImports,
            tablePath = scalarTablePath,
            tablePathKinds = scalarTablePathKinds,
            tablePathImportPackages = scalarTablePathImportPackages,
        )
    }

    private fun collectDefaultParams(
        property: KSPropertyDeclaration,
        properties: List<KSPropertyDeclaration>,
    ): List<LowQueryParamMeta> {
        val propertyName = property.simpleName.asString()
        if (propertyName in DEFAULT_QUERY_EXCLUDED_PROPERTIES) {
            return emptyList()
        }
        if (property.hasAnnotation(JIMMER_ID_VIEW_ANNOTATION)) {
            return emptyList()
        }
        val type = property.type.resolve()
        if (type.isScalarType()) {
            return listOf(property.toDefaultScalarParam(type))
        }
        return property.toDefaultAssociationIdParams(type, properties)
    }

    private fun KSPropertyDeclaration.toDefaultScalarParam(type: KSType): LowQueryParamMeta {
        val propertyName = simpleName.asString()
        val scalarTypeName = type.renderType(forceNullable = false, preserveNullable = false)
        return LowQueryParamMeta(
            propertyName = propertyName,
            parameterName = propertyName,
            typeName = type.renderParamType(LowQueryOperator.EQ, forceNullable = true),
            targetTypeName = scalarTypeName,
            operator = LowQueryOperator.EQ,
            nullable = true,
            sourceApplies = true,
            parameterAnnotations = renderParameterAnnotations(),
        )
    }

    private fun KSPropertyDeclaration.toDefaultAssociationIdParams(
        type: KSType,
        properties: List<KSPropertyDeclaration>,
    ): List<LowQueryParamMeta> {
        val associationTarget = type.resolveAssociationTarget() ?: return emptyList()
        val targetEntity = associationTarget.first
        val pathKind = associationTarget.second
        val idProperty = targetEntity.getAllProperties().firstOrNull { targetProperty ->
            targetProperty.hasAnnotation(JIMMER_ID_ANNOTATION)
        } ?: return emptyList()
        val idType = idProperty.type.resolve()
        val propertyName = simpleName.asString()
        val idViewPropertyName = findIdViewPropertyName(propertyName, properties)
        val associatedIdName = if (pathKind == LowQueryTablePathKind.COLLECTION) {
            idViewPropertyName?.singularAssociatedIdName() ?: "${propertyName.removeSuffix("s")}Id"
        } else {
            idViewPropertyName ?: "${propertyName}Id"
        }
        val scalarTablePath = listOf(propertyName, idProperty.simpleName.asString())
        val scalarTablePathKinds = listOf(pathKind)
        val scalarTablePathImportPackages = listOf(
            packageName.asString(),
            targetEntity.packageName.asString(),
        )
        val scalarParam = LowQueryParamMeta(
            propertyName = associatedIdName,
            parameterName = associatedIdName,
            typeName = idType.renderParamType(LowQueryOperator.EQ, forceNullable = true),
            targetTypeName = idType.renderType(forceNullable = false, preserveNullable = false),
            operator = LowQueryOperator.EQ,
            nullable = true,
            tablePropertyName = associatedIdName,
            loadedPropertyName = if (pathKind == LowQueryTablePathKind.REFERENCE) propertyName else associatedIdName,
            entityValuePath = if (pathKind == LowQueryTablePathKind.REFERENCE) {
                listOf(propertyName, idProperty.simpleName.asString())
            } else {
                listOf(associatedIdName)
            },
            entityValuePathNullable = if (pathKind == LowQueryTablePathKind.REFERENCE) {
                listOf(type.isMarkedNullable, idType.isMarkedNullable)
            } else {
                emptyList()
            },
            sourceApplies = true,
            entityApplies = pathKind == LowQueryTablePathKind.REFERENCE,
            tablePath = scalarTablePath,
            tablePathKinds = scalarTablePathKinds,
            tablePathImportPackages = scalarTablePathImportPackages,
        )
        if (pathKind == LowQueryTablePathKind.REFERENCE) {
            return listOf(scalarParam)
        }
        val collectionParamName = associatedIdName.defaultParameterName(LowQueryOperator.IN)
        val collectionParam = scalarParam.copy(
            propertyName = collectionParamName,
            parameterName = collectionParamName,
            typeName = idType.renderParamType(LowQueryOperator.IN, forceNullable = true),
            operator = LowQueryOperator.IN,
            loadedPropertyName = collectionParamName,
            entityValuePath = listOf(collectionParamName),
            entityValuePathNullable = emptyList(),
            entityApplies = false,
        )
        return listOf(scalarParam, collectionParam)
    }

    private fun String.singularAssociatedIdName(): String {
        if (endsWith("Ids") && length > 3) {
            return removeSuffix("s")
        }
        if (endsWith("s") && length > 1) {
            return removeSuffix("s")
        }
        return this
    }

    private fun findIdViewPropertyName(
        associationPropertyName: String,
        properties: List<KSPropertyDeclaration>,
    ): String? {
        return properties.firstOrNull { property ->
            val annotation = property.findAnnotation(JIMMER_ID_VIEW_ANNOTATION) ?: return@firstOrNull false
            annotation.stringValue("value") == associationPropertyName
        }?.simpleName?.asString()
    }

    private fun collectClassParams(
        entity: KSClassDeclaration,
        properties: List<KSPropertyDeclaration>,
        fieldParams: List<LowQueryParamMeta>,
    ): List<LowQueryParamMeta> {
        return entity.annotations.mapNotNull { annotation ->
            val qualifiedName = annotation.annotationType.resolve().declaration.qualifiedName?.asString()
            if (qualifiedName != TIME_RANGE_ANNOTATION) {
                return@mapNotNull null
            }
            val propertyName = annotation.stringValue("name")?.takeIf { it.isNotBlank() }
            if (propertyName == null) {
                error("类级 @TimeRange 必须通过 name 指定实体字段。", entity)
                return@mapNotNull null
            }
            if (fieldParams.any { param -> param.propertyName == propertyName }) {
                return@mapNotNull null
            }
            val property = properties.firstOrNull { property -> property.simpleName.asString() == propertyName }
            if (property == null) {
                error("类级 @TimeRange 指定的字段不存在：$propertyName", entity)
                return@mapNotNull null
            }
            if (property.isCalculatedProperty()) {
                error("类级 @TimeRange 不能指定 Jimmer @Transient/@Formula 计算字段：$propertyName", entity)
                return@mapNotNull null
            }
            val type = property.type.resolve()
            LowQueryParamMeta(
                propertyName = propertyName,
                parameterName = propertyName,
                typeName = type.renderParamType(LowQueryOperator.TIME_RANGE, forceNullable = true),
                targetTypeName = type.renderType(forceNullable = false, preserveNullable = false),
                operator = LowQueryOperator.TIME_RANGE,
                nullable = true,
                parameterAnnotations = property.renderParameterAnnotations(),
            )
        }.toList()
    }

    private fun collectOrder(property: KSPropertyDeclaration): LowQueryOrderMeta? {
        val orderAnnotations = property.findLowQueryOrderAnnotations()
        if (orderAnnotations.size > 1) {
            error("字段 ${property.simpleName.asString()} 只能标记一个低代码查询排序注解。", property)
            return null
        }
        val annotation = orderAnnotations.singleOrNull() ?: return null
        if (property.isCalculatedProperty()) {
            error("Jimmer @Transient/@Formula 计算字段不能参与低代码查询排序。", property)
            return null
        }
        val qualifiedName = annotation.annotationType.resolve().declaration.qualifiedName?.asString()
        val direction = when (qualifiedName) {
            ORDER_BY_ASC_ANNOTATION -> LowQueryOrderDirection.ASC
            ORDER_BY_DESC_ANNOTATION -> LowQueryOrderDirection.DESC
            else -> return null
        }
        return LowQueryOrderMeta(
            propertyName = property.simpleName.asString(),
            direction = direction,
            priority = annotation.intValue("priority") ?: 0,
        )
    }

    private fun collectKeywordProps(
        entity: KSClassDeclaration,
        properties: List<KSPropertyDeclaration>,
    ): List<LowQueryKeywordMeta> {
        val directProps = properties.mapNotNull { property -> collectDirectKeywordProp(property) }
        val collectNestedAssociatedProps = directProps.isEmpty()
        val associatedProps = properties.flatMap { property ->
            collectAssociatedKeywordProps(entity, property, collectNestedAssociatedProps)
        }
        return (directProps + associatedProps)
            .distinctBy { keyword -> keyword.tablePath }
    }

    private fun collectDirectKeywordProp(property: KSPropertyDeclaration): LowQueryKeywordMeta? {
        if (!property.hasKeywordAnnotation()) {
            return null
        }
        val propertyName = property.simpleName.asString()
        if (!property.validateKeywordProp(propertyName)) {
            return null
        }
        return LowQueryKeywordMeta(propertyName = propertyName)
    }

    private fun collectAssociatedKeywordProps(
        entity: KSClassDeclaration,
        property: KSPropertyDeclaration,
        collectNestedAssociatedProps: Boolean,
    ): List<LowQueryKeywordMeta> {
        val entityName = entity.qualifiedName?.asString().orEmpty()
        return collectAssociatedKeywordProps(
            property = property,
            visitedEntityNames = setOf(entityName),
            tablePathPrefix = emptyList(),
            tablePathKindPrefix = emptyList(),
            tablePathImportPackagePrefix = emptyList(),
            collectNestedAssociatedProps = collectNestedAssociatedProps,
        )
    }

    private fun collectAssociatedKeywordProps(
        property: KSPropertyDeclaration,
        visitedEntityNames: Set<String>,
        tablePathPrefix: List<String>,
        tablePathKindPrefix: List<LowQueryTablePathKind>,
        tablePathImportPackagePrefix: List<String>,
        collectNestedAssociatedProps: Boolean,
    ): List<LowQueryKeywordMeta> {
        if (property.isCalculatedProperty()) {
            return emptyList()
        }
        val association = property.type.resolve().resolveAssociationTarget() ?: return emptyList()
        val targetEntity = association.first
        val targetEntityName = targetEntity.qualifiedName?.asString().orEmpty()
        if (targetEntityName in visitedEntityNames) {
            return emptyList()
        }
        val associationDepth = tablePathKindPrefix.size + 1
        if (associationDepth > MAX_KEYWORD_ASSOCIATION_DEPTH) {
            return emptyList()
        }
        val associationName = property.simpleName.asString()
        val tablePath = tablePathPrefix + associationName
        val tablePathKinds = tablePathKindPrefix + association.second
        val tablePathImportPackages = tablePathImportPackagePrefix + property.packageName.asString()
        val nextVisitedEntityNames = visitedEntityNames + targetEntityName
        val properties = targetEntity.getAllProperties().toList()
        val directProps = properties.mapNotNull { targetProperty ->
            if (!targetProperty.hasKeywordAnnotation()) {
                return@mapNotNull null
            }
            val targetPropertyName = targetProperty.simpleName.asString()
            if (!targetProperty.validateKeywordProp(targetPropertyName)) {
                return@mapNotNull null
            }
            val keywordTablePath = tablePath + targetPropertyName
            LowQueryKeywordMeta(
                propertyName = keywordTablePath.joinToString("."),
                tablePath = keywordTablePath,
                tablePathKinds = tablePathKinds,
                tablePathImportPackages = tablePathImportPackages + targetEntity.packageName.asString(),
            )
        }
        if (associationDepth == MAX_KEYWORD_ASSOCIATION_DEPTH) {
            return directProps
        }
        if (!collectNestedAssociatedProps || directProps.isEmpty()) {
            return directProps
        }
        val nestedProps = properties.flatMap { targetProperty ->
            collectAssociatedKeywordProps(
                property = targetProperty,
                visitedEntityNames = nextVisitedEntityNames,
                tablePathPrefix = tablePath,
                tablePathKindPrefix = tablePathKinds,
                tablePathImportPackagePrefix = tablePathImportPackages,
                collectNestedAssociatedProps = false,
            )
        }
        return directProps + nestedProps
    }

    private fun KSPropertyDeclaration.validateKeywordProp(propertyName: String): Boolean {
        if (isCalculatedProperty()) {
            error("Jimmer @Transient/@Formula 计算字段不能参与 keyword 查询。", this)
            return false
        }
        val type = this.type.resolve()
        if (!type.isStringType()) {
            error("@Keyword 只能标记 String 字段：$propertyName", this)
            return false
        }
        return true
    }

    private fun KSPropertyDeclaration.hasKeywordAnnotation(): Boolean {
        if (hasAnnotation(KEYWORD_ANNOTATION)) {
            return true
        }
        return getter?.hasAnnotation(KEYWORD_ANNOTATION) == true
    }

    private fun KSType.resolveAssociationTarget(): Pair<KSClassDeclaration, LowQueryTablePathKind>? {
        val directDeclaration = declaration as? KSClassDeclaration
        if (directDeclaration?.hasAnnotation(JIMMER_ENTITY_ANNOTATION) == true) {
            return directDeclaration to LowQueryTablePathKind.REFERENCE
        }
        val targetEntity = arguments.firstNotNullOfOrNull { argument ->
            val argumentDeclaration = argument.type?.resolve()?.declaration as? KSClassDeclaration
            argumentDeclaration?.takeIf { declaration -> declaration.hasAnnotation(JIMMER_ENTITY_ANNOTATION) }
        } ?: return null
        return targetEntity to LowQueryTablePathKind.COLLECTION
    }

    private fun KSAnnotated.findLowQueryFieldAnnotations(): List<KSAnnotation> {
        return annotations.filter { annotation ->
            annotation.annotationType.resolve().declaration.qualifiedName?.asString() in LOW_QUERY_FIELD_ANNOTATIONS
        }.toList()
    }

    private fun KSAnnotated.findLowQueryOrderAnnotations(): List<KSAnnotation> {
        return annotations.filter { annotation ->
            annotation.annotationType.resolve().declaration.qualifiedName?.asString() in LOW_QUERY_ORDER_ANNOTATIONS
        }.toList()
    }

    private fun KSClassDeclaration.isLowQueryTriggered(properties: List<KSPropertyDeclaration>): Boolean {
        if (hasAnnotation(LOW_QUERY_ANNOTATION)) {
            return true
        }
        if (hasAnnotation(TIME_RANGE_ANNOTATION)) {
            return true
        }
        return properties.any { property ->
            property.findLowQueryFieldAnnotations().isNotEmpty() ||
                property.findLowQueryOrderAnnotations().isNotEmpty() ||
                property.hasKeywordAnnotation()
        }
    }

    private fun KSAnnotated.findAnnotation(qualifiedName: String): KSAnnotation? {
        return annotations.firstOrNull { annotation ->
            annotation.annotationType.resolve().declaration.qualifiedName?.asString() == qualifiedName
        }
    }

    private fun KSAnnotated.hasAnnotation(qualifiedName: String): Boolean {
        return findAnnotation(qualifiedName) != null
    }

    private fun KSPropertyDeclaration.isCalculatedProperty(): Boolean {
        return hasAnnotation(JIMMER_TRANSIENT_ANNOTATION) ||
            hasAnnotation(JIMMER_FORMULA_ANNOTATION) ||
            getter?.hasAnnotation(JIMMER_TRANSIENT_ANNOTATION) == true ||
            getter?.hasAnnotation(JIMMER_FORMULA_ANNOTATION) == true
    }

    private fun KSAnnotated.isGeneratedSource(): Boolean {
        val path = when (this) {
            is KSClassDeclaration -> containingFile?.filePath
            is KSPropertyDeclaration -> containingFile?.filePath
            else -> null
        } ?: return false
        return path.contains("/build/generated/") || path.contains("\\build\\generated\\")
    }

    private fun KSAnnotation.stringValue(name: String): String? {
        val namedValue = arguments.firstOrNull { it.name?.asString() == name }?.value as? String
        if (namedValue != null) {
            return namedValue
        }
        if (name != "value") {
            return null
        }
        return arguments.firstOrNull { it.name == null }?.value as? String
    }

    private fun KSAnnotation.booleanValue(name: String): Boolean? {
        return arguments.firstOrNull { it.name?.asString() == name }?.value as? Boolean
    }

    private fun KSAnnotation.intValue(name: String): Int? {
        return arguments.firstOrNull { it.name?.asString() == name }?.value as? Int
    }

    private fun KSAnnotation.stringArrayValue(name: String): List<String> {
        val value = arguments.firstOrNull { it.name?.asString() == name }?.value
        val values = value as? List<*> ?: return emptyList()
        return values.filterIsInstance<String>().filter { item -> item.isNotBlank() }
    }

    private fun KSAnnotation.hasCustomFindByName(): Boolean {
        return listOf("name", "listFunctionName", "oneFunctionName").any { name ->
            !stringValue(name).isNullOrBlank()
        }
    }

    private inline fun <reified E : Enum<E>> KSAnnotation.enumValue(name: String, defaultValue: E): E {
        val value = arguments.firstOrNull { it.name?.asString() == name }?.value?.toString()
        return enumValues<E>().firstOrNull { it.name == value } ?: defaultValue
    }

    private fun KSAnnotation.lowQueryOperator(): LowQueryOperator {
        val qualifiedName = annotationType.resolve().declaration.qualifiedName?.asString()
        if (qualifiedName == LOW_QUERY_PARAM_ANNOTATION) {
            return enumValue("operator", LowQueryOperator.EQ)
        }
        return LOW_QUERY_OPERATOR_BY_ANNOTATION.getValue(checkNotNull(qualifiedName))
    }

    private fun KSAnnotation.parameterName(
        propertyName: String,
        operator: LowQueryOperator,
    ): String {
        val annotationName = when (operator) {
            LowQueryOperator.BETWEEN -> "startName"
            else -> "name"
        }
        return stringValue(annotationName)
            ?.takeIf { it.isNotBlank() }
            ?: propertyName.defaultParameterName(operator)
    }

    private fun KSAnnotation.endParameterName(
        propertyName: String,
        operator: LowQueryOperator,
    ): String? {
        if (operator != LowQueryOperator.BETWEEN) {
            return null
        }
        return stringValue("endName")
            ?.takeIf { it.isNotBlank() }
            ?: "${propertyName}End"
    }

    private fun String.defaultParameterName(operator: LowQueryOperator): String {
        return when (operator) {
            LowQueryOperator.IN,
            LowQueryOperator.NOT_IN -> {
                if (endsWith("s")) {
                    this
                } else {
                    "${this}s"
                }
            }
            LowQueryOperator.BETWEEN -> "${this}Start"
            else -> this
        }
    }


    private fun KSType.isScalarType(): Boolean {
        if (declaration is KSClassDeclaration && (declaration as KSClassDeclaration).hasAnnotation(JIMMER_ENTITY_ANNOTATION)) {
            return false
        }
        if (arguments.isNotEmpty()) {
            return false
        }
        val qualifiedName = declaration.qualifiedName?.asString() ?: return false
        if (qualifiedName in DEFAULT_QUERY_SCALAR_TYPES) {
            return true
        }
        return declaration.packageName.asString() == "java.time" ||
            declaration.packageName.asString() == "java.math" ||
            (declaration as? KSClassDeclaration)?.classKind?.name == "ENUM_CLASS"
    }

    private fun KSType.isStringType(): Boolean {
        val qualifiedName = declaration.qualifiedName?.asString() ?: return false
        return qualifiedName == "kotlin.String" || qualifiedName == "java.lang.String"
    }

    private fun KSType.isAssociationType(): Boolean {
        val directDeclaration = declaration as? KSClassDeclaration
        if (directDeclaration?.hasAnnotation(JIMMER_ENTITY_ANNOTATION) == true) {
            return true
        }
        return arguments.any { argument ->
            val argumentDeclaration = argument.type?.resolve()?.declaration as? KSClassDeclaration
            argumentDeclaration?.hasAnnotation(JIMMER_ENTITY_ANNOTATION) == true
        }
    }

    private fun KSType.renderParamType(operator: LowQueryOperator, forceNullable: Boolean): String {
        val scalarType = renderType(forceNullable = false, preserveNullable = false)
        if (operator == LowQueryOperator.IN || operator == LowQueryOperator.NOT_IN) {
            val nullableSuffix = if (forceNullable) "?" else ""
            return "Collection<$scalarType>$nullableSuffix"
        }
        if (operator == LowQueryOperator.BETWEEN) {
            return renderType(forceNullable = true, preserveNullable = false)
        }
        if (operator == LowQueryOperator.TIME_RANGE) {
            return "Array<$scalarType>?"
        }
        return renderType(forceNullable = forceNullable, preserveNullable = true)
    }

    private fun KSPropertyDeclaration.renderParameterAnnotations(): List<String> {
        val allAnnotations = annotations.toList() + getter?.annotations.orEmpty().toList()
        return allAnnotations.mapNotNull { annotation ->
            annotation.renderParameterAnnotation()
        }
    }

    private fun KSAnnotation.renderParameterAnnotation(): String? {
        return null
    }

    private fun KSType.renderType(forceNullable: Boolean, preserveNullable: Boolean): String {
        val declaration = declaration
        val qualifiedName = declaration.qualifiedName?.asString()
        val baseName = when {
            qualifiedName == null -> toString().removeSuffix("?")
            declaration.packageName.asString() == "kotlin" -> declaration.simpleName.asString()
            else -> qualifiedName
        }
        val typeArguments = arguments
            .mapNotNull { it.type?.resolve()?.renderType(forceNullable = false, preserveNullable = true) }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "<", postfix = ">")
            ?: ""
        val nullableSuffix = if (forceNullable || preserveNullable && isMarkedNullable) "?" else ""
        return "$baseName$typeArguments$nullableSuffix"
    }

    private fun String.escapeString(): String =
        replace("\\", "\\\\")
            .replace("\"", "\\\"")

    private fun error(message: String, symbol: KSAnnotated) {
        hasErrors = true
        logger.error(message, symbol)
    }

    private companion object {
        private val LOW_QUERY_OPERATOR_BY_ANNOTATION = mapOf(
            "$ANNOTATION_PACKAGE.Eq" to LowQueryOperator.EQ,
            "$ANNOTATION_PACKAGE.Ne" to LowQueryOperator.NE,
            "$ANNOTATION_PACKAGE.Like" to LowQueryOperator.LIKE,
            "$ANNOTATION_PACKAGE.StartsWith" to LowQueryOperator.STARTS_WITH,
            "$ANNOTATION_PACKAGE.EndsWith" to LowQueryOperator.ENDS_WITH,
            "$ANNOTATION_PACKAGE.Gt" to LowQueryOperator.GT,
            "$ANNOTATION_PACKAGE.Ge" to LowQueryOperator.GE,
            "$ANNOTATION_PACKAGE.Lt" to LowQueryOperator.LT,
            "$ANNOTATION_PACKAGE.Le" to LowQueryOperator.LE,
            "$ANNOTATION_PACKAGE.In" to LowQueryOperator.IN,
            "$ANNOTATION_PACKAGE.NotIn" to LowQueryOperator.NOT_IN,
            "$ANNOTATION_PACKAGE.Between" to LowQueryOperator.BETWEEN,
            "$ANNOTATION_PACKAGE.TimeRange" to LowQueryOperator.TIME_RANGE,
        )

        private val LOW_QUERY_FIELD_ANNOTATIONS =
            LOW_QUERY_OPERATOR_BY_ANNOTATION.keys + LOW_QUERY_PARAM_ANNOTATION

        private val LOW_QUERY_ORDER_ANNOTATIONS = setOf(
            ORDER_BY_ASC_ANNOTATION,
            ORDER_BY_DESC_ANNOTATION,
        )

        private val DEFAULT_QUERY_EXCLUDED_PROPERTIES = setOf(
            "id",
            "createTime",
            "updateTime",
            "updater",
            "updaterId",
            "creator",
            "creatorId",
            "deleted",
            "deletedTime",
            "tenantId",
        )

        private val DEFAULT_QUERY_SCALAR_TYPES = setOf(
            "kotlin.String",
            "kotlin.Boolean",
            "kotlin.Byte",
            "kotlin.Short",
            "kotlin.Int",
            "kotlin.Long",
            "kotlin.Float",
            "kotlin.Double",
            "kotlin.UByte",
            "kotlin.UShort",
            "kotlin.UInt",
            "kotlin.ULong",
            "java.lang.String",
            "java.lang.Boolean",
            "java.lang.Byte",
            "java.lang.Short",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Float",
            "java.lang.Double",
        )
    }
}
