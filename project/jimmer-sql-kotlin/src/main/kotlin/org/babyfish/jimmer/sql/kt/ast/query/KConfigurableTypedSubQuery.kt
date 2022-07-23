package org.babyfish.jimmer.sql.kt.ast.query

interface KConfigurableTypedSubQuery<R> : KTypedSubQuery<R> {

    fun limit(limit: Int, offset: Int = 0): KConfigurableTypedSubQuery<R>

    fun distinct(): KConfigurableTypedSubQuery<R>
}