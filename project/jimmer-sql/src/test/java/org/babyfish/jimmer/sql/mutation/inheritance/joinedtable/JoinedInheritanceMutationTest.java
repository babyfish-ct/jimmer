package org.babyfish.jimmer.sql.mutation.inheritance.joinedtable;

import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.TypeMatchMode;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.*;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.dto.ClientExhaustiveInput;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class JoinedInheritanceMutationTest extends AbstractMutationTest {

    private static String joinedClientRow(Connection con, long id) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select c.CLIENT_TYPE, c.NAME, o.TAX_CODE, p.FIRST_NAME, p.LAST_NAME " +
                        "from JOINED_CLIENT c " +
                        "left join JOINED_ORGANIZATION o on c.ID = o.ID " +
                        "left join JOINED_PERSON p on c.ID = p.ID " +
                        "where c.ID = ?"
        )) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return "[" +
                        rs.getString(1) +
                        ", " +
                        rs.getString(2) +
                        ", " +
                        rs.getString(3) +
                        ", " +
                        rs.getString(4) +
                        ", " +
                        rs.getString(5) +
                        "]";
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String joinedClientProjectTargetId(Connection con, long id) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select CLIENT_ID from JOINED_CLIENT_PROJECT where ID = ?"
        )) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Object value = rs.getObject(1);
                return value != null ? value.toString() : null;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String joinedOrgProjectTargetId(Connection con, long id) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select ORGANIZATION_ID from JOINED_ORG_PROJECT where ID = ?"
        )) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Object value = rs.getObject(1);
                return value != null ? value.toString() : null;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testInsertDerivedType() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                OrganizationDraft.$.produce(organization -> {
                                    organization.setId(300L);
                                    organization.setName("New Org");
                                    organization.setTaxCode("NEW-001");
                                })
                        )
                        .setMode(SaveMode.INSERT_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_CLIENT(ID, CLIENT_TYPE, NAME) " +
                                        "values(?, ?, ?)"
                        );
                        it.variables(300L, "ORG", "New Org");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_ORGANIZATION(ID, TAX_CODE) " +
                                        "values(?, ?)"
                        );
                        it.variables(300L, "NEW-001");
                    });
                    ctx.rowCount(AffectedTable.of(Client.class), 1);
                    ctx.rowCount(AffectedTable.of(Organization.class), 1);
                    ctx.entity(it -> {
                        it.original("{\"id\":300,\"name\":\"New Org\",\"taxCode\":\"NEW-001\"}");
                        it.modified("{\"id\":300,\"name\":\"New Org\",\"taxCode\":\"NEW-001\"}");
                    });
                }
        );
    }

    @Test
    public void testInsertDerivedTypeWithStageReturning() {
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                })
                        .saveCommand(
                                OrganizationDraft.$.produce(organization -> {
                                    organization.setName("Generated Org With Defaults");
                                    organization.setTaxCode("GEN-DEFAULT-001");
                                })
                        )
                        .setMode(SaveMode.INSERT_ONLY)
                        .execute(
                                con,
                                OrganizationFetcher.$
                                        .name()
                                        .description()
                                        .taxCode()
                                        .status()
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, DESCRIPTION " +
                                        "from final table (" +
                                        "--->insert into JOINED_CLIENT(NAME, CLIENT_TYPE) " +
                                        "--->values(?, ?)" +
                                        ")"
                        );
                        it.variables("Generated Org With Defaults", "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, STATUS " +
                                        "from final table (" +
                                        "--->insert into JOINED_ORGANIZATION(ID, TAX_CODE) " +
                                        "--->values(?, ?)" +
                                        ")"
                        );
                        it.variables(UNKNOWN_VARIABLE, "GEN-DEFAULT-001");
                    });
                    ctx.value(organization -> {
                        assertTrue(organization.id() > 0);
                        assertEquals("Generated Org With Defaults", organization.name());
                        assertEquals("DEFAULT_CLIENT_DESCRIPTION", organization.description());
                        assertEquals("GEN-DEFAULT-001", organization.taxCode());
                        assertEquals("DEFAULT_ORGANIZATION_STATUS", organization.status());
                    });
                }
        );
    }

    @Test
    public void testUpdateDerivedTypeWithStageReturning() {
        connectAndExpect(
                con -> getSqlClient(it -> it.setDialect(new H2Dialect()))
                        .getEntities()
                        .saveCommand(
                                OrganizationDraft.$.produce(organization -> {
                                    organization.setId(200L);
                                    organization.setName("Globex Returning Update");
                                    organization.setTaxCode("GLOBEX-RETURNING-UPDATE");
                                })
                        )
                        .setMode(SaveMode.UPDATE_ONLY)
                        .execute(
                                con,
                                OrganizationFetcher.$
                                        .name()
                                        .description()
                                        .taxCode()
                                        .status()
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, NAME, DESCRIPTION " +
                                        "from final table (" +
                                        "--->merge into JOINED_CLIENT tb_1_ " +
                                        "--->using(values(?, ?, ?)) tb_2_(ID, NAME, CLIENT_TYPE) " +
                                        "--->on tb_1_.ID = tb_2_.ID and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                                        "--->when matched then update set NAME = tb_2_.NAME" +
                                        ")"
                        );
                        it.variables(200L, "Globex Returning Update", "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, TAX_CODE, STATUS " +
                                        "from final table (" +
                                        "--->merge into JOINED_ORGANIZATION tb_1_ " +
                                        "--->using(values(?, ?, ?)) tb_2_(ID, TAX_CODE, CLIENT_TYPE) " +
                                        "--->on tb_1_.ID = tb_2_.ID and exists(" +
                                        "--->--->select 1 from JOINED_CLIENT tb_root_ " +
                                        "--->--->where tb_root_.ID = tb_2_.ID " +
                                        "--->--->and tb_root_.CLIENT_TYPE = tb_2_.CLIENT_TYPE" +
                                        "--->) " +
                                        "--->when matched then update set TAX_CODE = tb_2_.TAX_CODE" +
                                        ")"
                        );
                        it.variables(200L, "GLOBEX-RETURNING-UPDATE", "ORG");
                    });
                    ctx.value(organization -> {
                        assertEquals(200L, organization.id());
                        assertEquals("Globex Returning Update", organization.name());
                        assertEquals("DEFAULT_CLIENT_DESCRIPTION", organization.description());
                        assertEquals("GLOBEX-RETURNING-UPDATE", organization.taxCode());
                        assertEquals("DEFAULT_ORGANIZATION_STATUS", organization.status());
                    });
                }
        );
    }

    @Test
    public void testUpdateDerivedTypeMismatchWithStageReturningDoesNotMaterializeExistingRow() {
        connectAndExpect(
                con -> {
                    SimpleSaveResult<Organization> result = getSqlClient(it -> it.setDialect(new H2Dialect()))
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(201L);
                                        organization.setName("Should not update");
                                        organization.setTaxCode("SHOULD-NOT-WRITE");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .execute(
                                    con,
                                    OrganizationFetcher.$
                                            .name()
                                            .description()
                                            .taxCode()
                                            .status()
                            );
                    return result.getTotalAffectedRowCount() +
                            "; " +
                            (result.getOriginalEntity() == result.getModifiedEntity()) +
                            "; " +
                            result.getModifiedEntity();
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, NAME, DESCRIPTION " +
                                        "from final table (" +
                                        "--->merge into JOINED_CLIENT tb_1_ " +
                                        "--->using(values(?, ?, ?)) tb_2_(ID, NAME, CLIENT_TYPE) " +
                                        "--->on tb_1_.ID = tb_2_.ID and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                                        "--->when matched then update set NAME = tb_2_.NAME" +
                                        ")"
                        );
                        it.variables(201L, "Should not update", "ORG");
                    });
                    ctx.value(
                            "0; true; " +
                                    "{" +
                                    "--->\"id\":201," +
                                    "--->\"name\":\"Should not update\"," +
                                    "--->\"taxCode\":\"SHOULD-NOT-WRITE\"" +
                                    "}"
                    );
                }
        );
    }

    @Test
    public void testUpsertDerivedTypeWithStageReturning() {
        connectAndExpect(
                con -> getSqlClient(it -> it.setDialect(new H2Dialect()))
                        .getEntities()
                        .saveCommand(
                                OrganizationDraft.$.produce(organization -> {
                                    organization.setId(200L);
                                    organization.setName("Globex Returning Upsert");
                                    organization.setTaxCode("GLOBEX-RETURNING-UPSERT");
                                })
                        )
                        .setMode(SaveMode.UPSERT)
                        .execute(
                                con,
                                OrganizationFetcher.$
                                        .name()
                                        .description()
                                        .taxCode()
                                        .status()
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, NAME, DESCRIPTION " +
                                        "from final table (" +
                                        "--->merge into JOINED_CLIENT tb_1_ " +
                                        "--->using(values(?, ?, ?)) tb_2_(ID, NAME, CLIENT_TYPE) " +
                                        "--->on tb_1_.ID = tb_2_.ID " +
                                        "--->when matched and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                                        "--->then update set NAME = tb_2_.NAME " +
                                        "--->when not matched then insert(ID, NAME, CLIENT_TYPE) " +
                                        "--->values(tb_2_.ID, tb_2_.NAME, tb_2_.CLIENT_TYPE)" +
                                        ")"
                        );
                        it.variables(200L, "Globex Returning Upsert", "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, TAX_CODE, STATUS " +
                                        "from final table (" +
                                        "--->merge into JOINED_ORGANIZATION tb_1_ " +
                                        "--->using(values(?, ?)) tb_2_(ID, TAX_CODE) " +
                                        "--->on tb_1_.ID = tb_2_.ID " +
                                        "--->when matched then update set TAX_CODE = tb_2_.TAX_CODE " +
                                        "--->when not matched then insert(ID, TAX_CODE) values(tb_2_.ID, tb_2_.TAX_CODE)" +
                                        ")"
                        );
                        it.variables(200L, "GLOBEX-RETURNING-UPSERT");
                    });
                    ctx.value(organization -> {
                        assertEquals(200L, organization.id());
                        assertEquals("Globex Returning Upsert", organization.name());
                        assertEquals("DEFAULT_CLIENT_DESCRIPTION", organization.description());
                        assertEquals("GLOBEX-RETURNING-UPSERT", organization.taxCode());
                        assertEquals("DEFAULT_ORGANIZATION_STATUS", organization.status());
                    });
                }
        );
    }

    @Test
    public void testUpsertDerivedTypeMismatchWithStageReturningDoesNotMaterializeExistingRow() {
        connectAndExpect(
                con -> {
                    SimpleSaveResult<Organization> result = getSqlClient(it -> it.setDialect(new H2Dialect()))
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(201L);
                                        organization.setName("Should not upsert");
                                        organization.setTaxCode("SHOULD-NOT-UPSERT");
                                    })
                            )
                            .setMode(SaveMode.UPSERT)
                            .execute(
                                    con,
                                    OrganizationFetcher.$
                                            .name()
                                            .description()
                                            .taxCode()
                                            .status()
                            );
                    return result.getTotalAffectedRowCount() +
                            "; " +
                            (result.getOriginalEntity() == result.getModifiedEntity()) +
                            "; " +
                            result.getModifiedEntity();
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, NAME, DESCRIPTION " +
                                        "from final table (" +
                                        "--->merge into JOINED_CLIENT tb_1_ " +
                                        "--->using(values(?, ?, ?)) tb_2_(ID, NAME, CLIENT_TYPE) " +
                                        "--->on tb_1_.ID = tb_2_.ID " +
                                        "--->when matched and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                                        "--->then update set NAME = tb_2_.NAME " +
                                        "--->when not matched then insert(ID, NAME, CLIENT_TYPE) " +
                                        "--->values(tb_2_.ID, tb_2_.NAME, tb_2_.CLIENT_TYPE)" +
                                        ")"
                        );
                        it.variables(201L, "Should not upsert", "ORG");
                    });
                    ctx.value(
                            "0; true; " +
                                    "{" +
                                    "--->\"id\":201," +
                                    "--->\"name\":\"Should not upsert\"," +
                                    "--->\"taxCode\":\"SHOULD-NOT-UPSERT\"" +
                                    "}"
                    );
                }
        );
    }

    @Test
    public void testUpsertDerivedTypeInsertWithStageReturning() {
        connectAndExpect(
                con -> getSqlClient(it -> it.setDialect(new H2Dialect()))
                        .getEntities()
                        .saveCommand(
                                OrganizationDraft.$.produce(organization -> {
                                    organization.setId(398L);
                                    organization.setName("Inserted By Upsert Returning");
                                    organization.setTaxCode("UPSERT-INSERT-RETURNING");
                                })
                        )
                        .setMode(SaveMode.UPSERT)
                        .execute(
                                con,
                                OrganizationFetcher.$
                                        .name()
                                        .description()
                                        .taxCode()
                                        .status()
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, NAME, DESCRIPTION " +
                                        "from final table (" +
                                        "--->merge into JOINED_CLIENT tb_1_ " +
                                        "--->using(values(?, ?, ?)) tb_2_(ID, NAME, CLIENT_TYPE) " +
                                        "--->on tb_1_.ID = tb_2_.ID " +
                                        "--->when matched and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                                        "--->then update set NAME = tb_2_.NAME " +
                                        "--->when not matched then insert(ID, NAME, CLIENT_TYPE) " +
                                        "--->values(tb_2_.ID, tb_2_.NAME, tb_2_.CLIENT_TYPE)" +
                                        ")"
                        );
                        it.variables(398L, "Inserted By Upsert Returning", "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, TAX_CODE, STATUS " +
                                        "from final table (" +
                                        "--->merge into JOINED_ORGANIZATION tb_1_ " +
                                        "--->using(values(?, ?)) tb_2_(ID, TAX_CODE) " +
                                        "--->on tb_1_.ID = tb_2_.ID " +
                                        "--->when matched then update set TAX_CODE = tb_2_.TAX_CODE " +
                                        "--->when not matched then insert(ID, TAX_CODE) values(tb_2_.ID, tb_2_.TAX_CODE)" +
                                        ")"
                        );
                        it.variables(398L, "UPSERT-INSERT-RETURNING");
                    });
                    ctx.value(organization -> {
                        assertEquals(398L, organization.id());
                        assertEquals("Inserted By Upsert Returning", organization.name());
                        assertEquals("DEFAULT_CLIENT_DESCRIPTION", organization.description());
                        assertEquals("UPSERT-INSERT-RETURNING", organization.taxCode());
                        assertEquals("DEFAULT_ORGANIZATION_STATUS", organization.status());
                    });
                }
        );
    }

    @Test
    public void testInsertIfAbsentDerivedTypeWithStageReturning() {
        connectAndExpect(
                con -> getSqlClient(it -> it.setDialect(new H2Dialect()))
                        .getEntities()
                        .saveCommand(
                                OrganizationDraft.$.produce(organization -> {
                                    organization.setId(399L);
                                    organization.setName("Inserted If Absent Returning");
                                    organization.setTaxCode("INSERT-IF-ABSENT-RETURNING");
                                })
                        )
                        .setMode(SaveMode.INSERT_IF_ABSENT)
                        .execute(
                                con,
                                OrganizationFetcher.$
                                        .name()
                                        .description()
                                        .taxCode()
                                        .status()
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, NAME, DESCRIPTION " +
                                        "from final table (" +
                                        "--->merge into JOINED_CLIENT tb_1_ " +
                                        "--->using(values(?, ?, ?)) tb_2_(ID, NAME, CLIENT_TYPE) " +
                                        "--->on tb_1_.ID = tb_2_.ID " +
                                        "--->when not matched then insert(ID, NAME, CLIENT_TYPE) " +
                                        "--->values(tb_2_.ID, tb_2_.NAME, tb_2_.CLIENT_TYPE)" +
                                        ")"
                        );
                        it.variables(399L, "Inserted If Absent Returning", "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, STATUS " +
                                        "from final table (" +
                                        "--->insert into JOINED_ORGANIZATION(ID, TAX_CODE) " +
                                        "--->values(?, ?)" +
                                        ")"
                        );
                        it.variables(399L, "INSERT-IF-ABSENT-RETURNING");
                    });
                    ctx.value(organization -> {
                        assertEquals(399L, organization.id());
                        assertEquals("Inserted If Absent Returning", organization.name());
                        assertEquals("DEFAULT_CLIENT_DESCRIPTION", organization.description());
                        assertEquals("INSERT-IF-ABSENT-RETURNING", organization.taxCode());
                        assertEquals("DEFAULT_ORGANIZATION_STATUS", organization.status());
                    });
                }
        );
    }

    @Test
    public void testBatchUpdateDerivedTypeWithStageReturningDoesNotMaterializeRejectedRows() {
        connectAndExpect(
                con -> {
                    Organization accepted = OrganizationDraft.$.produce(organization -> {
                        organization.setId(200L);
                        organization.setName("Globex Batch Returning Update");
                        organization.setTaxCode("GLOBEX-BATCH-RETURNING");
                    });
                    Organization notAccepted = OrganizationDraft.$.produce(organization -> {
                        organization.setId(201L);
                        organization.setName("Should not batch update");
                        organization.setTaxCode("SHOULD-NOT-BATCH-WRITE");
                    });
                    BatchSaveResult<Organization> result = getSqlClient(it -> it.setDialect(new H2Dialect()))
                            .getEntities()
                            .saveEntitiesCommand(Arrays.asList(accepted, notAccepted))
                            .setMode(SaveMode.UPDATE_ONLY)
                            .execute(
                                    con,
                                    OrganizationFetcher.$
                                            .name()
                                            .description()
                                            .taxCode()
                                            .status()
                            );
                    return result.getTotalAffectedRowCount() +
                            "; " +
                            (result.getItems().get(0).getOriginalEntity() ==
                                    result.getItems().get(0).getModifiedEntity()) +
                            "; " +
                            result.getItems().get(0).getModifiedEntity() +
                            "; " +
                            (result.getItems().get(1).getOriginalEntity() ==
                                    result.getItems().get(1).getModifiedEntity()) +
                            "; " +
                            result.getItems().get(1).getModifiedEntity();
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, NAME, DESCRIPTION " +
                                        "from final table (" +
                                        "--->merge into JOINED_CLIENT tb_1_ " +
                                        "--->using(values(?, ?, ?), (?, ?, ?)) tb_2_(ID, NAME, CLIENT_TYPE) " +
                                        "--->on tb_1_.ID = tb_2_.ID and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                                        "--->when matched then update set NAME = tb_2_.NAME" +
                                        ")"
                        );
                        it.variables(
                                200L, "Globex Batch Returning Update", "ORG",
                                201L, "Should not batch update", "ORG"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, TAX_CODE, STATUS " +
                                        "from final table (" +
                                        "--->merge into JOINED_ORGANIZATION tb_1_ " +
                                        "--->using(values(?, ?, ?)) tb_2_(ID, TAX_CODE, CLIENT_TYPE) " +
                                        "--->on tb_1_.ID = tb_2_.ID and exists(" +
                                        "--->--->select 1 from JOINED_CLIENT tb_root_ " +
                                        "--->--->where tb_root_.ID = tb_2_.ID " +
                                        "--->--->and tb_root_.CLIENT_TYPE = tb_2_.CLIENT_TYPE" +
                                        "--->) " +
                                        "--->when matched then update set TAX_CODE = tb_2_.TAX_CODE" +
                                        ")"
                        );
                        it.variables(200L, "GLOBEX-BATCH-RETURNING", "ORG");
                    });
                    ctx.value(
                            "2; false; " +
                                    "{" +
                                    "--->\"id\":200," +
                                    "--->\"name\":\"Globex Batch Returning Update\"," +
                                    "--->\"description\":\"DEFAULT_CLIENT_DESCRIPTION\"," +
                                    "--->\"taxCode\":\"GLOBEX-BATCH-RETURNING\"," +
                                    "--->\"status\":\"DEFAULT_ORGANIZATION_STATUS\"" +
                                    "}; true; " +
                                    "{" +
                                    "--->\"id\":201," +
                                    "--->\"name\":\"Should not batch update\"," +
                                    "--->\"taxCode\":\"SHOULD-NOT-BATCH-WRITE\"" +
                                    "}"
                    );
                }
        );
    }

    @Test
    public void testBatchInsertPolymorphicInputsUsesJoinedTableStageBatches() {
        ClientExhaustiveInput.Person person1Input = new ClientExhaustiveInput.Person();
        person1Input.setId(311L);
        person1Input.setName("Batch Person 1");
        person1Input.setFirstName("Alice");
        person1Input.setLastName("Green");

        ClientExhaustiveInput.Organization organization1Input = new ClientExhaustiveInput.Organization();
        organization1Input.setId(312L);
        organization1Input.setName("Batch Org 1");
        organization1Input.setTaxCode("B-ORG-1");

        ClientExhaustiveInput.Person person2Input = new ClientExhaustiveInput.Person();
        person2Input.setId(313L);
        person2Input.setName("Batch Person 2");
        person2Input.setFirstName("Charlie");
        person2Input.setLastName("Blue");

        ClientExhaustiveInput.Organization organization2Input = new ClientExhaustiveInput.Organization();
        organization2Input.setId(314L);
        organization2Input.setName("Batch Org 2");
        organization2Input.setTaxCode("B-ORG-2");

        connectAndExpect(
                con -> getSqlClient()
                        .getEntities()
                        .saveInputsCommand(Arrays.<ClientExhaustiveInput>asList(
                                person1Input,
                                organization1Input,
                                person2Input,
                                organization2Input
                        ))
                        .setMode(SaveMode.INSERT_ONLY)
                        .execute(
                                con,
                                ClientFetcher.$
                                        .name()
                                        .forType(OrganizationFetcher.$.taxCode())
                                        .forType(PersonFetcher.$.firstName().lastName())
                        )
                        .getItems()
                        .stream()
                        .map(BatchSaveResult.Item::getModifiedEntity)
                        .collect(Collectors.toList()),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_CLIENT(ID, CLIENT_TYPE, NAME) " +
                                        "values(?, ?, ?)"
                        );
                        it.batches(4);
                        it.batchVariables(0, 311L, "Person", "Batch Person 1");
                        it.batchVariables(1, 312L, "ORG", "Batch Org 1");
                        it.batchVariables(2, 313L, "Person", "Batch Person 2");
                        it.batchVariables(3, 314L, "ORG", "Batch Org 2");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_PERSON(ID, FIRST_NAME, LAST_NAME) " +
                                        "values(?, ?, ?)"
                        );
                        it.batches(2);
                        it.batchVariables(0, 311L, "Alice", "Green");
                        it.batchVariables(1, 313L, "Charlie", "Blue");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_ORGANIZATION(ID, TAX_CODE) " +
                                        "values(?, ?)"
                        );
                        it.batches(2);
                        it.batchVariables(0, 312L, "B-ORG-1");
                        it.batchVariables(1, 314L, "B-ORG-2");
                    });
                    ctx.value(clients -> {
                        assertEquals(4, clients.size());
                        assertEquals(311L, clients.get(0).id());
                        assertEquals(312L, clients.get(1).id());
                        assertEquals(313L, clients.get(2).id());
                        assertEquals(314L, clients.get(3).id());
                    });
                }
        );
    }

    @Test
    public void testBatchInsertPolymorphicInputsWithGeneratedIdsUsesJoinedTableStageBatches() {
        ClientExhaustiveInput.Person person1Input = new ClientExhaustiveInput.Person();
        person1Input.setName("Generated Person 1");
        person1Input.setFirstName("Alice");
        person1Input.setLastName("Green");

        ClientExhaustiveInput.Organization organization1Input = new ClientExhaustiveInput.Organization();
        organization1Input.setName("Generated Org 1");
        organization1Input.setTaxCode("G-ORG-1");

        ClientExhaustiveInput.Person person2Input = new ClientExhaustiveInput.Person();
        person2Input.setName("Generated Person 2");
        person2Input.setFirstName("Charlie");
        person2Input.setLastName("Blue");

        ClientExhaustiveInput.Organization organization2Input = new ClientExhaustiveInput.Organization();
        organization2Input.setName("Generated Org 2");
        organization2Input.setTaxCode("G-ORG-2");

        connectAndExpect(
                con -> getSqlClient(it -> it.setIdGenerator(IdentityIdGenerator.INSTANCE))
                        .getEntities()
                        .saveInputsCommand(Arrays.<ClientExhaustiveInput>asList(
                                person1Input,
                                organization1Input,
                                person2Input,
                                organization2Input
                        ))
                        .setMode(SaveMode.INSERT_ONLY)
                        .execute(
                                con,
                                ClientFetcher.$
                                        .name()
                                        .forType(OrganizationFetcher.$.taxCode())
                                        .forType(PersonFetcher.$.firstName().lastName())
                        )
                        .getItems()
                        .stream()
                        .map(BatchSaveResult.Item::getModifiedEntity)
                        .collect(Collectors.toList()),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_CLIENT(CLIENT_TYPE, NAME) " +
                                        "values(?, ?)"
                        );
                        it.batches(4);
                        it.batchVariables(0, "Person", "Generated Person 1");
                        it.batchVariables(1, "ORG", "Generated Org 1");
                        it.batchVariables(2, "Person", "Generated Person 2");
                        it.batchVariables(3, "ORG", "Generated Org 2");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_PERSON(ID, FIRST_NAME, LAST_NAME) " +
                                        "values(?, ?, ?)"
                        );
                        it.batches(2);
                        it.batchVariables(0, UNKNOWN_VARIABLE, "Alice", "Green");
                        it.batchVariables(1, UNKNOWN_VARIABLE, "Charlie", "Blue");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_ORGANIZATION(ID, TAX_CODE) " +
                                        "values(?, ?)"
                        );
                        it.batches(2);
                        it.batchVariables(0, UNKNOWN_VARIABLE, "G-ORG-1");
                        it.batchVariables(1, UNKNOWN_VARIABLE, "G-ORG-2");
                    });
                    ctx.value(clients -> {
                        assertEquals(4, clients.size());
                        assertTrue(clients.get(0).id() > 0);
                        assertTrue(clients.get(1).id() > 0);
                        assertTrue(clients.get(2).id() > 0);
                        assertTrue(clients.get(3).id() > 0);
                    });
                }
        );
    }

    @Test
    public void testUpdateAbstractRootWithUserOptimisticLock() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    ClientDraft.$.produce(client -> {
                                        client.setId(200L);
                                        client.setName("Globex Base+");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setOptimisticLock(
                                    ClientTable.class,
                                    (table, it) -> table.name().eq("Globex")
                            )
                            .execute(con);
                    return joinedClientRow(con, 200L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set NAME = ? " +
                                        "where ID = ? and NAME = ?"
                        );
                        it.variables("Globex Base+", 200L, "Globex");
                    });
                    ctx.value("[ORG, Globex Base+, GLOBEX-001, null, null]");
                }
        );
    }

    @Test
    public void testUpsertDerivedType() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                OrganizationDraft.$.produce(organization -> {
                                    organization.setId(300L);
                                    organization.setName("New Org");
                                    organization.setTaxCode("NEW-001");
                                })
                        ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                                        "then update set NAME = tb_2_.NAME " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(300L, "ORG", "New Org");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_ORGANIZATION(ID, TAX_CODE) " +
                                        "key(ID) values(?, ?)"
                        );
                        it.variables(300L, "NEW-001");
                    });
                    ctx.rowCount(AffectedTable.of(Client.class), 1);
                    ctx.rowCount(AffectedTable.of(Organization.class), 1);
                    ctx.entity(it -> {
                        it.original("{\"id\":300,\"name\":\"New Org\",\"taxCode\":\"NEW-001\"}");
                        it.modified("{\"id\":300,\"name\":\"New Org\",\"taxCode\":\"NEW-001\"}");
                    });
                }
        );
    }

    @Test
    public void testUpsertDerivedTypeWithChangingDiscriminator() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    PersonDraft.$.produce(person -> {
                                        person.setId(200L);
                                        person.setName("Globex Person");
                                        person.setFirstName("Gary");
                                        person.setLastName("Stone");
                                    })
                            )
                            .setTypeChangeAllowed(true)
                            .execute(con);
                    return joinedClientRow(con, 200L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select ID, CLIENT_TYPE from JOINED_CLIENT where ID = ? order by ID");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set CLIENT_TYPE = ?, NAME = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Person", "Globex Person", 200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_PERSON(ID, FIRST_NAME, LAST_NAME) " +
                                        "key(ID) values(?, ?, ?)"
                        );
                        it.variables(200L, "Gary", "Stone");
                    });
                    ctx.value("[Person, Globex Person, null, Gary, Stone]");
                }
        );
    }

    @Test
    public void testUpdateDerivedTypeWithChangingDiscriminator() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    PersonDraft.$.produce(person -> {
                                        person.setId(200L);
                                        person.setName("Globex Person");
                                        person.setFirstName("Gary");
                                        person.setLastName("Stone");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setTypeChangeAllowed(true)
                            .execute(con);
                    return joinedClientRow(con, 200L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select ID, CLIENT_TYPE from JOINED_CLIENT where ID = ? order by ID");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set CLIENT_TYPE = ?, NAME = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Person", "Globex Person", 200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_PERSON(ID, FIRST_NAME, LAST_NAME) " +
                                        "key(ID) values(?, ?, ?)"
                        );
                        it.variables(200L, "Gary", "Stone");
                    });
                    ctx.value("[Person, Globex Person, null, Gary, Stone]");
                }
        );
    }

    @Test
    public void testUpdateDerivedTypeWithTypeChangeAllowedButSameDiscriminator() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(200L);
                                        organization.setName("Globex Same");
                                        organization.setTaxCode("GLOBEX-SAME");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setTypeChangeAllowed(true)
                            .execute(con);
                    return joinedClientRow(con, 200L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select ID, CLIENT_TYPE from JOINED_CLIENT where ID = ? order by ID");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set NAME = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Globex Same", 200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_ORGANIZATION(ID, TAX_CODE) " +
                                        "key(ID) values(?, ?)"
                        );
                        it.variables(200L, "GLOBEX-SAME");
                    });
                    ctx.value("[ORG, Globex Same, GLOBEX-SAME, null, null]");
                }
        );
    }

    @Test
    public void testUpdateDerivedTypeWithTypeChangeAllowedMixedBatch() {
        connectAndExpect(
                con -> {
                    Person changed = PersonDraft.$.produce(person -> {
                        person.setId(200L);
                        person.setName("Globex Person Batch");
                        person.setFirstName("Gary");
                        person.setLastName("Stone");
                    });
                    Person same = PersonDraft.$.produce(person -> {
                        person.setId(201L);
                        person.setName("Alice Person Batch");
                        person.setFirstName("Alice+");
                        person.setLastName("Smith+");
                    });
                    Person missing = PersonDraft.$.produce(person -> {
                        person.setId(399L);
                        person.setName("Missing Person Batch");
                        person.setFirstName("Missing");
                        person.setLastName("Person");
                    });
                    getSqlClient()
                            .getEntities()
                            .saveEntitiesCommand(Arrays.asList(changed, same, missing))
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setTypeChangeAllowed(true)
                            .execute(con);
                    return joinedClientRow(con, 200L) +
                            "; " +
                            joinedClientRow(con, 201L) +
                            "; " +
                            joinedClientRow(con, 399L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select ID, CLIENT_TYPE from JOINED_CLIENT where ID in (?, ?, ?) order by ID");
                        it.variables(200L, 201L, 399L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set CLIENT_TYPE = ?, NAME = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Person", "Globex Person Batch", 200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set NAME = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Alice Person Batch", 201L, "Person");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_PERSON(ID, FIRST_NAME, LAST_NAME) " +
                                        "key(ID) values(?, ?, ?)"
                        );
                        it.batchVariables(0, 200L, "Gary", "Stone");
                        it.batchVariables(1, 201L, "Alice+", "Smith+");
                    });
                    ctx.value("[Person, Globex Person Batch, null, Gary, Stone]; " +
                            "[Person, Alice Person Batch, null, Alice+, Smith+]; " +
                            "null");
                }
        );
    }

    @Test
    public void testUpdateDerivedTypeWithChangingDiscriminatorMissingSkipsChildAndPostAssociation() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(399L);
                                        organization.setName("Missing Org");
                                        organization.setTaxCode("MISSING-ORG");
                                        organization.addIntoProjects(project -> {
                                            project.setId(2313L);
                                            project.setName("Should not be saved");
                                        });
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setTypeChangeAllowed(true)
                            .setAssociatedMode(OrganizationProps.PROJECTS, AssociatedSaveMode.APPEND)
                            .execute(con);
                    return joinedClientRow(con, 399L) + "; " + joinedOrgProjectTargetId(con, 2313L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select ID, CLIENT_TYPE from JOINED_CLIENT where ID = ? order by ID");
                        it.variables(399L);
                    });
                    ctx.value("null; null");
                }
        );
    }

    @Test
    public void testUpsertDerivedTypeWithTypeChangeAllowedMixedBatch() {
        connectAndExpect(
                con -> {
                    Person changed = PersonDraft.$.produce(person -> {
                        person.setId(200L);
                        person.setName("Globex Person Upsert");
                        person.setFirstName("Gary");
                        person.setLastName("Stone");
                    });
                    Person same = PersonDraft.$.produce(person -> {
                        person.setId(201L);
                        person.setName("Alice Person Upsert");
                        person.setFirstName("Alice+");
                        person.setLastName("Smith+");
                    });
                    Person inserted = PersonDraft.$.produce(person -> {
                        person.setId(399L);
                        person.setName("Inserted Person Upsert");
                        person.setFirstName("Inserted");
                        person.setLastName("Person");
                    });
                    getSqlClient()
                            .getEntities()
                            .saveEntitiesCommand(Arrays.asList(changed, same, inserted))
                            .setTypeChangeAllowed(true)
                            .execute(con);
                    return joinedClientRow(con, 200L) +
                            "; " +
                            joinedClientRow(con, 201L) +
                            "; " +
                            joinedClientRow(con, 399L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select ID, CLIENT_TYPE from JOINED_CLIENT where ID in (?, ?, ?) order by ID");
                        it.variables(200L, 201L, 399L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set CLIENT_TYPE = ?, NAME = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Person", "Globex Person Upsert", 200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set NAME = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Alice Person Upsert", 201L, "Person");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(399L, "Person", "Inserted Person Upsert");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_PERSON(ID, FIRST_NAME, LAST_NAME) " +
                                        "key(ID) values(?, ?, ?)"
                        );
                        it.batchVariables(0, 200L, "Gary", "Stone");
                        it.batchVariables(1, 201L, "Alice+", "Smith+");
                        it.batchVariables(2, 399L, "Inserted", "Person");
                    });
                    ctx.value("[Person, Globex Person Upsert, null, Gary, Stone]; " +
                            "[Person, Alice Person Upsert, null, Alice+, Smith+]; " +
                            "[Person, Inserted Person Upsert, null, Inserted, Person]");
                }
        );
    }

    @Test
    public void testInsertIfAbsentIgnoresTypeChangeAllowed() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(201L);
                                        organization.setName("Should not update");
                                        organization.setTaxCode("SHOULD-NOT-WRITE");
                                        organization.addIntoProjects(project -> {
                                            project.setId(2314L);
                                            project.setName("Should not be saved");
                                        });
                                    })
                            )
                            .setMode(SaveMode.INSERT_IF_ABSENT)
                            .setTypeChangeAllowed(true)
                            .setAssociatedMode(OrganizationProps.PROJECTS, AssociatedSaveMode.APPEND)
                            .execute(con);
                    return joinedClientRow(con, 201L) + "; " + joinedOrgProjectTargetId(con, 2314L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(201L, "ORG", "Should not update");
                    });
                    ctx.value("[Person, Alice, null, Alice, Smith]; null");
                }
        );
    }

    @Test
    public void testUpdateDerivedTypeWithoutChangingDiscriminator() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(200L);
                                        organization.setName("Globex+");
                                        organization.setTaxCode("GLOBEX-002");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .execute(con);
                    return joinedClientRow(con, 200L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set NAME = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Globex+", 200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_ORGANIZATION " +
                                        "set TAX_CODE = ? " +
                                        "where ID = ? and exists(" +
                                        "select 1 from JOINED_CLIENT tb_root_ " +
                                        "where tb_root_.ID = ? and tb_root_.CLIENT_TYPE = ?)"
                        );
                        it.variables("GLOBEX-002", 200L, 200L, "ORG");
                    });
                    ctx.value("[ORG, Globex+, GLOBEX-002, null, null]");
                }
        );
    }

    @Test
    public void testUpdateRootAssociationToDerivedTypeTarget() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                ClientProjectDraft.$.produce(project -> {
                                    project.setId(2000L);
                                    project.setName("Joined root project+");
                                    project.setClientId(201L);
                                })
                        )
                        .setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT_PROJECT " +
                                        "set NAME = ?, CLIENT_ID = ? " +
                                        "where ID = ?"
                        );
                        it.variables("Joined root project+", 201L, 2000L);
                    });
                    ctx.rowCount(AffectedTable.of(ClientProject.class), 1);
                    ctx.entity(it -> {
                    });
                }
        );
    }

    @Test
    public void testAssociatedTypeChangeDefaultNoOp() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    ClientProjectDraft.$.produce(project -> {
                                        project.setId(2000L);
                                        project.setName("Joined associated no-op");
                                        project.setClient(
                                                PersonDraft.$.produce(person -> {
                                                    person.setId(200L);
                                                    person.setName("Should not replace");
                                                    person.setFirstName("Should");
                                                    person.setLastName("NotReplace");
                                                })
                                        );
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .execute(con);
                    return joinedClientRow(con, 200L) + "; " + joinedClientProjectTargetId(con, 2000L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                                        "then update set NAME = tb_2_.NAME " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(200L, "Person", "Should not replace");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT_PROJECT " +
                                        "set NAME = ?, CLIENT_ID = ? " +
                                        "where ID = ?"
                        );
                        it.variables("Joined associated no-op", 200L, 2000L);
                    });
                    ctx.value("[ORG, Globex, GLOBEX-001, null, null]; 200");
                }
        );
    }

    @Test
    public void testAssociatedTypeChangeAllowedByClass() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    ClientProjectDraft.$.produce(project -> {
                                        project.setId(2000L);
                                        project.setName("Joined associated replace");
                                        project.setClient(
                                                PersonDraft.$.produce(person -> {
                                                    person.setId(200L);
                                                    person.setName("Globex Associated Person");
                                                    person.setFirstName("Gary");
                                                    person.setLastName("Stone");
                                                })
                                        );
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setAssociatedTypeChangeAllowed(Client.class)
                            .execute(con);
                    return joinedClientRow(con, 200L) + "; " + joinedClientProjectTargetId(con, 2000L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select ID, CLIENT_TYPE from JOINED_CLIENT where ID = ? order by ID");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set CLIENT_TYPE = ?, NAME = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Person", "Globex Associated Person", 200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_PERSON(ID, FIRST_NAME, LAST_NAME) " +
                                        "key(ID) values(?, ?, ?)"
                        );
                        it.variables(200L, "Gary", "Stone");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT_PROJECT " +
                                        "set NAME = ?, CLIENT_ID = ? " +
                                        "where ID = ?"
                        );
                        it.variables("Joined associated replace", 200L, 2000L);
                    });
                    ctx.value("[Person, Globex Associated Person, null, Gary, Stone]; 200");
                }
        );
    }

    @Test
    public void testUpdateDerivedTypeAssociationToDerivedTypeTarget() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                OrganizationProjectDraft.$.produce(project -> {
                                    project.setId(2001L);
                                    project.setName("Joined organization project+");
                                    project.setOrganizationId(202L);
                                })
                        )
                        .setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_ORG_PROJECT " +
                                        "set NAME = ?, ORGANIZATION_ID = ? " +
                                        "where ID = ?"
                        );
                        it.variables("Joined organization project+", 202L, 2001L);
                    });
                    ctx.rowCount(AffectedTable.of(OrganizationProject.class), 1);
                    ctx.entity(it -> {
                    });
                }
        );
    }

    @Test
    public void testUpdateDerivedTypeWithAcceptedPostAssociation() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(200L);
                                        organization.setTaxCode("GLOBEX-003");
                                        organization.addIntoProjects(project -> {
                                            project.setId(2300L);
                                            project.setName("Accepted project");
                                        });
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setAssociatedMode(OrganizationProps.PROJECTS, AssociatedSaveMode.APPEND)
                            .execute(con);
                    return joinedClientRow(con, 200L) + "; " + joinedOrgProjectTargetId(con, 2300L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set /* fake update to return all ids */ ID = ID " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables(200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_ORGANIZATION " +
                                        "set TAX_CODE = ? " +
                                        "where ID = ? and exists(" +
                                        "select 1 from JOINED_CLIENT tb_root_ " +
                                        "where tb_root_.ID = ? and tb_root_.CLIENT_TYPE = ?)"
                        );
                        it.variables("GLOBEX-003", 200L, 200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_ORG_PROJECT(ID, NAME, ORGANIZATION_ID) " +
                                        "values(?, ?, ?)"
                        );
                        it.variables(2300L, "Accepted project", 200L);
                    });
                    ctx.value("[ORG, Globex, GLOBEX-003, null, null]; 200");
                }
        );
    }

    @Test
    public void testUpdateDerivedTypeMismatchSkipsPostAssociation() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(201L);
                                        organization.setTaxCode("SHOULD-NOT-WRITE");
                                        organization.addIntoProjects(project -> {
                                            project.setId(2301L);
                                            project.setName("Should not be saved");
                                        });
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .execute(con);
                    return joinedClientRow(con, 201L) + "; " + joinedOrgProjectTargetId(con, 2301L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set /* fake update to return all ids */ ID = ID " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables(201L, "ORG");
                    });
                    ctx.value("[Person, Alice, null, Alice, Smith]; null");
                }
        );
    }

    @Test
    public void testInsertIfAbsentDerivedTypeWithAcceptedPostAssociation() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(300L);
                                        organization.setName("New Org");
                                        organization.setTaxCode("NEW-001");
                                        organization.addIntoProjects(project -> {
                                            project.setId(2302L);
                                            project.setName("Inserted project");
                                        });
                                    })
                            )
                            .setMode(SaveMode.INSERT_IF_ABSENT)
                            .setAssociatedMode(OrganizationProps.PROJECTS, AssociatedSaveMode.APPEND)
                            .execute(con);
                    return joinedClientRow(con, 300L) + "; " + joinedOrgProjectTargetId(con, 2302L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(300L, "ORG", "New Org");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_ORGANIZATION(ID, TAX_CODE) " +
                                        "values(?, ?)"
                        );
                        it.variables(300L, "NEW-001");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_ORG_PROJECT(ID, NAME, ORGANIZATION_ID) " +
                                        "values(?, ?, ?)"
                        );
                        it.variables(2302L, "Inserted project", 300L);
                    });
                    ctx.value("[ORG, New Org, NEW-001, null, null]; 300");
                }
        );
    }

    @Test
    public void testInsertIfAbsentDerivedTypeExistingSameSkipsPostAssociation() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(200L);
                                        organization.setName("Should not update");
                                        organization.setTaxCode("SHOULD-NOT-WRITE");
                                        organization.addIntoProjects(project -> {
                                            project.setId(2303L);
                                            project.setName("Should not be saved");
                                        });
                                    })
                            )
                            .setMode(SaveMode.INSERT_IF_ABSENT)
                            .setAssociatedMode(OrganizationProps.PROJECTS, AssociatedSaveMode.APPEND)
                            .execute(con);
                    return joinedClientRow(con, 200L) + "; " + joinedOrgProjectTargetId(con, 2303L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(200L, "ORG", "Should not update");
                    });
                    ctx.value("[ORG, Globex, GLOBEX-001, null, null]; null");
                }
        );
    }

    @Test
    public void testInsertIfAbsentDerivedTypeExistingDifferentSkipsPostAssociation() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(201L);
                                        organization.setName("Should not update");
                                        organization.setTaxCode("SHOULD-NOT-WRITE");
                                        organization.addIntoProjects(project -> {
                                            project.setId(2304L);
                                            project.setName("Should not be saved");
                                        });
                                    })
                            )
                            .setMode(SaveMode.INSERT_IF_ABSENT)
                            .setAssociatedMode(OrganizationProps.PROJECTS, AssociatedSaveMode.APPEND)
                            .execute(con);
                    return joinedClientRow(con, 201L) + "; " + joinedOrgProjectTargetId(con, 2304L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(201L, "ORG", "Should not update");
                    });
                    ctx.value("[Person, Alice, null, Alice, Smith]; null");
                }
        );
    }

    @Test
    public void testUpdateDerivedTypeBatchRoutesOnlyAcceptedRows() {
        connectAndExpect(
                con -> {
                    Organization accepted = OrganizationDraft.$.produce(organization -> {
                        organization.setId(200L);
                        organization.setTaxCode("GLOBEX-004");
                        organization.addIntoProjects(project -> {
                            project.setId(2305L);
                            project.setName("Accepted batch project");
                        });
                    });
                    Organization rejected = OrganizationDraft.$.produce(organization -> {
                        organization.setId(201L);
                        organization.setTaxCode("SHOULD-NOT-WRITE");
                        organization.addIntoProjects(project -> {
                            project.setId(2306L);
                            project.setName("Rejected batch project");
                        });
                    });
                    getSqlClient()
                            .getEntities()
                            .saveEntitiesCommand(Arrays.asList(accepted, rejected))
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setAssociatedMode(OrganizationProps.PROJECTS, AssociatedSaveMode.APPEND)
                            .execute(con);
                    return joinedClientRow(con, 200L) +
                            "; " +
                            joinedClientRow(con, 201L) +
                            "; " +
                            joinedOrgProjectTargetId(con, 2305L) +
                            "; " +
                            joinedOrgProjectTargetId(con, 2306L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set /* fake update to return all ids */ ID = ID " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.batchVariables(0, 200L, "ORG");
                        it.batchVariables(1, 201L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_ORGANIZATION " +
                                        "set TAX_CODE = ? " +
                                        "where ID = ? and exists(" +
                                        "select 1 from JOINED_CLIENT tb_root_ " +
                                        "where tb_root_.ID = ? and tb_root_.CLIENT_TYPE = ?)"
                        );
                        it.variables("GLOBEX-004", 200L, 200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_ORG_PROJECT(ID, NAME, ORGANIZATION_ID) " +
                                        "values(?, ?, ?)"
                        );
                        it.variables(2305L, "Accepted batch project", 200L);
                    });
                    ctx.value("[ORG, Globex, GLOBEX-004, null, null]; " +
                            "[Person, Alice, null, Alice, Smith]; " +
                            "200; null");
                }
        );
    }

    @Test
    public void testUpdateDerivedTypeDumbBatchUsesSelfGuardedChildDml() {
        connectAndExpect(
                con -> {
                    Organization accepted = OrganizationDraft.$.produce(organization -> {
                        organization.setId(200L);
                        organization.setTaxCode("GLOBEX-DUMB-UPDATE");
                    });
                    Organization rejected = OrganizationDraft.$.produce(organization -> {
                        organization.setId(201L);
                        organization.setTaxCode("SHOULD-NOT-WRITE");
                    });
                    getSqlClient(it -> it.setDialect(new H2Dialect() {
                        @Override
                        public boolean isBatchDumb() {
                            return true;
                        }
                    }))
                            .getEntities()
                            .saveEntitiesCommand(Arrays.asList(accepted, rejected))
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setDumbBatchAcceptable(true)
                            .execute(con);
                    return joinedClientRow(con, 200L) +
                            "; " +
                            joinedClientRow(con, 201L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_ORGANIZATION " +
                                        "set TAX_CODE = ? " +
                                        "where ID = ? and exists(" +
                                        "select 1 from JOINED_CLIENT tb_root_ " +
                                        "where tb_root_.ID = ? and tb_root_.CLIENT_TYPE = ?)"
                        );
                        it.batchVariables(0, "GLOBEX-DUMB-UPDATE", 200L, 200L, "ORG");
                        it.batchVariables(1, "SHOULD-NOT-WRITE", 201L, 201L, "ORG");
                    });
                    ctx.value("[ORG, Globex, GLOBEX-DUMB-UPDATE, null, null]; " +
                            "[Person, Alice, null, Alice, Smith]");
                }
        );
    }

    @Test
    public void testUpdateDerivedTypeDumbBatchUsesOneByOneRootAcceptanceWhenDownstreamIsLoaded() {
        connectAndExpect(
                con -> {
                    Organization accepted = OrganizationDraft.$.produce(organization -> {
                        organization.setId(200L);
                        organization.setTaxCode("GLOBEX-DUMB-DOWNSTREAM");
                        organization.addIntoProjects(project -> {
                            project.setId(2313L);
                            project.setName("Accepted dumb project");
                        });
                    });
                    Organization rejected = OrganizationDraft.$.produce(organization -> {
                        organization.setId(201L);
                        organization.setTaxCode("SHOULD-NOT-WRITE");
                        organization.addIntoProjects(project -> {
                            project.setId(2314L);
                            project.setName("Rejected dumb project");
                        });
                    });
                    getSqlClient(it -> it.setDialect(new H2Dialect() {
                        @Override
                        public boolean isBatchDumb() {
                            return true;
                        }
                    }))
                            .getEntities()
                            .saveEntitiesCommand(Arrays.asList(accepted, rejected))
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setAssociatedMode(OrganizationProps.PROJECTS, AssociatedSaveMode.APPEND)
                            .execute(con);
                    return joinedClientRow(con, 200L) +
                            "; " +
                            joinedClientRow(con, 201L) +
                            "; " +
                            joinedOrgProjectTargetId(con, 2313L) +
                            "; " +
                            joinedOrgProjectTargetId(con, 2314L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set /* fake update to return all ids */ ID = ID " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables(200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set /* fake update to return all ids */ ID = ID " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables(201L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_ORGANIZATION " +
                                        "set TAX_CODE = ? " +
                                        "where ID = ? and exists(" +
                                        "select 1 from JOINED_CLIENT tb_root_ " +
                                        "where tb_root_.ID = ? and tb_root_.CLIENT_TYPE = ?)"
                        );
                        it.variables("GLOBEX-DUMB-DOWNSTREAM", 200L, 200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_ORG_PROJECT(ID, NAME, ORGANIZATION_ID) " +
                                        "values(?, ?, ?)"
                        );
                        it.variables(2313L, "Accepted dumb project", 200L);
                    });
                    ctx.value("[ORG, Globex, GLOBEX-DUMB-DOWNSTREAM, null, null]; " +
                            "[Person, Alice, null, Alice, Smith]; " +
                            "200; null");
                }
        );
    }

    @Test
    public void testUpsertDerivedTypeDumbBatchUsesOneByOneRootAcceptance() {
        connectAndExpect(
                con -> {
                    Organization accepted = OrganizationDraft.$.produce(organization -> {
                        organization.setId(200L);
                        organization.setName("Globex Dumb Upsert");
                        organization.setTaxCode("GLOBEX-DUMB-UPSERT");
                    });
                    Organization rejected = OrganizationDraft.$.produce(organization -> {
                        organization.setId(201L);
                        organization.setName("Should not update");
                        organization.setTaxCode("SHOULD-NOT-WRITE");
                    });
                    getSqlClient(it -> it.setDialect(new H2Dialect() {
                        @Override
                        public boolean isBatchDumb() {
                            return true;
                        }
                    }))
                            .getEntities()
                            .saveEntitiesCommand(Arrays.asList(accepted, rejected))
                            .execute(con);
                    return joinedClientRow(con, 200L) +
                            "; " +
                            joinedClientRow(con, 201L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                                        "then update set NAME = tb_2_.NAME " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(200L, "ORG", "Globex Dumb Upsert");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                                        "then update set NAME = tb_2_.NAME " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(201L, "ORG", "Should not update");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_ORGANIZATION(ID, TAX_CODE) " +
                                        "key(ID) values(?, ?)"
                        );
                        it.variables(200L, "GLOBEX-DUMB-UPSERT");
                    });
                    ctx.value("[ORG, Globex Dumb Upsert, GLOBEX-DUMB-UPSERT, null, null]; " +
                            "[Person, Alice, null, Alice, Smith]");
                }
        );
    }

    @Test
    public void testInsertIfAbsentDerivedTypeBatchRoutesOnlyInsertedRows() {
        connectAndExpect(
                con -> {
                    Organization inserted = OrganizationDraft.$.produce(organization -> {
                        organization.setId(300L);
                        organization.setName("New Batch Org");
                        organization.setTaxCode("NEW-BATCH-001");
                        organization.addIntoProjects(project -> {
                            project.setId(2307L);
                            project.setName("Inserted batch project");
                        });
                    });
                    Organization existingSame = OrganizationDraft.$.produce(organization -> {
                        organization.setId(200L);
                        organization.setName("Should not update");
                        organization.setTaxCode("SHOULD-NOT-WRITE");
                        organization.addIntoProjects(project -> {
                            project.setId(2308L);
                            project.setName("Rejected same batch project");
                        });
                    });
                    Organization existingDifferent = OrganizationDraft.$.produce(organization -> {
                        organization.setId(201L);
                        organization.setName("Should not update");
                        organization.setTaxCode("SHOULD-NOT-WRITE");
                        organization.addIntoProjects(project -> {
                            project.setId(2309L);
                            project.setName("Rejected different batch project");
                        });
                    });
                    getSqlClient()
                            .getEntities()
                            .saveEntitiesCommand(Arrays.asList(inserted, existingSame, existingDifferent))
                            .setMode(SaveMode.INSERT_IF_ABSENT)
                            .setAssociatedMode(OrganizationProps.PROJECTS, AssociatedSaveMode.APPEND)
                            .execute(con);
                    return joinedClientRow(con, 300L) +
                            "; " +
                            joinedClientRow(con, 200L) +
                            "; " +
                            joinedClientRow(con, 201L) +
                            "; " +
                            joinedOrgProjectTargetId(con, 2307L) +
                            "; " +
                            joinedOrgProjectTargetId(con, 2308L) +
                            "; " +
                            joinedOrgProjectTargetId(con, 2309L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.batchVariables(0, 300L, "ORG", "New Batch Org");
                        it.batchVariables(1, 200L, "ORG", "Should not update");
                        it.batchVariables(2, 201L, "ORG", "Should not update");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_ORGANIZATION(ID, TAX_CODE) " +
                                        "values(?, ?)"
                        );
                        it.variables(300L, "NEW-BATCH-001");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_ORG_PROJECT(ID, NAME, ORGANIZATION_ID) " +
                                        "values(?, ?, ?)"
                        );
                        it.variables(2307L, "Inserted batch project", 300L);
                    });
                    ctx.value("[ORG, New Batch Org, NEW-BATCH-001, null, null]; " +
                            "[ORG, Globex, GLOBEX-001, null, null]; " +
                            "[Person, Alice, null, Alice, Smith]; " +
                            "300; null; null");
                }
        );
    }

    @Test
    public void testInsertIfAbsentDerivedTypeDumbBatchUsesOneByOneRootAcceptance() {
        connectAndExpect(
                con -> {
                    Organization inserted = OrganizationDraft.$.produce(organization -> {
                        organization.setId(301L);
                        organization.setName("New Dumb Org");
                        organization.setTaxCode("NEW-DUMB-001");
                        organization.addIntoProjects(project -> {
                            project.setId(2310L);
                            project.setName("Inserted dumb project");
                        });
                    });
                    Organization existingSame = OrganizationDraft.$.produce(organization -> {
                        organization.setId(200L);
                        organization.setName("Should not update");
                        organization.setTaxCode("SHOULD-NOT-WRITE");
                        organization.addIntoProjects(project -> {
                            project.setId(2311L);
                            project.setName("Rejected same dumb project");
                        });
                    });
                    Organization existingDifferent = OrganizationDraft.$.produce(organization -> {
                        organization.setId(201L);
                        organization.setName("Should not update");
                        organization.setTaxCode("SHOULD-NOT-WRITE");
                        organization.addIntoProjects(project -> {
                            project.setId(2312L);
                            project.setName("Rejected different dumb project");
                        });
                    });
                    getSqlClient(it -> it.setDialect(new H2Dialect() {
                        @Override
                        public boolean isBatchDumb() {
                            return true;
                        }
                    }))
                            .getEntities()
                            .saveEntitiesCommand(Arrays.asList(inserted, existingSame, existingDifferent))
                            .setMode(SaveMode.INSERT_IF_ABSENT)
                            .setAssociatedMode(OrganizationProps.PROJECTS, AssociatedSaveMode.APPEND)
                            .execute(con);
                    return joinedClientRow(con, 301L) +
                            "; " +
                            joinedClientRow(con, 200L) +
                            "; " +
                            joinedClientRow(con, 201L) +
                            "; " +
                            joinedOrgProjectTargetId(con, 2310L) +
                            "; " +
                            joinedOrgProjectTargetId(con, 2311L) +
                            "; " +
                            joinedOrgProjectTargetId(con, 2312L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(301L, "ORG", "New Dumb Org");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(200L, "ORG", "Should not update");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(201L, "ORG", "Should not update");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_ORGANIZATION(ID, TAX_CODE) " +
                                        "values(?, ?)"
                        );
                        it.variables(301L, "NEW-DUMB-001");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_ORG_PROJECT(ID, NAME, ORGANIZATION_ID) " +
                                        "values(?, ?, ?)"
                        );
                        it.variables(2310L, "Inserted dumb project", 301L);
                    });
                    ctx.value("[ORG, New Dumb Org, NEW-DUMB-001, null, null]; " +
                            "[ORG, Globex, GLOBEX-001, null, null]; " +
                            "[Person, Alice, null, Alice, Smith]; " +
                            "301; null; null");
                }
        );
    }

    @Test
    public void testDeleteDerivedType() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .deleteCommand(Organization.class, 202L)
                            .setMode(DeleteMode.PHYSICAL)
                            .execute(con);
                    return joinedClientRow(con, 202L) + "; " + joinedClientRow(con, 201L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from JOINED_ORG_PROJECT tb_1_ " +
                                        "where tb_1_.ORGANIZATION_ID = ? limit ?"
                        );
                        it.variables(202L, 1);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
                        it.variables(202L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from JOINED_CLIENT_PROJECT tb_1_ " +
                                        "where tb_1_.CLIENT_ID = ? limit ?"
                        );
                        it.variables(202L, 1);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_CLIENT where ID = ? and CLIENT_TYPE = ?");
                        it.variables(202L, "ORG");
                    });
                    ctx.value("null; [Person, Alice, null, Alice, Smith]");
                }
        );
    }

    @Test
    public void testBatchDeleteDerivedType() {
        connectAndExpect(
                con -> {
                    int affectedRowCount = getSqlClient()
                            .getEntities()
                            .deleteAllCommand(Organization.class, Arrays.asList(202L, 201L))
                            .setMode(DeleteMode.PHYSICAL)
                            .execute(con)
                            .getTotalAffectedRowCount();
                    return affectedRowCount +
                            "; " +
                            joinedClientRow(con, 202L) +
                            "; " +
                            joinedClientRow(con, 201L) +
                            "; " +
                            joinedClientProjectTargetId(con, 2002L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from JOINED_ORG_PROJECT tb_1_ " +
                                        "where tb_1_.ORGANIZATION_ID in (?, ?) limit ?"
                        );
                        it.variables(202L, 201L, 1);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
                        it.batchVariables(0, 202L);
                        it.batchVariables(1, 201L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from JOINED_CLIENT_PROJECT tb_1_ " +
                                        "where tb_1_.CLIENT_ID = ? limit ?"
                        );
                        it.variables(202L, 1);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_CLIENT where ID = ? and CLIENT_TYPE = ?");
                        it.variables(202L, "ORG");
                    });
                    ctx.value("1; null; [Person, Alice, null, Alice, Smith]; 201");
                }
        );
    }

    @Test
    public void testDeleteDerivedTypeWithAssociationTargets() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .deleteCommand(Organization.class, 200L)
                            .setMode(DeleteMode.PHYSICAL)
                            .setDissociateAction(ClientProjectProps.CLIENT, DissociateAction.SET_NULL)
                            .setDissociateAction(OrganizationProjectProps.ORGANIZATION, DissociateAction.SET_NULL)
                            .execute(con);
                    return joinedClientProjectTargetId(con, 2000L) +
                            "; " +
                            joinedOrgProjectTargetId(con, 2001L) +
                            "; " +
                            joinedClientRow(con, 200L) +
                            "; " +
                            joinedClientRow(con, 201L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update JOINED_ORG_PROJECT set ORGANIZATION_ID = null where ORGANIZATION_ID = ?");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql("update JOINED_CLIENT_PROJECT set CLIENT_ID = null where CLIENT_ID = ?");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_CLIENT where ID = ? and CLIENT_TYPE = ?");
                        it.variables(200L, "ORG");
                    });
                    ctx.value("null; null; null; [Person, Alice, null, Alice, Smith]");
                }
        );
    }

    @Test
    public void testDeleteDerivedTypeWithMismatchedDiscriminator() {
        connectAndExpect(
                con -> {
                    int affectedRowCount = getSqlClient()
                            .getEntities()
                            .deleteCommand(Organization.class, 201L)
                            .setMode(DeleteMode.PHYSICAL)
                            .execute(con)
                            .getTotalAffectedRowCount();
                    return affectedRowCount + "; " + joinedClientRow(con, 201L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from JOINED_ORG_PROJECT tb_1_ " +
                                        "where tb_1_.ORGANIZATION_ID = ? limit ?"
                        );
                        it.variables(201L, 1);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
                        it.variables(201L);
                    });
                    ctx.value("0; [Person, Alice, null, Alice, Smith]");
                }
        );
    }

    @Test
    public void testDeleteDerivedTypeWithMismatchedDiscriminatorDoesNotCleanBaseAssociations() {
        connectAndExpect(
                con -> {
                    int affectedRowCount = getSqlClient()
                            .getEntities()
                            .deleteCommand(Organization.class, 201L)
                            .setMode(DeleteMode.PHYSICAL)
                            .setDissociateAction(ClientProjectProps.CLIENT, DissociateAction.SET_NULL)
                            .setDissociateAction(OrganizationProjectProps.ORGANIZATION, DissociateAction.SET_NULL)
                            .execute(con)
                            .getTotalAffectedRowCount();
                    return affectedRowCount +
                            "; " +
                            joinedClientProjectTargetId(con, 2002L) +
                            "; " +
                            joinedClientRow(con, 201L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update JOINED_ORG_PROJECT set ORGANIZATION_ID = null where ORGANIZATION_ID = ?");
                        it.variables(201L);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
                        it.variables(201L);
                    });
                    ctx.value("0; 201; [Person, Alice, null, Alice, Smith]");
                }
        );
    }

    @Test
    public void testDeleteRoot() {
        connectAndExpect(
                con -> {
                    ExecutionException ex = assertThrows(
                            ExecutionException.class,
                            () -> getSqlClient()
                                    .getEntities()
                                    .deleteCommand(Client.class, 200L)
                                    .setMode(DeleteMode.PHYSICAL)
                                    .setTypeMatchMode(TypeMatchMode.EXACT)
                                    .execute(con)
                    );
                    return ex.getMessage();
                },
                ctx -> {
                    ctx.value(
                            "Cannot delete inheritance entity type " +
                                    "\"org.babyfish.jimmer.sql.model.inheritance.joinedtable.Client\" " +
                                    "exactly because it is abstract. Delete an instantiable type or use POLYMORPHIC type match mode."
                    );
                }
        );
    }

    @Test
    public void testDeleteRootPolymorphically() {
        connectAndExpect(
                con -> {
                    int affectedRowCount = getSqlClient()
                            .getEntities()
                            .deleteAllCommand(Client.class, Arrays.asList(200L, 201L))
                            .setMode(DeleteMode.PHYSICAL)
                            .setTypeMatchMode(TypeMatchMode.POLYMORPHIC)
                            .setDissociateAction(ClientProjectProps.CLIENT, DissociateAction.SET_NULL)
                            .setDissociateAction(OrganizationProjectProps.ORGANIZATION, DissociateAction.SET_NULL)
                            .execute(con)
                            .getTotalAffectedRowCount();
                    return affectedRowCount +
                            "; " +
                            joinedClientRow(con, 200L) +
                            "; " +
                            joinedClientRow(con, 201L) +
                            "; " +
                            joinedClientProjectTargetId(con, 2000L) +
                            "; " +
                            joinedClientProjectTargetId(con, 2002L) +
                            "; " +
                            joinedOrgProjectTargetId(con, 2001L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.RESOLVE_ACCEPTED_INHERITANCE_DELETE_TARGETS);
                        it.sql(
                                "select tb_1_.ID, tb_1_.CLIENT_TYPE " +
                                        "from JOINED_CLIENT tb_1_ " +
                                        "where tb_1_.ID in (?, ?) and tb_1_.CLIENT_TYPE in (?, ?)"
                        );
                        it.variables(200L, 201L, "ORG", "Person");
                    });
                    ctx.statement(it -> {
                        it.sql("update JOINED_CLIENT_PROJECT set CLIENT_ID = null where CLIENT_ID in (?, ?)");
                        it.variables(200L, 201L);
                    });
                    ctx.statement(it -> {
                        it.sql("update JOINED_ORG_PROJECT set ORGANIZATION_ID = null where ORGANIZATION_ID = ?");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_PERSON where ID = ?");
                        it.variables(201L);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_CLIENT where ID in (?, ?) and CLIENT_TYPE in (?, ?)");
                        it.variables(200L, 201L, "ORG", "Person");
                    });
                    ctx.value("5; null; null; null; null; null");
                }
        );
    }

    @Test
    public void testCreateDeleteRootPolymorphicallyWithSubtypePredicateUsesAcceptedTargetFallback() {
        executeAndExpectRowCount(
                getLambdaClient().createDelete(ClientTable.class, (d, client) -> {
                    d.setMode(DeleteMode.PHYSICAL);
                    d.setTypeMatchMode(TypeMatchMode.POLYMORPHIC);
                    d.where(client.treatAs(OrganizationTable.class).taxCode().eq("INI-001"));
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select distinct tb_1_.ID " +
                                        "from JOINED_CLIENT tb_1_ " +
                                        "inner join JOINED_ORGANIZATION tb_2_ on " +
                                        "tb_1_.ID = tb_2_.ID " +
                                        "and tb_1_.CLIENT_TYPE = ? " +
                                        "where tb_2_.TAX_CODE = ? and tb_1_.CLIENT_TYPE in (?, ?)"
                        );
                        it.variables("ORG", "INI-001", "ORG", "Person");
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.RESOLVE_ACCEPTED_INHERITANCE_DELETE_TARGETS);
                        it.sql(
                                "select tb_1_.ID, tb_1_.CLIENT_TYPE " +
                                        "from JOINED_CLIENT tb_1_ " +
                                        "where tb_1_.ID = ? and tb_1_.CLIENT_TYPE in (?, ?)"
                        );
                        it.variables(202L, "ORG", "Person");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from JOINED_CLIENT_PROJECT tb_1_ " +
                                        "where tb_1_.CLIENT_ID = ? limit ?"
                        );
                        it.variables(202L, 1);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from JOINED_ORG_PROJECT tb_1_ " +
                                        "where tb_1_.ORGANIZATION_ID = ? limit ?"
                        );
                        it.variables(202L, 1);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
                        it.variables(202L);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_CLIENT where ID = ? and CLIENT_TYPE in (?, ?)");
                        it.variables(202L, "ORG", "Person");
                    });
                    ctx.rowCount(1);
                }
        );
    }
}
