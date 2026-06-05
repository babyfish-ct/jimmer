package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.exception.SaveException;
import org.babyfish.jimmer.sql.model.mapsid.DualParentChild;
import org.babyfish.jimmer.sql.model.mapsid.DualParentChildDraft;
import org.babyfish.jimmer.sql.model.mapsid.MappedTenantDocument;
import org.babyfish.jimmer.sql.model.mapsid.MappedTenantDocumentDraft;
import org.babyfish.jimmer.sql.model.mapsid.MapsIdMessage;
import org.babyfish.jimmer.sql.model.mapsid.MapsIdMessageDelivery;
import org.babyfish.jimmer.sql.model.mapsid.MapsIdMessageDeliveryDraft;
import org.babyfish.jimmer.sql.model.mapsid.MapsIdPrincipal;
import org.babyfish.jimmer.sql.model.mapsid.MapsIdProfile;
import org.babyfish.jimmer.sql.model.mapsid.MapsIdProfileDraft;
import org.babyfish.jimmer.sql.model.mapsid.Tenant;
import org.babyfish.jimmer.sql.model.mapsid.TenantDraft;
import org.babyfish.jimmer.sql.model.mapsid.TenantDocument;
import org.babyfish.jimmer.sql.model.mapsid.TenantDocumentDraft;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class MapsIdMutationTest extends AbstractMutationTest {

    @Test
    public void testSaveFullMappedId() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                MapsIdProfileDraft.$.produce(profile -> {
                                    profile.setNickname("Alex");
                                    profile.applyPrincipal(principal -> {
                                        principal.applyId(id -> id.setA(1L).setB(2L));
                                        principal.setName("Root");
                                    });
                                })
                        )
                        .setMode(SaveMode.INSERT_ONLY)
                        .setAssociatedModeAll(AssociatedSaveMode.APPEND),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into MAPS_ID_PRINCIPAL(A, B, NAME) values(?, ?, ?)");
                        it.variables(1L, 2L, "Root");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into MAPS_ID_PROFILE(A, B, NICKNAME) values(?, ?, ?)");
                        it.variables(1L, 2L, "Alex");
                    });
                    ctx.totalRowCount(2);
                    ctx.rowCount(AffectedTable.of(MapsIdPrincipal.class), 1);
                    ctx.rowCount(AffectedTable.of(MapsIdProfile.class), 1);
                    ctx.entity(it -> it.modified(
                            "{" +
                                    "--->\"id\":{\"a\":1,\"b\":2}," +
                                    "--->\"nickname\":\"Alex\"," +
                                    "--->\"principal\":{\"id\":{\"a\":1,\"b\":2},\"name\":\"Root\"}" +
                                    "}"
                    ));
                }
        );
    }

    @Test
    public void testSaveFullScalarMappedIdWithAssociationIdName() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                MapsIdMessageDeliveryDraft.$.produce(delivery -> {
                                    delivery.setMessageId(101L);
                                    delivery.setStatus("READ");
                                    delivery.applyMessage(message -> {
                                        message.setId(101L);
                                        message.setText("Hi");
                                    });
                                })
                        )
                        .setMode(SaveMode.INSERT_ONLY)
                        .setAssociatedModeAll(AssociatedSaveMode.APPEND),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into MAPS_ID_MESSAGE(ID, TEXT) values(?, ?)");
                        it.variables(101L, "Hi");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into MAPS_ID_MESSAGE_DELIVERY(MESSAGE_ID, STATUS) values(?, ?)");
                        it.variables(101L, "READ");
                    });
                    ctx.totalRowCount(2);
                    ctx.rowCount(AffectedTable.of(MapsIdMessage.class), 1);
                    ctx.rowCount(AffectedTable.of(MapsIdMessageDelivery.class), 1);
                    ctx.entity(it -> it.modified(
                            "{" +
                                    "--->\"messageId\":101," +
                                    "--->\"status\":\"READ\"," +
                                    "--->\"message\":{\"id\":101,\"text\":\"Hi\"}" +
                                    "}"
                    ));
                }
        );
    }

    @Test
    public void testSavePartialMappedId() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                TenantDocumentDraft.$.produce(document -> {
                                    document.applyId(id -> id.setDocumentId(10L));
                                    document.setName("Spec");
                                    document.applyTenant(tenant -> {
                                        tenant.setId(3L);
                                        tenant.setName("Tenant");
                                    });
                                })
                        )
                        .setMode(SaveMode.INSERT_ONLY)
                        .setAssociatedModeAll(AssociatedSaveMode.APPEND),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into TENANT(ID, NAME) values(?, ?)");
                        it.variables(3L, "Tenant");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into TENANT_DOCUMENT(TENANT_ID, DOCUMENT_ID, NAME) values(?, ?, ?)");
                        it.variables(3L, 10L, "Spec");
                    });
                    ctx.totalRowCount(2);
                    ctx.rowCount(AffectedTable.of(Tenant.class), 1);
                    ctx.rowCount(AffectedTable.of(TenantDocument.class), 1);
                    ctx.entity(it -> it.modified(
                            "{" +
                                    "--->\"id\":{\"tenantId\":3,\"documentId\":10}," +
                                    "--->\"name\":\"Spec\"," +
                                    "--->\"tenant\":{\"id\":3,\"name\":\"Tenant\"}" +
                                    "}"
                    ));
                }
        );
    }

    @Test
    public void testSaveMappedIdDeclaredByMappedSuperclass() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                MappedTenantDocumentDraft.$.produce(document -> {
                                    document.applyId(id -> id.setDocumentId(10L));
                                    document.setName("Spec");
                                    document.applyTenant(tenant -> {
                                        tenant.setId(3L);
                                        tenant.setName("Tenant");
                                    });
                                })
                        )
                        .setMode(SaveMode.INSERT_ONLY)
                        .setAssociatedModeAll(AssociatedSaveMode.APPEND),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into TENANT(ID, NAME) values(?, ?)");
                        it.variables(3L, "Tenant");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into MAPPED_TENANT_DOCUMENT(TENANT_ID, DOCUMENT_ID, NAME) values(?, ?, ?)");
                        it.variables(3L, 10L, "Spec");
                    });
                    ctx.totalRowCount(2);
                    ctx.rowCount(AffectedTable.of(Tenant.class), 1);
                    ctx.rowCount(AffectedTable.of(MappedTenantDocument.class), 1);
                    ctx.entity(it -> it.modified(
                            "{" +
                                    "--->\"id\":{\"tenantId\":3,\"documentId\":10}," +
                                    "--->\"tenant\":{\"id\":3,\"name\":\"Tenant\"}," +
                                    "--->\"name\":\"Spec\"" +
                                    "}"
                    ));
                }
        );
    }

    @Test
    public void testSavePartialMappedIdFromInverseSide() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                TenantDraft.$.produce(tenant -> {
                                    tenant.setId(4L);
                                    tenant.setName("Tenant");
                                    tenant.addIntoDocuments(document -> {
                                        document.applyId(id -> id.setDocumentId(11L));
                                        document.setName("Spec");
                                    });
                                })
                        )
                        .setMode(SaveMode.INSERT_ONLY)
                        .setAssociatedModeAll(AssociatedSaveMode.APPEND),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into TENANT(ID, NAME) values(?, ?)");
                        it.variables(4L, "Tenant");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into TENANT_DOCUMENT(TENANT_ID, DOCUMENT_ID, NAME) values(?, ?, ?)");
                        it.variables(4L, 11L, "Spec");
                    });
                    ctx.totalRowCount(2);
                    ctx.rowCount(AffectedTable.of(Tenant.class), 1);
                    ctx.rowCount(AffectedTable.of(TenantDocument.class), 1);
                    ctx.entity(it -> it.modified(
                            "{" +
                                    "--->\"id\":4," +
                                    "--->\"name\":\"Tenant\"," +
                                    "--->\"documents\":[" +
                                    "--->--->{" +
                                    "--->--->--->\"id\":{\"tenantId\":4,\"documentId\":11}," +
                                    "--->--->--->\"name\":\"Spec\"," +
                                    "--->--->--->\"tenant\":{\"id\":4}" +
                                    "--->--->}" +
                                    "--->]" +
                                    "}"
                    ));
                }
        );
    }

    @Test
    public void testBatchSaveFullMappedId() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveEntitiesCommand(
                                Arrays.asList(
                                        MapsIdProfileDraft.$.produce(profile -> {
                                            profile.setNickname("Alex");
                                            profile.applyPrincipal(principal -> {
                                                principal.applyId(id -> id.setA(1L).setB(2L));
                                                principal.setName("Root");
                                            });
                                        }),
                                        MapsIdProfileDraft.$.produce(profile -> {
                                            profile.setNickname("Bob");
                                            profile.applyPrincipal(principal -> {
                                                principal.applyId(id -> id.setA(3L).setB(4L));
                                                principal.setName("Branch");
                                            });
                                        })
                                )
                        )
                        .setMode(SaveMode.INSERT_ONLY)
                        .setAssociatedModeAll(AssociatedSaveMode.APPEND),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into MAPS_ID_PRINCIPAL(A, B, NAME) values(?, ?, ?)");
                        it.batchVariables(0, 1L, 2L, "Root");
                        it.batchVariables(1, 3L, 4L, "Branch");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into MAPS_ID_PROFILE(A, B, NICKNAME) values(?, ?, ?)");
                        it.batchVariables(0, 1L, 2L, "Alex");
                        it.batchVariables(1, 3L, 4L, "Bob");
                    });
                    ctx.totalRowCount(4);
                    ctx.rowCount(AffectedTable.of(MapsIdPrincipal.class), 2);
                    ctx.rowCount(AffectedTable.of(MapsIdProfile.class), 2);
                    ctx.entity(it -> it.modified(
                            "{" +
                                    "--->\"id\":{\"a\":1,\"b\":2}," +
                                    "--->\"nickname\":\"Alex\"," +
                                    "--->\"principal\":{\"id\":{\"a\":1,\"b\":2},\"name\":\"Root\"}" +
                                    "}"
                    ));
                    ctx.entity(it -> it.modified(
                            "{" +
                                    "--->\"id\":{\"a\":3,\"b\":4}," +
                                    "--->\"nickname\":\"Bob\"," +
                                    "--->\"principal\":{\"id\":{\"a\":3,\"b\":4},\"name\":\"Branch\"}" +
                                    "}"
                    ));
                }
        );
    }

    @Test
    public void testConflictingMappedId() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                MapsIdProfileDraft.$.produce(profile -> {
                                    profile.applyId(id -> id.setA(9L).setB(9L));
                                    profile.setNickname("Alex");
                                    profile.applyPrincipal(principal -> {
                                        principal.applyId(id -> id.setA(1L).setB(2L));
                                        principal.setName("Root");
                                    });
                                })
                        )
                        .setMode(SaveMode.INSERT_ONLY)
                        .setAssociatedModeAll(AssociatedSaveMode.APPEND),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into MAPS_ID_PRINCIPAL(A, B, NAME) values(?, ?, ?)");
                        it.variables(1L, 2L, "Root");
                    });
                    ctx.throwable(it -> it.type(SaveException.InconsistentMappedId.class));
                }
        );
    }

    @Test
    public void testSaveMultiplePartialMappedIds() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                DualParentChildDraft.$.produce(child -> {
                                    child.applyId(id -> id.setLocalId(30L));
                                    child.setName("Child");
                                    child.applyLeft(left -> {
                                        left.setId(10L);
                                        left.setName("Left");
                                    });
                                    child.applyRight(right -> {
                                        right.setId(20L);
                                        right.setName("Right");
                                    });
                                })
                        )
                        .setMode(SaveMode.INSERT_ONLY)
                        .setAssociatedModeAll(AssociatedSaveMode.APPEND),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into TENANT(ID, NAME) values(?, ?)");
                        it.variables(10L, "Left");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into TENANT(ID, NAME) values(?, ?)");
                        it.variables(20L, "Right");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into DUAL_PARENT_CHILD(LEFT_ID, RIGHT_ID, LOCAL_ID, NAME) values(?, ?, ?, ?)");
                        it.variables(10L, 20L, 30L, "Child");
                    });
                    ctx.totalRowCount(3);
                    ctx.rowCount(AffectedTable.of(Tenant.class), 2);
                    ctx.rowCount(AffectedTable.of(DualParentChild.class), 1);
                    ctx.entity(it -> it.modified(
                            "{" +
                                    "--->\"id\":{\"leftId\":10,\"rightId\":20,\"localId\":30}," +
                                    "--->\"name\":\"Child\"," +
                                    "--->\"left\":{\"id\":10,\"name\":\"Left\"}," +
                                    "--->\"right\":{\"id\":20,\"name\":\"Right\"}" +
                                    "}"
                    ));
                }
        );
    }

    @Test
    public void testMappedIdBackReferenceDoesNotRequireTransferCheckSelect() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                TenantDraft.$.produce(tenant -> {
                                    tenant.setId(10L);
                                    tenant.setName("Tenant");
                                    tenant.addIntoDocuments(document -> {
                                        document.applyId(id -> id.setDocumentId(30L));
                                        document.setName("Spec");
                                    });
                                })
                        ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("merge into TENANT(ID, NAME) key(ID) values(?, ?)");
                        it.variables(10L, "Tenant");
                    });
                    ctx.statement(it -> {
                        it.sql("merge into TENANT_DOCUMENT(TENANT_ID, DOCUMENT_ID, NAME) key(TENANT_ID, DOCUMENT_ID) values(?, ?, ?)");
                        it.variables(10L, 30L, "Spec");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.TENANT_ID, tb_1_.DOCUMENT_ID " +
                                        "from TENANT_DOCUMENT tb_1_ " +
                                        "where tb_1_.TENANT_ID = ? and " +
                                        "(tb_1_.TENANT_ID, tb_1_.DOCUMENT_ID) <> (?, ?) limit ?"
                        );
                        it.variables(10L, 10L, 30L, 1);
                    });
                    ctx.totalRowCount(2);
                    ctx.rowCount(AffectedTable.of(Tenant.class), 1);
                    ctx.rowCount(AffectedTable.of(TenantDocument.class), 1);
                    ctx.entity(it -> it.modified(
                            "{" +
                                    "--->\"id\":10," +
                                    "--->\"name\":\"Tenant\"," +
                                    "--->\"documents\":[" +
                                    "--->--->{" +
                                    "--->--->--->\"id\":{\"tenantId\":10,\"documentId\":30}," +
                                    "--->--->--->\"name\":\"Spec\"," +
                                    "--->--->--->\"tenant\":{\"id\":10}" +
                                    "--->--->}" +
                                    "--->]" +
                                    "}"
                    ));
                }
        );
    }
}
