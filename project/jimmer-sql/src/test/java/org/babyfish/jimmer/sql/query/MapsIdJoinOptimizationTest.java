package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.fetcher.ReferenceFetchType;
import org.babyfish.jimmer.sql.model.mapsid.DualParentChildFetcher;
import org.babyfish.jimmer.sql.model.mapsid.DualParentChildTable;
import org.babyfish.jimmer.sql.model.mapsid.MappedTenantDocumentTable;
import org.babyfish.jimmer.sql.model.mapsid.TenantDocumentDetailFetcher;
import org.babyfish.jimmer.sql.model.mapsid.TenantDocumentDetailTable;
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
    public void testMappedSuperclassAssociationIdUsageDoesNotJoinTargetTable() {
        executeAndExpect(
                getLambdaClient().createQuery(MappedTenantDocumentTable.class, (q, document) -> {
                    q.where(document.asTableEx().tenant().id().eq(1L));
                    return q.select(document.asTableEx().tenant().id());
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.TENANT_ID " +
                                    "from MAPPED_TENANT_DOCUMENT tb_1_ " +
                                    "where tb_1_.TENANT_ID = ?"
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

    @Test
    public void testMultiplePartialMappedIdAssociationIdUsagesDoNotJoinTargetTables() {
        executeAndExpect(
                getLambdaClient().createQuery(DualParentChildTable.class, (q, child) -> {
                    q.where(child.asTableEx().left().id().eq(10L));
                    q.where(child.asTableEx().right().id().eq(20L));
                    q.orderBy(child.asTableEx().left().id().asc());
                    return q.select(
                            child.asTableEx().left().id(),
                            child.asTableEx().right().id(),
                            child.id().localId()
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.LEFT_ID, tb_1_.RIGHT_ID, tb_1_.LOCAL_ID " +
                                    "from DUAL_PARENT_CHILD tb_1_ " +
                                    "where tb_1_.LEFT_ID = ? and tb_1_.RIGHT_ID = ? " +
                                    "order by tb_1_.LEFT_ID asc"
                    );
                    ctx.variables(10L, 20L);
                }
        );
    }

    @Test
    public void testMultiplePartialMappedIdAssociationIdOnlyJoinFetcherDoesNotJoinTargetTables() {
        executeAndExpect(
                getSqlClient()
                        .createQuery(DualParentChildTable.$)
                        .select(
                                DualParentChildTable.$.fetch(
                                        DualParentChildFetcher.$
                                                .name()
                                                .left(ReferenceFetchType.JOIN_ALWAYS, TenantFetcher.$)
                                                .right(ReferenceFetchType.JOIN_ALWAYS, TenantFetcher.$)
                                )
                        ),
                ctx -> ctx.sql(
                        "select tb_1_.LEFT_ID, tb_1_.RIGHT_ID, tb_1_.LOCAL_ID, " +
                                "tb_1_.NAME, tb_1_.LEFT_ID, tb_1_.RIGHT_ID " +
                                "from DUAL_PARENT_CHILD tb_1_"
                )
        );
    }

    @Test
    public void testMappedIdAssociationIdChainDoesNotJoinIntermediateTargetTables() {
        executeAndExpect(
                getLambdaClient().createQuery(TenantDocumentDetailTable.class, (q, detail) -> {
                    q.where(detail.asTableEx().document().tenant().id().eq(10L));
                    q.orderBy(detail.asTableEx().document().tenant().id().asc());
                    return q.select(
                            detail.asTableEx().document().id(),
                            detail.asTableEx().document().tenant().id()
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.TENANT_ID, tb_1_.DOCUMENT_ID, tb_1_.TENANT_ID " +
                                    "from TENANT_DOCUMENT_DETAIL tb_1_ " +
                                    "where tb_1_.TENANT_ID = ? " +
                                    "order by tb_1_.TENANT_ID asc"
                    );
                    ctx.variables(10L);
                }
        );
    }

    @Test
    public void testMappedIdAssociationIdOnlyJoinFetcherChainDoesNotJoinIntermediateTargetTables() {
        executeAndExpect(
                getSqlClient()
                        .createQuery(TenantDocumentDetailTable.$)
                        .select(
                                TenantDocumentDetailTable.$.fetch(
                                        TenantDocumentDetailFetcher.$
                                                .description()
                                                .document(
                                                        ReferenceFetchType.JOIN_ALWAYS,
                                                        TenantDocumentFetcher.$.tenant(
                                                                ReferenceFetchType.JOIN_ALWAYS,
                                                                TenantFetcher.$
                                                        )
                                                )
                                )
                        ),
                ctx -> ctx.sql(
                        "select tb_1_.TENANT_ID, tb_1_.DOCUMENT_ID, tb_1_.DESCRIPTION, " +
                                "tb_1_.TENANT_ID, tb_1_.DOCUMENT_ID, tb_1_.TENANT_ID " +
                                "from TENANT_DOCUMENT_DETAIL tb_1_"
                )
        );
    }
}
