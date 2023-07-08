package org.babyfish.jimmer.spring.repository

import org.babyfish.jimmer.spring.repository.support.Utils
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.fetchPage
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.sql.Connection

fun <E> KConfigurableRootQuery<*, E>.fetchPage(
    pageIndex: Int,
    pageSize: Int,
    con: Connection? = null
): Page<E> =
    fetchPage(
        pageIndex,
        pageSize,
        con
    ) { entities, totalCount, queryImplementor ->
        PageImpl(
            entities,
            PageRequest.of(
                pageIndex,
                pageSize,
                Utils.toSort(
                    queryImplementor.javaOrders,
                    queryImplementor.javaSqlClient.metadataStrategy
                )
            ),
            totalCount.toLong()
        )
    }