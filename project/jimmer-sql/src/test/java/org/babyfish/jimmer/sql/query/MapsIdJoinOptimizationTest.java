package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.mapsid.TenantDocumentTable;
import org.junit.jupiter.api.Test;

public class MapsIdJoinOptimizationTest extends AbstractQueryTest {

    @Test
    public void testMappedIdIntermediateJoin() {
        executeAndExpect(
                getLambdaClient().createQuery(TenantDocumentTable.class, (q, document) -> {
                    q.where(
                            document
                                    .asTableEx()
                                    .tenant()
                                    .documents()
                                    .name()
                                    .eq("Spec")
                    );
                    return q.select(document.id());
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.TENANT_ID, tb_1_.DOCUMENT_ID " +
                                    "from TENANT_DOCUMENT tb_1_ " +
                                    "inner join TENANT_DOCUMENT tb_2_ on tb_1_.TENANT_ID = tb_2_.TENANT_ID " +
                                    "where tb_2_.NAME = ?"
                    );
                    ctx.variables("Spec");
                }
        );
    }

    @Test
    public void testMappedIdIntermediateJoinCannotBeOptimizedWhenBridgeFieldIsUsed() {
        executeAndExpect(
                getLambdaClient().createQuery(TenantDocumentTable.class, (q, document) -> {
                    q.where(
                            document
                                    .asTableEx()
                                    .tenant()
                                    .name()
                                    .eq("Tenant")
                    );
                    q.where(
                            document
                                    .asTableEx()
                                    .tenant()
                                    .documents()
                                    .name()
                                    .eq("Spec")
                    );
                    return q.select(document.id());
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.TENANT_ID, tb_1_.DOCUMENT_ID " +
                                    "from TENANT_DOCUMENT tb_1_ " +
                                    "inner join TENANT tb_2_ on tb_1_.TENANT_ID = tb_2_.ID " +
                                    "inner join TENANT_DOCUMENT tb_3_ on tb_2_.ID = tb_3_.TENANT_ID " +
                                    "where tb_2_.NAME = ? and tb_3_.NAME = ?"
                    );
                    ctx.variables("Tenant", "Spec");
                }
        );
    }
}
