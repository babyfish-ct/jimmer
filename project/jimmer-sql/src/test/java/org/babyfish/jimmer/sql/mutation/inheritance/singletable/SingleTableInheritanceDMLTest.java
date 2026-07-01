package org.babyfish.jimmer.sql.mutation.inheritance.singletable;

import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.inheritance.singletable.OrganizationTable;
import org.junit.jupiter.api.Test;

public class SingleTableInheritanceDMLTest extends AbstractMutationTest {

    @Test
    public void testUpdateDerivedTypeCanSetBaseAndDerivedProps() {
        executeAndExpectRowCount(
                getLambdaClient().createUpdate(OrganizationTable.class, (u, organization) -> {
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
