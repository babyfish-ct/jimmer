package org.babyfish.jimmer.sql.kt.fetcher.impl

import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.fetcher.FieldConfig
import org.babyfish.jimmer.sql.fetcher.RecursiveListFieldConfig
import org.babyfish.jimmer.sql.fetcher.ReferenceFetchType
import org.babyfish.jimmer.sql.kt.fetcher.*
import java.util.function.Consumer

object JavaFieldConfigUtils {

    fun <E : Any> simple(
        block: (KFieldDsl<E>.() -> Unit)?
    ): Consumer<FieldConfig<E, Table<E>>>? =
        block?.let {
            Consumer<FieldConfig<E, Table<E>>> { args ->
                it(FieldDslImpl(args as RecursiveListFieldConfig<E, Table<E>>))
            }
        }

    fun <E : Any> reference(
        block: (KReferenceFieldDsl<E>.() -> Unit)?
    ): Consumer<FieldConfig<E, Table<E>>>? =
        block?.let {
            Consumer<FieldConfig<E, Table<E>>> { args ->
                it(FieldDslImpl(args as RecursiveListFieldConfig<E, Table<E>>))
            }
        }

    fun <E: Any> reference(
        fetchType: ReferenceFetchType
    ): Consumer<FieldConfig<E, Table<E>>>? =
        reference {
            fetchType(fetchType)
        }

    fun <E : Any> list(
        block: (KListFieldDsl<E>.() -> Unit)?
    ): Consumer<FieldConfig<E, Table<E>>>? =
        block?.let {
            Consumer<FieldConfig<E, Table<E>>> { args ->
                it(FieldDslImpl(args as RecursiveListFieldConfig<E, Table<E>>))
            }
        }

    fun <E : Any> recursiveReference(
        block: (KRecursiveReferenceFieldDsl<E>.() -> Unit)?
    ): Consumer<FieldConfig<E, Table<E>>>? =
        block?.let {
            Consumer<FieldConfig<E, Table<E>>> { args ->
                it(FieldDslImpl(args as RecursiveListFieldConfig<E, Table<E>>))
            }
        }

    fun <E : Any> recursiveList(
        block: (KRecursiveListFieldDsl<E>.() -> Unit)?
    ): Consumer<FieldConfig<E, Table<E>>>? =
        block?.let {
            Consumer<FieldConfig<E, Table<E>>> { args ->
                it(FieldDslImpl(args as RecursiveListFieldConfig<E, Table<E>>))
            }
        }
}