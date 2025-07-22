package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression
import org.babyfish.jimmer.sql.kt.ast.table.*

interface KConfigurableBaseQuery<T: KBaseTable> : KTypedBaseQuery<T> {

    interface Query1<
        T1: Selection<*>,
        T1Nullable: Selection<*>
        > : KConfigurableBaseQuery<KNonNullBaseTable1<T1, T1Nullable>> {

        fun <T: Any> add(
            table: KNonNullTable<T>
        ): Query2<
            T1,
            KNonNullTable<T>,
            T1Nullable,
            KNullableTable<T>
        >

        fun <T: Any> add(
            table: KNullableTable<T>
        ): Query2<
            T1,
            KNullableTable<T>,
            T1Nullable,
            KNullableTable<T>
        >

        fun <T: Any> add(
            expression: KNonNullExpression<T>
        ): Query2<
            T1,
            KNonNullExpression<T>,
            T1Nullable,
            KNullableExpression<T>
        >

        fun <T: Any> add(
            expression: KNullableExpression<T>
        ): Query2<
            T1,
            KNullableExpression<T>,
            T1Nullable,
            KNullableExpression<T>
        >
    }

    interface Query2<
        T1: Selection<*>,
        T2: Selection<*>,
        T1Nullable: Selection<*>,
        T2Nullable: Selection<*>
    > : KConfigurableBaseQuery<KNonNullBaseTable2<T1, T2, T1Nullable, T2Nullable>>
}