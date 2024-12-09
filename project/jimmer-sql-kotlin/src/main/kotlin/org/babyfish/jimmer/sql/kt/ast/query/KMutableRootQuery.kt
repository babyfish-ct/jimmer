package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.Specification
import org.babyfish.jimmer.sql.kt.ast.query.specification.KSpecification
import java.sql.Connection

interface KMutableRootQuery<E: Any> : KMutableQuery<E>, KRootSelectable<E> {

    fun where(specification: Specification<E>?)

    fun where(specification: KSpecification<E>?)
}