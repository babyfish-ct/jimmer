package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.sql.ast.table.RecursiveRef

class KRecursiveRef<B: KBaseTable>(
    internal val javaRef: RecursiveRef<*>
)