package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.sql.kt.ast.table.impl.KRemoteRefImpl
import org.babyfish.jimmer.sql.kt.ast.table.impl.KTableImplementor

interface KRemoteRef<E: Any> {

    interface NonNull<E: Any> : KRemoteRef<E>

    interface Nullable<E: Any> : KRemoteRef<E>

    companion object {

        @JvmStatic
        fun <E: Any> protect(table: KNonNullTable<E>): KRemoteRef.NonNull<E> =
            KRemoteRefImpl.NonNull((table as KTableImplementor<*>).javaTable)

        @JvmStatic
        fun <E: Any> protect(table: KNullableTable<E>): KRemoteRef.Nullable<E> =
            KRemoteRefImpl.Nullable((table as KTableImplementor<*>).javaTable)
    }
}