package org.babyfish.jimmer.sql.kt.fetcher

import org.babyfish.jimmer.sql.kt.ast.query.KSortable
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable

interface KFieldFilterDsl<E: Any> : KSortable<KNonNullTable<E>>