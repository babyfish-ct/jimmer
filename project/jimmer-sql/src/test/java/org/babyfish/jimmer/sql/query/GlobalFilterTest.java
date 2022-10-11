package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.query.selectable.RootSelectable;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.babyfish.jimmer.sql.model.inheritance.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GlobalFilterTest extends AbstractQueryTest {

    private static final Filter<NamedEntityColumns> UNDELETED_FILTER =
            new Filter<NamedEntityColumns>() {
                @Override
                public void filter(FilterArgs<NamedEntityColumns> args) {
                    args.where(args.getTable().deleted().eq(false));
                }
            };

    private static final Filter<NamedEntityColumns> DELETED_FILTER =
            new Filter<NamedEntityColumns>() {
                @Override
                public void filter(FilterArgs<NamedEntityColumns> args) {
                    args.where(args.getTable().deleted().eq(true));
                }
            };

    private JSqlClient sqlClient;

    private JSqlClient sqlClientForDeletedData;

    @BeforeEach
    public void initialize() {
        this.sqlClient = getSqlClient(it -> {
           it.addFilter(UNDELETED_FILTER);
           it.addDisabledFilter(DELETED_FILTER);
        });
        this.sqlClientForDeletedData = sqlClient
                .filters(it ->
                        it
                                .enable(DELETED_FILTER)
                                .disable(UNDELETED_FILTER)
                );
    }

    @Test
    public void testQueryUndeletedRoleWithIdOnlyPermissions() {
        executeAndExpect(
                sqlClient.createQuery(RoleTable.class, (q, role) -> {
                    return q.select(
                            role.fetch(
                                    RoleFetcher.$
                                            .allScalarFields()
                                            .permissions()
                            )
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                    "from ROLE as tb_1_ " +
                                    "where tb_1_.DELETED = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID " +
                                    "from PERMISSION as tb_1_ " +
                                    "where tb_1_.DELETED = ? and tb_1_.ROLE_ID = ?"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"name\":\"r_1\"," +
                                    "--->--->\"deleted\":false," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"permissions\":[" +
                                    "--->--->--->{\"id\":1}" +
                                    "--->--->]," +
                                    "--->--->\"id\":1" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testQueryUndeletedRoleWithPermissions() {
        executeAndExpect(
                sqlClient.createQuery(RoleTable.class, (q, role) -> {
                    return q.select(
                            role.fetch(
                                    RoleFetcher.$
                                            .allScalarFields()
                                            .permissions(
                                                    PermissionFetcher.$
                                                            .allScalarFields()
                                            )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                    "from ROLE as tb_1_ " +
                                    "where tb_1_.DELETED = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                    "from PERMISSION as tb_1_ " +
                                    "where tb_1_.DELETED = ? and tb_1_.ROLE_ID = ?"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"name\":\"r_1\"," +
                                    "--->--->\"deleted\":false," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"permissions\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"name\":\"p_1\"," +
                                    "--->--->--->--->\"deleted\":false," +
                                    "--->--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->--->--->\"id\":1" +
                                    "--->--->--->}" +
                                    "--->--->]," +
                                    "--->--->\"id\":1" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testUndeletedPermissionAndIdOnlyRole() {
        executeAndExpect(
                sqlClient.createQuery(PermissionTable.class, RootSelectable::select),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.ROLE_ID " +
                                    "from PERMISSION as tb_1_ " +
                                    "where tb_1_.DELETED = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID from ROLE as tb_1_ " +
                                    "where tb_1_.DELETED = ? and " +
                                    "tb_1_.ID in (?, ?)"
                    );
                }
        );
    }

    @Test
    public void testUndeletedPermissionAndRole() {
        executeAndExpect(
                sqlClient.createQuery(PermissionTable.class, (q, permission) -> {
                    return q.select(
                            permission.fetch(
                                    PermissionFetcher.$
                                            .allScalarFields()
                                            .role(
                                                    RoleFetcher.$
                                                            .allScalarFields()
                                            )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.ROLE_ID " +
                                    "from PERMISSION as tb_1_ " +
                                    "where tb_1_.DELETED = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                    "from ROLE as tb_1_ " +
                                    "where tb_1_.DELETED = ? and tb_1_.ID in (?, ?)"
                    );
                }
        );
    }
}
