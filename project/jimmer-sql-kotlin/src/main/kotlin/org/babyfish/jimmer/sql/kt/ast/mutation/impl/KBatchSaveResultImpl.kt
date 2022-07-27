package org.babyfish.jimmer.sql.kt.ast.mutation.impl

import org.babyfish.jimmer.sql.ast.mutation.BatchSaveResult
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult
import org.babyfish.jimmer.sql.kt.ast.mutation.KBatchSaveResult
import org.babyfish.jimmer.sql.kt.ast.mutation.KSimpleSaveResult

internal class KBatchSaveResultImpl<E: Any>(
    javaResult: BatchSaveResult<E>
) : KMutationResultImpl(javaResult), KBatchSaveResult<E> {

    @Suppress("UNCHECKED_CAST")
    override val simpleResults: List<KSimpleSaveResult<E>> =
        javaResult.simpleResults.map { KSimpleSaveResultImpl(it) }
}