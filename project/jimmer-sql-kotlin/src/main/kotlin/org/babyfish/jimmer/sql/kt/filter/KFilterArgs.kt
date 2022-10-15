package org.babyfish.jimmer.sql.kt.filter

import org.babyfish.jimmer.sql.kt.ast.query.KAbstractSortable
import org.babyfish.jimmer.sql.kt.ast.table.KProps

interface KFilterArgs<E: Any> : KAbstractSortable<E, KProps<E>>