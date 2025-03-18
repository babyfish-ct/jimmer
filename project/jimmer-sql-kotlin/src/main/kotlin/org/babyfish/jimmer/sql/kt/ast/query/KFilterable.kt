package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable

@DslScope
interface KFilterable<E: Any> : AbstractKFilterable<E, KNonNullTable<E>>