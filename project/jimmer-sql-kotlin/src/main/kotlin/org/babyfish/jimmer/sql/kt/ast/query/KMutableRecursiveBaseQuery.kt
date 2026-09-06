package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.kt.ast.table.KPropsLike

interface KMutableRecursiveBaseQuery<E : Any, R : KPropsLike> : KMutableBaseQuery<E> {

    val recursive: R
}
