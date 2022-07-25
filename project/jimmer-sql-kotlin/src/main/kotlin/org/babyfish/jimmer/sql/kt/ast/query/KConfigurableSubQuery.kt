package org.babyfish.jimmer.sql.kt.ast.query

interface KConfigurableSubQuery<R> : KTypedSubQuery<R> {

    fun limit(limit: Int, offset: Int = 0): KConfigurableSubQuery<R>

    fun distinct(): KConfigurableSubQuery<R>

    interface NonNull<R: Any>: KConfigurableSubQuery<R>, KTypedSubQuery.NonNull<R>

    interface Nullable<R: Any>: KConfigurableSubQuery<R>, KTypedSubQuery.Nullable<R>
}