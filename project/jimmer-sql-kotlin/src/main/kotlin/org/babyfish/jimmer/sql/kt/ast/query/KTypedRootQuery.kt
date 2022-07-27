package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.kt.KExecutable

interface KTypedRootQuery<R> : KExecutable<List<R>> {

    infix fun union(other: KTypedRootQuery<R>): KTypedRootQuery<R>

    infix fun unionAll(other: KTypedRootQuery<R>): KTypedRootQuery<R>

    infix operator fun minus(other: KTypedRootQuery<R>): KTypedRootQuery<R>

    infix fun intersect(other: KTypedRootQuery<R>): KTypedRootQuery<R>
}