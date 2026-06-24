package org.babyfish.jimmer.sql.kt.mutation.inheritance.singletable

import org.babyfish.jimmer.sql.DissociateAction
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.inheritance.key.KNaturalOrganization
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.KClientProject
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.KOrganization
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.KOrganizationProject
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.KPerson
import java.sql.Connection
import kotlin.test.Test

class SingleTableInheritanceMutationTest : AbstractMutationTest() {

    @Test
    fun testInsertSubtype() {
        executeAndExpectResult({ con ->
            sqlClient.entities.forConnection(con).save(
                KOrganization {
                    id = 300L
                    name = "New Org"
                    taxCode = "NEW-001"
                }
            ) {
                setMode(SaveMode.INSERT_ONLY)
            }
        }) {
            statement {
                sql(
                    "insert into CLIENT(ID, CLIENT_TYPE, NAME, TAX_CODE) " +
                        "values(?, ?, ?, ?)"
                )
                variables(300L, "ORG", "New Org", "NEW-001")
            }
            rowCount(KOrganization::class, 1)
            entity {
                original("""{"id":300,"name":"New Org","taxCode":"NEW-001"}""")
                modified("""{"id":300,"name":"New Org","taxCode":"NEW-001"}""")
            }
        }
    }

    @Test
    fun testUpsertSubtype() {
        executeAndExpectResult({ con ->
            sqlClient {
                setDialect(H2Dialect())
            }.saveCommand(
                KOrganization {
                    id = 300L
                    name = "New Org"
                    taxCode = "NEW-001"
                }
            ).execute(con)
        }) {
            statement {
                sql(
                    "merge into CLIENT(ID, CLIENT_TYPE, NAME, TAX_CODE, FIRST_NAME, LAST_NAME) " +
                        "key(ID) values(?, ?, ?, ?, null, null)"
                )
                variables(300L, "ORG", "New Org", "NEW-001")
            }
            rowCount(KOrganization::class, 1)
            entity {
                original("""{"id":300,"name":"New Org","taxCode":"NEW-001"}""")
                modified("""{"id":300,"name":"New Org","taxCode":"NEW-001"}""")
            }
        }
    }

    @Test
    fun testUpdateSubtypeWithoutChangingDiscriminator() {
        connectAndExpect({ con ->
            sqlClient.entities.forConnection(con).save(
                KOrganization {
                    id = 100L
                    name = "Acme+"
                    taxCode = "ACME-002"
                }
            ) {
                setMode(SaveMode.UPDATE_ONLY)
            }
            clientRow(con, 100L)
        }) {
            statement {
                sql(
                    "update CLIENT " +
                        "set CLIENT_TYPE = ?, FIRST_NAME = null, LAST_NAME = null, NAME = ?, TAX_CODE = ? " +
                        "where ID = ?"
                )
                variables("ORG", "Acme+", "ACME-002", 100L)
            }
            value("[ORG, Acme+, ACME-002, null, null]")
        }
    }

    @Test
    fun testUpsertSubtypeWithChangingDiscriminator() {
        connectAndExpect({ con ->
            sqlClient {
                setDialect(H2Dialect())
            }.saveCommand(
                KPerson {
                    id = 100L
                    name = "Acme Person"
                    firstName = "Ann"
                    lastName = "Smith"
                }
            ).execute(con)
            clientRow(con, 100L)
        }) {
            statement {
                sql(
                    "merge into CLIENT(ID, CLIENT_TYPE, NAME, FIRST_NAME, LAST_NAME, TAX_CODE) " +
                        "key(ID) values(?, ?, ?, ?, ?, null)"
                )
                variables(100L, "KPerson", "Acme Person", "Ann", "Smith")
            }
            value("[KPerson, Acme Person, null, Ann, Smith]")
        }
    }

    @Test
    fun testUpdateSubtypeWithChangingDiscriminator() {
        connectAndExpect({ con ->
            sqlClient.entities.forConnection(con).save(
                KPerson {
                    id = 100L
                    name = "Acme Person"
                    firstName = "Ann"
                    lastName = "Smith"
                }
            ) {
                setMode(SaveMode.UPDATE_ONLY)
            }
            clientRow(con, 100L)
        }) {
            statement {
                sql(
                    "update CLIENT " +
                        "set CLIENT_TYPE = ?, TAX_CODE = null, NAME = ?, FIRST_NAME = ?, LAST_NAME = ? " +
                        "where ID = ?"
                )
                variables("KPerson", "Acme Person", "Ann", "Smith", 100L)
            }
            value("[KPerson, Acme Person, null, Ann, Smith]")
        }
    }

    @Test
    fun testUpdateByDiscriminatorKey() {
        connectAndExpect({ con ->
            sqlClient.entities.forConnection(con).save(
                KNaturalOrganization {
                    code = "same-code"
                    name = "Acme Natural+"
                    taxCode = "ACME-N-002"
                }
            ) {
                setMode(SaveMode.UPDATE_ONLY)
            }
            naturalClientRows(con)
        }) {
            statement {
                sql(
                    "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.CODE " +
                        "from NATURAL_CLIENT tb_1_ " +
                        "where (tb_1_.CLIENT_TYPE, tb_1_.CODE) = (?, ?) and tb_1_.CLIENT_TYPE = ?"
                )
                variables("ORG", "same-code", "ORG")
            }
            statement {
                sql(
                    "update NATURAL_CLIENT " +
                        "set CLIENT_TYPE = ?, FIRST_NAME = null, LAST_NAME = null, NAME = ?, TAX_CODE = ? " +
                        "where CLIENT_TYPE = ? and CODE = ?"
                )
                variables("ORG", "Acme Natural+", "ACME-N-002", "ORG", "same-code")
            }
            value(
                "[300, ORG, same-code, Acme Natural+, ACME-N-002, null, null]; " +
                    "[301, KNaturalPerson, same-code, Bob Natural, null, Bob, Brown]"
            )
        }
    }

    @Test
    fun testUpdateRootAssociationToSubtypeTarget() {
        executeAndExpectResult({ con ->
            sqlClient.entities.forConnection(con).save(
                KClientProject {
                    id = 1000L
                    name = "Single root project+"
                    clientId = 101L
                }
            ) {
                setMode(SaveMode.UPDATE_ONLY)
            }
        }) {
            statement {
                sql(
                    "update SINGLE_CLIENT_PROJECT " +
                        "set NAME = ?, CLIENT_ID = ? " +
                        "where ID = ?"
                )
                variables("Single root project+", 101L, 1000L)
            }
            rowCount(KClientProject::class, 1)
            entity {}
        }
    }

    @Test
    fun testUpdateSubtypeAssociationToSubtypeTarget() {
        executeAndExpectResult({ con ->
            sqlClient.entities.forConnection(con).save(
                KOrganizationProject {
                    id = 1001L
                    name = "Single organization project+"
                    organizationId = 102L
                }
            ) {
                setMode(SaveMode.UPDATE_ONLY)
            }
        }) {
            statement {
                sql(
                    "update SINGLE_ORG_PROJECT " +
                        "set NAME = ?, ORGANIZATION_ID = ? " +
                        "where ID = ?"
                )
                variables("Single organization project+", 102L, 1001L)
            }
            rowCount(KOrganizationProject::class, 1)
            entity {}
        }
    }

    @Test
    fun testDeleteSubtype() {
        connectAndExpect({ con ->
            sqlClient.entities.forConnection(con).delete(KOrganization::class, 100L) {
                setMode(DeleteMode.PHYSICAL)
            }
            "${clientRow(con, 100L)}; ${clientRow(con, 101L)}"
        }) {
            statement {
                sql("delete from CLIENT where ID = ? and CLIENT_TYPE = ?")
                variables(100L, "ORG")
            }
            value("null; [KPerson, Bob, null, Bob, Brown]")
        }
    }

    @Test
    fun testDeleteSubtypeWithAssociationTargets() {
        connectAndExpect({ con ->
            sqlClient.entities.forConnection(con).delete(KOrganization::class, 100L) {
                setMode(DeleteMode.PHYSICAL)
                setDissociateAction(KClientProject::client, DissociateAction.SET_NULL)
                setDissociateAction(KOrganizationProject::organization, DissociateAction.SET_NULL)
            }
            "${singleClientProjectTargetId(con, 1000L)}; " +
                "${singleOrgProjectTargetId(con, 1001L)}; " +
                "${clientRow(con, 100L)}; " +
                "${clientRow(con, 101L)}"
        }) {
            statement {
                sql("update SINGLE_CLIENT_PROJECT set CLIENT_ID = null where CLIENT_ID = ?")
                variables(100L)
            }
            statement {
                sql("update SINGLE_ORG_PROJECT set ORGANIZATION_ID = null where ORGANIZATION_ID = ?")
                variables(100L)
            }
            statement {
                sql("delete from CLIENT where ID = ? and CLIENT_TYPE = ?")
                variables(100L, "ORG")
            }
            value("null; null; null; [KPerson, Bob, null, Bob, Brown]")
        }
    }

    private fun clientRow(con: Connection, id: Long): String? =
        con.prepareStatement(
            "select CLIENT_TYPE, NAME, TAX_CODE, FIRST_NAME, LAST_NAME from CLIENT where ID = ?"
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

    private fun naturalClientRows(con: Connection): String =
        con.prepareStatement(
            "select ID, CLIENT_TYPE, CODE, NAME, TAX_CODE, FIRST_NAME, LAST_NAME " +
                "from NATURAL_CLIENT where CODE = 'same-code' order by ID"
        ).use { stmt ->
            stmt.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        add(
                            "[${rs.getLong(1)}, ${rs.getString(2)}, ${rs.getString(3)}, " +
                                "${rs.getString(4)}, ${rs.getString(5)}, ${rs.getString(6)}, " +
                                "${rs.getString(7)}]"
                        )
                    }
                }.joinToString("; ")
            }
        }

    private fun singleClientProjectTargetId(con: Connection, id: Long): String? =
        con.prepareStatement(
            "select CLIENT_ID from SINGLE_CLIENT_PROJECT where ID = ?"
        ).use { stmt ->
            stmt.setLong(1, id)
            stmt.executeQuery().use { rs ->
                if (!rs.next()) {
                    null
                } else {
                    rs.getObject(1)?.toString()
                }
            }
        }

    private fun singleOrgProjectTargetId(con: Connection, id: Long): String? =
        con.prepareStatement(
            "select ORGANIZATION_ID from SINGLE_ORG_PROJECT where ID = ?"
        ).use { stmt ->
            stmt.setLong(1, id)
            stmt.executeQuery().use { rs ->
                if (!rs.next()) {
                    null
                } else {
                    rs.getObject(1)?.toString()
                }
            }
        }
}
