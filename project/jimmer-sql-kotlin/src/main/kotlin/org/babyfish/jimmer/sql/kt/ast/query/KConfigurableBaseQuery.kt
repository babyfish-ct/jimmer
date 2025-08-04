package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression
import org.babyfish.jimmer.sql.kt.ast.table.*

interface KConfigurableBaseQuery<T: KNonNullBaseTable<*>> : KTypedBaseQuery<T> {

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
    > : KConfigurableBaseQuery<KNonNullBaseTable2<T1, T2, T1Nullable, T2Nullable>> {

        fun <T: Any> add(
            table: KNonNullTable<T>
        ): Query3<
            T1,
            T2,
            KNonNullTable<T>,
            T1Nullable,
            T2Nullable,
            KNullableTable<T>
        >

        fun <T: Any> add(
            table: KNullableTable<T>
        ): Query3<
            T1,
            T2,
            KNullableTable<T>,
            T1Nullable,
            T2Nullable,
            KNullableTable<T>
        >

        fun <T: Any> add(
            expression: KNonNullExpression<T>
        ): Query3<
            T1,
            T2,
            KNonNullExpression<T>,
            T1Nullable,
            T2Nullable,
            KNullableExpression<T>
        >

        fun <T: Any> add(
            expression: KNullableExpression<T>
        ): Query3<
            T1,
            T2,
            KNullableExpression<T>,
            T1Nullable,
            T2Nullable,
            KNullableExpression<T>
        >
    }

    interface Query3<
        T1: Selection<*>,
        T2: Selection<*>,
        T3: Selection<*>,
        T1Nullable: Selection<*>,
        T2Nullable: Selection<*>,
        T3Nullable: Selection<*>,
    > : KConfigurableBaseQuery<KNonNullBaseTable3<T1, T2, T3, T1Nullable, T2Nullable, T3Nullable>> {

        fun <T: Any> add(
            table: KNonNullTable<T>
        ): Query4<
            T1,
            T2,
            T3,
            KNonNullTable<T>,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            KNullableTable<T>
        >

        fun <T: Any> add(
            table: KNullableTable<T>
        ): Query4<
            T1,
            T2,
            T3,
            KNullableTable<T>,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            KNullableTable<T>
        >

        fun <T: Any> add(
            expression: KNonNullExpression<T>
        ): Query4<
            T1,
            T2,
            T3,
            KNonNullExpression<T>,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            KNullableExpression<T>
        >

        fun <T: Any> add(
            expression: KNullableExpression<T>
        ): Query4<
            T1,
            T2,
            T3,
            KNullableExpression<T>,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            KNullableExpression<T>
        >
    }

    interface Query4<
        T1: Selection<*>,
        T2: Selection<*>,
        T3: Selection<*>,
        T4: Selection<*>,
        T1Nullable: Selection<*>,
        T2Nullable: Selection<*>,
        T3Nullable: Selection<*>,
        T4Nullable: Selection<*>,
    > : KConfigurableBaseQuery<KNonNullBaseTable4<T1, T2, T3, T4, T1Nullable, T2Nullable, T3Nullable, T4Nullable>> {

        fun <T: Any> add(
            table: KNonNullTable<T>
        ): Query5<
            T1,
            T2,
            T3,
            T4,
            KNonNullTable<T>,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            KNullableTable<T>
        >

        fun <T: Any> add(
            table: KNullableTable<T>
        ): Query5<
            T1,
            T2,
            T3,
            T4,
            KNullableTable<T>,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            KNullableTable<T>
        >

        fun <T: Any> add(
            expression: KNonNullExpression<T>
        ): Query5<
            T1,
            T2,
            T3,
            T4,
            KNonNullExpression<T>,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            KNullableExpression<T>
        >

        fun <T: Any> add(
            expression: KNullableExpression<T>
        ): Query5<
            T1,
            T2,
            T3,
            T4,
            KNullableExpression<T>,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            KNullableExpression<T>
        >
    }

    interface Query5<
        T1: Selection<*>,
        T2: Selection<*>,
        T3: Selection<*>,
        T4: Selection<*>,
        T5: Selection<*>,
        T1Nullable: Selection<*>,
        T2Nullable: Selection<*>,
        T3Nullable: Selection<*>,
        T4Nullable: Selection<*>,
        T5Nullable: Selection<*>,
    > : KConfigurableBaseQuery<KNonNullBaseTable5<T1, T2, T3, T4, T5, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable>> {

        fun <T: Any> add(
            table: KNonNullTable<T>
        ): Query6<
            T1,
            T2,
            T3,
            T4,
            T5,
            KNonNullTable<T>,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            T5Nullable,
            KNullableTable<T>
        >

        fun <T: Any> add(
            table: KNullableTable<T>
        ): Query6<
            T1,
            T2,
            T3,
            T4,
            T5,
            KNullableTable<T>,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            T5Nullable,
            KNullableTable<T>
        >

        fun <T: Any> add(
            expression: KNonNullExpression<T>
        ): Query6<
            T1,
            T2,
            T3,
            T4,
            T5,
            KNonNullExpression<T>,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            T5Nullable,
            KNullableExpression<T>
        >

        fun <T: Any> add(
            expression: KNullableExpression<T>
        ): Query6<
            T1,
            T2,
            T3,
            T4,
            T5,
            KNullableExpression<T>,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            T5Nullable,
            KNullableExpression<T>
        >
    }

    interface Query6<
        T1: Selection<*>,
        T2: Selection<*>,
        T3: Selection<*>,
        T4: Selection<*>,
        T5: Selection<*>,
        T6: Selection<*>,
        T1Nullable: Selection<*>,
        T2Nullable: Selection<*>,
        T3Nullable: Selection<*>,
        T4Nullable: Selection<*>,
        T5Nullable: Selection<*>,
        T6Nullable: Selection<*>
    > : KConfigurableBaseQuery<KNonNullBaseTable6<T1, T2, T3, T4, T5, T6, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable>> {

        fun <T: Any> add(
            table: KNonNullTable<T>
        ): Query7<
            T1,
            T2,
            T3,
            T4,
            T5,
            T6,
            KNonNullTable<T>,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            T5Nullable,
            T6Nullable,
            KNullableTable<T>
        >

        fun <T: Any> add(
            table: KNullableTable<T>
        ): Query7<
            T1,
            T2,
            T3,
            T4,
            T5,
            T6,
            KNullableTable<T>,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            T5Nullable,
            T6Nullable,
            KNullableTable<T>
        >

        fun <T: Any> add(
            expression: KNonNullExpression<T>
        ): Query7<
            T1,
            T2,
            T3,
            T4,
            T5,
            T6,
            KNonNullExpression<T>,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            T5Nullable,
            T6Nullable,
            KNullableExpression<T>
        >

        fun <T: Any> add(
            expression: KNullableExpression<T>
        ): Query7<
            T1,
            T2,
            T3,
            T4,
            T5,
            T6,
            KNullableExpression<T>,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            T5Nullable,
            T6Nullable,
            KNullableExpression<T>
        >
    }

    interface Query7<
        T1: Selection<*>,
        T2: Selection<*>,
        T3: Selection<*>,
        T4: Selection<*>,
        T5: Selection<*>,
        T6: Selection<*>,
        T7: Selection<*>,
        T1Nullable: Selection<*>,
        T2Nullable: Selection<*>,
        T3Nullable: Selection<*>,
        T4Nullable: Selection<*>,
        T5Nullable: Selection<*>,
        T6Nullable: Selection<*>,
        T7Nullable: Selection<*>
    > : KConfigurableBaseQuery<KNonNullBaseTable7<T1, T2, T3, T4, T5, T6, T7, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable>> {

        fun <T: Any> add(
            table: KNonNullTable<T>
        ): Query8<
            T1,
            T2,
            T3,
            T4,
            T5,
            T6,
            T7,
            KNonNullTable<T>,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            T5Nullable,
            T6Nullable,
            T7Nullable,
            KNullableTable<T>
        >

        fun <T: Any> add(
            table: KNullableTable<T>
        ): Query8<
            T1,
            T2,
            T3,
            T4,
            T5,
            T6,
            T7,
            KNullableTable<T>,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            T5Nullable,
            T6Nullable,
            T7Nullable,
            KNullableTable<T>
        >

        fun <T: Any> add(
            expression: KNonNullExpression<T>
        ): Query8<
            T1,
            T2,
            T3,
            T4,
            T5,
            T6,
            T7,
            KNonNullExpression<T>,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            T5Nullable,
            T6Nullable,
            T7Nullable,
            KNullableExpression<T>
        >

        fun <T: Any> add(
            expression: KNullableExpression<T>
        ): Query8<
            T1,
            T2,
            T3,
            T4,
            T5,
            T6,
            T7,
            KNullableExpression<T>,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            T5Nullable,
            T6Nullable,
            T7Nullable,
            KNullableExpression<T>
        >
    }

    interface Query8<
        T1: Selection<*>,
        T2: Selection<*>,
        T3: Selection<*>,
        T4: Selection<*>,
        T5: Selection<*>,
        T6: Selection<*>,
        T7: Selection<*>,
        T8: Selection<*>,
        T1Nullable: Selection<*>,
        T2Nullable: Selection<*>,
        T3Nullable: Selection<*>,
        T4Nullable: Selection<*>,
        T5Nullable: Selection<*>,
        T6Nullable: Selection<*>,
        T7Nullable: Selection<*>,
        T8Nullable: Selection<*>
    > : KConfigurableBaseQuery<KNonNullBaseTable8<T1, T2, T3, T4, T5, T6, T7, T8, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable, T8Nullable>> {

        fun <T: Any> add(
            table: KNonNullTable<T>
        ): Query9<
            T1,
            T2,
            T3,
            T4,
            T5,
            T6,
            T7,
            T8,
            KNonNullTable<T>,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            T5Nullable,
            T6Nullable,
            T7Nullable,
            T8Nullable,
            KNullableTable<T>
        >

        fun <T: Any> add(
            table: KNullableTable<T>
        ): Query9<
            T1,
            T2,
            T3,
            T4,
            T5,
            T6,
            T7,
            T8,
            KNullableTable<T>,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            T5Nullable,
            T6Nullable,
            T7Nullable,
            T8Nullable,
            KNullableTable<T>
        >

        fun <T: Any> add(
            expression: KNonNullExpression<T>
        ): Query9<
            T1,
            T2,
            T3,
            T4,
            T5,
            T6,
            T7,
            T8,
            KNonNullExpression<T>,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            T5Nullable,
            T6Nullable,
            T7Nullable,
            T8Nullable,
            KNullableExpression<T>
        >

        fun <T: Any> add(
            expression: KNullableExpression<T>
        ): Query9<
            T1,
            T2,
            T3,
            T4,
            T5,
            T6,
            T7,
            T8,
            KNullableExpression<T>,
            T1Nullable,
            T2Nullable,
            T3Nullable,
            T4Nullable,
            T5Nullable,
            T6Nullable,
            T7Nullable,
            T8Nullable,
            KNullableExpression<T>
        >
    }

    interface Query9<
        T1: Selection<*>,
        T2: Selection<*>,
        T3: Selection<*>,
        T4: Selection<*>,
        T5: Selection<*>,
        T6: Selection<*>,
        T7: Selection<*>,
        T8: Selection<*>,
        T9: Selection<*>,
        T1Nullable: Selection<*>,
        T2Nullable: Selection<*>,
        T3Nullable: Selection<*>,
        T4Nullable: Selection<*>,
        T5Nullable: Selection<*>,
        T6Nullable: Selection<*>,
        T7Nullable: Selection<*>,
        T8Nullable: Selection<*>,
        T9Nullable: Selection<*>
    > : KConfigurableBaseQuery<KNonNullBaseTable9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T1Nullable, T2Nullable, T3Nullable, T4Nullable, T5Nullable, T6Nullable, T7Nullable, T8Nullable, T9Nullable>>
}