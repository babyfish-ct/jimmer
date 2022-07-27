package org.babyfish.jimmer.sql.kt.ast.mutation.impl

import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult
import org.babyfish.jimmer.sql.kt.ast.mutation.KSimpleSaveResult

internal class KSimpleSaveResultImpl<E: Any>(
    javaResult: SimpleSaveResult<E>
) : KMutationResultImpl(javaResult), KSimpleSaveResult<E> {

    @Suppress("UNCHECKED_CAST")
    override val originalEntity: E
        get() = (javaResult as SimpleSaveResult<E>).originalEntity

    @Suppress("UNCHECKED_CAST")
    override val modifiedEntity: E
        get() = (javaResult as SimpleSaveResult<E>).modifiedEntity

    @Suppress("UNCHECKED_CAST")
    override val isModified: Boolean
        get() = (javaResult as SimpleSaveResult<E>).isModified
}