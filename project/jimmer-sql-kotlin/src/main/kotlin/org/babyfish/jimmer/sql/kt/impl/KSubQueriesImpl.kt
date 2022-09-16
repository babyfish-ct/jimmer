package org.babyfish.jimmer.sql.kt.impl

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.association.Association
import org.babyfish.jimmer.sql.association.meta.AssociationType
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl
import org.babyfish.jimmer.sql.ast.impl.query.MutableSubQueryImpl
import org.babyfish.jimmer.sql.kt.KSubQueries
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableSubQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableSubQuery
import org.babyfish.jimmer.sql.kt.ast.query.impl.KMutableSubQueryImpl
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal class KSubQueriesImpl<P: Any>(
    private val parent: AbstractMutableStatementImpl
) : KSubQueries<P> {

    override fun <E : Any, R, SQ: KConfigurableSubQuery<R>> forEntity(
        entityType: KClass<E>,
        block: KMutableSubQuery<P, E>.() -> SQ
    ): SQ {
        val immutableType = ImmutableType.get(entityType.java)
        val subQuery = MutableSubQueryImpl(parent, immutableType)
        val typedSubQuery = KMutableSubQueryImpl<P, E>(subQuery).block()
        subQuery.freeze()
        return typedSubQuery
    }

    override fun <S : Any, T : Any, R, SQ : KConfigurableSubQuery<R>> forReference(
        prop: KProperty1<S, T?>,
        block: KMutableSubQuery<P, Association<S, T>>.() -> SQ
    ): SQ {
        return forAssociation(prop.toImmutableProp(), block)
    }

    override fun <S : Any, T : Any, R, SQ : KConfigurableSubQuery<R>> forList(
        prop: KProperty1<S, List<T>>,
        block: KMutableSubQuery<P, Association<S, T>>.() -> SQ
    ): SQ {
        return forAssociation(prop.toImmutableProp(), block)
    }

    private fun <S : Any, T : Any, R, SQ : KConfigurableSubQuery<R>> forAssociation(
        immutableProp: ImmutableProp,
        block: KMutableSubQuery<P, Association<S, T>>.() -> SQ
    ): SQ {
        val associationType = AssociationType.of(immutableProp)
        val subQuery = MutableSubQueryImpl(parent, associationType)
        val typedSubQuery = KMutableSubQueryImpl<P, Association<S, T>>(subQuery).block()
        subQuery.freeze()
        return typedSubQuery
    }
}