package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.ImmutableObjects
import org.babyfish.jimmer.runtime.ImmutableSpi
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.instantiable.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class JoinedInstantiableRootQueryTest : AbstractQueryTest() {

    @Test
    fun testRootFetcherMaterializesInstantiableRootWithoutUserDiscriminator() {
        executeAndExpect(
            sqlClient.createQuery(KClient::class) {
                where(table.id eq 600L)
                select(table.fetchBy {
                    name()
                })
            }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME " +
                    "from JOINED_INST_CLIENT tb_1_ " +
                    "where tb_1_.ID = ?"
            )
            variables(600L)
            row(0) {
                assertEquals(KClient::class.java, (it as ImmutableSpi).__type().javaClass)
                assertEquals(600L, it.id)
                assertEquals("Joined Root", it.name)
                assertFalse(ImmutableObjects.isLoaded(it, KClientProps.TYPE))
            }
        }
    }

    @Test
    fun testRootFetcherMaterializesSubtypeWithoutUserDiscriminator() {
        executeAndExpect(
            sqlClient.createQuery(KClient::class) {
                where(table.id eq 601L)
                select(table.fetchBy {
                    name()
                })
            }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME " +
                    "from JOINED_INST_CLIENT tb_1_ " +
                    "where tb_1_.ID = ?"
            )
            variables(601L)
            row(0) {
                assertEquals(KOrganization::class.java, (it as ImmutableSpi).__type().javaClass)
                assertEquals(601L, it.id)
                assertEquals("Joined Inst Org", it.name)
                assertFalse(ImmutableObjects.isLoaded(it, KClientProps.TYPE))
            }
        }
    }
}
