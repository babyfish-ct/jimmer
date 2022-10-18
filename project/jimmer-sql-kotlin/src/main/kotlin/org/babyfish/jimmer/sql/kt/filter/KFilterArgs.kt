package org.babyfish.jimmer.sql.kt.filter

import org.babyfish.jimmer.sql.kt.ast.query.AbstractKSortable
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullProps

interface KFilterArgs<E: Any> : AbstractKSortable<E, KNonNullProps<E>>