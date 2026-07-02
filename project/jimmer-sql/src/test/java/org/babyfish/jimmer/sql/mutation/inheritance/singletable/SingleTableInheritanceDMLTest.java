package org.babyfish.jimmer.sql.mutation.inheritance.singletable;

import org.babyfish.jimmer.sql.ast.TypeMatchMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.model.inheritance.singletable.ClientTable;
import org.babyfish.jimmer.sql.model.inheritance.singletable.OrganizationTable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class SingleTableInheritanceDMLTest extends AbstractMutationTest {

    @Test
    public void testUpdateDerivedTypeCanSetBaseAndDerivedProps() {
        executeAndExpectRowCount(
                getLambdaClient().createUpdate(OrganizationTable.class, (u, organization) -> {
                    u.setTypeMatchMode(TypeMatchMode.EXACT);
                    u.set(organization.name(), "Acme+");
                    u.set(organization.taxCode(), "ACME-002");
                    u.where(organization.id().eq(100L));
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update CLIENT tb_1_ " +
                                        "set NAME = ?, TAX_CODE = ? " +
                                        "where tb_1_.ID = ? and tb_1_.CLIENT_TYPE = ?"
                        );
                        it.variables("Acme+", "ACME-002", 100L, "ORG");
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
                    u.where(client.id().in(Arrays.asList(100L, 101L)));
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update CLIENT tb_1_ " +
                                        "set NAME = ? " +
                                        "where tb_1_.ID in (?, ?)"
                        );
                        it.variables("Client+", 100L, 101L);
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
                                    "org.babyfish.jimmer.sql.model.inheritance.singletable.Client" +
                                    "\" exactly because it is abstract. Update an instantiable type or use " +
                                    TypeMatchMode.POLYMORPHIC +
                                    " type match mode."
                    );
                })
        );
    }

    @Test
    public void testUpdateDerivedTypeCanSetDiscriminatorProp() {
        executeAndExpectRowCount(
                getLambdaClient().createUpdate(OrganizationTable.class, (u, organization) -> {
                    u.set(organization.type(), "Person");
                    u.where(organization.id().eq(100L));
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update CLIENT tb_1_ " +
                                        "set CLIENT_TYPE = ? " +
                                        "where tb_1_.ID = ? and tb_1_.CLIENT_TYPE = ?"
                        );
                        it.variables("Person", 100L, "ORG");
                    });
                    ctx.rowCount(1);
                }
        );
    }
}
