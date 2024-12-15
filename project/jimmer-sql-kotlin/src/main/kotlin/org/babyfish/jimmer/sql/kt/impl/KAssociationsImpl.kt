package org.babyfish.jimmer.sql.kt.impl

import org.babyfish.jimmer.sql.Associations
import org.babyfish.jimmer.sql.ast.tuple.Tuple2
import org.babyfish.jimmer.sql.kt.KAssociations
import java.sql.Connection

internal class KAssociationsImpl(
    private val javaAssociations: Associations
): KAssociations {

    override fun forConnection(con: Connection): KAssociations =
        javaAssociations.forConnection(con).let {
            if (javaAssociations === it) {
                this
            } else {
                KAssociationsImpl(it)
            }
        }

    override fun reverse(): KAssociations =
        KAssociationsImpl(javaAssociations.reverse())

    override fun ignoreConflict(ignoreConflict: Boolean): KAssociations =
        javaAssociations.ignoreConflict(ignoreConflict).let {
            if (javaAssociations === it) {
                this
            } else {
                KAssociationsImpl(it)
            }
        }

    override fun deleteUnnecessary(deleteUnnecessary: Boolean): KAssociations =
        javaAssociations.deleteUnnecessary(deleteUnnecessary).let {
            if (javaAssociations === it) {
                this
            } else {
                KAssociationsImpl(it)
            }
        }

    override fun dumbBatchAcceptable(acceptable: Boolean): KAssociations =
        javaAssociations.setDumbBatchAcceptable(acceptable).let {
            if (javaAssociations === it) {
                this
            } else {
                KAssociationsImpl(it)
            }
        }

    override fun save(
        sourceId: Any,
        targetId: Any,
        ignoreConflict: Boolean?,
        deleteUnnecessary: Boolean?,
        con: Connection?
    ): Int =
        javaAssociations
            .saveCommand(sourceId, targetId)
            .ignoreConflict(ignoreConflict)
            .deleteUnnecessary(deleteUnnecessary)
            .execute(con)

    override fun saveAll(
        sourceIds: Collection<*>,
        targetIds: Collection<*>,
        ignoreConflict: Boolean?,
        deleteUnnecessary: Boolean?,
        con: Connection?
    ): Int =
        javaAssociations
            .saveAllCommand(sourceIds, targetIds)
            .ignoreConflict(ignoreConflict)
            .deleteUnnecessary(deleteUnnecessary)
            .execute(con)

    override fun saveAll(
        idTuples: Collection<Tuple2<*, *>>,
        ignoreConflict: Boolean?,
        deleteUnnecessary: Boolean?,
        con: Connection?
    ): Int =
        javaAssociations
            .saveAllCommand(idTuples)
            .ignoreConflict(ignoreConflict)
            .deleteUnnecessary(deleteUnnecessary)
            .execute(con)

    override fun delete(
        sourceId: Any,
        targetId: Any,
        con: Connection?
    ): Int =
        javaAssociations
            .deleteCommand(sourceId, targetId)
            .execute(con)

    override fun deleteAll(
        sourceIds: Collection<*>,
        targetIds: Collection<*>,
        con: Connection?
    ): Int =
        javaAssociations
            .deleteAllCommand(sourceIds, targetIds)
            .execute(con)

    override fun deleteAll(
        idTuples: Collection<Tuple2<*, *>>,
        con: Connection?
    ): Int =
        javaAssociations
            .deleteAllCommand(idTuples)
            .execute(con)
}