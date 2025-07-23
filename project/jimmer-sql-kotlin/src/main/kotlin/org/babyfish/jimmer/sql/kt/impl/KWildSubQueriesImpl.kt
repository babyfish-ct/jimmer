package org.babyfish.jimmer.sql.kt.impl

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.association.Association
import org.babyfish.jimmer.sql.association.meta.AssociationType
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl
import org.babyfish.jimmer.sql.ast.impl.query.MutableSubQueryImpl
import org.babyfish.jimmer.sql.kt.KWildSubQueries
import org.babyfish.jimmer.sql.kt.ast.query.KMutableSubQuery
import org.babyfish.jimmer.sql.kt.ast.query.impl.KMutableSubQueryImpl
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx
import org.babyfish.jimmer.sql.kt.ast.table.KPropsLike
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal class KWildSubQueriesImpl<P: KPropsLike>(
    private val parent: AbstractMutableStatementImpl,
    private val parentTable: P? = null
) : KWildSubQueries<P> {

    override fun <E : Any> forEntity(
        entityType: KClass<E>,
        block: KMutableSubQuery<P, KNonNullTableEx<E>>.() -> Unit
    ): KMutableSubQuery<P, KNonNullTableEx<E>> {
        val immutableType = ImmutableType.get(entityType.java)
        val subQuery = if (parent.context != null) {
            MutableSubQueryImpl(parent, immutableType)
        } else {
            MutableSubQueryImpl(parent.sqlClient, immutableType)
        }
        val wrappedQuery = KMutableSubQueryImpl<P, E>(subQuery, parentTable)
        wrappedQuery.block()
        return wrappedQuery
    }

    override fun <S : Any, T: Any> forReference(
        prop: KProperty1<S, T?>,
        block: KMutableSubQuery<P, KNonNullTableEx<Association<S, T>>>.() -> Unit
    ): KMutableSubQuery<P, KNonNullTableEx<Association<S, T>>> {
        return forAssociation(prop.toImmutableProp(), block)
    }

    override fun <S : Any, T : Any> forList(
        prop: KProperty1<S, List<T>>,
        block: KMutableSubQuery<P, KNonNullTableEx<Association<S, T>>>.() -> Unit
    ): KMutableSubQuery<P, KNonNullTableEx<Association<S, T>>> {
        return forAssociation(prop.toImmutableProp(), block)
    }

    private fun <S : Any, T : Any> forAssociation(
        immutableProp: ImmutableProp,
        block: KMutableSubQuery<P, KNonNullTableEx<Association<S, T>>>.() -> Unit
    ): KMutableSubQuery<P, KNonNullTableEx<Association<S, T>>> {
        val associationType = AssociationType.of(immutableProp)
        val subQuery = MutableSubQueryImpl(parent, associationType)
        val wrappedQuery = KMutableSubQueryImpl<P, Association<S, T>>(subQuery, parentTable)
        wrappedQuery.block()
        return wrappedQuery
    }
}