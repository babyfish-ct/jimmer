package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable

interface KSortable<E: Any> : KFilterable<E>, AbstractKSortable<E, KNonNullTable<E>>