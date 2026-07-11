package org.babyfish.jimmer.lowquery.runtime

import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
import kotlin.reflect.KClass

/**
 * Jimmer 低代码查询运行时提供者。
 */
interface JimmerLowQueryProvider<E : Any> {
    /**
     * 当前提供者关联的实体类型。
     */
    val entityType: KClass<E>

    /**
     * 实体字段到查询参数名的映射。
     */
    val parameterNames: Map<String, String>

    /**
     * 当前提供者是否会写入排序条件。
     */
    val hasOrderBy
        get() = false

    /**
     * 将已加载实体字段转换为查询条件，并写入注解声明的排序。
     */
    fun apply(
        query: KMutableRootQuery.ForEntity<E>,
        entity: E,
    )

    /**
     * 将无法塞进实体草稿的范围参数写入查询条件。
     */
    fun apply(
        query: KMutableRootQuery.ForEntity<E>,
        source: JimmerLowQueryParameterSource,
    ) = Unit
}

/**
 * 低代码查询请求参数读取源。
 */
interface JimmerLowQueryParameterSource {
    /**
     * 读取并转换单个查询参数。
     */
    fun <T : Any> value(
        name: String,
        type: KClass<T>,
    ): T?

    /**
     * 读取并转换集合查询参数。
     */
    fun <T : Any> values(
        name: String,
        type: KClass<T>,
    ): List<T>
}
