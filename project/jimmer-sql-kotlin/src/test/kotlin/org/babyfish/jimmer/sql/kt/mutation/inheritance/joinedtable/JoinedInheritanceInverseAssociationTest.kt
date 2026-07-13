package org.babyfish.jimmer.sql.kt.mutation.inheritance.joinedtable

import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.inverse.*
import kotlin.test.Test

class JoinedInheritanceInverseAssociationTest : AbstractQueryTest() {

    @Test
    fun testFetchInverseOneToOneDeclaredInSubType() {
        executeAndExpect(
            sqlClient.createQuery(KCitizen::class) {
                where(table.id eq 700L)
                select(table.fetchBy {
                    name()
                    passport {
                        name()
                    }
                })
            }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.NAME " +
                    "from JOINED_CITIZEN tb_1_ " +
                    "where tb_1_.ID = ?"
            )
            variables(700L)
            statement(1).sql(
                "select tb_1_.ID, tb_1_.NAME " +
                    "from JOINED_DOCUMENT tb_1_ " +
                    "inner join JOINED_PASSPORT tb_1__sub " +
                    "on tb_1_.ID = tb_1__sub.ID " +
                    "where tb_1__sub.CITIZEN_ID = ? " +
                    "and tb_1_.DOCUMENT_TYPE = ?"
            )
            statement(1).variables(700L, "PASSPORT")
            rows(
                """[{"id":700,"name":"PassportHolder","passport":{"id":701,"name":"primary-passport"}}]"""
            )
        }
    }

    @Test
    fun testBackRefIdsOnInverseSideEvict() {
        // TriggersImpl.fireEvictEvents -> BackRefIds.findBackRefIds, branch
        // "backProp without mappedBy": KPassport.citizen points to the changed KCitizen
        val client = triggerClient()
        connectAndExpect({ con ->
            client.javaClient.triggers.fireEntityEvict(
                ImmutableType.get(KCitizen::class.java),
                700L,
                con
            )
            null
        }) {
            sql(
                "select distinct tb_1_.ID " +
                    "from JOINED_DOCUMENT tb_1_ " +
                    "inner join JOINED_PASSPORT tb_1__sub " +
                    "on tb_1_.ID = tb_1__sub.ID " +
                    "where tb_1__sub.CITIZEN_ID = ? " +
                    "and tb_1_.DOCUMENT_TYPE = ?"
            )
            variables(700L, "PASSPORT")
        }
    }

    @Test
    fun testBackRefIdsOnOwningSideEvict() {
        // TriggersImpl.fireEvictEvents -> BackRefIds.findBackRefIds, branch
        // "backProp with column-definition mappedBy": KCitizen.passport mappedBy KPassport.citizen
        val client = triggerClient()
        connectAndExpect({ con ->
            client.javaClient.triggers.fireEntityEvict(
                ImmutableType.get(KPassport::class.java),
                701L,
                con
            )
            null
        }) {
            sql(
                "select tb_1__sub.CITIZEN_ID " +
                    "from JOINED_DOCUMENT tb_1_ " +
                    "inner join JOINED_PASSPORT tb_1__sub " +
                    "on tb_1_.ID = tb_1__sub.ID " +
                    "where tb_1_.ID = ? " +
                    "and tb_1__sub.CITIZEN_ID is not null " +
                    "and tb_1_.DOCUMENT_TYPE = ?"
            )
            variables(701L, "PASSPORT")
        }
    }

    @Test
    fun testForwardJoinThroughSubTypeForeignKey() {
        executeAndExpect(
            sqlClient.createQuery(KPassport::class) {
                where(table.citizen.name eq "PassportHolder")
                select(table.name, table.citizen.name)
            }
        ) {
            sql(
                "select tb_1_.NAME, tb_2_.NAME " +
                    "from JOINED_DOCUMENT tb_1_ " +
                    "inner join JOINED_PASSPORT tb_1__sub " +
                    "on tb_1_.ID = tb_1__sub.ID " +
                    "inner join JOINED_CITIZEN tb_2_ " +
                    "on tb_1__sub.CITIZEN_ID = tb_2_.ID " +
                    "where tb_2_.NAME = ? " +
                    "and tb_1_.DOCUMENT_TYPE = ?"
            )
            variables("PassportHolder", "PASSPORT")
            rows(
                """[{"_1":"primary-passport","_2":"PassportHolder"}]"""
            )
        }
    }

    @Test
    fun testSelectSubTypeForeignKeyId() {
        executeAndExpect(
            sqlClient.createQuery(KPassport::class) {
                where(table.id eq 701L)
                select(table.citizenId)
            }
        ) {
            sql(
                "select tb_1__sub.CITIZEN_ID " +
                    "from JOINED_DOCUMENT tb_1_ " +
                    "inner join JOINED_PASSPORT tb_1__sub " +
                    "on tb_1_.ID = tb_1__sub.ID " +
                    "where tb_1_.ID = ? " +
                    "and tb_1_.DOCUMENT_TYPE = ?"
            )
            variables(701L, "PASSPORT")
            rows(
                "[700]"
            )
        }
    }

    private fun triggerClient(): KSqlClient =
        sqlClient().apply {
            javaClient.triggers.addEntityListener(
                ImmutableType.get(KCitizen::class.java)
            ) { }
            javaClient.triggers.addEntityListener(
                ImmutableType.get(KPassport::class.java)
            ) { }
        }
}
