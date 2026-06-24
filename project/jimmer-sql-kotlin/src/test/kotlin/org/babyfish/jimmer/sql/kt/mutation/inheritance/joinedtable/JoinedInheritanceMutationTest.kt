package org.babyfish.jimmer.sql.kt.mutation.inheritance.joinedtable

import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.KClient
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.KOrganization
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
}
