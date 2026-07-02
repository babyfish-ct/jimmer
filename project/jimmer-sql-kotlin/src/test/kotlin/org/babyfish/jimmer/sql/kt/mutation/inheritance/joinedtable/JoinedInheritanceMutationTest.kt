package org.babyfish.jimmer.sql.kt.mutation.inheritance.joinedtable

import org.babyfish.jimmer.sql.DissociateAction
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode
import org.babyfish.jimmer.sql.ast.mutation.QueryReason
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.ast.TypeMatchMode
import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.exception.ExecutionException
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.*
import java.sql.Connection
import kotlin.test.Test
import kotlin.test.assertFailsWith

class JoinedInheritanceMutationTest : AbstractMutationTest() {

    @Test
    fun testInsertDerivedType() {
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
    fun testUpdateAbstractRootWithUserOptimisticLock() {
        connectAndExpect({ con ->
            sqlClient.entities.forConnection(con).save(
                KClient {
                    id = 200L
                    name = "Globex Base+"
                }
            ) {
                setMode(SaveMode.UPDATE_ONLY)
                setOptimisticLock(KClient::class) {
                    table.name eq "Globex"
                }
            }
            joinedClientRow(con, 200L)
        }) {
            statement {
                sql(
                    "update JOINED_CLIENT " +
                            "set NAME = ? " +
                            "where ID = ? and NAME = ?"
                )
                variables("Globex Base+", 200L, "Globex")
            }
            value("[ORG, Globex Base+, GLOBEX-001, null, null]")
        }
    }

    @Test
    fun testUpsertDerivedType() {
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
                    "merge into JOINED_CLIENT tb_1_ " +
                            "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                            "on tb_1_.ID = tb_2_.ID " +
                            "when matched and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                            "then update set NAME = tb_2_.NAME " +
                            "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                            "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
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

    @Test
    fun testUpsertDerivedTypeWithChangingDiscriminator() {
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
            ) {
                setTypeChangeAllowed()
            }.execute(con)
            joinedClientRow(con, 200L)
        }) {
            statement {
                sql("select ID, CLIENT_TYPE from JOINED_CLIENT where ID = ? order by ID")
                variables(200L)
            }
            statement {
                sql(
                    "update JOINED_CLIENT " +
                            "set CLIENT_TYPE = ?, NAME = ? " +
                            "where ID = ? and CLIENT_TYPE = ?"
                )
                variables("KPerson", "Globex Person", 200L, "ORG")
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
    fun testUpdateDerivedTypeWithChangingDiscriminator() {
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
                setTypeChangeAllowed()
            }
            joinedClientRow(con, 200L)
        }) {
            statement {
                sql("select ID, CLIENT_TYPE from JOINED_CLIENT where ID = ? order by ID")
                variables(200L)
            }
            statement {
                sql(
                    "update JOINED_CLIENT " +
                            "set CLIENT_TYPE = ?, NAME = ? " +
                            "where ID = ? and CLIENT_TYPE = ?"
                )
                variables("KPerson", "Globex Person", 200L, "ORG")
            }
            statement {
                sql("delete from JOINED_ORGANIZATION where ID = ?")
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
    fun testUpdateDerivedTypeWithoutChangingDiscriminator() {
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
                            "set NAME = ? " +
                            "where ID = ? and CLIENT_TYPE = ?"
                )
                variables("Globex+", 200L, "ORG")
            }
            statement {
                sql(
                    "update JOINED_ORGANIZATION " +
                            "set TAX_CODE = ? " +
                            "where ID = ? and exists(" +
                            "select 1 from JOINED_CLIENT " +
                            "where JOINED_CLIENT.ID = ? and CLIENT_TYPE = ?)"
                )
                variables("GLOBEX-002", 200L, 200L, "ORG")
            }
            value("[ORG, Globex+, GLOBEX-002, null, null]")
        }
    }

    @Test
    fun testUpdateRootAssociationToDerivedTypeTarget() {
        executeAndExpectResult({ con ->
            sqlClient.entities.forConnection(con).save(
                KClientProject {
                    id = 2000L
                    name = "Joined root project+"
                    clientId = 201L
                }
            ) {
                setMode(SaveMode.UPDATE_ONLY)
            }
        }) {
            statement {
                sql(
                    "update JOINED_CLIENT_PROJECT " +
                            "set NAME = ?, CLIENT_ID = ? " +
                            "where ID = ?"
                )
                variables("Joined root project+", 201L, 2000L)
            }
            rowCount(KClientProject::class, 1)
            entity {}
        }
    }

    @Test
    fun testUpdateDerivedTypeAssociationToDerivedTypeTarget() {
        executeAndExpectResult({ con ->
            sqlClient.entities.forConnection(con).save(
                KOrganizationProject {
                    id = 2001L
                    name = "Joined organization project+"
                    organizationId = 202L
                }
            ) {
                setMode(SaveMode.UPDATE_ONLY)
            }
        }) {
            statement {
                sql(
                    "update JOINED_ORG_PROJECT " +
                            "set NAME = ?, ORGANIZATION_ID = ? " +
                            "where ID = ?"
                )
                variables("Joined organization project+", 202L, 2001L)
            }
            rowCount(KOrganizationProject::class, 1)
            entity {}
        }
    }

    @Test
    fun testUpdateDerivedTypeWithAcceptedPostAssociation() {
        connectAndExpect({ con ->
            sqlClient.entities.forConnection(con).save(
                KOrganization {
                    id = 200L
                    taxCode = "GLOBEX-003"
                    projects().addBy {
                        id = 2300L
                        name = "Accepted project"
                    }
                }
            ) {
                setMode(SaveMode.UPDATE_ONLY)
                setAssociatedMode(KOrganization::projects, AssociatedSaveMode.APPEND)
            }
            "${joinedClientRow(con, 200L)}; ${joinedOrgProjectTargetId(con, 2300L)}"
        }) {
            statement {
                sql(
                    "update JOINED_CLIENT " +
                            "set /* fake update to return all ids */ ID = ID " +
                            "where ID = ? and CLIENT_TYPE = ?"
                )
                variables(200L, "ORG")
            }
            statement {
                sql(
                    "update JOINED_ORGANIZATION " +
                            "set TAX_CODE = ? " +
                            "where ID = ? and exists(" +
                            "select 1 from JOINED_CLIENT " +
                            "where JOINED_CLIENT.ID = ? and CLIENT_TYPE = ?)"
                )
                variables("GLOBEX-003", 200L, 200L, "ORG")
            }
            statement {
                sql(
                    "insert into JOINED_ORG_PROJECT(ID, NAME, ORGANIZATION_ID) " +
                            "values(?, ?, ?)"
                )
                variables(2300L, "Accepted project", 200L)
            }
            value("[ORG, Globex, GLOBEX-003, null, null]; 200")
        }
    }

    @Test
    fun testUpdateDerivedTypeMismatchSkipsPostAssociation() {
        connectAndExpect({ con ->
            sqlClient.entities.forConnection(con).save(
                KOrganization {
                    id = 201L
                    taxCode = "SHOULD-NOT-WRITE"
                    projects().addBy {
                        id = 2301L
                        name = "Should not be saved"
                    }
                }
            ) {
                setMode(SaveMode.UPDATE_ONLY)
            }
            "${joinedClientRow(con, 201L)}; ${joinedOrgProjectTargetId(con, 2301L)}"
        }) {
            statement {
                sql(
                    "update JOINED_CLIENT " +
                            "set /* fake update to return all ids */ ID = ID " +
                            "where ID = ? and CLIENT_TYPE = ?"
                )
                variables(201L, "ORG")
            }
            value("[KPerson, Alice, null, Alice, Smith]; null")
        }
    }

    @Test
    fun testInsertIfAbsentDerivedTypeWithAcceptedPostAssociation() {
        connectAndExpect({ con ->
            sqlClient {
                setDialect(H2Dialect())
            }.entities.forConnection(con).save(
                KOrganization {
                    id = 300L
                    name = "New Org"
                    taxCode = "NEW-001"
                    projects().addBy {
                        id = 2302L
                        name = "Inserted project"
                    }
                }
            ) {
                setMode(SaveMode.INSERT_IF_ABSENT)
                setAssociatedMode(KOrganization::projects, AssociatedSaveMode.APPEND)
            }
            "${joinedClientRow(con, 300L)}; ${joinedOrgProjectTargetId(con, 2302L)}"
        }) {
            statement {
                sql(
                    "merge into JOINED_CLIENT tb_1_ " +
                            "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                            "on tb_1_.ID = tb_2_.ID " +
                            "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                            "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
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
            statement {
                sql(
                    "insert into JOINED_ORG_PROJECT(ID, NAME, ORGANIZATION_ID) " +
                            "values(?, ?, ?)"
                )
                variables(2302L, "Inserted project", 300L)
            }
            value("[ORG, New Org, NEW-001, null, null]; 300")
        }
    }

    @Test
    fun testInsertIfAbsentDerivedTypeExistingSameSkipsPostAssociation() {
        connectAndExpect({ con ->
            sqlClient {
                setDialect(H2Dialect())
            }.entities.forConnection(con).save(
                KOrganization {
                    id = 200L
                    name = "Should not update"
                    taxCode = "SHOULD-NOT-WRITE"
                    projects().addBy {
                        id = 2303L
                        name = "Should not be saved"
                    }
                }
            ) {
                setMode(SaveMode.INSERT_IF_ABSENT)
                setAssociatedMode(KOrganization::projects, AssociatedSaveMode.APPEND)
            }
            "${joinedClientRow(con, 200L)}; ${joinedOrgProjectTargetId(con, 2303L)}"
        }) {
            statement {
                sql(
                    "merge into JOINED_CLIENT tb_1_ " +
                            "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                            "on tb_1_.ID = tb_2_.ID " +
                            "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                            "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                )
                variables(200L, "ORG", "Should not update")
            }
            value("[ORG, Globex, GLOBEX-001, null, null]; null")
        }
    }

    @Test
    fun testInsertIfAbsentDerivedTypeExistingDifferentSkipsPostAssociation() {
        connectAndExpect({ con ->
            sqlClient {
                setDialect(H2Dialect())
            }.entities.forConnection(con).save(
                KOrganization {
                    id = 201L
                    name = "Should not update"
                    taxCode = "SHOULD-NOT-WRITE"
                    projects().addBy {
                        id = 2304L
                        name = "Should not be saved"
                    }
                }
            ) {
                setMode(SaveMode.INSERT_IF_ABSENT)
                setAssociatedMode(KOrganization::projects, AssociatedSaveMode.APPEND)
            }
            "${joinedClientRow(con, 201L)}; ${joinedOrgProjectTargetId(con, 2304L)}"
        }) {
            statement {
                sql(
                    "merge into JOINED_CLIENT tb_1_ " +
                            "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                            "on tb_1_.ID = tb_2_.ID " +
                            "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                            "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                )
                variables(201L, "ORG", "Should not update")
            }
            value("[KPerson, Alice, null, Alice, Smith]; null")
        }
    }

    @Test
    fun testUpdateDerivedTypeBatchRoutesOnlyAcceptedRows() {
        connectAndExpect({ con ->
            val accepted = KOrganization {
                id = 200L
                taxCode = "GLOBEX-004"
                projects().addBy {
                    id = 2305L
                    name = "Accepted batch project"
                }
            }
            val rejected = KOrganization {
                id = 201L
                taxCode = "SHOULD-NOT-WRITE"
                projects().addBy {
                    id = 2306L
                    name = "Rejected batch project"
                }
            }
            sqlClient.entities.forConnection(con).saveEntities(
                listOf(accepted, rejected),
                SaveMode.UPDATE_ONLY,
                AssociatedSaveMode.APPEND
            )
            "${joinedClientRow(con, 200L)}; " +
                    "${joinedClientRow(con, 201L)}; " +
                    "${joinedOrgProjectTargetId(con, 2305L)}; " +
                    "${joinedOrgProjectTargetId(con, 2306L)}"
        }) {
            statement {
                sql(
                    "update JOINED_CLIENT " +
                            "set /* fake update to return all ids */ ID = ID " +
                            "where ID = ? and CLIENT_TYPE = ?"
                )
                batchVariables(0, 200L, "ORG")
                batchVariables(1, 201L, "ORG")
            }
            statement {
                sql(
                    "update JOINED_ORGANIZATION " +
                            "set TAX_CODE = ? " +
                            "where ID = ? and exists(" +
                            "select 1 from JOINED_CLIENT " +
                            "where JOINED_CLIENT.ID = ? and CLIENT_TYPE = ?)"
                )
                variables("GLOBEX-004", 200L, 200L, "ORG")
            }
            statement {
                sql(
                    "insert into JOINED_ORG_PROJECT(ID, NAME, ORGANIZATION_ID) " +
                            "values(?, ?, ?)"
                )
                variables(2305L, "Accepted batch project", 200L)
            }
            value(
                "[ORG, Globex, GLOBEX-004, null, null]; " +
                        "[KPerson, Alice, null, Alice, Smith]; " +
                        "200; null"
            )
        }
    }

    @Test
    fun testDeleteDerivedType() {
        connectAndExpect({ con ->
            sqlClient.entities.forConnection(con).delete(KOrganization::class, 202L) {
                setMode(DeleteMode.PHYSICAL)
            }
            "${joinedClientRow(con, 202L)}; ${joinedClientRow(con, 201L)}"
        }) {
            statement {
                sql(
                    "select tb_1_.ID " +
                            "from JOINED_ORG_PROJECT tb_1_ " +
                            "where tb_1_.ORGANIZATION_ID = ? limit ?"
                )
                variables(202L, 1)
            }
            statement {
                sql("delete from JOINED_ORGANIZATION where ID = ?")
                variables(202L)
            }
            statement {
                sql(
                    "select tb_1_.ID " +
                            "from JOINED_CLIENT_PROJECT tb_1_ " +
                            "where tb_1_.CLIENT_ID = ? limit ?"
                )
                variables(202L, 1)
            }
            statement {
                sql("delete from JOINED_CLIENT where ID = ? and CLIENT_TYPE = ?")
                variables(202L, "ORG")
            }
            value("null; [KPerson, Alice, null, Alice, Smith]")
        }
    }

    @Test
    fun testDeleteDerivedTypeWithAssociationTargets() {
        connectAndExpect({ con ->
            sqlClient.entities.forConnection(con).delete(KOrganization::class, 200L) {
                setMode(DeleteMode.PHYSICAL)
                setDissociateAction(KClientProject::client, DissociateAction.SET_NULL)
                setDissociateAction(KOrganizationProject::organization, DissociateAction.SET_NULL)
            }
            "${joinedClientProjectTargetId(con, 2000L)}; " +
                    "${joinedOrgProjectTargetId(con, 2001L)}; " +
                    "${joinedClientRow(con, 200L)}; " +
                    "${joinedClientRow(con, 201L)}"
        }) {
            statement {
                sql("update JOINED_ORG_PROJECT set ORGANIZATION_ID = null where ORGANIZATION_ID = ?")
                variables(200L)
            }
            statement {
                sql("delete from JOINED_ORGANIZATION where ID = ?")
                variables(200L)
            }
            statement {
                sql("update JOINED_CLIENT_PROJECT set CLIENT_ID = null where CLIENT_ID = ?")
                variables(200L)
            }
            statement {
                sql("delete from JOINED_CLIENT where ID = ? and CLIENT_TYPE = ?")
                variables(200L, "ORG")
            }
            value("null; null; null; [KPerson, Alice, null, Alice, Smith]")
        }
    }

    @Test
    fun testDeleteDerivedTypeWithMismatchedDiscriminator() {
        connectAndExpect({ con ->
            val affectedRowCount = sqlClient.entities.forConnection(con).delete(KOrganization::class, 201L) {
                setMode(DeleteMode.PHYSICAL)
            }.totalAffectedRowCount
            "$affectedRowCount; ${joinedClientRow(con, 201L)}"
        }) {
            statement {
                sql(
                    "select tb_1_.ID " +
                            "from JOINED_ORG_PROJECT tb_1_ " +
                            "where tb_1_.ORGANIZATION_ID = ? limit ?"
                )
                variables(201L, 1)
            }
            statement {
                sql("delete from JOINED_ORGANIZATION where ID = ?")
                variables(201L)
            }
            value("0; [KPerson, Alice, null, Alice, Smith]")
        }
    }

    @Test
    fun testDeleteRoot() {
        connectAndExpect({ con ->
            val ex = assertFailsWith<ExecutionException> {
                sqlClient.entities.forConnection(con).delete(KClient::class, 200L) {
                    setMode(DeleteMode.PHYSICAL)
                    setTypeMatchMode(TypeMatchMode.EXACT)
                }
            }
            ex.message
        }) {
            value(
                "Cannot delete inheritance entity type " +
                        "\"org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.KClient\" " +
                        "exactly because it is abstract. Delete an instantiable type or use POLYMORPHIC type match mode."
            )
        }
    }

    @Test
    fun testDeleteRootPolymorphically() {
        connectAndExpect({ con ->
            val affectedRowCount = sqlClient.entities.deleteAll(
                KClient::class,
                listOf(200L, 201L),
                con
            ) {
                setMode(DeleteMode.PHYSICAL)
                setTypeMatchMode(TypeMatchMode.POLYMORPHIC)
                setDissociateAction(KClientProject::client, DissociateAction.SET_NULL)
                setDissociateAction(KOrganizationProject::organization, DissociateAction.SET_NULL)
            }.totalAffectedRowCount
            "$affectedRowCount; " +
                    "${joinedClientRow(con, 200L)}; " +
                    "${joinedClientRow(con, 201L)}; " +
                    "${joinedClientProjectTargetId(con, 2000L)}; " +
                    "${joinedClientProjectTargetId(con, 2002L)}; " +
                    "${joinedOrgProjectTargetId(con, 2001L)}"
        }) {
            statement {
                queryReason(QueryReason.RESOLVE_ACCEPTED_INHERITANCE_DELETE_TARGETS)
                sql(
                    "select tb_1_.ID, tb_1_.CLIENT_TYPE " +
                            "from JOINED_CLIENT tb_1_ " +
                            "where tb_1_.ID in (?, ?) and tb_1_.CLIENT_TYPE in (?, ?)"
                )
                variables(200L, 201L, "ORG", "KPerson")
            }
            statement {
                sql("update JOINED_CLIENT_PROJECT set CLIENT_ID = null where CLIENT_ID in (?, ?)")
                variables(200L, 201L)
            }
            statement {
                sql("update JOINED_ORG_PROJECT set ORGANIZATION_ID = null where ORGANIZATION_ID = ?")
                variables(200L)
            }
            statement {
                sql("delete from JOINED_ORGANIZATION where ID = ?")
                variables(200L)
            }
            statement {
                sql("delete from JOINED_PERSON where ID = ?")
                variables(201L)
            }
            statement {
                sql("delete from JOINED_CLIENT where ID in (?, ?) and CLIENT_TYPE in (?, ?)")
                variables(200L, 201L, "ORG", "KPerson")
            }
            value("5; null; null; null; null; null")
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

    private fun joinedClientProjectTargetId(con: Connection, id: Long): String? =
        con.prepareStatement(
            "select CLIENT_ID from JOINED_CLIENT_PROJECT where ID = ?"
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

    private fun joinedOrgProjectTargetId(con: Connection, id: Long): String? =
        con.prepareStatement(
            "select ORGANIZATION_ID from JOINED_ORG_PROJECT where ID = ?"
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
