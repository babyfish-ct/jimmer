package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.runtime.ImmutableSpi
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.KPerson
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.dto.KPersonSpecification
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
}
