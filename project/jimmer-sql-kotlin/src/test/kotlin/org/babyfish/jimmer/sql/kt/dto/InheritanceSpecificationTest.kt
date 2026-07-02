package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.runtime.ImmutableSpi
import org.babyfish.jimmer.sql.kt.ast.query.specification.allOf
import org.babyfish.jimmer.sql.kt.ast.query.specification.anyOf
import org.babyfish.jimmer.sql.kt.ast.query.specification.not
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.KClient
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.KPerson
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.dto.KClientSpecification
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.dto.KOrganizationSpecification
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.dto.KPersonSpecification
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.id
import kotlin.test.Test
import kotlin.test.assertEquals

class InheritanceSpecificationTest : AbstractQueryTest() {

    @Test
    fun testLeafSpecificationIsOrdinarySpecification() {
        val specification = KPersonSpecification(
            name = "Bob",
            firstName = "Bob"
        )
        executeAndExpect(
            sqlClient.createQuery(KPerson::class) {
                where(specification)
                select(table)
            }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                        "from CLIENT tb_1_ " +
                        "where tb_1_.NAME = ? " +
                        "and tb_1_.FIRST_NAME = ? " +
                        "and tb_1_.CLIENT_TYPE = ?"
            )
            variables("Bob", "Bob", "KPerson")
            row(0) {
                assertEquals(KPerson::class.java, (it as ImmutableSpi).__type().javaClass)
                assertEquals(101L, it.id)
                assertEquals("Bob", it.name)
                assertEquals("Bob", it.firstName)
                assertEquals("Brown", it.lastName)
            }
        }
    }

    @Test
    fun testLeafSpecificationCanBeAppliedToRootQuery() {
        val specification = KPersonSpecification(name = "Bob")
        executeAndExpect(
            sqlClient.createQuery(KClient::class) {
                where(specification)
                orderBy(table.id)
                select(table.id)
            }
        ) {
            sql(
                "select tb_1_.ID " +
                        "from CLIENT tb_1_ " +
                        "where tb_1_.CLIENT_TYPE = ? " +
                        "and tb_1_.NAME = ? " +
                        "order by tb_1_.ID asc"
            )
            variables("KPerson", "Bob")
            rows("[101]")
        }
    }

    @Test
    fun testLeafSpecificationCanUseSubtypeFieldOnRootQuery() {
        val specification = KPersonSpecification(firstName = "Bob")
        executeAndExpect(
            sqlClient.createQuery(KClient::class) {
                where(specification)
                orderBy(table.id)
                select(table.id)
            }
        ) {
            sql(
                "select tb_1_.ID " +
                        "from CLIENT tb_1_ " +
                        "left join CLIENT tb_2_ on tb_1_.ID = tb_2_.ID and tb_2_.CLIENT_TYPE = ? " +
                        "where tb_1_.CLIENT_TYPE = ? " +
                        "and tb_2_.FIRST_NAME = ? " +
                        "order by tb_1_.ID asc"
            )
            variables("KPerson", "KPerson", "Bob")
            rows("[101]")
        }
    }

    @Test
    fun testNotSpecificationForLeafOnRootQuery() {
        val specification = !KPersonSpecification(name = "Bob")
        executeAndExpect(
            sqlClient.createQuery(KClient::class) {
                where(specification)
                orderBy(table.id)
                select(table.id)
            }
        ) {
            sql(
                "select tb_1_.ID " +
                        "from CLIENT tb_1_ " +
                        "where not (tb_1_.CLIENT_TYPE = ? and tb_1_.NAME = ?) " +
                        "order by tb_1_.ID asc"
            )
            variables("KPerson", "Bob")
            rows("[100,102]")
        }
    }

    @Test
    fun testOrSpecificationForInheritanceSiblings() {
        val specification = anyOf(
            listOf(
                KPersonSpecification(name = "Acme"),
                KOrganizationSpecification(name = "Bob")
            )
        )
        executeAndExpect(
            sqlClient.createQuery(KClient::class) {
                where(specification)
                orderBy(table.id)
                select(table.id)
            }
        ) {
            sql(
                "select tb_1_.ID " +
                        "from CLIENT tb_1_ " +
                        "where " +
                        "tb_1_.CLIENT_TYPE = ? and tb_1_.NAME = ? " +
                        "or tb_1_.CLIENT_TYPE = ? and tb_1_.NAME = ? " +
                        "order by tb_1_.ID asc"
            )
            variables("KPerson", "Acme", "ORG", "Bob")
            rows("[]")
        }
    }

    @Test
    fun testOrSpecificationCanUseSubtypeFieldsForInheritanceSiblings() {
        val specification = anyOf(
            KPersonSpecification(firstName = "Bob"),
            KOrganizationSpecification(taxCode = "UMB-001")
        )
        executeAndExpect(
            sqlClient.createQuery(KClient::class) {
                where(specification)
                orderBy(table.id)
                select(table.id)
            }
        ) {
            sql(
                "select tb_1_.ID " +
                        "from CLIENT tb_1_ " +
                        "left join CLIENT tb_2_ on tb_1_.ID = tb_2_.ID and tb_2_.CLIENT_TYPE = ? " +
                        "left join CLIENT tb_3_ on tb_1_.ID = tb_3_.ID and tb_3_.CLIENT_TYPE = ? " +
                        "where tb_1_.CLIENT_TYPE = ? and tb_2_.FIRST_NAME = ? " +
                        "or tb_1_.CLIENT_TYPE = ? and tb_3_.TAX_CODE = ? " +
                        "order by tb_1_.ID asc"
            )
            variables("KPerson", "ORG", "KPerson", "Bob", "ORG", "UMB-001")
            rows("[101,102]")
        }
    }

    @Test
    fun testNestedAndOrSpecificationForInheritance() {
        val specification = allOf(
            KClientSpecification(name = "Bob"),
            anyOf(
                KPersonSpecification(firstName = "Bob"),
                KOrganizationSpecification(taxCode = "UMB-001")
            )
        )
        executeAndExpect(
            sqlClient.createQuery(KClient::class) {
                where(specification)
                orderBy(table.id)
                select(table.id)
            }
        ) {
            sql(
                "select tb_1_.ID " +
                        "from CLIENT tb_1_ " +
                        "left join CLIENT tb_2_ on tb_1_.ID = tb_2_.ID and tb_2_.CLIENT_TYPE = ? " +
                        "left join CLIENT tb_3_ on tb_1_.ID = tb_3_.ID and tb_3_.CLIENT_TYPE = ? " +
                        "where tb_1_.NAME = ? " +
                        "and (tb_1_.CLIENT_TYPE = ? and tb_2_.FIRST_NAME = ? " +
                        "or tb_1_.CLIENT_TYPE = ? and tb_3_.TAX_CODE = ?) " +
                        "order by tb_1_.ID asc"
            )
            variables("KPerson", "ORG", "Bob", "KPerson", "Bob", "ORG", "UMB-001")
            rows("[101]")
        }
    }
}
