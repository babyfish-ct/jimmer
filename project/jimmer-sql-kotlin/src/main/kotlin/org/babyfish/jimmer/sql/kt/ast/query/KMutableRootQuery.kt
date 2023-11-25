package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.kt.ast.query.specification.KSpecification

interface KMutableRootQuery<E: Any> : KMutableQuery<E>, KRootSelectable<E> {

    fun where(specification: KSpecification<E>?)
}