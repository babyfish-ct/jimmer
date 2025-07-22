package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.sql.ast.Selection

interface KBaseTable

interface KNonNullBaseTable : KBaseTable

interface KNullableBaseTable : KBaseTable

interface KNonNullBaseTable1<
    T1: Selection<*>,
    T1Nullable: Selection<*>
> : KNonNullBaseTable {

    val _1: T1
}

interface KNullableBaseTable1<
    T1: Selection<*>,
    T1Nullable: Selection<*>
> : KNullableBaseTable {

    val _1: T1Nullable
}

interface KNonNullBaseTable2<
    T1: Selection<*>,
    T2: Selection<*>,
    T1Nullable: Selection<*>,
    T2Nullable: Selection<*>
> : KNonNullBaseTable {

    val _1: T1

    val _2: T2
}

interface KNullableBaseTable2<
    T1: Selection<*>,
    T2: Selection<*>,
    T1Nullable: Selection<*>,
    T2Nullable: Selection<*>
> : KNullableBaseTable {

    val _1: T1Nullable

    val _2: T2Nullable
}

interface KNonNullBaseTable3<
    T1: Selection<*>,
    T2: Selection<*>,
    T3: Selection<*>,
    T1Nullable: Selection<*>,
    T2Nullable: Selection<*>,
    T3Nullable: Selection<*>
> : KNonNullBaseTable

interface KNonNullBaseTable4<
    T1: Selection<*>,
    T2: Selection<*>,
    T3: Selection<*>,
    T4: Selection<*>
> : KNonNullBaseTable

interface KNonNullBaseTable5<
    T1: Selection<*>,
    T2: Selection<*>,
    T3: Selection<*>,
    T4: Selection<*>,
    T5: Selection<*>
> : KNonNullBaseTable

interface KNonNullBaseTable6<
    T1: Selection<*>,
    T2: Selection<*>,
    T3: Selection<*>,
    T4: Selection<*>,
    T5: Selection<*>,
    T6: Selection<*>
> : KNonNullBaseTable

interface KNonNullBaseTable7<
    T1: Selection<*>,
    T2: Selection<*>,
    T3: Selection<*>,
    T4: Selection<*>,
    T5: Selection<*>,
    T6: Selection<*>,
    T7: Selection<*>
> : KNonNullBaseTable

interface KNonNullBaseTable8<
    T1: Selection<*>,
    T2: Selection<*>,
    T3: Selection<*>,
    T4: Selection<*>,
    T5: Selection<*>,
    T6: Selection<*>,
    T7: Selection<*>,
    T8: Selection<*>,
> : KNonNullBaseTable

interface KNonNullBaseTable9<
    T1: Selection<*>,
    T2: Selection<*>,
    T3: Selection<*>,
    T4: Selection<*>,
    T5: Selection<*>,
    T6: Selection<*>,
    T7: Selection<*>,
    T8: Selection<*>,
    T9: Selection<*>
> : KNonNullBaseTable


