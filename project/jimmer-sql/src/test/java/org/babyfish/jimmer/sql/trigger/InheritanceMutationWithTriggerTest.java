package org.babyfish.jimmer.sql.trigger;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.babyfish.jimmer.sql.model.inheritance.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class InheritanceMutationWithTriggerTest extends AbstractTriggerTest {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final LocalDateTime CREATED_TIME =
            LocalDateTime.parse("2022-10-03 00:00:00", FORMATTER);

    private static final LocalDateTime MODIFIED_TIME =
            LocalDateTime.parse("2022-10-03 00:10:00", FORMATTER);

    private final static DraftInterceptor<NamedEntityDraft> INTERCEPTOR =
            new DraftInterceptor<NamedEntityDraft>() {
                @Override
                public void beforeSave(@NotNull NamedEntityDraft draft, boolean isNew) {
                    if (!ImmutableObjects.isLoaded(draft, NamedEntityProps.MODIFIED_TIME)) {
                        draft.setModifiedTime(MODIFIED_TIME);
                    }
                    if (!ImmutableObjects.isLoaded(draft, NamedEntityProps.DELETED)) {
                        draft.setDeleted(false);
                    }
                    if (isNew && !ImmutableObjects.isLoaded(draft, NamedEntityProps.CREATED_TIME)) {
                        draft.setCreatedTime(CREATED_TIME);
                    }
                }
            };

    private JSqlClient sqlClient;

    @BeforeEach
    public void initialize() {
        sqlClient = getSqlClient(it -> {
            UserIdGenerator idGenerator = this::autoId;
            it.setIdGenerator(idGenerator);
            it.addDraftInterceptor(INTERCEPTOR);
        });
    }

    @Test
    public void testSaveRole() {
        setAutoIds(Role.class, 101L);
        setAutoIds(Permission.class, 101L, 102L);
        executeAndExpectResult(
                sqlClient.getEntities().saveCommand(
                        RoleDraft.$.produce(role -> {
                            role.setName("role");
                            role.addIntoPermissions(permission -> {
                                permission.setName("permission_1");
                            });
                            role.addIntoPermissions(permission -> {
                                permission.setName("permission_2");
                            });
                        })
                ).configure(it -> it.setAutoAttachingAll()),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                        "from ROLE as tb_1_ " +
                                        "where tb_1_.NAME = ? " +
                                        "for update"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into ROLE(NAME, DELETED, CREATED_TIME, MODIFIED_TIME, ID) " +
                                        "values(?, ?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, " +
                                        "tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.ROLE_ID " +
                                        "from PERMISSION as tb_1_ " +
                                        "where tb_1_.NAME = ? " +
                                        "for update"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into PERMISSION(NAME, DELETED, CREATED_TIME, MODIFIED_TIME, ROLE_ID, ID) " +
                                        "values(?, ?, ?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, " +
                                        "tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.ROLE_ID " +
                                        "from PERMISSION as tb_1_ " +
                                        "where tb_1_.NAME = ? " +
                                        "for update"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into PERMISSION(NAME, DELETED, CREATED_TIME, MODIFIED_TIME, ROLE_ID, ID) " +
                                        "values(?, ?, ?, ?, ?, ?)"
                        );
                    });
                    ctx.entity(it -> {
                        it.original(
                                "{" +
                                        "--->\"name\":\"role\"," +
                                        "--->\"permissions\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"name\":\"permission_1\"" +
                                        "--->--->},{" +
                                        "--->--->--->\"name\":\"permission_2\"" +
                                        "--->}--->" +
                                        "--->]" +
                                        "}"
                        );
                        it.modified(
                                "{" +
                                        "--->\"name\":\"role\"," +
                                        "--->\"deleted\":false," +
                                        "--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                        "--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                        "--->\"permissions\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"name\":\"permission_1\"," +
                                        "--->--->--->\"deleted\":false," +
                                        "--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                        "--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                        "--->--->--->\"role\":{\"id\":101}," +
                                        "--->--->--->\"id\":101" +
                                        "--->--->},{" +
                                        "--->--->--->\"name\":\"permission_2\"," +
                                        "--->--->--->\"deleted\":false," +
                                        "--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                        "--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                        "--->--->--->\"role\":{\"id\":101}," +
                                        "--->--->--->\"id\":102" +
                                        "--->--->}" +
                                        "--->]," +
                                        "--->\"id\":101" +
                                        "}"
                        );
                    });
                    ctx.rowCount(AffectedTable.of(Role.class), 1);
                    ctx.rowCount(AffectedTable.of(Permission.class), 2);
                }
        );
        assertEvents(
                "Event{" +
                        "--->oldEntity=null, " +
                        "--->newEntity={" +
                        "--->--->\"name\":\"role\"," +
                        "--->--->\"deleted\":false," +
                        "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                        "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                        "--->--->\"id\":101" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity=null, " +
                        "--->newEntity={" +
                        "--->--->\"name\":\"permission_1\"," +
                        "--->--->\"deleted\":false," +
                        "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                        "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                        "--->--->\"role\":{" +
                        "--->--->--->\"id\":101" +
                        "--->--->}," +
                        "--->--->\"id\":101" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.inheritance.Permission.role, " +
                        "--->sourceId=101, " +
                        "--->detachedTargetId=null, " +
                        "--->attachedTargetId=101, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.inheritance.Role.permissions, " +
                        "--->sourceId=101, " +
                        "--->detachedTargetId=null, " +
                        "--->attachedTargetId=101, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity=null, " +
                        "--->newEntity={" +
                        "--->--->\"name\":\"permission_2\"," +
                        "--->--->\"deleted\":false," +
                        "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                        "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                        "--->--->\"role\":{" +
                        "--->--->--->\"id\":101" +
                        "--->--->}," +
                        "--->--->\"id\":102" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.inheritance.Permission.role, " +
                        "--->sourceId=102, " +
                        "--->detachedTargetId=null, " +
                        "--->attachedTargetId=101, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.inheritance.Role.permissions, " +
                        "--->sourceId=101, " +
                        "--->detachedTargetId=null, " +
                        "--->attachedTargetId=102, " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void testSavePermission() {
        setAutoIds(Role.class, 101L);
        setAutoIds(Permission.class, 101L);
        executeAndExpectResult(
                sqlClient.getEntities().saveCommand(
                        PermissionDraft.$.produce(permission -> {
                            permission.setName("Permission")
                                    .setRole(role -> role.setName("role"));
                        })
                ).configure(it -> it.setAutoAttachingAll()),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                        "from ROLE as tb_1_ where tb_1_.NAME = ? " +
                                        "for update"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into ROLE(NAME, DELETED, CREATED_TIME, MODIFIED_TIME, ID) " +
                                        "values(?, ?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, " +
                                        "tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.ROLE_ID " +
                                        "from PERMISSION as tb_1_ " +
                                        "where tb_1_.NAME = ? " +
                                        "for update"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into PERMISSION(NAME, DELETED, CREATED_TIME, MODIFIED_TIME, ROLE_ID, ID) " +
                                        "values(?, ?, ?, ?, ?, ?)"
                        );
                    });
                    ctx.entity(it -> {
                        it.original(
                                "{\"name\":\"Permission\",\"role\":{\"name\":\"role\"}}"
                        );
                        it.modified(
                                "{" +
                                        "--->\"name\":\"Permission\"," +
                                        "--->\"deleted\":false," +
                                        "--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                        "--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                        "--->\"role\":{" +
                                        "--->--->\"name\":\"role\"," +
                                        "--->--->\"deleted\":false," +
                                        "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                        "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                        "--->--->\"id\":101" +
                                        "}," +
                                        "--->\"id\":101" +
                                        "}"
                        );
                    });
                    ctx.rowCount(AffectedTable.of(Role.class), 1);
                    ctx.rowCount(AffectedTable.of(Permission.class), 1);
                }
        );
        assertEvents(
                "Event{" +
                        "--->oldEntity=null, " +
                        "--->newEntity={" +
                        "--->--->\"name\":\"role\"," +
                        "--->--->\"deleted\":false," +
                        "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                        "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                        "--->--->\"id\":101" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity=null, " +
                        "--->newEntity={" +
                        "--->--->\"name\":\"Permission\"," +
                        "--->--->\"deleted\":false," +
                        "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                        "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                        "--->--->\"role\":{" +
                        "--->--->--->\"id\":101" +
                        "--->--->}," +
                        "--->--->\"id\":101" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.inheritance.Permission.role, " +
                        "--->sourceId=101, " +
                        "--->detachedTargetId=null, " +
                        "--->attachedTargetId=101, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.inheritance.Role.permissions, " +
                        "--->sourceId=101, " +
                        "--->detachedTargetId=null, " +
                        "--->attachedTargetId=101, " +
                        "--->reason=null" +
                        "}"
        );
    }
}
