package org.babyfish.jimmer.sql.ast.tuple

fun <T1, T2, R> Tuple2<T1, T2>.map(block: (T1, T2) -> R): R = block(_1, _2)

fun <T1, T2, T3, R> Tuple3<T1, T2, T3>.map(block: (T1, T2, T3) -> R): R = block(_1, _2, _3)

fun <T1, T2, T3, T4, R> Tuple4<T1, T2, T3, T4>.map(block: (T1, T2, T3, T4) -> R): R = block(_1, _2, _3, _4)

fun <T1, T2, T3, T4, T5, R> Tuple5<T1, T2, T3, T4, T5>.map(block: (T1, T2, T3, T4, T5) -> R): R =
    block(_1, _2, _3, _4, _5)

fun <T1, T2, T3, T4, T5, T6, R> Tuple6<T1, T2, T3, T4, T5, T6>.map(block: (T1, T2, T3, T4, T5, T6) -> R): R =
    block(_1, _2, _3, _4, _5, _6)

fun <T1, T2, T3, T4, T5, T6, T7, R> Tuple7<T1, T2, T3, T4, T5, T6, T7>.map(block: (T1, T2, T3, T4, T5, T6, T7) -> R): R =
    block(_1, _2, _3, _4, _5, _6, _7)

fun <T1, T2, T3, T4, T5, T6, T7, T8, R> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>.map(block: (T1, T2, T3, T4, T5, T6, T7, T8) -> R): R =
    block(_1, _2, _3, _4, _5, _6, _7, _8)

fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>.map(block: (T1, T2, T3, T4, T5, T6, T7, T8, T9) -> R): R =
    block(_1, _2, _3, _4, _5, _6, _7, _8, _9)