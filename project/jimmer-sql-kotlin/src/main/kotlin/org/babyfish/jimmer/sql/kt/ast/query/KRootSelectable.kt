package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.ast.tuple.Tuple2
import org.babyfish.jimmer.sql.kt.ast.expression.KSelection

interface KRootSelectable {

    fun <T> select(selection: KSelection<T>): KConfigurableTypedRootQuery<T>

    fun <T1, T2> select2(
        selection1: KSelection<T1>,
        selection2: KSelection<T2>
    ): KConfigurableTypedRootQuery<Tuple2<T1, T2>>
}