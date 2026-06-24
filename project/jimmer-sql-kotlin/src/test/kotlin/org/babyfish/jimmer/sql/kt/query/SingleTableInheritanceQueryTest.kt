package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.runtime.ImmutableSpi
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.inheritance.enumdiscriminator.*
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.*
import kotlin.test.Test
import kotlin.test.assertEquals

class SingleTableInheritanceQueryTest : AbstractQueryTest() {

    @Test
    fun testRootFetcherMaterializesSubtype() {
        executeAndExpect(
            sqlClient.createQuery(KClient::class) {
                where(table.id eq 100L)
                select(table.fetchBy {
                    type()
                    name()
                })
            }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME " +
                    "from CLIENT tb_1_ " +
                    "where tb_1_.ID = ?"
            )
            variables(100L)
            row(0) {
                assertEquals(KOrganization::class.java, (it as ImmutableSpi).__type().javaClass)
                assertEquals(100L, it.id)
                assertEquals("ORG", it.type)
                assertEquals("Acme", it.name)
            }
        }
    }

    @Test
    fun testEnumDiscriminatorRootFetcherMaterializesSubtype() {
        executeAndExpect(
            sqlClient.createQuery(KEnumClient::class) {
                where(table.id eq 110L)
                select(table.fetchBy {
                    type()
                })
            }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.CLIENT_TYPE " +
                    "from K_ENUM_CLIENT tb_1_ " +
                    "where tb_1_.ID = ?"
            )
            variables(110L)
            row(0) {
                assertEquals(KEnumOrganization::class.java, (it as ImmutableSpi).__type().javaClass)
                assertEquals(110L, it.id)
                assertEquals(KClientType.ORG, it.type)
            }
        }
    }

    @Test
    fun testSubtypeQueryWithRootAndSubtypeFields() {
        executeAndExpect(
            sqlClient.createQuery(KOrganization::class) {
                select(table.type, table.name, table.taxCode)
            }
        ) {
            sql(
                "select tb_1_.CLIENT_TYPE, tb_1_.NAME, tb_1_.TAX_CODE " +
                    "from CLIENT tb_1_ " +
                    "where tb_1_.CLIENT_TYPE = ?"
            )
            variables("ORG")
            row(0) {
                assertEquals("ORG", it._1)
                assertEquals("Acme", it._2)
                assertEquals("ACME-001", it._3)
            }
        }
    }
}
