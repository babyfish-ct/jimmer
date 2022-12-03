package org.babyfish.jimmer.sql.ast.tuple

import org.babyfish.jimmer.sql.ast.impl.TupleImplementor

data class Tuple5<T1, T2, T3, T4, T5>(
    val _1: T1,
    val _2: T2,
    val _3: T3,
    val _4: T4,
    val _5: T5
) : TupleImplementor