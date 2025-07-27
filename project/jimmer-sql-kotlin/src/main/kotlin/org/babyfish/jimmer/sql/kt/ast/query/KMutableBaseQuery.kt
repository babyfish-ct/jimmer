package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression
import org.babyfish.jimmer.sql.kt.ast.table.*

interface KMutableBaseQuery<E: Any> : KMutableQuery<KNonNullTable<E>> {

    val selections: Selections

    interface Selections {

        fun <T: Any> add(
            table: KNonNullTable<T>
        ): KConfigurableBaseQuery.Query1<
            KNonNullTable<T>,
            KNullableTable<T>
        >

        fun <T: Any> add(
            table: KNullableTable<T>
        ): KConfigurableBaseQuery.Query1<
            KNullableTable<T>,
            KNullableTable<T>
        >

        fun <T: Any> add(
            expression: KNonNullExpression<T>
        ): KConfigurableBaseQuery.Query1<
            KNonNullExpression<T>,
            KNullableExpression<T>
        >

        fun <T: Any> add(
            expression: KNullableExpression<T>
        ): KConfigurableBaseQuery.Query1<
            KNullableExpression<T>,
            KNullableExpression<T>
        >
    }
}