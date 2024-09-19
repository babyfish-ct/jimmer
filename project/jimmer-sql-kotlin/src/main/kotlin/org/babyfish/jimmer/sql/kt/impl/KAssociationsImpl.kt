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

    override fun checkExistence(checkExistence: Boolean): KAssociations =
        javaAssociations.checkExistence(checkExistence).let {
            if (javaAssociations === it) {
                this
            } else {
                KAssociationsImpl(it)
            }
        }

    override fun deleteUnnecessary(deleteUnnecessary: Boolean): KAssociations =
        javaAssociations.deleteUnnecessary(deleteUnnecessary).let {
            if (javaAssociations == it) {
                this
            } else {
                KAssociationsImpl(it)
            }
        }

    override fun save(
        sourceId: Any,
        targetId: Any,
        checkExistence: Boolean?,
        deleteUnnecessary: Boolean?,
        con: Connection?
    ): Int =
        javaAssociations
            .saveCommand(sourceId, targetId)
            .checkExistence(checkExistence)
            .deleteUnnecessary(deleteUnnecessary)
            .execute(con)

    override fun saveAll(
        sourceIds: Collection<*>,
        targetIds: Collection<*>,
        checkExistence: Boolean?,
        deleteUnnecessary: Boolean?,
        con: Connection?
    ): Int =
        javaAssociations
            .batchSaveCommand(sourceIds, targetIds)
            .checkExistence(checkExistence)
            .deleteUnnecessary(deleteUnnecessary)
            .execute(con)

    override fun saveAll(
        idTuples: Collection<Tuple2<*, *>>,
        checkExistence: Boolean?,
        deleteUnnecessary: Boolean?,
        con: Connection?
    ): Int =
        javaAssociations
            .batchSaveCommand(idTuples)
            .checkExistence(checkExistence)
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
            .batchDeleteCommand(sourceIds, targetIds)
            .execute(con)

    override fun deleteAll(
        idTuples: Collection<Tuple2<*, *>>,
        con: Connection?
    ): Int =
        javaAssociations
            .batchDeleteCommand(idTuples)
            .execute(con)
}