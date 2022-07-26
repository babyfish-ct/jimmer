package org.babyfish.jimmer.sql.kt.fetcher.impl

import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.fetcher.FieldConfig
import org.babyfish.jimmer.sql.fetcher.RecursiveListFieldConfig
import org.babyfish.jimmer.sql.kt.fetcher.KFieldDsl
import org.babyfish.jimmer.sql.kt.fetcher.KListFieldDsl
import org.babyfish.jimmer.sql.kt.fetcher.KRecursiveFieldDsl
import org.babyfish.jimmer.sql.kt.fetcher.KRecursiveListFieldDsl
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

    fun <E : Any> list(
        block: (KListFieldDsl<E>.() -> Unit)?
    ): Consumer<FieldConfig<E, Table<E>>>? =
        block?.let {
            Consumer<FieldConfig<E, Table<E>>> { args ->
                it(FieldDslImpl(args as RecursiveListFieldConfig<E, Table<E>>))
            }
        }

    fun <E : Any> recursive(
        block: (KRecursiveFieldDsl<E>.() -> Unit)?
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