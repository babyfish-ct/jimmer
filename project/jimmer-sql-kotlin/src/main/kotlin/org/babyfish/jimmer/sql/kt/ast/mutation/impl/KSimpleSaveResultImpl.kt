package org.babyfish.jimmer.sql.kt.ast.mutation.impl

import org.babyfish.jimmer.View
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult
import org.babyfish.jimmer.sql.kt.ast.mutation.KSimpleSaveResult

internal open class KSimpleSaveResultImpl<E: Any>(
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

    internal class ViewImpl<E: Any, V: View<E>>(
        javaResult: SimpleSaveResult.View<E, V>
    ) : KSimpleSaveResultImpl<E>(javaResult), KSimpleSaveResult.View<E, V> {

        @Suppress("UNCHECKED_CAST")
        override val modifiedView: V
            get() = (javaResult as SimpleSaveResult.View<E, V>).modifiedView
    }
}