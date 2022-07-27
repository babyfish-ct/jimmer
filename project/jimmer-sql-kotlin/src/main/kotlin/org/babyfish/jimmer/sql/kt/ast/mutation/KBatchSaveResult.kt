package org.babyfish.jimmer.sql.kt.ast.mutation

interface KBatchSaveResult<E: Any> : KMutationResult {

    val simpleResults: List<KSimpleSaveResult<E>>
}