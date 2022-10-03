package org.babyfish.jimmer.sql.mutation;

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

public class InheritanceMutationTest extends AbstractMutationTest {

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
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from ROLE as tb_1_ " +
                                        "where tb_1_.NAME = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into ROLE(NAME, CREATED_TIME, MODIFIED_TIME, ID) values(?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from PERMISSION as tb_1_ " +
                                        "where tb_1_.NAME = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into PERMISSION(NAME, CREATED_TIME, MODIFIED_TIME, ROLE_ID, ID) " +
                                        "values(?, ?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from PERMISSION as tb_1_ " +
                                        "where tb_1_.NAME = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into PERMISSION(NAME, CREATED_TIME, MODIFIED_TIME, ROLE_ID, ID) " +
                                        "values(?, ?, ?, ?, ?)"
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
                                        "--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                        "--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                        "--->\"permissions\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"name\":\"permission_1\"," +
                                        "--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                        "--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                        "--->--->--->\"role\":{\"id\":101}," +
                                        "--->--->--->\"id\":101" +
                                        "--->--->},{" +
                                        "--->--->--->\"name\":\"permission_2\"," +
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
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from ROLE as tb_1_ where tb_1_.NAME = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into ROLE(NAME, CREATED_TIME, MODIFIED_TIME, ID) " +
                                        "values(?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from PERMISSION as tb_1_ " +
                                        "where tb_1_.NAME = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into PERMISSION(NAME, CREATED_TIME, MODIFIED_TIME, ROLE_ID, ID) " +
                                        "values(?, ?, ?, ?, ?)"
                        );
                    });
                    ctx.entity(it -> {
                        it.original(
                                "{\"name\":\"Permission\",\"role\":{\"name\":\"role\"}}"
                        );
                        it.modified(
                                "{" +
                                        "--->\"name\":\"Permission\"," +
                                        "--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                        "--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                        "--->\"role\":{" +
                                        "--->--->\"name\":\"role\"," +
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
    }
}
