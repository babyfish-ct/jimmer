package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.sql.association.Association
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx

val <S: Any> KNonNullTableEx<Association<S, *>>.source: KNonNullTableEx<S>
    get() = join("source")

val <T: Any> KNonNullTableEx<Association<*, T>>.target: KNonNullTableEx<T>
    get() = join("target")

val <S: Any, T: Any> KNonNullTable<Association<S, T>>.source: KNonNullTable<S>
    get() = join("source")

val <T: Any> KNonNullTable<Association<*, T>>.target: KNonNullTable<T>
    get() = join("target")