package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.inheritance.Permission;
import org.babyfish.jimmer.sql.model.inheritance.PermissionDraft;
import org.babyfish.jimmer.sql.model.inheritance.Role;
import org.babyfish.jimmer.sql.model.inheritance.RoleDraft;
import org.junit.jupiter.api.Test;

public class InheritanceMutationTest extends AbstractMutationTest {

    @Test
    public void testSaveRole() {
        setAutoIds(Role.class, 101L);
        setAutoIds(Permission.class, 101L, 102L);
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
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
                                "insert into ROLE(NAME, ID) values(?, ?)"
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
                                "insert into PERMISSION(NAME, ROLE_ID, ID) values(?, ?, ?)"
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
                                "insert into PERMISSION(NAME, ROLE_ID, ID) values(?, ?, ?)"
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
                                        "--->\"permissions\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"name\":\"permission_1\"," +
                                        "--->--->--->\"role\":{\"id\":101}," +
                                        "--->--->--->\"id\":101" +
                                        "--->--->},{" +
                                        "--->--->--->\"name\":\"permission_2\"," +
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
                getSqlClient().getEntities().saveCommand(
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
                                "insert into ROLE(NAME, ID) values(?, ?)"
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
                                "insert into PERMISSION(NAME, ROLE_ID, ID) values(?, ?, ?)"
                        );
                    });
                    ctx.entity(it -> {
                        it.original(
                                "{\"name\":\"Permission\",\"role\":{\"name\":\"role\"}}"
                        );
                        it.modified(
                                "{\"name\":\"Permission\",\"role\":{\"name\":\"role\",\"id\":101},\"id\":101}"
                        );
                    });
                    ctx.rowCount(AffectedTable.of(Role.class), 1);
                    ctx.rowCount(AffectedTable.of(Permission.class), 1);
                }
        );
    }
}
