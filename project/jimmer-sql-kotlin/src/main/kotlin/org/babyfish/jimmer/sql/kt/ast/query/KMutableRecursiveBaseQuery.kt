package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.kt.ast.table.KBaseTable

interface KMutableRecursiveBaseQuery<E: Any, R: KBaseTable> : KMutableBaseQuery<E> {

    val recursive: R
}