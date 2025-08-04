package org.babyfish.jimmer.sql.kt.filter

import org.babyfish.jimmer.sql.kt.ast.query.KSortable
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullProps

interface KFilterArgs<E: Any> : KSortable<KNonNullProps<E>>