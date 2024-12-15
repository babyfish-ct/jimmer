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
    fun ignoreConflict(ignoreConflict: Boolean = true): KAssociations

    @NewChain
    fun deleteUnnecessary(deleteUnnecessary: Boolean = true): KAssociations

    @NewChain
    fun dumbBatchAcceptable(acceptable: Boolean = true): KAssociations

    fun insert(sourceId: Any, targetId: Any, con: Connection? = null): Int =
        save(sourceId, targetId, ignoreConflict = false, deleteUnnecessary = false, con)

    fun insertIfAbsent(sourceId: Any, targetId: Any, con: Connection? = null): Int =
        save(sourceId, targetId, ignoreConflict = true, deleteUnnecessary = false, con)

    fun replace(sourceId: Any, targetId: Any, con: Connection? = null): Int =
        save(sourceId, targetId, ignoreConflict = true, deleteUnnecessary = true, con)

    fun insertAll(sourceIds: Collection<*>, targetIds: Collection<*>, con: Connection? = null): Int =
        saveAll(sourceIds, targetIds, ignoreConflict = false, deleteUnnecessary = false, con)

    fun insertAllIfAbsent(sourceIds: Collection<*>, targetIds: Collection<*>, con: Connection? = null): Int =
        saveAll(sourceIds, targetIds, ignoreConflict = true, deleteUnnecessary = false, con)

    fun replaceAll(sourceIds: Collection<*>, targetIds: Collection<*>, con: Connection? = null): Int =
        saveAll(sourceIds, targetIds, ignoreConflict = true, deleteUnnecessary = true, con)

    fun insertAll(idTuples: Collection<Tuple2<*, *>>, con: Connection? = null): Int =
        saveAll(idTuples, ignoreConflict = false, deleteUnnecessary = false, con)

    fun insertAllIfAbsent(idTuples: Collection<Tuple2<*, *>>, con: Connection? = null): Int =
        saveAll(idTuples, ignoreConflict = true, deleteUnnecessary = false, con)

    fun replaceAll(idTuples: Collection<Tuple2<*, *>>, con: Connection? = null): Int =
        saveAll(idTuples, ignoreConflict = true, deleteUnnecessary = true, con)

    fun save(
        sourceId: Any,
        targetId: Any,
        ignoreConflict: Boolean? = null,
        deleteUnnecessary: Boolean? = null,
        con: Connection? = null
    ): Int

    fun saveAll(
        sourceIds: Collection<*>,
        targetIds: Collection<*>,
        ignoreConflict: Boolean? = null,
        deleteUnnecessary: Boolean? = null,
        con: Connection? = null
    ): Int

    fun saveAll(
        idTuples: Collection<Tuple2<*, *>>,
        ignoreConflict: Boolean? = null,
        deleteUnnecessary: Boolean? = null,
        con: Connection? = null
    ): Int

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

    fun deleteAll(
        idTuples: Collection<Tuple2<*, *>>,
        con: Connection? = null
    ): Int
}