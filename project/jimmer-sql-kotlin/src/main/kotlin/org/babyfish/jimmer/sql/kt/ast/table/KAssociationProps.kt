package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.sql.association.Association
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullPropExpression

val <S: Any, T: Any> KNonNullProps<Association<S, T>>.sourceId: KNonNullPropExpression<Any>
    get() = getAssociatedId<Any>("source") as KNonNullPropExpression<Any>

val <S: Any, T: Any> KNonNullProps<Association<S, T>>.targetId: KNonNullPropExpression<Any>
    get() = getAssociatedId<Any>("target") as KNonNullPropExpression<Any>

val <S: Any, T: Any>  KNonNullTable<Association<S, T>>.source: KNonNullTable<S>
    get() = join("source")

val <S: Any, T: Any>  KNonNullTable<Association<S, T>>.target: KNonNullTable<T>
    get() = join("target")

val <S: Any, T: Any>  KNonNullTableEx<Association<S, T>>.source: KNonNullTableEx<S>
    get() = join("source")

val <S: Any, T: Any>  KNonNullTableEx<Association<S, T>>.target: KNonNullTableEx<T>
    get() = join("target")
