package org.babyfish.jimmer.lowquery.processor

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies

class JimmerLowQueryGeneratorTest {
    @Test
    fun `generator emits typed query extension`() {
        val codeGenerator = RecordingCodeGenerator()
        val entity = LowQueryEntityMeta(
            packageName = "demo.system",
            simpleName = "SystemConfig",
            qualifiedName = "demo.system.SystemConfig",
            functionName = "query",
            clientFunctionName = "createLowQuery",
            visibility = LowQueryVisibility.PRIVATE,
            clientVisibility = LowQueryVisibility.PUBLIC,
            fetcher = LowQueryFetcher.ALL_SCALAR_FIELDS,
            params = listOf(
                LowQueryParamMeta(
                    propertyName = "configKey",
                    parameterName = "key",
                    typeName = "String",
                    targetTypeName = "String",
                    operator = LowQueryOperator.EQ,
                    nullable = false,
                ),
            ),
            orders = emptyList(),
        )

        JimmerLowQueryGenerator(codeGenerator).generate(setOf(entity), generatedPackage = null)

        val code = codeGenerator.generated.values.single().toString(Charsets.UTF_8.name())
        assertContains(code, "package demo.system.generated.lowquery")
        assertContains(code, "import demo.system.configKey")
        assertContains(code, "@JvmName(\"queryForSystemConfigLowQuery\")")
        assertContains(code, "private fun KMutableRootQuery.ForEntity<SystemConfig>.query(")
        assertContains(code, "key: String")
        assertContains(code, "applySystemConfigLowQueryParams(key = key)")
        assertContains(code, "public fun KMutableRootQuery.ForEntity<SystemConfig>.applySystemConfigLowQueryParams(")
        assertContains(code, "where(table.configKey `eq?` key)")
        assertContains(code, "return select(table.fetchBy { allScalarFields() })")
        assertContains(code, "@JvmName(\"createLowQueryForSystemConfigByEntity\")")
        assertContains(code, "public fun KSqlClient.createLowQuery(")
        assertContains(code, "entity: SystemConfig")
        assertContains(code, "private fun KMutableRootQuery.ForEntity<SystemConfig>.applyLowQuery(")
        assertContains(code, "return createQuery(SystemConfig::class) {")
        assertContains(code, "applyLowQuery(entity)")
        assertContains(code, "if (ImmutableObjects.isLoaded(entity, \"configKey\"))")
        assertContains(code, "where(table.configKey `eq?` entity.configKey)")
        assertContains(code, "select(table.fetchBy { allScalarFields() })")
    }


    @Test
    fun `generator defaults to id desc order when entity has id property`() {
        val codeGenerator = RecordingCodeGenerator()
        val entity = LowQueryEntityMeta(
            packageName = "demo.system",
            simpleName = "SystemConfig",
            qualifiedName = "demo.system.SystemConfig",
            functionName = "query",
            clientFunctionName = "createLowQuery",
            visibility = LowQueryVisibility.PUBLIC,
            clientVisibility = LowQueryVisibility.PUBLIC,
            fetcher = LowQueryFetcher.ALL_SCALAR_FIELDS,
            params = listOf(
                LowQueryParamMeta(
                    propertyName = "configKey",
                    parameterName = "key",
                    typeName = "String?",
                    targetTypeName = "String",
                    operator = LowQueryOperator.EQ,
                    nullable = true,
                ),
            ),
            orders = emptyList(),
            hasIdProperty = true,
        )

        JimmerLowQueryGenerator(codeGenerator)
            .generate(setOf(entity), generatedPackage = null, springComponentAvailable = true)

        val code = codeGenerator.generated.values.single().toString(Charsets.UTF_8.name())
        assertContains(code, "import demo.system.id")
        assertContains(code, "orderBy(table.id.desc())")
        assertContains(code, "override val hasOrderBy: Boolean = true")
    }

    @Test
    fun `generator emits spring provider when spring component exists`() {
        val codeGenerator = RecordingCodeGenerator()
        val entity = LowQueryEntityMeta(
            packageName = "demo.system",
            simpleName = "SystemConfig",
            qualifiedName = "demo.system.SystemConfig",
            functionName = "query",
            clientFunctionName = "createLowQuery",
            visibility = LowQueryVisibility.PUBLIC,
            clientVisibility = LowQueryVisibility.PUBLIC,
            fetcher = LowQueryFetcher.ALL_SCALAR_FIELDS,
            params = listOf(
                LowQueryParamMeta(
                    propertyName = "configKey",
                    parameterName = "key",
                    typeName = "String",
                    targetTypeName = "String",
                    operator = LowQueryOperator.EQ,
                    nullable = false,
                ),
            ),
            orders = emptyList(),
        )

        JimmerLowQueryGenerator(codeGenerator)
            .generate(setOf(entity), generatedPackage = null, springComponentAvailable = true)

        val code = codeGenerator.generated.values.single().toString(Charsets.UTF_8.name())
        assertContains(code, "import org.springframework.stereotype.Component")
        assertContains(code, "import org.babyfish.jimmer.lowquery.runtime.JimmerLowQueryProvider")
        assertContains(code, "@Component(\"demo.system.SystemConfig.SystemConfigJimmerLowQueryProvider\")")
        assertContains(code, "public class SystemConfigJimmerLowQueryProvider : JimmerLowQueryProvider<SystemConfig>")
        assertContains(code, "override val entityType: KClass<SystemConfig> = SystemConfig::class")
        assertContains(code, "override val parameterNames: Map<String, String> = mapOf(\"configKey\" to \"key\")")
        assertContains(code, "query.applyLowQuery(entity)")
    }

    @Test
    fun `generator skips nullable condition when null`() {
        val codeGenerator = RecordingCodeGenerator()
        val entity = LowQueryEntityMeta(
            packageName = "demo.device",
            simpleName = "Device",
            qualifiedName = "demo.device.Device",
            functionName = "queryByName",
            clientFunctionName = "createLowQuery",
            visibility = LowQueryVisibility.PUBLIC,
            clientVisibility = LowQueryVisibility.PUBLIC,
            fetcher = LowQueryFetcher.ALL_TABLE_FIELDS,
            params = listOf(
                LowQueryParamMeta(
                    propertyName = "name",
                    parameterName = "name",
                    typeName = "String?",
                    targetTypeName = "String",
                    operator = LowQueryOperator.LIKE,
                    nullable = true,
                ),
            ),
            orders = emptyList(),
        )

        JimmerLowQueryGenerator(codeGenerator).generate(setOf(entity), generatedPackage = "demo.generated")

        val code = codeGenerator.generated.values.single().toString(Charsets.UTF_8.name())
        assertContains(code, "package demo.generated")
        assertContains(code, "name: String? = null")
        assertContains(code, "where(table.name.`ilike?`(name, LikeMode.ANYWHERE))")
        assertContains(code, "return select(table.fetchBy { allTableFields() })")
    }

    @Test
    fun `generator emits collection parameter for in operator`() {
        val codeGenerator = RecordingCodeGenerator()
        val entity = LowQueryEntityMeta(
            packageName = "demo.system",
            simpleName = "SystemConfig",
            qualifiedName = "demo.system.SystemConfig",
            functionName = "queryByIds",
            clientFunctionName = "createLowQuery",
            visibility = LowQueryVisibility.PUBLIC,
            clientVisibility = LowQueryVisibility.PUBLIC,
            fetcher = LowQueryFetcher.TABLE,
            params = listOf(
                LowQueryParamMeta(
                    propertyName = "id",
                    parameterName = "ids",
                    typeName = "Collection<Long>",
                    targetTypeName = "Long",
                    operator = LowQueryOperator.IN,
                    nullable = false,
                ),
            ),
            orders = emptyList(),
        )

        JimmerLowQueryGenerator(codeGenerator).generate(setOf(entity), generatedPackage = null)

        val code = codeGenerator.generated.values.single().toString(Charsets.UTF_8.name())
        assertContains(code, "ids: Collection<Long>")
        assertContains(code, "where(table.id `valueIn?` ids)")
        assertContains(code, "return select(table)")
    }

    @Test
    fun `source collection predicate skips missing values`() {
        val codeGenerator = RecordingCodeGenerator()
        val entity = LowQueryEntityMeta(
            packageName = "demo.system",
            simpleName = "SystemUser",
            qualifiedName = "demo.system.SystemUser",
            functionName = "query",
            clientFunctionName = "createLowQuery",
            visibility = LowQueryVisibility.PUBLIC,
            clientVisibility = LowQueryVisibility.PUBLIC,
            fetcher = LowQueryFetcher.ALL_SCALAR_FIELDS,
            params = listOf(
                LowQueryParamMeta(
                    propertyName = "roleIds",
                    parameterName = "roleIds",
                    typeName = "Collection<Long>?",
                    targetTypeName = "Long",
                    operator = LowQueryOperator.IN,
                    nullable = true,
                    tablePropertyName = "id",
                    sourceApplies = true,
                    tablePath = listOf("roles", "id"),
                    tablePathKinds = listOf(LowQueryTablePathKind.COLLECTION),
                    tablePathImportPackages = listOf("demo.system.role"),
                ),
            ),
            orders = emptyList(),
        )

        JimmerLowQueryGenerator(codeGenerator)
            .generate(setOf(entity), generatedPackage = null, springComponentAvailable = true)

        val code = codeGenerator.generated.values.single().toString(Charsets.UTF_8.name())
        assertContains(code, "val roleIds = source.values(\"roleIds\", Long::class)")
        assertContains(code, "if (roleIds.isNotEmpty())")
        assertContains(code, "where(table.roles { this.id `valueIn?` roleIds })")
    }

    @Test
    fun `generator emits order by expressions`() {
        val codeGenerator = RecordingCodeGenerator()
        val entity = LowQueryEntityMeta(
            packageName = "demo.power",
            simpleName = "PowerRuntimeStatus",
            qualifiedName = "demo.power.PowerRuntimeStatus",
            functionName = "query",
            clientFunctionName = "createLowQuery",
            visibility = LowQueryVisibility.PUBLIC,
            clientVisibility = LowQueryVisibility.PUBLIC,
            fetcher = LowQueryFetcher.TABLE,
            params = listOf(
                LowQueryParamMeta(
                    propertyName = "code",
                    parameterName = "code",
                    typeName = "String?",
                    targetTypeName = "String",
                    operator = LowQueryOperator.EQ,
                    nullable = true,
                ),
            ),
            orders = listOf(
                LowQueryOrderMeta(
                    propertyName = "snapshotTime",
                    direction = LowQueryOrderDirection.DESC,
                    priority = 0,
                ),
                LowQueryOrderMeta(
                    propertyName = "id",
                    direction = LowQueryOrderDirection.DESC,
                    priority = 1,
                ),
            ),
        )

        JimmerLowQueryGenerator(codeGenerator)
            .generate(setOf(entity), generatedPackage = null, springComponentAvailable = true)

        val code = codeGenerator.generated.values.single().toString(Charsets.UTF_8.name())
        assertContains(code, "import demo.power.snapshotTime")
        assertContains(code, "import demo.power.id")
        assertContains(code, "orderBy(table.snapshotTime.desc(), table.id.desc())")
        assertContains(code, "override val hasOrderBy: Boolean = true")
    }

    @Test
    fun `generator emits between and time range predicates`() {
        val codeGenerator = RecordingCodeGenerator()
        val entity = LowQueryEntityMeta(
            packageName = "demo.notice",
            simpleName = "Notice",
            qualifiedName = "demo.notice.Notice",
            functionName = "query",
            clientFunctionName = "createLowQuery",
            visibility = LowQueryVisibility.PUBLIC,
            clientVisibility = LowQueryVisibility.PUBLIC,
            fetcher = LowQueryFetcher.ALL_SCALAR_FIELDS,
            params = listOf(
                LowQueryParamMeta(
                    propertyName = "createTime",
                    parameterName = "beginTime",
                    typeName = "java.time.LocalDateTime?",
                    targetTypeName = "java.time.LocalDateTime",
                    operator = LowQueryOperator.BETWEEN,
                    nullable = true,
                    endParameterName = "endTime",
                    endTypeName = "java.time.LocalDateTime?",
                ),
                LowQueryParamMeta(
                    propertyName = "publishedTime",
                    parameterName = "publishedTime",
                    typeName = "Array<java.time.LocalDateTime>?",
                    targetTypeName = "java.time.LocalDateTime",
                    operator = LowQueryOperator.TIME_RANGE,
                    nullable = true,
                ),
            ),
            orders = emptyList(),
        )

        JimmerLowQueryGenerator(codeGenerator).generate(setOf(entity), generatedPackage = null)

        val code = codeGenerator.generated.values.single().toString(Charsets.UTF_8.name())
        assertContains(code, "beginTime: java.time.LocalDateTime? = null")
        assertContains(code, "endTime: java.time.LocalDateTime? = null")
        assertContains(code, "publishedTime: Array<java.time.LocalDateTime>? = null")
        assertContains(code, "applyNoticeLowQueryParams(beginTime = beginTime, endTime = endTime, publishedTime = publishedTime)")
        assertContains(code, "where(table.createTime.`between?`(beginTime, endTime))")
        assertContains(code, "where(table.publishedTime.`between?`(publishedTime?.getOrNull(0), publishedTime?.getOrNull(1)))")
        assertContains(code, "entity: Notice,")
        assertContains(code, "applyLowQuery(entity, beginTime = beginTime, endTime = endTime, publishedTime = publishedTime)")
    }

    @Test
    fun `generator emits default scalar and association id predicates`() {
        val codeGenerator = RecordingCodeGenerator()
        val entity = LowQueryEntityMeta(
            packageName = "demo.order",
            simpleName = "Order",
            qualifiedName = "demo.order.Order",
            functionName = "query",
            clientFunctionName = "createLowQuery",
            visibility = LowQueryVisibility.PUBLIC,
            clientVisibility = LowQueryVisibility.PUBLIC,
            fetcher = LowQueryFetcher.ALL_SCALAR_FIELDS,
            params = listOf(
                LowQueryParamMeta(
                    propertyName = "status",
                    parameterName = "status",
                    typeName = "Int?",
                    targetTypeName = "Int",
                    operator = LowQueryOperator.EQ,
                    nullable = true,
                    sourceApplies = true,
                ),
                LowQueryParamMeta(
                    propertyName = "tenantId",
                    parameterName = "tenantId",
                    typeName = "Long?",
                    targetTypeName = "Long",
                    operator = LowQueryOperator.EQ,
                    nullable = true,
                    tablePropertyName = "tenantId",
                    loadedPropertyName = "tenant",
                    entityValuePath = listOf("tenant", "id"),
                    entityValuePathNullable = listOf(false, false),
                    sourceApplies = true,
                ),
            ),
            orders = emptyList(),
        )

        JimmerLowQueryGenerator(codeGenerator)
            .generate(setOf(entity), generatedPackage = null, springComponentAvailable = true)

        val code = codeGenerator.generated.values.single().toString(Charsets.UTF_8.name())
        assertContains(code, "import demo.order.status")
        assertContains(code, "import demo.order.tenantId")
        assertContains(code, "status: Int? = null")
        assertContains(code, "tenantId: Long? = null")
        assertContains(code, "where(table.status `eq?` status)")
        assertContains(code, "where(table.tenantId `eq?` tenantId)")
        assertContains(code, "if (ImmutableObjects.isLoaded(entity, \"tenant\"))")
        assertContains(code, "where(table.tenantId `eq?` entity.tenant.id)")
        assertContains(code, "override val parameterNames: Map<String, String> = mapOf(\"status\" to \"status\", \"tenantId\" to \"tenantId\")")
        assertContains(code, "val status = source.value(\"status\", Int::class)")
        assertContains(code, "val tenantId = source.value(\"tenantId\", Long::class)")
    }

    @Test
    fun `generator emits association id predicate through reference path`() {
        val codeGenerator = RecordingCodeGenerator()
        val entity = LowQueryEntityMeta(
            packageName = "demo.rule",
            simpleName = "RuleCondition",
            qualifiedName = "demo.rule.RuleCondition",
            functionName = "query",
            clientFunctionName = "createLowQuery",
            visibility = LowQueryVisibility.PUBLIC,
            clientVisibility = LowQueryVisibility.PUBLIC,
            fetcher = LowQueryFetcher.ALL_SCALAR_FIELDS,
            params = listOf(
                LowQueryParamMeta(
                    propertyName = "modelId",
                    parameterName = "modelId",
                    typeName = "Long?",
                    targetTypeName = "Long",
                    operator = LowQueryOperator.EQ,
                    nullable = true,
                    tablePropertyName = "modelId",
                    loadedPropertyName = "thingModel",
                    entityValuePath = listOf("thingModel", "id"),
                    entityValuePathNullable = listOf(true, false),
                    sourceApplies = true,
                    tablePath = listOf("thingModel", "id"),
                    tablePathKinds = listOf(LowQueryTablePathKind.REFERENCE),
                    tablePathImportPackages = listOf("demo.rule", "demo.thingmodel"),
                ),
            ),
            orders = emptyList(),
        )

        JimmerLowQueryGenerator(codeGenerator)
            .generate(setOf(entity), generatedPackage = null, springComponentAvailable = true)

        val code = codeGenerator.generated.values.single().toString(Charsets.UTF_8.name())
        assertContains(code, "import demo.rule.thingModel")
        assertContains(code, "import demo.thingmodel.id")
        assertContains(code, "modelId: Long? = null")
        assertContains(code, "where(table.thingModel.id `eq?` modelId)")
        assertContains(code, "if (ImmutableObjects.isLoaded(entity, \"thingModel\"))")
        assertContains(code, "where(table.thingModel.id `eq?` entity.thingModel?.id)")
        assertContains(code, "val modelId = source.value(\"modelId\", Long::class)")
    }

    @Test
    fun `generator emits collection path predicate as implicit sub query`() {
        val codeGenerator = RecordingCodeGenerator()
        val entity = LowQueryEntityMeta(
            packageName = "demo.product",
            simpleName = "Product",
            qualifiedName = "demo.product.Product",
            functionName = "query",
            clientFunctionName = "createLowQuery",
            visibility = LowQueryVisibility.PUBLIC,
            clientVisibility = LowQueryVisibility.PUBLIC,
            fetcher = LowQueryFetcher.ALL_SCALAR_FIELDS,
            params = listOf(
                LowQueryParamMeta(
                    propertyName = "thingModelIdentifier",
                    parameterName = "thingModelIdentifier",
                    typeName = "String?",
                    targetTypeName = "String",
                    operator = LowQueryOperator.LIKE,
                    nullable = true,
                    loadedPropertyName = "thingModels",
                    entityValuePath = listOf("thingModelIdentifier"),
                    sourceApplies = true,
                    tablePath = listOf("thingModels", "identifier"),
                    tablePathKinds = listOf(LowQueryTablePathKind.COLLECTION),
                    tablePathImportPackages = listOf("demo.product", "demo.thingmodel"),
                ),
            ),
            orders = emptyList(),
        )

        JimmerLowQueryGenerator(codeGenerator)
            .generate(setOf(entity), generatedPackage = null, springComponentAvailable = true)

        val code = codeGenerator.generated.values.single().toString(Charsets.UTF_8.name())
        assertContains(code, "import demo.product.thingModels")
        assertContains(code, "import demo.thingmodel.identifier")
        assertContains(code, "where(table.thingModels { this.identifier.`ilike?`(thingModelIdentifier, LikeMode.ANYWHERE) })")
    }

    @Test
    fun `generator emits keyword predicate for keyword props and associated keyword props`() {
        val codeGenerator = RecordingCodeGenerator()
        val entity = LowQueryEntityMeta(
            packageName = "demo.dict",
            simpleName = "DictType",
            qualifiedName = "demo.dict.DictType",
            functionName = "query",
            clientFunctionName = "createLowQuery",
            visibility = LowQueryVisibility.PUBLIC,
            clientVisibility = LowQueryVisibility.PUBLIC,
            fetcher = LowQueryFetcher.ALL_SCALAR_FIELDS,
            params = listOf(
                LowQueryParamMeta(
                    propertyName = "type",
                    parameterName = "type",
                    typeName = "String?",
                    targetTypeName = "String",
                    operator = LowQueryOperator.LIKE,
                    nullable = true,
                    sourceApplies = true,
                ),
            ),
            orders = emptyList(),
            keywordProps = listOf(
                LowQueryKeywordMeta(
                    propertyName = "name",
                ),
                LowQueryKeywordMeta(
                    propertyName = "dataList.label",
                    tablePath = listOf("dataList", "label"),
                    tablePathKinds = listOf(LowQueryTablePathKind.COLLECTION),
                    tablePathImportPackages = listOf("demo.dict", "demo.dict"),
                ),
                LowQueryKeywordMeta(
                    propertyName = "dataList.device.name",
                    tablePath = listOf("dataList", "device", "name"),
                    tablePathKinds = listOf(LowQueryTablePathKind.COLLECTION, LowQueryTablePathKind.REFERENCE),
                    tablePathImportPackages = listOf("demo.dict", "demo.record", "demo.device"),
                ),
            ),
        )

        JimmerLowQueryGenerator(codeGenerator)
            .generate(setOf(entity), generatedPackage = null, springComponentAvailable = true)

        val code = codeGenerator.generated.values.single().toString(Charsets.UTF_8.name())
        assertContains(code, "import org.babyfish.jimmer.sql.kt.ast.expression.ilike")
        assertContains(code, "import org.babyfish.jimmer.sql.kt.ast.expression.or")
        assertContains(code, "import demo.dict.dataList")
        assertContains(code, "import demo.record.device")
        assertContains(code, "import demo.dict.label")
        assertContains(code, "import demo.dict.name")
        assertContains(code, "import demo.device.name")
        assertContains(code, "keyword: String? = null")
        assertContains(code, "applyDictTypeLowQueryParams(type = type, keyword = keyword)")
        assertContains(code, "val keywordTrimmed = keyword?.trim()?.takeIf { it.isNotEmpty() }")
        assertContains(code, "table.name.`ilike?`(keywordTrimmed, LikeMode.ANYWHERE)")
        assertContains(code, "table.dataList { this.label.`ilike?`(keywordTrimmed, LikeMode.ANYWHERE) }")
        assertContains(code, "table.dataList { this.device.name.`ilike?`(keywordTrimmed, LikeMode.ANYWHERE) }")
        assertContains(code, "val keyword = source.value(\"keyword\", String::class)")
    }

    @Test
    fun `generator emits find by helpers`() {
        val codeGenerator = RecordingCodeGenerator()
        val entity = LowQueryEntityMeta(
            packageName = "demo.system",
            simpleName = "User",
            qualifiedName = "demo.system.User",
            functionName = "query",
            clientFunctionName = "createLowQuery",
            visibility = LowQueryVisibility.PUBLIC,
            clientVisibility = LowQueryVisibility.PUBLIC,
            fetcher = LowQueryFetcher.ALL_SCALAR_FIELDS,
            params = emptyList(),
            orders = emptyList(),
            findBys = listOf(
                LowQueryFindByMeta(
                    fields = listOf(
                        LowQueryFindByFieldMeta(
                            propertyName = "username",
                            parameterName = "username",
                            typeName = "String",
                        ),
                    ),
                    listFunctionName = "findUserByUsername",
                    oneFunctionName = "findOneOrNullUserByUsername",
                    visibility = LowQueryVisibility.PUBLIC,
                    fetcher = LowQueryFetcher.ALL_SCALAR_FIELDS,
                    extraFetchFields = emptyList(),
                ),
            ),
        )

        JimmerLowQueryGenerator(codeGenerator).generate(setOf(entity), generatedPackage = null)

        val code = codeGenerator.generated.values.single().toString(Charsets.UTF_8.name())
        assertContains(code, "import demo.system.User")
        assertContains(code, "import demo.system.username")
        assertContains(code, "public fun KSqlClient.findUserByUsername(")
        assertContains(code, "): List<User> {")
        assertContains(code, "return executeQuery(User::class) {")
        assertContains(code, "where(table.username eq username)")
        assertContains(code, "select(table.fetchBy { allScalarFields() })")
        assertFalse(code.contains("password()"))
        assertContains(code, "public fun KSqlClient.findOneOrNullUserByUsername(")
        assertContains(code, "): User? {")
        assertContains(code, "return createQuery(User::class) {")
        assertContains(code, "}.fetchOneOrNull()")
        assertFalse(code.contains(".firstOrNull()"))
        assertFalse(code.contains("applyUserLowQueryParams"))
    }

    @Test
    fun `generator emits composite find by helpers`() {
        val codeGenerator = RecordingCodeGenerator()
        val entity = LowQueryEntityMeta(
            packageName = "demo.social",
            simpleName = "AuthSocialUser",
            qualifiedName = "demo.social.AuthSocialUser",
            functionName = "query",
            clientFunctionName = "createLowQuery",
            visibility = LowQueryVisibility.PUBLIC,
            clientVisibility = LowQueryVisibility.PUBLIC,
            fetcher = LowQueryFetcher.ALL_SCALAR_FIELDS,
            params = emptyList(),
            orders = emptyList(),
            findBys = listOf(
                LowQueryFindByMeta(
                    fields = listOf(
                        LowQueryFindByFieldMeta(
                            propertyName = "type",
                            parameterName = "type",
                            typeName = "Int",
                        ),
                        LowQueryFindByFieldMeta(
                            propertyName = "code",
                            parameterName = "code",
                            typeName = "String",
                        ),
                        LowQueryFindByFieldMeta(
                            propertyName = "state",
                            parameterName = "state",
                            typeName = "String",
                        ),
                    ),
                    listFunctionName = "findAuthSocialUserByTypeAndCodeAndState",
                    oneFunctionName = "findOneOrNullAuthSocialUserByTypeAndCodeAndState",
                    visibility = LowQueryVisibility.PUBLIC,
                    fetcher = LowQueryFetcher.ALL_SCALAR_FIELDS,
                    extraFetchFields = emptyList(),
                ),
            ),
        )

        JimmerLowQueryGenerator(codeGenerator).generate(setOf(entity), generatedPackage = null)

        val code = codeGenerator.generated.values.single().toString(Charsets.UTF_8.name())
        assertContains(code, "import demo.social.type")
        assertContains(code, "import demo.social.code")
        assertContains(code, "import demo.social.state")
        assertContains(code, "@JvmName(\"findAuthSocialUserByTypeAndCodeAndStateForAuthSocialUserByTypeAndCodeAndStateList\")")
        assertContains(code, "public fun KSqlClient.findAuthSocialUserByTypeAndCodeAndState(")
        assertContains(code, "type: Int")
        assertContains(code, "code: String")
        assertContains(code, "state: String")
        assertContains(code, "where(table.type eq type)")
        assertContains(code, "where(table.code eq code)")
        assertContains(code, "where(table.state eq state)")
        assertContains(code, "public fun KSqlClient.findOneOrNullAuthSocialUserByTypeAndCodeAndState(")
        assertContains(code, "}.fetchOneOrNull()")
    }

    @Test
    fun `generator emits find by helper constant eq predicates and entity order by`() {
        val codeGenerator = RecordingCodeGenerator()
        val entity = LowQueryEntityMeta(
            packageName = "demo.dict",
            simpleName = "DictData",
            qualifiedName = "demo.dict.DictData",
            functionName = "query",
            clientFunctionName = "createLowQuery",
            visibility = LowQueryVisibility.PUBLIC,
            clientVisibility = LowQueryVisibility.PUBLIC,
            fetcher = LowQueryFetcher.ALL_SCALAR_FIELDS,
            params = listOf(
                LowQueryParamMeta(
                    propertyName = "status",
                    parameterName = "status",
                    typeName = "Int?",
                    targetTypeName = "Int",
                    operator = LowQueryOperator.EQ,
                    nullable = true,
                    expression = "CommonStatusEnum.ENABLE.status",
                    expressionImports = listOf("demo.common.CommonStatusEnum"),
                ),
            ),
            orders = listOf(
                LowQueryOrderMeta(
                    propertyName = "sort",
                    direction = LowQueryOrderDirection.ASC,
                    priority = 0,
                ),
            ),
            findBys = listOf(
                LowQueryFindByMeta(
                    fields = listOf(
                        LowQueryFindByFieldMeta(
                            propertyName = "dictType",
                            parameterName = "dictType",
                            typeName = "String",
                        ),
                    ),
                    listFunctionName = "findDictDataByDictType",
                    oneFunctionName = "findOneOrNullDictDataByDictType",
                    visibility = LowQueryVisibility.PUBLIC,
                    fetcher = LowQueryFetcher.ALL_SCALAR_FIELDS,
                    extraFetchFields = emptyList(),
                ),
            ),
        )

        JimmerLowQueryGenerator(codeGenerator).generate(setOf(entity), generatedPackage = null)

        val code = codeGenerator.generated.values.single().toString(Charsets.UTF_8.name())
        assertContains(code, "import demo.common.CommonStatusEnum")
        assertContains(code, "import demo.dict.dictType")
        assertContains(code, "import demo.dict.sort")
        assertContains(code, "import demo.dict.status")
        assertContains(code, "where(table.dictType eq dictType)")
        assertContains(code, "where(table.status eq CommonStatusEnum.ENABLE.status)")
        assertContains(code, "orderBy(table.sort.asc())")
        assertContains(code, "select(table.fetchBy { allScalarFields() })")
    }

    @Test
    fun `generator emits no arg find by helper when field has constant eq expression`() {
        val codeGenerator = RecordingCodeGenerator()
        val entity = LowQueryEntityMeta(
            packageName = "demo.project",
            simpleName = "IotProjectDO",
            qualifiedName = "demo.project.IotProjectDO",
            functionName = "query",
            clientFunctionName = "createLowQuery",
            visibility = LowQueryVisibility.PUBLIC,
            clientVisibility = LowQueryVisibility.PUBLIC,
            fetcher = LowQueryFetcher.ALL_SCALAR_FIELDS,
            params = listOf(
                LowQueryParamMeta(
                    propertyName = "status",
                    parameterName = "status",
                    typeName = "Int?",
                    targetTypeName = "Int",
                    operator = LowQueryOperator.EQ,
                    nullable = true,
                    expression = "CommonStatusEnum.ENABLE.status",
                    expressionImports = listOf("demo.common.CommonStatusEnum"),
                ),
            ),
            orders = emptyList(),
            hasIdProperty = true,
            findBys = listOf(
                LowQueryFindByMeta(
                    fields = listOf(
                        LowQueryFindByFieldMeta(
                            propertyName = "status",
                            parameterName = "status",
                            typeName = "Int",
                        ),
                    ),
                    listFunctionName = "findIotProjectDOByStatus",
                    oneFunctionName = "findOneOrNullIotProjectDOByStatus",
                    visibility = LowQueryVisibility.PUBLIC,
                    fetcher = LowQueryFetcher.ALL_SCALAR_FIELDS,
                    extraFetchFields = emptyList(),
                ),
            ),
        )

        JimmerLowQueryGenerator(codeGenerator).generate(setOf(entity), generatedPackage = null)

        val code = codeGenerator.generated.values.single().toString(Charsets.UTF_8.name())
        assertContains(code, "import demo.common.CommonStatusEnum")
        assertContains(code, "import demo.project.id")
        assertContains(code, "import demo.project.status")
        assertContains(code, "public fun KSqlClient.findIotProjectDOByStatus(): List<IotProjectDO> {")
        assertFalse(code.contains("public fun KSqlClient.findIotProjectDOByStatus(\n    status: Int,"))
        assertContains(code, "where(table.status eq CommonStatusEnum.ENABLE.status)")
        assertContains(code, "orderBy(table.id.desc())")
        assertContains(code, "select(table.fetchBy { allScalarFields() })")
    }
}

private class RecordingCodeGenerator : CodeGenerator {
    val generated = linkedMapOf<String, ByteArrayOutputStream>()

    override val generatedFile: Collection<java.io.File>
        get() = emptyList()

    override fun createNewFile(
        dependencies: Dependencies,
        packageName: String,
        fileName: String,
        extensionName: String,
    ): OutputStream {
        val stream = ByteArrayOutputStream()
        generated["$packageName.$fileName.$extensionName"] = stream
        return stream
    }

    override fun createNewFileByPath(
        dependencies: Dependencies,
        path: String,
        extensionName: String,
    ): OutputStream {
        val stream = ByteArrayOutputStream()
        generated["$path.$extensionName"] = stream
        return stream
    }

    override fun associate(
        sources: List<com.google.devtools.ksp.symbol.KSFile>,
        packageName: String,
        fileName: String,
        extensionName: String,
    ) = Unit

    override fun associateByPath(
        sources: List<com.google.devtools.ksp.symbol.KSFile>,
        path: String,
        extensionName: String,
    ) = Unit

    override fun associateWithClasses(
        classes: List<com.google.devtools.ksp.symbol.KSClassDeclaration>,
        packageName: String,
        fileName: String,
        extensionName: String,
    ) = Unit
}
