package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.lang.NewChain
import org.babyfish.jimmer.sql.ast.tuple.Tuple2
import java.sql.Connection

interface KAssociations {

    @NewChain
    fun forConnection(con: Connection): KAssociations

    @NewChain
    fun reverse(): KAssociations

    @NewChain
    fun checkExistence(checkExistence: Boolean = true): KAssociations

    fun save(
        sourceId: Any,
        targetId: Any,
        checkExistence: Boolean? = null,
        con: Connection? = null
    ): Int

    fun saveAll(
        sourceIds: Collection<*>,
        targetIds: Collection<*>,
        checkExistence: Boolean? = null,
        con: Connection? = null
    ): Int

    @Deprecated(
        "Will be deleted in 1.0, please use saveAll",
        replaceWith = ReplaceWith("saveAll")
    )
    fun batchSave(
        sourceIds: Collection<*>,
        targetIds: Collection<*>,
        checkExistence: Boolean? = null,
        con: Connection? = null
    ): Int =
        saveAll(sourceIds, targetIds, checkExistence, con)

    fun saveAll(
        idTuples: Collection<Tuple2<*, *>>,
        checkExistence: Boolean? = null,
        con: Connection? = null
    ): Int

    @Deprecated(
        "Will be deleted in 1.0, please use saveAll",
        replaceWith = ReplaceWith("saveAll")
    )
    fun batchSave(
        idTuples: Collection<Tuple2<*, *>>,
        checkExistence: Boolean? = null,
        con: Connection? = null
    ): Int =
        saveAll(idTuples, checkExistence, con)

    fun delete(
        sourceId: Any,
        targetId: Any,
        con: Connection? = null
    ): Int

    fun deleteAll(
        sourceIds: Collection<*>,
        targetIds: Collection<*>,
        con: Connection? = null
    ): Int

    @Deprecated(
        "Will be deleted in 1.0, please use deleteAll",
        replaceWith = ReplaceWith("deleteAll")
    )
    fun batchDelete(
        sourceIds: Collection<*>,
        targetIds: Collection<*>,
        con: Connection? = null
    ): Int =
        deleteAll(sourceIds, targetIds, con)

    fun deleteAll(
        idTuples: Collection<Tuple2<*, *>>,
        con: Connection? = null
    ): Int

    @Deprecated(
        "Will be deleted in 1.0, please use deleteAll",
        replaceWith = ReplaceWith("deleteAll")
    )
    fun batchDelete(
        idTuples: Collection<Tuple2<*, *>>,
        con: Connection? = null
    ): Int =
        deleteAll(idTuples, con)
}