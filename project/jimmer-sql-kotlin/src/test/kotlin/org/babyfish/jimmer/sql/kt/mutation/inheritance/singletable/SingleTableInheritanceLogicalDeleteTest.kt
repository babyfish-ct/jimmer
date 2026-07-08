package org.babyfish.jimmer.sql.kt.mutation.inheritance.singletable

import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.inheritance.logical.singletable.KOrganization
import java.sql.Connection
import kotlin.test.Test

class SingleTableInheritanceLogicalDeleteTest : AbstractMutationTest() {

    @Test
    fun testLogicalDeleteDerivedType() {
        connectAndExpect({ con ->
            sqlClient.entities.forConnection(con).delete(KOrganization::class, 400L)
            "${clientRow(con, 400L)}; ${clientRow(con, 401L)}"
        }) {
            statement {
                sql("update LOGICAL_CLIENT set DELETED = ? where ID = ? and CLIENT_TYPE = ?")
                variables(true, 400L, "ORG")
            }
            value(
                "[ORG, Logical Acme, L-ACME-001, null, null, true]; " +
                    "[KPerson, Logical Bob, null, Bob, Brown, false]"
            )
        }
    }

    private fun clientRow(con: Connection, id: Long): String? =
        con.prepareStatement(
            "select CLIENT_TYPE, NAME, TAX_CODE, FIRST_NAME, LAST_NAME, DELETED " +
                "from LOGICAL_CLIENT where ID = ?"
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
