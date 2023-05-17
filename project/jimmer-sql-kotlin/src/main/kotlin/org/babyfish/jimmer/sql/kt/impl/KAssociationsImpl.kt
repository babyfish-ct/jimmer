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

    override fun save(
        sourceId: Any,
        targetId: Any,
        checkExistence: Boolean?,
        con: Connection?
    ): Int =
        javaAssociations
            .saveCommand(sourceId, targetId)
            .checkExistence(checkExistence)
            .execute(con)

    override fun batchSave(
        sourceIds: Collection<Any>,
        targetIds: Collection<Any>,
        checkExistence: Boolean?,
        con: Connection?
    ): Int =
        javaAssociations
            .batchSaveCommand(sourceIds, targetIds)
            .checkExistence(checkExistence)
            .execute(con)

    override fun batchSave(
        idTuples: Collection<Tuple2<Any, Any>>,
        checkExistence: Boolean?,
        con: Connection?
    ): Int =
        javaAssociations
            .batchSaveCommand(idTuples)
            .checkExistence(checkExistence)
            .execute(con)

    override fun delete(
        sourceId: Any,
        targetId: Any,
        con: Connection?
    ): Int =
        javaAssociations
            .deleteCommand(sourceId, targetId)
            .execute(con)

    override fun batchDelete(
        sourceIds: Collection<Any>,
        targetIds: Collection<Any>,
        con: Connection?
    ): Int =
        javaAssociations
            .batchDeleteCommand(sourceIds, targetIds)
            .execute(con)

    override fun batchDelete(
        idTuples: Collection<Tuple2<Any, Any>>,
        con: Connection?
    ): Int =
        javaAssociations
            .batchDeleteCommand(idTuples)
            .execute(con)
}