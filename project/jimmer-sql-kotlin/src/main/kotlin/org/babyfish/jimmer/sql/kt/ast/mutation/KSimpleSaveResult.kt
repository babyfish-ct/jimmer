package org.babyfish.jimmer.sql.kt.ast.mutation

interface KSimpleSaveResult<E: Any> : KMutationResult {

    val originalEntity: E

    val modifiedEntity: E

    val isModified: Boolean
}