package org.babyfish.jimmer.sql.kt.mutation.inheritance.joinedtable

import org.babyfish.jimmer.sql.ast.mutation.DeleteMode
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.cascade.KClient
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.cascade.KOrganization
import java.sql.Connection
import kotlin.test.Test

class JoinedInheritanceCascadeDeleteTest : AbstractMutationTest() {

    @Test
    fun testDeleteSubtypeByDatabaseCascade() {
        connectAndExpect({ con ->
            sqlClient.entities.forConnection(con).delete(KOrganization::class, 500L) {
                setMode(DeleteMode.PHYSICAL)
            }
            "${joinedClientRow(con, 500L)}; ${joinedClientRow(con, 501L)}"
        }) {
            statement {
                sql("delete from JOINED_CASCADE_CLIENT where ID = ? and CLIENT_TYPE = ?")
                variables(500L, "ORG")
            }
            value("null; [KPerson, Cascade Alice, null, Alice, Smith]")
        }
    }

    @Test
    fun testDeleteRootByDatabaseCascade() {
        connectAndExpect({ con ->
            sqlClient.entities.forConnection(con).delete(KClient::class, 500L) {
                setMode(DeleteMode.PHYSICAL)
                setPolymorphic()
            }
            "${joinedClientRow(con, 500L)}; ${joinedClientRow(con, 501L)}"
        }) {
            statement {
                sql("delete from JOINED_CASCADE_CLIENT where ID = ? and CLIENT_TYPE in (?, ?)")
                variables(500L, "ORG", "KPerson")
            }
            value("null; [KPerson, Cascade Alice, null, Alice, Smith]")
        }
    }

    private fun joinedClientRow(con: Connection, id: Long): String? =
        con.prepareStatement(
            "select c.CLIENT_TYPE, c.NAME, o.TAX_CODE, p.FIRST_NAME, p.LAST_NAME " +
                    "from JOINED_CASCADE_CLIENT c " +
                    "left join JOINED_CASCADE_ORGANIZATION o on c.ID = o.ID " +
                    "left join JOINED_CASCADE_PERSON p on c.ID = p.ID " +
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
