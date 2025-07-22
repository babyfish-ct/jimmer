package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.kt.ast.table.KBaseTable

interface KTypedBaseQuery<T: KBaseTable> {

    fun asBaseTable(): T

    fun asCteBaseTable(): T
}