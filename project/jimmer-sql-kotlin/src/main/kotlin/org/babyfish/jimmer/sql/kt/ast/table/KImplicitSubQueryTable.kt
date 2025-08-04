package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.sql.kt.ast.query.KSubQueryProvider

@DslScope
interface KImplicitSubQueryTable<E: Any> : KNonNullTableEx<E>, KSubQueryProvider<KNonNullTableEx<E>>