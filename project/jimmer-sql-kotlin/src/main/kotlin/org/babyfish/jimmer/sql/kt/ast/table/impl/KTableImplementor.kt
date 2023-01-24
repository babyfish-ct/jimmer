package org.babyfish.jimmer.sql.kt.ast.table.impl

import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.kt.ast.table.KTableEx

interface KTableImplementor<E: Any> : KTableEx<E> {

    val javaTable: TableImplementor<E>
}