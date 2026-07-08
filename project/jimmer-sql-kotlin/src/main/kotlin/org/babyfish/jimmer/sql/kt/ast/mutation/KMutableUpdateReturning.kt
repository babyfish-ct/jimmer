package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.kt.DslScope

@DslScope
interface KMutableUpdateReturning<E : Any> : KMutableUpdate<E>, KReturningSelectable
