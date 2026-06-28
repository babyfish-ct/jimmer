package org.babyfish.jimmer.sql.kt.mutation.inheritance.joinedtable.instantiable

import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.instantiable.KClient
import java.sql.Connection
import kotlin.test.Test

class JoinedInstantiableRootMutationTest : AbstractMutationTest() {

    @Test
    fun testUpdateRootBranchWithSubtypeChangeAllowed() {
        connectAndExpect({ con ->
            sqlClient.entities.forConnection(con).save(
                KClient {
                    id = 601L
                    name = "Joined Root Replacement"
                }
            ) {
                setMode(SaveMode.UPDATE_ONLY)
                setSubtypeChangeAllowed()
            }
            joinedClientRow(con, 601L)
        }) {
            statement {
                sql("select ID, CLIENT_TYPE from JOINED_INST_CLIENT where ID = ? order by ID")
                variables(601L)
            }
            statement {
                sql(
                    "update JOINED_INST_CLIENT " +
                        "set CLIENT_TYPE = ?, NAME = ? " +
                        "where ID = ? and CLIENT_TYPE = ?"
                )
                variables("CLIENT", "Joined Root Replacement", 601L, "ORG")
            }
            statement {
                sql("delete from JOINED_INST_ORGANIZATION where ID = ?")
                variables(601L)
            }
            value("[CLIENT, Joined Root Replacement, null, null, null]")
        }
    }

    private fun joinedClientRow(con: Connection, id: Long): String? =
        con.prepareStatement(
            "select c.CLIENT_TYPE, c.NAME, o.TAX_CODE, p.FIRST_NAME, p.LAST_NAME " +
                "from JOINED_INST_CLIENT c " +
                "left join JOINED_INST_ORGANIZATION o on c.ID = o.ID " +
                "left join JOINED_INST_PERSON p on c.ID = p.ID " +
                "where c.ID = ?"
        ).use { stmt ->
            stmt.setLong(1, id)
            stmt.executeQuery().use { rs ->
                if (!rs.next()) {
                    null
                } else {
                    "[${rs.getString(1)}, ${rs.getString(2)}, ${rs.getString(3)}, " +
                        "${rs.getString(4)}, ${rs.getString(5)}]"
                }
            }
        }
}
