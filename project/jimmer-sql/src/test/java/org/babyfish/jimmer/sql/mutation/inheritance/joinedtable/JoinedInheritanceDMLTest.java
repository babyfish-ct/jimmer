package org.babyfish.jimmer.sql.mutation.inheritance.joinedtable;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.TypeMatchMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.dialect.MySqlDialect;
import org.babyfish.jimmer.sql.dialect.UpdateJoin;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.ClientTable;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.Organization;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.OrganizationTable;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JoinedInheritanceDMLTest extends AbstractMutationTest {

    @Test
    public void testUpdateDerivedTypeCanSetDerivedProp() {
        executeAndExpectRowCount(
                getLambdaClient().createUpdate(OrganizationTable.class, (u, organization) -> {
                    u.setTypeMatchMode(TypeMatchMode.EXACT);
                    u.set(organization.taxCode(), "GLOBEX-002");
                    u.where(organization.id().eq(200L));
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_ORGANIZATION tb_1_ " +
                                        "set TAX_CODE = ? " +
                                        "where tb_1_.ID = ?"
                        );
                        it.variables("GLOBEX-002", 200L);
                    });
                    ctx.rowCount(1);
                }
        );
    }

    @Test
    public void testUpdateDerivedTypeCanSetRootProp() {
        executeAndExpectRowCount(
                sqlOnlyUpdateJoinClient(1)
                        .createUpdate(OrganizationTable.class, (u, organization) -> {
                    u.set(organization.name(), "Globex+");
                    u.where(organization.taxCode().eq("GLOBEX-001"));
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT tb_1_ " +
                                        "set NAME = ? " +
                                        "from JOINED_ORGANIZATION tb_1__sub " +
                                        "where tb_1_.ID = tb_1__sub.ID " +
                                        "and tb_1__sub.TAX_CODE = ? " +
                                        "and tb_1_.CLIENT_TYPE = ?"
                        );
                        it.variables("Globex+", "GLOBEX-001", "ORG");
                    });
                    ctx.rowCount(1);
                }
        );
    }

    @Test
    public void testUpdateDerivedTypeCanSetRootPropByPortableExists() {
        executeAndExpectRowCount(
                h2Client(1)
                        .createUpdate(OrganizationTable.class, (u, organization) -> {
                            u.set(organization.name(), "Globex+");
                            u.where(organization.name().eq("Globex"));
                            u.where(organization.taxCode().eq("GLOBEX-001"));
                        }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                        "update JOINED_CLIENT tb_1_ " +
                                        "set NAME = ? " +
                                        "where tb_1_.NAME = ? " +
                                        "and exists(" +
                                        "--->select 1 from JOINED_ORGANIZATION tb_1__sub " +
                                        "--->where tb_1_.ID = tb_1__sub.ID " +
                                        "--->and tb_1__sub.TAX_CODE = ?" +
                                        ") " +
                                        "and tb_1_.CLIENT_TYPE = ?"
                        );
                        it.variables("Globex+", "Globex", "GLOBEX-001", "ORG");
                    });
                    ctx.rowCount(1);
                }
        );
    }

    @Test
    public void testUpdateDerivedTypeCanUseOrPredicateByPortableExists() {
        executeAndExpectRowCount(
                h2Client(1)
                        .createUpdate(OrganizationTable.class, (u, organization) -> {
                            u.set(organization.name(), "Globex+");
                            u.where(
                                    Predicate.or(
                                            organization.name().eq("Globex"),
                                            organization.taxCode().eq("GLOBEX-001")
                                    )
                            );
                        }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                        "update JOINED_CLIENT tb_1_ " +
                                        "set NAME = ? " +
                                        "where (tb_1_.NAME = ? " +
                                        "or exists(" +
                                        "--->select 1 from JOINED_ORGANIZATION tb_1__sub " +
                                        "--->where tb_1_.ID = tb_1__sub.ID " +
                                        "--->and tb_1__sub.TAX_CODE = ?" +
                                        ")) " +
                                        "and tb_1_.CLIENT_TYPE = ?"
                        );
                        it.variables("Globex+", "Globex", "GLOBEX-001", "ORG");
                    });
                    ctx.rowCount(1);
                }
        );
    }

    @Test
    public void testUpdateDerivedTypeCanSetDerivedPropByPortableExists() {
        executeAndExpectRowCount(
                h2Client(1)
                        .createUpdate(OrganizationTable.class, (u, organization) -> {
                            u.set(organization.taxCode(), "GLOBEX-002");
                            u.where(organization.name().eq("Globex"));
                            u.where(organization.taxCode().eq("GLOBEX-001"));
                        }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                        "update JOINED_ORGANIZATION tb_1_ " +
                                        "set TAX_CODE = ? " +
                                        "where exists(" +
                                        "--->select 1 from JOINED_CLIENT tb_2_ " +
                                        "--->where tb_1_.ID = tb_2_.ID " +
                                        "--->and tb_2_.NAME = ?" +
                                        ") " +
                                        "and tb_1_.TAX_CODE = ?"
                        );
                        it.variables("GLOBEX-002", "Globex", "GLOBEX-001");
                    });
                    ctx.rowCount(1);
                }
        );
    }

    @Test
    public void testUpdateDerivedTypeCannotSetPropsOfTwoPhysicalTables() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> getLambdaClient().createUpdate(OrganizationTable.class, (u, organization) -> {
                    u.set(organization.name(), "Globex+");
                    u.set(organization.taxCode(), "GLOBEX-002");
                    u.where(organization.id().eq(200L));
                })
        );
        assertEquals(
                "Cannot update property \"" +
                        "org.babyfish.jimmer.sql.model.inheritance.joinedtable.Organization.taxCode" +
                        "\" by createUpdate for joined inheritance type \"" +
                        "org.babyfish.jimmer.sql.model.inheritance.joinedtable.Organization" +
                        "\" because all assignment targets must belong to the same physical table. " +
                        "Current assignment targets table \"" +
                        "JOINED_ORGANIZATION" +
                        "\" but previous assignments target table \"" +
                        "JOINED_CLIENT" +
                        "\". Updating columns in multiple database tables by one createUpdate " +
                        "for joined inheritance requires a dialect that supports multi-table update assignment",
                ex.getMessage()
        );
    }

    @Test
    public void testUpdateDerivedTypeCanSetPropsOfTwoPhysicalTablesByMySql() {
        executeAndExpectRowCount(
                mysqlStyleUpdateJoinClient(1)
                        .createUpdate(OrganizationTable.class, (u, organization) -> {
                    u.set(organization.name(), "Globex+");
                    u.set(organization.taxCode(), "GLOBEX-002");
                    u.where(organization.taxCode().eq("GLOBEX-001"));
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT tb_1_ " +
                                        "inner join JOINED_ORGANIZATION tb_1__sub " +
                                        "on tb_1_.ID = tb_1__sub.ID " +
                                        "set tb_1_.NAME = ?, tb_1__sub.TAX_CODE = ? " +
                                        "where tb_1__sub.TAX_CODE = ? " +
                                        "and tb_1_.CLIENT_TYPE = ?"
                        );
                        it.variables("Globex+", "GLOBEX-002", "GLOBEX-001", "ORG");
                    });
                    ctx.rowCount(1);
                }
        );
    }

    @Test
    public void testUpdateRootTypeCanSetRootProp() {
        executeAndExpectRowCount(
                getLambdaClient().createUpdate(ClientTable.class, (u, client) -> {
                    u.set(client.name(), "Globex+");
                    u.where(client.id().eq(200L));
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT tb_1_ " +
                                        "set NAME = ? " +
                                        "where tb_1_.ID = ?"
                        );
                        it.variables("Globex+", 200L);
                    });
                    ctx.rowCount(1);
                }
        );
    }

    @Test
    public void testUpdateRootTypePolymorphically() {
        executeAndExpectRowCount(
                getLambdaClient().createUpdate(ClientTable.class, (u, client) -> {
                    u.setTypeMatchMode(TypeMatchMode.POLYMORPHIC);
                    u.set(client.name(), "Client+");
                    u.where(client.id().in(Arrays.asList(200L, 201L)));
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT tb_1_ " +
                                        "set NAME = ? " +
                                        "where tb_1_.ID in (?, ?)"
                        );
                        it.variables("Client+", 200L, 201L);
                    });
                    ctx.rowCount(2);
                }
        );
    }

    @Test
    public void testUpdateAbstractRootCannotBeExact() {
        executeAndExpectRowCount(
                getLambdaClient().createUpdate(ClientTable.class, (u, client) -> {
                    u.setTypeMatchMode(TypeMatchMode.EXACT);
                    u.set(client.name(), "Client+");
                }),
                ctx -> ctx.throwable(it -> {
                    it.type(ExecutionException.class);
                    it.message(
                            "Cannot update inheritance entity type \"" +
                                    "org.babyfish.jimmer.sql.model.inheritance.joinedtable.Client" +
                                    "\" exactly because it is abstract. Update an instantiable type or use " +
                                    TypeMatchMode.POLYMORPHIC +
                                    " type match mode."
                    );
                })
        );
    }

    @Test
    public void testUpdateRootTypeCannotSetDiscriminatorProp() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> getLambdaClient().createUpdate(ClientTable.class, (u, client) -> {
                    u.set(client.type(), "Person");
                    u.where(client.id().eq(200L));
                })
        );
        assertEquals(
                "The discriminator property \"" +
                        "org.babyfish.jimmer.sql.model.inheritance.joinedtable.Client.type" +
                        "\" cannot be updated by createUpdate for joined inheritance type \"" +
                        "org.babyfish.jimmer.sql.model.inheritance.joinedtable.Client" +
                        "\"",
                ex.getMessage()
        );
    }

    @Test
    public void testUpdateDerivedTypeCannotSetDiscriminatorProp() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> getLambdaClient().createUpdate(OrganizationTable.class, (u, organization) -> {
                    u.set(organization.type(), "Person");
                    u.where(organization.id().eq(200L));
                })
        );
        assertEquals(
                "The discriminator property \"" +
                        "org.babyfish.jimmer.sql.model.inheritance.joinedtable.Organization.type" +
                        "\" cannot be updated by createUpdate for joined inheritance type \"" +
                        "org.babyfish.jimmer.sql.model.inheritance.joinedtable.Organization" +
                        "\"",
                ex.getMessage()
        );
    }

    @Test
    public void testUpdateDerivedTypeCanUseRootPropInWhere() {
        executeAndExpectRowCount(
                sqlOnlyUpdateJoinClient(1)
                        .createUpdate(OrganizationTable.class, (u, organization) -> {
                    u.set(organization.taxCode(), "GLOBEX-002");
                    u.where(
                            organization.name().eq("Globex"),
                            organization.taxCode().eq("GLOBEX-001")
                    );
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_ORGANIZATION tb_1_ " +
                                        "set TAX_CODE = ? " +
                                        "from JOINED_CLIENT tb_2_ " +
                                        "where tb_1_.ID = tb_2_.ID " +
                                        "and tb_2_.NAME = ? " +
                                        "and tb_1_.TAX_CODE = ?"
                        );
                        it.variables("GLOBEX-002", "Globex", "GLOBEX-001");
                    });
                    ctx.rowCount(1);
                }
        );
    }

    @Test
    public void testUpdateDerivedTypeCanUseRootPropExpressionInWhere() {
        executeAndExpectRowCount(
                sqlOnlyUpdateJoinClient(1)
                        .createUpdate(OrganizationTable.class, (u, organization) -> {
                    u.set(organization.taxCode(), "GLOBEX-002");
                    u.where(organization.name().isNotNull());
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_ORGANIZATION tb_1_ " +
                                        "set TAX_CODE = ? " +
                                        "from JOINED_CLIENT tb_2_ " +
                                        "where tb_1_.ID = tb_2_.ID " +
                                        "and tb_2_.NAME is not null"
                        );
                        it.variables("GLOBEX-002");
                    });
                    ctx.rowCount(1);
                }
        );
    }

    @Test
    public void testUpdateDerivedTypeCanUseDerivedPropInWhere() {
        executeAndExpectRowCount(
                getLambdaClient().createUpdate(OrganizationTable.class, (u, organization) -> {
                    u.set(organization.taxCode(), "GLOBEX-002");
                    u.where(organization.taxCode().eq("GLOBEX-001"));
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_ORGANIZATION tb_1_ " +
                                        "set TAX_CODE = ? " +
                                        "where tb_1_.TAX_CODE = ?"
                        );
                        it.variables("GLOBEX-002", "GLOBEX-001");
                    });
                    ctx.rowCount(1);
                }
        );
    }

    @Test
    public void testUpdateDerivedTypeCanUseDiscriminatorInWhere() {
        executeAndExpectRowCount(
                sqlOnlyUpdateJoinClient(2)
                        .createUpdate(OrganizationTable.class, (u, organization) -> {
                    u.set(organization.taxCode(), "ORG-UPDATED");
                    u.where(organization.type().eq("ORG"));
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_ORGANIZATION tb_1_ " +
                                        "set TAX_CODE = ? " +
                                        "from JOINED_CLIENT tb_2_ " +
                                        "where tb_1_.ID = tb_2_.ID " +
                                        "and tb_2_.CLIENT_TYPE = ?"
                        );
                        it.variables("ORG-UPDATED", "ORG");
                    });
                    ctx.rowCount(2);
                }
        );
    }

    @Test
    public void testUpdateRootTypeCanUseInstanceOfInWhere() {
        executeAndExpectRowCount(
                getLambdaClient().createUpdate(ClientTable.class, (u, client) -> {
                    u.set(client.name(), "Organization+");
                    u.where(client.instanceOf(Organization.class));
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT tb_1_ " +
                                        "set NAME = ? " +
                                        "where tb_1_.CLIENT_TYPE = ?"
                        );
                        it.variables("Organization+", "ORG");
                    });
                    ctx.rowCount(2);
                }
        );
    }

    private LambdaClient sqlOnlyUpdateJoinClient(int rowCount) {
        return updateJoinClient(rowCount, new H2UpdateJoinDialect());
    }

    private LambdaClient h2Client(int rowCount) {
        return updateJoinClient(rowCount, new H2Dialect());
    }

    private LambdaClient mysqlStyleUpdateJoinClient(int rowCount) {
        return updateJoinClient(rowCount, new MySqlDialect());
    }

    private LambdaClient updateJoinClient(int rowCount, Dialect dialect) {
        return getLambdaClient(it -> {
            it.setDialect(dialect);
            it.setExecutor(new Executor() {
                @Override
                @SuppressWarnings("unchecked")
                public <R> R execute(Args<R> args) {
                    getExecutions().add(Execution.simple(args.sql, args.purpose, args.variables));
                    return (R) Integer.valueOf(rowCount);
                }

                @Override
                public BatchContext executeBatch(
                        Connection con,
                        String sql,
                        ImmutableProp generatedIdProp,
                        ExecutionPurpose purpose,
                        JSqlClientImplementor sqlClient,
                        boolean constraintViolationTranslatable
                ) {
                    throw new AssertionError("Batch execution is not expected");
                }
            });
        });
    }

    private static class H2UpdateJoinDialect extends H2Dialect {

        @Override
        public UpdateJoin getUpdateJoin() {
            return new UpdateJoin(false, UpdateJoin.From.AS_JOIN);
        }
    }
}
