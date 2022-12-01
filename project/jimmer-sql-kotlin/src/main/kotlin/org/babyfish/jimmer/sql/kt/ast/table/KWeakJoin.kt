package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.sql.ast.Predicate
import org.babyfish.jimmer.sql.ast.impl.table.CustomWeakJoinTableExporter
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.ast.table.WeakJoin
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.toJavaPredicate
import org.babyfish.jimmer.sql.kt.ast.table.impl.KNonNullTableExImpl

abstract class KWeakJoin<S: Any, T: Any> : WeakJoin<Table<S>, Table<T>>, CustomWeakJoinTableExporter {

    final override fun on(source: Table<S>, target: Table<T>): Predicate =
        on(
            KNonNullTableExImpl(source as TableImplementor<S>, JOIN_ERROR_REASON),
            KNonNullTableExImpl(target as TableImplementor<T>, JOIN_ERROR_REASON)
        ).toJavaPredicate()

    abstract fun on(
        source: KNonNullTable<S>,
        target: KNonNullTable<T>
    ): KNonNullExpression<Boolean>

    companion object {
        private val JOIN_ERROR_REASON = "it is forbidden in the implementation of \"" +
            KWeakJoin::class.java.name +
            "\""
    }
}