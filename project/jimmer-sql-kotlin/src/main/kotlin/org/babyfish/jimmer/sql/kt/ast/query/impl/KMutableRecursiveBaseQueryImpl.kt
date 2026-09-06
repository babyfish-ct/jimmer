package org.babyfish.jimmer.sql.kt.ast.query.impl

import org.babyfish.jimmer.sql.ast.impl.query.MutableRecursiveBaseQueryImpl
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRecursiveBaseQuery
import org.babyfish.jimmer.sql.kt.ast.table.KPropsLike

internal class KMutableRecursiveBaseQueryImpl<E : Any, R : KPropsLike>(
    javaBaseQuery: MutableRecursiveBaseQueryImpl<*>,
    override val recursive: R
) : KMutableBaseQueryImpl<E>(javaBaseQuery), KMutableRecursiveBaseQuery<E, R>
