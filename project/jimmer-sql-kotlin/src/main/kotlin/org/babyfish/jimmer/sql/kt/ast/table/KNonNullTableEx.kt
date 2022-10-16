package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.sql.association.Association

interface KNonNullTableEx<E: Any> : KNonNullTable<E>, KTableEx<E>

val <S: Any, T: Any> KNonNullTableEx<Association<S, T>>.source: KNonNullTableEx<S>
    get() = join("source")

val <S: Any, T: Any> KNonNullTableEx<Association<S, T>>.target: KNonNullTableEx<T>
    get() = join("target")
