package org.babyfish.jimmer.sql.kt.ast.query.impl

import org.babyfish.jimmer.sql.ast.impl.query.MutableRecursiveBaseQueryImpl
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRecursiveBaseQuery
import org.babyfish.jimmer.sql.kt.ast.table.KBaseTable

internal class KMutableRecursiveBaseQueryImpl<E: Any, R: KBaseTable>(
    javaBaseQuery: MutableRecursiveBaseQueryImpl<*>,
    override val recursive: R
) : KMutableBaseQueryImpl<E>(javaBaseQuery), KMutableRecursiveBaseQuery<E, R>