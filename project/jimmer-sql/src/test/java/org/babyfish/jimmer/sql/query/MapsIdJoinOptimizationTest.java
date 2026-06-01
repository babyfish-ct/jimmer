package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.fetcher.ReferenceFetchType;
import org.babyfish.jimmer.sql.model.mapsid.TenantDocumentFetcher;
import org.babyfish.jimmer.sql.model.mapsid.TenantDocumentTable;
import org.babyfish.jimmer.sql.model.mapsid.TenantFetcher;
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

    @Test
    public void testMappedIdAssociationIdUsagesDoNotJoinTargetTable() {
        executeAndExpect(
                getLambdaClient().createQuery(TenantDocumentTable.class, (q, document) -> {
                    q.where(document.asTableEx().tenant().id().eq(1L));
                    q.groupBy(document.asTableEx().tenant().id());
                    q.orderBy(document.asTableEx().tenant().id().asc());
                    return q.select(document.asTableEx().tenant().id());
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.TENANT_ID " +
                                    "from TENANT_DOCUMENT tb_1_ " +
                                    "where tb_1_.TENANT_ID = ? " +
                                    "group by tb_1_.TENANT_ID " +
                                    "order by tb_1_.TENANT_ID asc"
                    );
                    ctx.variables(1L);
                }
        );
    }

    @Test
    public void testMappedIdAssociationIdOnlyJoinFetcherDoesNotJoinTargetTable() {
        executeAndExpect(
                getSqlClient()
                        .createQuery(TenantDocumentTable.$)
                        .select(
                                TenantDocumentTable.$.fetch(
                                        TenantDocumentFetcher.$
                                                .name()
                                                .tenant(ReferenceFetchType.JOIN_ALWAYS, TenantFetcher.$)
                                )
                        ),
                ctx -> ctx.sql(
                        "select tb_1_.TENANT_ID, tb_1_.DOCUMENT_ID, tb_1_.NAME, tb_1_.TENANT_ID " +
                                "from TENANT_DOCUMENT tb_1_"
                )
        );
    }
}
