package org.babyfish.jimmer.sql.kt.mutation.inheritance.joinedtable

import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.inheritance.logical.joinedtable.KOrganization
import java.sql.Connection
import kotlin.test.Test

class JoinedInheritanceLogicalDeleteTest : AbstractMutationTest() {

    @Test
    fun testLogicalDeleteDerivedType() {
        connectAndExpect({ con ->
            sqlClient.entities.forConnection(con).delete(KOrganization::class, 500L)
            "${joinedClientRow(con, 500L)}; ${joinedClientRow(con, 501L)}"
        }) {
            statement {
                sql("update LOGICAL_JOINED_CLIENT set DELETED = ? where ID = ? and CLIENT_TYPE = ?")
                variables(true, 500L, "ORG")
            }
            value(
                "[ORG, Logical Globex, L-GLOBEX-001, null, null, true]; " +
                    "[KPerson, Logical Alice, null, Alice, Smith, false]"
            )
        }
    }

    private fun joinedClientRow(con: Connection, id: Long): String? =
        con.prepareStatement(
            "select c.CLIENT_TYPE, c.NAME, o.TAX_CODE, p.FIRST_NAME, p.LAST_NAME, c.DELETED " +
                "from LOGICAL_JOINED_CLIENT c " +
                "left join LOGICAL_JOINED_ORGANIZATION o on c.ID = o.ID " +
                "left join LOGICAL_JOINED_PERSON p on c.ID = p.ID " +
                "where c.ID = ?"
        ).use { stmt ->
            stmt.setLong(1, id)
            stmt.executeQuery().use { rs ->
                if (!rs.next()) {
                    null
                } else {
                    "[${rs.getString(1)}, ${rs.getString(2)}, ${rs.getString(3)}, " +
                        "${rs.getString(4)}, ${rs.getString(5)}, ${rs.getBoolean(6)}]"
                }
            }
        }
}
