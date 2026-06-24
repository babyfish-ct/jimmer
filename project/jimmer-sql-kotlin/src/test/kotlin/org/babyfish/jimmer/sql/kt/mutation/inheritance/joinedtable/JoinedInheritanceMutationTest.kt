package org.babyfish.jimmer.sql.kt.mutation.inheritance.joinedtable

import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.KClient
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.KOrganization
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.KPerson
import java.sql.Connection
import kotlin.test.Test

class JoinedInheritanceMutationTest : AbstractMutationTest() {

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
                    "insert into JOINED_CLIENT(ID, CLIENT_TYPE, NAME) " +
                        "values(?, ?, ?)"
                )
                variables(300L, "ORG", "New Org")
            }
            statement {
                sql(
                    "insert into JOINED_ORGANIZATION(ID, TAX_CODE) " +
                        "values(?, ?)"
                )
                variables(300L, "NEW-001")
            }
            rowCount(KClient::class, 1)
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
                    "merge into JOINED_CLIENT(ID, CLIENT_TYPE, NAME) " +
                        "key(ID) values(?, ?, ?)"
                )
                variables(300L, "ORG", "New Org")
            }
            statement {
                sql("delete from JOINED_PERSON where ID = ?")
                variables(300L)
            }
            statement {
                sql(
                    "merge into JOINED_ORGANIZATION(ID, TAX_CODE) " +
                        "key(ID) values(?, ?)"
                )
                variables(300L, "NEW-001")
            }
            rowCount(KClient::class, 1)
            rowCount(KOrganization::class, 1)
            entity {
                original("""{"id":300,"name":"New Org","taxCode":"NEW-001"}""")
                modified("""{"id":300,"name":"New Org","taxCode":"NEW-001"}""")
            }
        }
    }

    @Test
    fun testUpsertSubtypeWithChangingDiscriminator() {
        connectAndExpect({ con ->
            sqlClient {
                setDialect(H2Dialect())
            }.saveCommand(
                KPerson {
                    id = 200L
                    name = "Globex Person"
                    firstName = "Gary"
                    lastName = "Stone"
                }
            ).execute(con)
            joinedClientRow(con, 200L)
        }) {
            statement {
                sql(
                    "merge into JOINED_CLIENT(ID, CLIENT_TYPE, NAME) " +
                        "key(ID) values(?, ?, ?)"
                )
                variables(200L, "KPerson", "Globex Person")
            }
            statement {
                sql("delete from JOINED_ORGANIZATION where ID = ?")
                variables(200L)
            }
            statement {
                sql(
                    "merge into JOINED_PERSON(ID, FIRST_NAME, LAST_NAME) " +
                        "key(ID) values(?, ?, ?)"
                )
                variables(200L, "Gary", "Stone")
            }
            value("[KPerson, Globex Person, null, Gary, Stone]")
        }
    }

    @Test
    fun testUpdateSubtypeWithChangingDiscriminator() {
        connectAndExpect({ con ->
            sqlClient.entities.forConnection(con).save(
                KPerson {
                    id = 200L
                    name = "Globex Person"
                    firstName = "Gary"
                    lastName = "Stone"
                }
            ) {
                setMode(SaveMode.UPDATE_ONLY)
            }
            joinedClientRow(con, 200L)
        }) {
            statement {
                sql(
                    "update JOINED_CLIENT " +
                        "set CLIENT_TYPE = ?, NAME = ? " +
                        "where ID = ?"
                )
                variables("KPerson", "Globex Person", 200L)
            }
            statement {
                sql("delete from JOINED_ORGANIZATION where ID = ?")
                variables(200L)
            }
            statement {
                sql("select ID from JOINED_PERSON where ID = ?")
                variables(200L)
            }
            statement {
                sql(
                    "insert into JOINED_PERSON(ID, FIRST_NAME, LAST_NAME) " +
                        "values(?, ?, ?)"
                )
                variables(200L, "Gary", "Stone")
            }
            value("[KPerson, Globex Person, null, Gary, Stone]")
        }
    }

    @Test
    fun testUpdateSubtypeWithoutChangingDiscriminator() {
        connectAndExpect({ con ->
            sqlClient.entities.forConnection(con).save(
                KOrganization {
                    id = 200L
                    name = "Globex+"
                    taxCode = "GLOBEX-002"
                }
            ) {
                setMode(SaveMode.UPDATE_ONLY)
            }
            joinedClientRow(con, 200L)
        }) {
            statement {
                sql(
                    "update JOINED_CLIENT " +
                        "set CLIENT_TYPE = ?, NAME = ? " +
                        "where ID = ?"
                )
                variables("ORG", "Globex+", 200L)
            }
            statement {
                sql("delete from JOINED_PERSON where ID = ?")
                variables(200L)
            }
            statement {
                sql("select ID from JOINED_ORGANIZATION where ID = ?")
                variables(200L)
            }
            statement {
                sql(
                    "update JOINED_ORGANIZATION " +
                        "set TAX_CODE = ? " +
                        "where ID = ?"
                )
                variables("GLOBEX-002", 200L)
            }
            value("[ORG, Globex+, GLOBEX-002, null, null]")
        }
    }

    private fun joinedClientRow(con: Connection, id: Long): String? =
        con.prepareStatement(
            "select c.CLIENT_TYPE, c.NAME, o.TAX_CODE, p.FIRST_NAME, p.LAST_NAME " +
                "from JOINED_CLIENT c " +
                "left join JOINED_ORGANIZATION o on c.ID = o.ID " +
                "left join JOINED_PERSON p on c.ID = p.ID " +
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
