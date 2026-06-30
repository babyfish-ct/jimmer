package org.babyfish.jimmer.sql.mutation.inheritance.singletable;

import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.model.inheritance.enumdiscriminator.EnumOrganization;
import org.babyfish.jimmer.sql.model.inheritance.enumdiscriminator.EnumOrganizationDraft;
import org.babyfish.jimmer.sql.model.inheritance.key.NaturalOrganizationDraft;
import org.babyfish.jimmer.sql.model.inheritance.singletable.*;
import org.babyfish.jimmer.sql.model.inheritance.singletable.dto.ClientPatchInput;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SingleTableInheritanceMutationTest extends AbstractMutationTest {

    @Test
    public void testInsertSubtype() {
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
                                "insert into CLIENT(ID, CLIENT_TYPE, NAME, TAX_CODE) " +
                                        "values(?, ?, ?, ?)"
                        );
                        it.variables(300L, "ORG", "New Org", "NEW-001");
                    });
                    ctx.rowCount(AffectedTable.of(Organization.class), 1);
                    ctx.entity(it -> {
                        it.original("{\"id\":300,\"name\":\"New Org\",\"taxCode\":\"NEW-001\"}");
                        it.modified("{\"id\":300,\"name\":\"New Org\",\"taxCode\":\"NEW-001\"}");
                    });
                }
        );
    }

    @Test
    public void testInsertAbstractRoot() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> getSqlClient()
                        .getEntities()
                        .saveCommand(
                                ClientDraft.$.produce(client -> {
                                    client.setId(300L);
                                    client.setName("Base");
                                })
                        )
                        .setMode(SaveMode.INSERT_ONLY)
                        .execute()
        );
        assertEquals(
                "Cannot save inheritance entity type " +
                        "\"org.babyfish.jimmer.sql.model.inheritance.singletable.Client\" " +
                        "because it is abstract; only UPDATE_ONLY with subtypeChangeAllowed=false is allowed",
                ex.getMessage()
        );
    }

    @Test
    public void testUpdateAbstractRoot() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    ClientDraft.$.produce(client -> {
                                        client.setId(100L);
                                        client.setName("Acme Base+");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .execute(con);
                    return clientRow(con, 100L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update CLIENT set NAME = ? where ID = ?");
                        it.variables("Acme Base+", 100L);
                    });
                    ctx.value("[ORG, Acme Base+, ACME-001, null, null]");
                }
        );
    }

    @Test
    public void testUpdateAbstractRootByDefaultInputWithoutDiscriminator() {
        connectAndExpect(
                con -> {
                    ClientPatchInput.Default input = new ClientPatchInput.Default();
                    input.setId(100L);
                    input.setName("Acme Input+");
                    getSqlClient()
                            .getEntities()
                            .saveCommand(input)
                            .setMode(SaveMode.UPDATE_ONLY)
                            .execute(con);
                    return clientRow(con, 100L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update CLIENT set NAME = ? where ID = ?");
                        it.variables("Acme Input+", 100L);
                    });
                    ctx.value("[ORG, Acme Input+, ACME-001, null, null]");
                }
        );
    }

    @Test
    public void testUpdateAbstractRootWithSubtypeChangeAllowedIsRejected() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> getSqlClient()
                        .getEntities()
                        .saveCommand(
                                ClientDraft.$.produce(client -> {
                                    client.setId(100L);
                                    client.setName("Acme Base+");
                                })
                        )
                        .setMode(SaveMode.UPDATE_ONLY)
                        .setSubtypeChangeAllowed(true)
                        .execute()
        );
        assertEquals(
                "Cannot save inheritance entity type " +
                        "\"org.babyfish.jimmer.sql.model.inheritance.singletable.Client\" " +
                        "because it is abstract; only UPDATE_ONLY with subtypeChangeAllowed=false is allowed",
                ex.getMessage()
        );
    }

    @Test
    public void testInsertSubtypeWithEnumDiscriminator() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                EnumOrganizationDraft.$.produce(organization -> {
                                    organization.setId(310L);
                                    organization.setName("Enum Org");
                                })
                        )
                        .setMode(SaveMode.INSERT_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into ENUM_CLIENT(ID, CLIENT_TYPE, NAME) " +
                                        "values(?, ?, ?)"
                        );
                        it.variables(310L, "ORG", "Enum Org");
                    });
                    ctx.rowCount(AffectedTable.of(EnumOrganization.class), 1);
                    ctx.entity(it -> {
                        it.original("{\"id\":310,\"name\":\"Enum Org\"}");
                        it.modified("{\"id\":310,\"name\":\"Enum Org\"}");
                    });
                }
        );
    }

    @Test
    public void testUpsertSubtype() {
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
	                                "merge into CLIENT tb_1_ " +
	                                        "using(values(?, ?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME, TAX_CODE) " +
	                                        "on tb_1_.ID = tb_2_.ID " +
	                                        "when matched and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
	                                        "then update set NAME = tb_2_.NAME, TAX_CODE = tb_2_.TAX_CODE " +
	                                        "when not matched then insert(ID, CLIENT_TYPE, NAME, TAX_CODE) " +
	                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME, tb_2_.TAX_CODE)"
	                        );
                        it.variables(300L, "ORG", "New Org", "NEW-001");
                    });
                    ctx.rowCount(AffectedTable.of(Organization.class), 1);
                    ctx.entity(it -> {
                        it.original("{\"id\":300,\"name\":\"New Org\",\"taxCode\":\"NEW-001\"}");
                        it.modified("{\"id\":300,\"name\":\"New Org\",\"taxCode\":\"NEW-001\"}");
                    });
                }
        );
    }

    @Test
    public void testUpdateSubtypeWithoutChangingDiscriminator() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(100L);
                                        organization.setName("Acme+");
                                        organization.setTaxCode("ACME-002");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .execute(con);
                    return clientRow(con, 100L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update CLIENT " +
                                        "set NAME = ?, TAX_CODE = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Acme+", "ACME-002", 100L, "ORG");
                    });
                    ctx.value("[ORG, Acme+, ACME-002, null, null]");
                }
        );
    }

    @Test
    public void testUpdateSubtypeWithMismatchedDiscriminator() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(101L);
                                        organization.setName("Should not update");
                                        organization.setTaxCode("SHOULD-NOT-WRITE");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .execute(con);
                    return clientRow(con, 101L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update CLIENT " +
                                        "set NAME = ?, TAX_CODE = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Should not update", "SHOULD-NOT-WRITE", 101L, "ORG");
                    });
                    ctx.value("[Person, Bob, null, Bob, Brown]");
                }
        );
    }

    @Test
    public void testUpsertSubtypeWithMismatchedDiscriminator() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(101L);
                                        organization.setName("Should not update");
                                        organization.setTaxCode("SHOULD-NOT-WRITE");
                                    })
                            )
                            .execute(con);
                    return clientRow(con, 101L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into CLIENT tb_1_ " +
                                        "using(values(?, ?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME, TAX_CODE) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                                        "then update set NAME = tb_2_.NAME, TAX_CODE = tb_2_.TAX_CODE " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME, TAX_CODE) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME, tb_2_.TAX_CODE)"
                        );
                        it.variables(101L, "ORG", "Should not update", "SHOULD-NOT-WRITE");
                    });
                    ctx.value("[Person, Bob, null, Bob, Brown]");
                }
        );
    }

    @Test
    public void testInsertIfAbsentSubtypeExistingSameDiscriminator() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(100L);
                                        organization.setName("Should not update");
                                        organization.setTaxCode("SHOULD-NOT-WRITE");
                                    })
                            )
                            .setMode(SaveMode.INSERT_IF_ABSENT)
                            .execute(con);
                    return clientRow(con, 100L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into CLIENT tb_1_ " +
                                        "using(values(?, ?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME, TAX_CODE) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME, TAX_CODE) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME, tb_2_.TAX_CODE)"
                        );
                        it.variables(100L, "ORG", "Should not update", "SHOULD-NOT-WRITE");
                    });
                    ctx.value("[ORG, Acme, ACME-001, null, null]");
                }
        );
    }

    @Test
    public void testInsertIfAbsentSubtypeExistingDifferentDiscriminator() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(101L);
                                        organization.setName("Should not update");
                                        organization.setTaxCode("SHOULD-NOT-WRITE");
                                    })
                            )
                            .setMode(SaveMode.INSERT_IF_ABSENT)
                            .execute(con);
                    return clientRow(con, 101L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into CLIENT tb_1_ " +
                                        "using(values(?, ?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME, TAX_CODE) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME, TAX_CODE) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME, tb_2_.TAX_CODE)"
                        );
                        it.variables(101L, "ORG", "Should not update", "SHOULD-NOT-WRITE");
                    });
                    ctx.value("[Person, Bob, null, Bob, Brown]");
                }
        );
    }

    @Test
    public void testInsertIfAbsentIgnoresSubtypeChangeAllowed() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(101L);
                                        organization.setName("Should not update");
                                        organization.setTaxCode("SHOULD-NOT-WRITE");
                                    })
                            )
                            .setMode(SaveMode.INSERT_IF_ABSENT)
                            .setSubtypeChangeAllowed(true)
                            .execute(con);
                    return clientRow(con, 101L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into CLIENT tb_1_ " +
                                        "using(values(?, ?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME, TAX_CODE) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME, TAX_CODE) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME, tb_2_.TAX_CODE)"
                        );
                        it.variables(101L, "ORG", "Should not update", "SHOULD-NOT-WRITE");
                    });
                    ctx.value("[Person, Bob, null, Bob, Brown]");
                }
        );
    }

    @Test
    public void testUpsertSubtypeWithChangingDiscriminator() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    PersonDraft.$.produce(person -> {
                                        person.setId(100L);
                                        person.setName("Acme Person");
                                        person.setFirstName("Ann");
                                        person.setLastName("Smith");
                                    })
                            )
                            .setSubtypeChangeAllowed(true)
                            .execute(con);
                    return clientRow(con, 100L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into CLIENT(ID, CLIENT_TYPE, NAME, FIRST_NAME, LAST_NAME, TAX_CODE) " +
                                        "key(ID) values(?, ?, ?, ?, ?, null)"
                        );
                        it.variables(100L, "Person", "Acme Person", "Ann", "Smith");
                    });
                    ctx.value("[Person, Acme Person, null, Ann, Smith]");
                }
        );
    }

    @Test
    public void testUpsertSubtypeWithSubtypeChangeAllowedMixedBatch() {
        connectAndExpect(
                con -> {
                    Person changed = PersonDraft.$.produce(person -> {
                        person.setId(100L);
                        person.setName("Acme Person Batch");
                        person.setFirstName("Ann");
                        person.setLastName("Smith");
                    });
                    Person same = PersonDraft.$.produce(person -> {
                        person.setId(101L);
                        person.setName("Bob Person Batch");
                        person.setFirstName("Bob+");
                        person.setLastName("Brown+");
                    });
                    Person inserted = PersonDraft.$.produce(person -> {
                        person.setId(399L);
                        person.setName("Inserted Person Batch");
                        person.setFirstName("Inserted");
                        person.setLastName("Person");
                    });
                    getSqlClient()
                            .getEntities()
                            .saveEntitiesCommand(Arrays.asList(changed, same, inserted))
                            .setSubtypeChangeAllowed(true)
                            .execute(con);
                    return clientRow(con, 100L) +
                            "; " +
                            clientRow(con, 101L) +
                            "; " +
                            clientRow(con, 399L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into CLIENT(ID, CLIENT_TYPE, NAME, FIRST_NAME, LAST_NAME, TAX_CODE) " +
                                        "key(ID) values(?, ?, ?, ?, ?, null)"
                        );
                        it.batchVariables(0, 100L, "Person", "Acme Person Batch", "Ann", "Smith");
                        it.batchVariables(1, 101L, "Person", "Bob Person Batch", "Bob+", "Brown+");
                        it.batchVariables(2, 399L, "Person", "Inserted Person Batch", "Inserted", "Person");
                    });
                    ctx.value("[Person, Acme Person Batch, null, Ann, Smith]; " +
                            "[Person, Bob Person Batch, null, Bob+, Brown+]; " +
                            "[Person, Inserted Person Batch, null, Inserted, Person]");
                }
        );
    }

    @Test
    public void testUpdateSubtypeWithChangingDiscriminator() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    PersonDraft.$.produce(person -> {
                                        person.setId(100L);
                                        person.setName("Acme Person");
                                        person.setFirstName("Ann");
                                        person.setLastName("Smith");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setSubtypeChangeAllowed(true)
                            .execute(con);
                    return clientRow(con, 100L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update CLIENT " +
                                        "set CLIENT_TYPE = ?, TAX_CODE = null, NAME = ?, FIRST_NAME = ?, LAST_NAME = ? " +
                                        "where ID = ?"
                        );
                        it.variables("Person", "Acme Person", "Ann", "Smith", 100L);
                    });
                    ctx.value("[Person, Acme Person, null, Ann, Smith]");
                }
        );
    }

    @Test
    public void testUpdateByDiscriminatorKey() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    NaturalOrganizationDraft.$.produce(organization -> {
                                        organization.setCode("same-code");
                                        organization.setName("Acme Natural+");
                                        organization.setTaxCode("ACME-N-002");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .execute(con);
                    return naturalClientRows(con);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update NATURAL_CLIENT " +
                                        "set NAME = ?, TAX_CODE = ? " +
                                        "where CLIENT_TYPE = ? and CODE = ?"
                        );
                        it.variables("Acme Natural+", "ACME-N-002", "ORG", "same-code");
                    });
                    ctx.value("[300, ORG, same-code, Acme Natural+, ACME-N-002, null, null]; " +
                            "[301, NaturalPerson, same-code, Bob Natural, null, Bob, Brown]");
                }
        );
    }

    @Test
    public void testUpdateRootAssociationToSubtypeTarget() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                ClientProjectDraft.$.produce(project -> {
                                    project.setId(1000L);
                                    project.setName("Single root project+");
                                    project.setClientId(101L);
                                })
                        )
                        .setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update SINGLE_CLIENT_PROJECT " +
                                        "set NAME = ?, CLIENT_ID = ? " +
                                        "where ID = ?"
                        );
                        it.variables("Single root project+", 101L, 1000L);
                    });
                    ctx.rowCount(AffectedTable.of(ClientProject.class), 1);
                    ctx.entity(it -> {});
                }
        );
    }

    @Test
    public void testAssociatedSubtypeChangeDefaultNoOp() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    ClientProjectDraft.$.produce(project -> {
                                        project.setId(1000L);
                                        project.setName("Single associated no-op");
                                        project.setClient(
                                                PersonDraft.$.produce(person -> {
                                                    person.setId(100L);
                                                    person.setName("Should not replace");
                                                    person.setFirstName("Should");
                                                    person.setLastName("NotReplace");
                                                })
                                        );
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .execute(con);
                    return clientRow(con, 100L) + "; " + singleClientProjectTargetId(con, 1000L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into CLIENT tb_1_ " +
                                        "using(values(?, ?, ?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME, FIRST_NAME, LAST_NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                                        "then update set NAME = tb_2_.NAME, FIRST_NAME = tb_2_.FIRST_NAME, LAST_NAME = tb_2_.LAST_NAME " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME, FIRST_NAME, LAST_NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME, tb_2_.FIRST_NAME, tb_2_.LAST_NAME)"
                        );
                        it.variables(100L, "Person", "Should not replace", "Should", "NotReplace");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update SINGLE_CLIENT_PROJECT " +
                                        "set NAME = ?, CLIENT_ID = ? " +
                                        "where ID = ?"
                        );
                        it.variables("Single associated no-op", 100L, 1000L);
                    });
                    ctx.value("[ORG, Acme, ACME-001, null, null]; 100");
                }
        );
    }

    @Test
    public void testAssociatedSubtypeChangeAllowedByProp() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    ClientProjectDraft.$.produce(project -> {
                                        project.setId(1000L);
                                        project.setName("Single associated replace");
                                        project.setClient(
                                                PersonDraft.$.produce(person -> {
                                                    person.setId(100L);
                                                    person.setName("Acme Associated Person");
                                                    person.setFirstName("Ann");
                                                    person.setLastName("Smith");
                                                })
                                        );
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setAssociatedSubtypeChangeAllowed(ClientProjectProps.CLIENT)
                            .execute(con);
                    return clientRow(con, 100L) + "; " + singleClientProjectTargetId(con, 1000L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into CLIENT(ID, CLIENT_TYPE, NAME, FIRST_NAME, LAST_NAME, TAX_CODE) " +
                                        "key(ID) values(?, ?, ?, ?, ?, null)"
                        );
                        it.variables(100L, "Person", "Acme Associated Person", "Ann", "Smith");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update SINGLE_CLIENT_PROJECT " +
                                        "set NAME = ?, CLIENT_ID = ? " +
                                        "where ID = ?"
                        );
                        it.variables("Single associated replace", 100L, 1000L);
                    });
                    ctx.value("[Person, Acme Associated Person, null, Ann, Smith]; 100");
                }
        );
    }

    @Test
    public void testUpdateSubtypeAssociationToSubtypeTarget() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                OrganizationProjectDraft.$.produce(project -> {
                                    project.setId(1001L);
                                    project.setName("Single organization project+");
                                    project.setOrganizationId(102L);
                                })
                        )
                        .setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update SINGLE_ORG_PROJECT " +
                                        "set NAME = ?, ORGANIZATION_ID = ? " +
                                        "where ID = ?"
                        );
                        it.variables("Single organization project+", 102L, 1001L);
                    });
                    ctx.rowCount(AffectedTable.of(OrganizationProject.class), 1);
                    ctx.entity(it -> {});
                }
        );
    }

    @Test
    public void testDeleteSubtype() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .deleteCommand(Organization.class, 102L)
                            .setMode(DeleteMode.PHYSICAL)
                            .execute(con);
                    return clientRow(con, 102L) + "; " + clientRow(con, 101L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.RESOLVE_ACCEPTED_INHERITANCE_DELETE_TARGETS);
                        it.sql(
                                "select tb_1_.ID " +
                                        "from CLIENT tb_1_ " +
                                        "where tb_1_.ID = ? and tb_1_.CLIENT_TYPE = ? for update"
                        );
                        it.variables(102L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from SINGLE_CLIENT_PROJECT tb_1_ " +
                                        "where tb_1_.CLIENT_ID = ? limit ?"
                        );
                        it.variables(102L, 1);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from SINGLE_ORG_PROJECT tb_1_ " +
                                        "where tb_1_.ORGANIZATION_ID = ? limit ?"
                        );
                        it.variables(102L, 1);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from CLIENT where ID = ? and CLIENT_TYPE = ?");
                        it.variables(102L, "ORG");
                    });
                    ctx.value("null; [Person, Bob, null, Bob, Brown]");
                }
        );
    }

    @Test
    public void testDeleteAbstractRootExactly() {
        connectAndExpect(
                con -> {
                    ExecutionException ex = assertThrows(
                            ExecutionException.class,
                            () -> getSqlClient()
                                    .getEntities()
                                    .deleteCommand(Client.class, 102L)
                                    .setMode(DeleteMode.PHYSICAL)
                                    .execute(con)
                    );
                    return ex.getMessage();
                },
                ctx -> ctx.value(
                        "Cannot delete inheritance entity type " +
                                "\"org.babyfish.jimmer.sql.model.inheritance.singletable.Client\" " +
                                "exactly because it is abstract. Delete an instantiable subtype or enable polymorphic delete."
                )
        );
    }

    @Test
    public void testDeleteRootPolymorphically() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .deleteCommand(Client.class, 102L)
                            .setMode(DeleteMode.PHYSICAL)
                            .setPolymorphic()
                            .execute(con);
                    return clientRow(con, 102L) + "; " + clientRow(con, 101L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.RESOLVE_ACCEPTED_INHERITANCE_DELETE_TARGETS);
                        it.sql(
                                "select tb_1_.ID " +
                                        "from CLIENT tb_1_ " +
                                        "where tb_1_.ID = ? and tb_1_.CLIENT_TYPE in (?, ?) for update"
                        );
                        it.variables(102L, "ORG", "Person");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from SINGLE_CLIENT_PROJECT tb_1_ " +
                                        "where tb_1_.CLIENT_ID = ? limit ?"
                        );
                        it.variables(102L, 1);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from SINGLE_ORG_PROJECT tb_1_ " +
                                        "where tb_1_.ORGANIZATION_ID = ? limit ?"
                        );
                        it.variables(102L, 1);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from CLIENT where ID = ? and CLIENT_TYPE in (?, ?)"
                        );
                        it.variables(102L, "ORG", "Person");
                    });
                    ctx.value("null; [Person, Bob, null, Bob, Brown]");
                }
        );
    }

    @Test
    public void testDeleteSubtypeWithAssociationTargets() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .deleteCommand(Organization.class, 100L)
                            .setMode(DeleteMode.PHYSICAL)
                            .setDissociateAction(ClientProjectProps.CLIENT, DissociateAction.SET_NULL)
                            .setDissociateAction(OrganizationProjectProps.ORGANIZATION, DissociateAction.SET_NULL)
                            .execute(con);
                    return singleClientProjectTargetId(con, 1000L) +
                            "; " +
                            singleOrgProjectTargetId(con, 1001L) +
                            "; " +
                            clientRow(con, 100L) +
                            "; " +
                            clientRow(con, 101L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.RESOLVE_ACCEPTED_INHERITANCE_DELETE_TARGETS);
                        it.sql(
                                "select tb_1_.ID " +
                                        "from CLIENT tb_1_ " +
                                        "where tb_1_.ID = ? and tb_1_.CLIENT_TYPE = ? for update"
                        );
                        it.variables(100L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql("update SINGLE_CLIENT_PROJECT set CLIENT_ID = null where CLIENT_ID = ?");
                        it.variables(100L);
                    });
                    ctx.statement(it -> {
                        it.sql("update SINGLE_ORG_PROJECT set ORGANIZATION_ID = null where ORGANIZATION_ID = ?");
                        it.variables(100L);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from CLIENT where ID = ? and CLIENT_TYPE = ?");
                        it.variables(100L, "ORG");
                    });
                    ctx.value("null; null; null; [Person, Bob, null, Bob, Brown]");
                }
        );
    }

    private static String clientRow(Connection con, long id) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select CLIENT_TYPE, NAME, TAX_CODE, FIRST_NAME, LAST_NAME from CLIENT where ID = ?"
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

    private static String naturalClientRows(Connection con) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select ID, CLIENT_TYPE, CODE, NAME, TAX_CODE, FIRST_NAME, LAST_NAME " +
                        "from NATURAL_CLIENT where CODE = 'same-code' order by ID"
        )) {
            try (ResultSet rs = stmt.executeQuery()) {
                StringBuilder builder = new StringBuilder();
                while (rs.next()) {
                    if (builder.length() != 0) {
                        builder.append("; ");
                    }
                    builder
                            .append('[')
                            .append(rs.getLong(1))
                            .append(", ")
                            .append(rs.getString(2))
                            .append(", ")
                            .append(rs.getString(3))
                            .append(", ")
                            .append(rs.getString(4))
                            .append(", ")
                            .append(rs.getString(5))
                            .append(", ")
                            .append(rs.getString(6))
                            .append(", ")
                            .append(rs.getString(7))
                            .append(']');
                }
                return builder.toString();
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String singleClientProjectTargetId(Connection con, long id) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select CLIENT_ID from SINGLE_CLIENT_PROJECT where ID = ?"
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

    private static String singleOrgProjectTargetId(Connection con, long id) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select ORGANIZATION_ID from SINGLE_ORG_PROJECT where ID = ?"
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
}
