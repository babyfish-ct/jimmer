package org.babyfish.jimmer.lowquery.annotation

/**
 * 配置一个 Jimmer 实体生成的低代码查询扩展函数。
 *
 * 实体字段上出现 @Eq、@Like、@In 等注解时也会自动参与生成；本注解负责覆盖函数名、可见性和 fetcher 策略。
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class JimmerLowQuery(
    /**
     * 生成的 KMutableRootQuery.ForEntity<E> 扩展函数名。
     */
    val functionName: String = "query",
    /**
     * 生成函数的可见性。
     */
    val visibility: JimmerLowQueryVisibility = JimmerLowQueryVisibility.PUBLIC,
    /**
     * select 阶段使用的 fetcher 策略。
     */
    val fetcher: JimmerLowQueryFetcher = JimmerLowQueryFetcher.ALL_SCALAR_FIELDS,
    /**
     * 生成的 KSqlClient 扩展函数名，用于通过实体对象创建查询。
     */
    val clientFunctionName: String = "createLowQuery",
    /**
     * 生成的 KSqlClient 入口函数可见性。
     */
    val clientVisibility: JimmerLowQueryVisibility = JimmerLowQueryVisibility.PUBLIC,
)

/**
 * 标记实体字段需要参与低代码查询函数入参和 where 条件生成。
 *
 * 当标注在关联属性（@ManyToOne/@OneToMany）上时，通过 [targetField] 指定要穿透到目标实体的标量字段。
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class JimmerLowQueryParam(
    /**
     * 生成函数的参数名，留空时使用实体字段名。
     */
    val name: String = "",
    /**
     * 当前参数对应的查询操作符。
     */
    val operator: JimmerLowQueryOperator = JimmerLowQueryOperator.EQ,
    /**
     * 默认生成可空参数，空值由动态谓词跳过；确需必填过滤时可显式设为 false。
     */
    val nullable: Boolean = true,
    /**
     * 当标注在关联属性上时，指定要穿透到目标实体的标量字段名。
     *
     * 例如：`@JimmerLowQueryParam(targetField = "operatorId") val repairRecords: List<RepairTaskRecordDO>`
     * 会生成 `repairRecordOperatorId: Long?` 参数，对应 `repairRecords { operatorId eq repairRecordOperatorId }`。
     */
    val targetField: String = "",
)

/**
 * 标记实体字段参与 keyword 关键词检索。
 *
 * 同一个实体内所有标记字段会被低代码查询合并成 `keyword` 参数的 OR 模糊查询条件。
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Keyword

/**
 * 标记字段生成常用辅助查询方法。
 *
 * 标在字段上时按当前字段生成；标在实体类上时通过 [fields] 指定字段名，适合 `id` 这类继承字段。
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@Repeatable
annotation class JimmerFindBy(
    /**
     * 生成函数的参数名，留空时使用字段名。
     */
    val name: String = "",
    /**
     * 类级标注时要生成辅助查询的实体字段名。
     */
    val fields: Array<String> = [],
    /**
     * 类级标注多个字段时，是否生成组合条件辅助查询。
     *
     * false 表示每个字段分别生成一组 findBy 方法；true 表示生成一组 `field1 AND field2` 组合查询方法。
     */
    val composite: Boolean = false,
    /**
     * List 查询函数名，留空时生成 `find实体名By字段名`。
     */
    val listFunctionName: String = "",
    /**
     * 单条查询函数名，留空时生成 `findOneOrNull实体名By字段名`。
     */
    val oneFunctionName: String = "",
    /**
     * 生成函数的可见性。
     */
    val visibility: JimmerLowQueryVisibility = JimmerLowQueryVisibility.PUBLIC,
    /**
     * select 阶段使用的 fetcher 策略。
     */
    val fetcher: JimmerLowQueryFetcher = JimmerLowQueryFetcher.ALL_SCALAR_FIELDS,
    /**
     * 在 `allScalarFields()` 或 `allTableFields()` 之外额外显式加载的字段。
     */
    val extraFetchFields: Array<String> = [],
)

/**
 * 字段等值查询，生成 `table.xxx eq param`。
 *
 * 当标注在关联属性（@ManyToOne/@OneToMany）上时，通过 [targetField] 指定要穿透到目标实体的标量字段。
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class Eq(
    /**
     * 生成函数的参数名，留空时使用实体字段名。
     */
    val name: String = "",
    /**
     * 默认生成可空参数，空值由动态谓词跳过；确需必填过滤时可显式设为 false。
     */
    val nullable: Boolean = true,
    /**
     * 常量表达式，保留给辅助查询生成器使用；不为空时，`@JimmerFindBy` 生成的辅助查询会改成固定条件。
     */
    val expression: String = "",
    /**
     * 常量表达式需要额外导入的类型全名。
     */
    val imports: Array<String> = [],
    /**
     * 当标注在关联属性上时，指定要穿透到目标实体的标量字段名。
     */
    val targetField: String = "",
)

/**
 * 字段不等查询，生成 `table.xxx ne param`。
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class Ne(
    /**
     * 生成函数的参数名，留空时使用实体字段名。
     */
    val name: String = "",
    /**
     * 默认生成可空参数，空值由动态谓词跳过；确需必填过滤时可显式设为 false。
     */
    val nullable: Boolean = true,
    /**
     * 当标注在关联属性上时，指定要穿透到目标实体的标量字段名。
     */
    val targetField: String = "",
)

/**
 * 字段模糊查询，默认生成 `table.xxx ilike(param, LikeMode.ANYWHERE)`。
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class Like(
    /**
     * 生成函数的参数名，留空时使用实体字段名。
     */
    val name: String = "",
    /**
     * 默认生成可空参数，空值由动态谓词跳过；确需必填过滤时可显式设为 false。
     */
    val nullable: Boolean = true,
    /**
     * 当标注在关联属性上时，指定要穿透到目标实体的标量字段名。
     */
    val targetField: String = "",
)

/**
 * 字段前缀匹配查询，生成 `table.xxx like(param, LikeMode.START)`。
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class StartsWith(
    /**
     * 生成函数的参数名，留空时使用实体字段名。
     */
    val name: String = "",
    /**
     * 默认生成可空参数，空值由动态谓词跳过；确需必填过滤时可显式设为 false。
     */
    val nullable: Boolean = true,
    /**
     * 当标注在关联属性上时，指定要穿透到目标实体的标量字段名。
     */
    val targetField: String = "",
)

/**
 * 字段后缀匹配查询，生成 `table.xxx like(param, LikeMode.END)`。
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class EndsWith(
    /**
     * 生成函数的参数名，留空时使用实体字段名。
     */
    val name: String = "",
    /**
     * 默认生成可空参数，空值由动态谓词跳过；确需必填过滤时可显式设为 false。
     */
    val nullable: Boolean = true,
    /**
     * 当标注在关联属性上时，指定要穿透到目标实体的标量字段名。
     */
    val targetField: String = "",
)

/**
 * 字段大于查询，生成 `table.xxx gt param`。
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class Gt(
    /**
     * 生成函数的参数名，留空时使用实体字段名。
     */
    val name: String = "",
    /**
     * 默认生成可空参数，空值由动态谓词跳过；确需必填过滤时可显式设为 false。
     */
    val nullable: Boolean = true,
    /**
     * 当标注在关联属性上时，指定要穿透到目标实体的标量字段名。
     */
    val targetField: String = "",
)

/**
 * 字段大于等于查询，生成 `table.xxx ge param`。
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class Ge(
    /**
     * 生成函数的参数名，留空时使用实体字段名。
     */
    val name: String = "",
    /**
     * 默认生成可空参数，空值由动态谓词跳过；确需必填过滤时可显式设为 false。
     */
    val nullable: Boolean = true,
    /**
     * 当标注在关联属性上时，指定要穿透到目标实体的标量字段名。
     */
    val targetField: String = "",
)

/**
 * 字段小于查询，生成 `table.xxx lt param`。
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class Lt(
    /**
     * 生成函数的参数名，留空时使用实体字段名。
     */
    val name: String = "",
    /**
     * 默认生成可空参数，空值由动态谓词跳过；确需必填过滤时可显式设为 false。
     */
    val nullable: Boolean = true,
    /**
     * 当标注在关联属性上时，指定要穿透到目标实体的标量字段名。
     */
    val targetField: String = "",
)

/**
 * 字段小于等于查询，生成 `table.xxx le param`。
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class Le(
    /**
     * 生成函数的参数名，留空时使用实体字段名。
     */
    val name: String = "",
    /**
     * 默认生成可空参数，空值由动态谓词跳过；确需必填过滤时可显式设为 false。
     */
    val nullable: Boolean = true,
    /**
     * 当标注在关联属性上时，指定要穿透到目标实体的标量字段名。
     */
    val targetField: String = "",
)

/**
 * 字段集合包含查询，生成 `table.xxx valueIn params`。
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class In(
    /**
     * 生成函数的参数名，留空时使用实体字段名的简单复数形式。
     */
    val name: String = "",
    /**
     * 集合参数默认生成可空类型，空值由动态谓词跳过；确需必填过滤时可显式设为 false。
     */
    val nullable: Boolean = true,
    /**
     * 当标注在关联属性上时，指定要穿透到目标实体的标量字段名。
     */
    val targetField: String = "",
)

/**
 * 字段集合排除查询，生成 `table.xxx valueNotIn params`。
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class NotIn(
    /**
     * 生成函数的参数名，留空时使用实体字段名的简单复数形式。
     */
    val name: String = "",
    /**
     * 集合参数默认生成可空类型，空值由动态谓词跳过；确需必填过滤时可显式设为 false。
     */
    val nullable: Boolean = true,
    /**
     * 当标注在关联属性上时，指定要穿透到目标实体的标量字段名。
     */
    val targetField: String = "",
)

/**
 * 字段范围查询，生成 `table.xxx.between?(start, end)`。
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class Between(
    /**
     * 起始边界参数名，留空时使用 `<字段名>Start`。
     */
    val startName: String = "",
    /**
     * 结束边界参数名，留空时使用 `<字段名>End`。
     */
    val endName: String = "",
    /**
     * 当标注在关联属性上时，指定要穿透到目标实体的标量字段名。
     */
    val targetField: String = "",
)

/**
 * 字段时间范围查询，生成单个数组参数并按前两个元素做 `between?`。
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class TimeRange(
    /**
     * 范围参数名，留空时使用实体字段名。
     */
    val name: String = "",
)

/**
 * 字段升序排序，priority 越小越靠前。
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class OrderByAsc(
    /**
     * 排序优先级，数值越小越先排序。
     */
    val priority: Int = 0,
)

/**
 * 字段降序排序，priority 越小越靠前。
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class OrderByDesc(
    /**
     * 排序优先级，数值越小越先排序。
     */
    val priority: Int = 0,
)

/**
 * 低代码查询 where 操作符。
 */
enum class JimmerLowQueryOperator {
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

/**
 * 低代码查询 select 阶段 fetcher 策略。
 */
enum class JimmerLowQueryFetcher {
    ALL_SCALAR_FIELDS,
    ALL_TABLE_FIELDS,
    TABLE,
}

/**
 * 低代码查询生成函数可见性。
 */
enum class JimmerLowQueryVisibility {
    PUBLIC,
    INTERNAL,
    PRIVATE,
}
