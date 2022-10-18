package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable

interface KFilterable<E: Any> : AbstractKFilterable<E, KNonNullTable<E>>