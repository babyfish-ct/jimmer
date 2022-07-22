package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.sql.JoinType
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression

interface KTable<E> {
    fun <EXP: KExpression<*>> get(prop: String): EXP
    fun <X: Any> join(prop: String): KTable<X>
    fun <X: Any> join(prop: String, joinType: JoinType): KTable<X>
    fun <X: Any> inverseJoin(prop: String): KTable<X>
    fun <X: Any> inverseJoin(prop: String, joinType: JoinType): KTable<X>
}