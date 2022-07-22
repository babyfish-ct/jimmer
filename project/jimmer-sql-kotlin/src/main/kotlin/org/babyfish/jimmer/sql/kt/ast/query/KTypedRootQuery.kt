package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.ast.Executable
import org.babyfish.jimmer.sql.ast.query.TypedRootQuery

interface KTypedRootQuery<R> : Executable<List<R>> {

    infix fun union(other: TypedRootQuery<R>): TypedRootQuery<R>

    infix fun unionAll(other: TypedRootQuery<R>): TypedRootQuery<R>

    infix operator fun minus(other: TypedRootQuery<R>): TypedRootQuery<R>

    infix fun intersect(other: TypedRootQuery<R>): TypedRootQuery<R>
}