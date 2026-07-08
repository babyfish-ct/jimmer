package org.babyfish.jimmer.sql.kt.ast.table.impl

import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.JoinType
import org.babyfish.jimmer.sql.ast.impl.PredicateImplementor
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.JavaToKotlinPredicateWrapper
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx
import org.babyfish.jimmer.sql.kt.ast.table.KNullableTableEx
import org.babyfish.jimmer.sql.kt.ast.table.KTableEx
import kotlin.reflect.KClass

object KPolymorphicTables {

    @Suppress("UNCHECKED_CAST")
    fun <S : Any> treatAs(table: KTableEx<*>, type: KClass<S>): KNonNullTableEx<S> =
        KNonNullTableExImpl(
            (table as KTableImplementor<*>).javaTable.treatAsImplementor<S>(
                ImmutableType.get(type.java),
                JoinType.INNER
            )
        )

    @Suppress("UNCHECKED_CAST")
    fun <S : Any> tryTreatAs(table: KTableEx<*>, type: KClass<S>): KNullableTableEx<S> =
        KNullableTableExImpl(
            (table as KTableImplementor<*>).javaTable.treatAsImplementor<S>(
                ImmutableType.get(type.java),
                JoinType.LEFT
            )
        )

    fun instanceOf(table: KTableEx<*>, type: KClass<*>): KNonNullExpression<Boolean> =
        JavaToKotlinPredicateWrapper(
            (table as KTableImplementor<*>).javaTable.instanceOf(
                ImmutableType.get(type.java)
            ) as PredicateImplementor
        )

    fun exactType(table: KTableEx<*>, type: KClass<*>): KNonNullExpression<Boolean> =
        JavaToKotlinPredicateWrapper(
            (table as KTableImplementor<*>).javaTable.exactType(
                ImmutableType.get(type.java)
            ) as PredicateImplementor
        )
}
