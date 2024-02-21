package org.babyfish.jimmer.spring.graphql

import graphql.schema.DataFetchingEnvironment
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import org.babyfish.jimmer.sql.kt.ast.table.KNullableTable
import org.babyfish.jimmer.sql.kt.ast.table.impl.KTableImplementor

@Suppress("UNCHECKED_CAST")
fun <E: Any> KNonNullTable<E>.fetch(env: DataFetchingEnvironment): Selection<E> =
    this.fetch(
        DataFetchingEnvironments.createFetcher(
            (this as KTableImplementor<E>).javaTable.immutableType.javaClass as Class<E>,
            env
        )
    )

@Suppress("UNCHECKED_CAST")
fun <E: Any> KNullableTable<E>.fetch(env: DataFetchingEnvironment): Selection<E?> =
    this.fetch(
        DataFetchingEnvironments.createFetcher(
            (this as KTableImplementor<E>).javaTable.immutableType.javaClass as Class<E>,
            env
        )
    )