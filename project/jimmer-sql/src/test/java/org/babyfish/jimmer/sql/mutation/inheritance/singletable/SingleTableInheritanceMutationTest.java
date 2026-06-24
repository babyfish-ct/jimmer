package org.babyfish.jimmer.sql.mutation.inheritance.singletable;

import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.inheritance.singletable.Organization;
import org.babyfish.jimmer.sql.model.inheritance.singletable.OrganizationDraft;
import org.junit.jupiter.api.Test;

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
                                "merge into CLIENT(ID, CLIENT_TYPE, NAME, TAX_CODE) " +
                                        "key(ID) values(?, ?, ?, ?)"
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
}
