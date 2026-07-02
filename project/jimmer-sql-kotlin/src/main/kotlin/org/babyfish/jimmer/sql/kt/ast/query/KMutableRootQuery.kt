package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.Specification
import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import org.babyfish.jimmer.sql.kt.ast.table.KPropsLike

@DslScope
interface KMutableRootQuery<P: KPropsLike> : KMutableQuery<P>, KRootSelectable<P> {

    interface ForEntity<E: Any> : KMutableRootQuery<KNonNullTable<E>> {

        fun where(specification: Specification<out E>?)
    }
}
