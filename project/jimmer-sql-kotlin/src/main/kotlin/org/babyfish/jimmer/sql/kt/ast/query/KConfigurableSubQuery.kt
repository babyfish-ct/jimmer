package org.babyfish.jimmer.sql.kt.ast.query

interface KConfigurableSubQuery<R> : KTypedSubQuery<R> {

    fun limit(limit: Int): KConfigurableSubQuery<R>

    fun offset(offset: Int): KConfigurableSubQuery<R>

    fun limit(limit: Int?, offset: Int?): KConfigurableSubQuery<R>

    fun distinct(): KConfigurableSubQuery<R>

    interface NonNull<R: Any>: KConfigurableSubQuery<R>, KTypedSubQuery.NonNull<R> {

        override fun limit(limit: Int): NonNull<R>

        override fun offset(offset: Int): NonNull<R>

        override fun limit(limit: Int?, offset: Int?): NonNull<R>

        override fun distinct(): NonNull<R>
    }

    interface Nullable<R: Any>: KConfigurableSubQuery<R>, KTypedSubQuery.Nullable<R> {

        override fun limit(limit: Int): Nullable<R>

        override fun offset(offset: Int): Nullable<R>

        override fun limit(limit: Int?, offset: Int?): Nullable<R>

        override fun distinct(): Nullable<R>
    }
}