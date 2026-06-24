package org.babyfish.jimmer.sql.kt.mutation.inheritance.singletable

import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.inheritance.key.KNaturalOrganization
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.KOrganization
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
}
