package org.babyfish.jimmer.sql.kt.mutation.inheritance.joinedtable

import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.key.KKeyOrganization
import java.sql.Connection
import kotlin.test.Test
import kotlin.test.assertEquals

class JoinedInheritanceKeyMutationTest : AbstractMutationTest() {

    @Test
    fun testUpdateSubtypeByKey() {
        connectAndExpect({ con ->
            sqlClient {
                setDialect(H2Dialect())
            }.saveCommand(
                KKeyOrganization {
                    code = "same-code"
                    taxCode = "KEY-GLOBEX-002"
                }
            ) {
                setMode(SaveMode.UPDATE_ONLY)
            }.execute(con)
            "${joinedKeyClientRow(con, "ORG", "same-code")}; " +
                joinedKeyClientRow(con, "KKeyPerson", "same-code")
        }) {
            statement {
                sql(
                    "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.CODE " +
                        "from JOINED_KEY_CLIENT tb_1_ " +
                        "where (tb_1_.CLIENT_TYPE, tb_1_.CODE) = (?, ?) " +
                        "and tb_1_.CLIENT_TYPE = ?"
                )
                variables("ORG", "same-code", "ORG")
            }
            statement {
                sql(
                    "update JOINED_KEY_ORGANIZATION " +
                        "set TAX_CODE = ? " +
                        "where ID = ? and exists(" +
                        "select 1 from JOINED_KEY_CLIENT " +
                        "where JOINED_KEY_CLIENT.ID = ? and CLIENT_TYPE = ?)"
                )
                variables("KEY-GLOBEX-002", 400L, 400L, "ORG")
            }
            value(
                "[400, ORG, same-code, Key Globex, KEY-GLOBEX-002, null, null]; " +
                    "[401, KKeyPerson, same-code, Key Alice, null, Key Alice, Smith]"
            )
        }
    }

    @Test
    fun testUpsertSubtypeByKey() {
        connectAndExpect({ con ->
            sqlClient {
                setDialect(H2Dialect())
            }.saveCommand(
                KKeyOrganization {
                    code = "same-code"
                    name = "Key Globex+"
                    taxCode = "KEY-GLOBEX-003"
                }
            ).execute(con)
            "${joinedKeyClientRow(con, "ORG", "same-code")}; " +
                joinedKeyClientRow(con, "KKeyPerson", "same-code")
        }) {
            statement {
                sql(
                    "merge into JOINED_KEY_CLIENT tb_1_ " +
                        "using(values(?, ?, ?)) tb_2_(CLIENT_TYPE, CODE, NAME) " +
                        "on tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE and tb_1_.CODE = tb_2_.CODE " +
                        "when matched and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                        "then update set NAME = tb_2_.NAME " +
                        "when not matched then insert(CLIENT_TYPE, CODE, NAME) " +
                        "values(tb_2_.CLIENT_TYPE, tb_2_.CODE, tb_2_.NAME)"
                )
                variables("ORG", "same-code", "Key Globex+")
            }
            statement {
                sql(
                    "merge into JOINED_KEY_ORGANIZATION(ID, TAX_CODE) " +
                        "key(ID) values(?, ?)"
                )
                variables(400L, "KEY-GLOBEX-003")
            }
            value(
                "[400, ORG, same-code, Key Globex+, KEY-GLOBEX-003, null, null]; " +
                    "[401, KKeyPerson, same-code, Key Alice, null, Key Alice, Smith]"
            )
        }
    }

    @Test
    fun testInsertIfAbsentSubtypeByKeyExistingSkipsChildTable() {
        connectAndExpect({ con ->
            sqlClient {
                setDialect(H2Dialect())
            }.saveCommand(
                KKeyOrganization {
                    code = "same-code"
                    name = "Should not update"
                    taxCode = "SHOULD-NOT-WRITE"
                }
            ) {
                setMode(SaveMode.INSERT_IF_ABSENT)
            }.execute(con)
            "${joinedKeyClientRow(con, "ORG", "same-code")}; " +
                joinedKeyClientRow(con, "KKeyPerson", "same-code")
        }) {
            statement {
                sql(
                    "merge into JOINED_KEY_CLIENT tb_1_ " +
                        "using(values(?, ?, ?)) tb_2_(CLIENT_TYPE, CODE, NAME) " +
                        "on tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE and tb_1_.CODE = tb_2_.CODE " +
                        "when not matched then insert(CLIENT_TYPE, CODE, NAME) " +
                        "values(tb_2_.CLIENT_TYPE, tb_2_.CODE, tb_2_.NAME)"
                )
                variables("ORG", "same-code", "Should not update")
            }
            value(
                "[400, ORG, same-code, Key Globex, KEY-GLOBEX-001, null, null]; " +
                    "[401, KKeyPerson, same-code, Key Alice, null, Key Alice, Smith]"
            )
        }
    }

    @Test
    fun testInsertIfAbsentSubtypeByKeyMissingInsertsChildTable() {
        connectAndExpect({ con ->
            sqlClient {
                setDialect(H2Dialect())
            }.saveCommand(
                KKeyOrganization {
                    code = "new-key-code"
                    name = "New Key Org"
                    taxCode = "NEW-KEY-001"
                }
            ) {
                setMode(SaveMode.INSERT_IF_ABSENT)
            }.execute(con)
            joinedKeyClientRow(con, "ORG", "new-key-code")
        }) {
            statement {
                sql(
                    "merge into JOINED_KEY_CLIENT tb_1_ " +
                        "using(values(?, ?, ?)) tb_2_(CLIENT_TYPE, CODE, NAME) " +
                        "on tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE and tb_1_.CODE = tb_2_.CODE " +
                        "when not matched then insert(CLIENT_TYPE, CODE, NAME) " +
                        "values(tb_2_.CLIENT_TYPE, tb_2_.CODE, tb_2_.NAME)"
                )
                variables("ORG", "new-key-code", "New Key Org")
            }
            statement {
                sql(
                    "insert into JOINED_KEY_ORGANIZATION(ID, TAX_CODE) " +
                        "values(?, ?)"
                )
                variables {
                    assertEquals(2, size)
                    assertEquals("NEW-KEY-001", this[1])
                }
            }
            value("[UNKNOWN, ORG, new-key-code, New Key Org, NEW-KEY-001, null, null]")
        }
    }

    private fun joinedKeyClientRow(con: Connection, type: String, code: String): String? =
        con.prepareStatement(
            "select c.ID, c.CLIENT_TYPE, c.CODE, c.NAME, o.TAX_CODE, p.FIRST_NAME, p.LAST_NAME " +
                "from JOINED_KEY_CLIENT c " +
                "left join JOINED_KEY_ORGANIZATION o on c.ID = o.ID " +
                "left join JOINED_KEY_PERSON p on c.ID = p.ID " +
                "where c.CLIENT_TYPE = ? and c.CODE = ?"
        ).use { stmt ->
            stmt.setString(1, type)
            stmt.setString(2, code)
            stmt.executeQuery().use { rs ->
                if (!rs.next()) {
                    null
                } else {
                    val id = rs.getLong(1)
                    val idText = if (id in 400L..401L) id.toString() else "UNKNOWN"
                    "[$idText, ${rs.getString(2)}, ${rs.getString(3)}, ${rs.getString(4)}, " +
                        "${rs.getString(5)}, ${rs.getString(6)}, ${rs.getString(7)}]"
                }
            }
        }
}
