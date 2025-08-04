package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode
import org.babyfish.jimmer.sql.kt.ast.query.KFilterable
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx

@DslScope
interface KMutableDelete<E: Any> : KFilterable<KNonNullTableEx<E>> {

    fun disableDissociation()

    fun setMode(mode: DeleteMode)
}