package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.KOrganization
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.name
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.taxCode
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.type
import kotlin.test.Test
import kotlin.test.assertEquals

class JoinedInheritanceQueryTest : AbstractQueryTest() {

    @Test
    fun testSubtypeQueryWithRootAndSubtypeFields() {
        executeAndExpect(
            sqlClient.createQuery(KOrganization::class) {
                select(table.type, table.name, table.taxCode)
            }
        ) {
            sql(
                "select tb_1_.CLIENT_TYPE, tb_1_.NAME, tb_1__sub.TAX_CODE " +
                    "from JOINED_CLIENT tb_1_ " +
                    "inner join JOINED_ORGANIZATION tb_1__sub " +
                    "on tb_1_.ID = tb_1__sub.ID " +
                    "where tb_1_.CLIENT_TYPE = ?"
            )
            variables("ORG")
            row(0) {
                assertEquals("ORG", it._1)
                assertEquals("Globex", it._2)
                assertEquals("GLOBEX-001", it._3)
            }
        }
    }

    @Test
    fun testSubtypeQueryWithRootFieldsOnly() {
        executeAndExpect(
            sqlClient.createQuery(KOrganization::class) {
                select(table.type, table.name)
            }
        ) {
            sql(
                "select tb_1_.CLIENT_TYPE, tb_1_.NAME " +
                    "from JOINED_CLIENT tb_1_ " +
                    "where tb_1_.CLIENT_TYPE = ?"
            )
            variables("ORG")
            row(0) {
                assertEquals("ORG", it._1)
                assertEquals("Globex", it._2)
            }
        }
    }
}
