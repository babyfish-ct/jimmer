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
           it.addFilterableReferenceProps(PermissionProps.ROLE);
        });
        this.sqlClientForDeletedData = sqlClient.filters(it -> {
            it.enable(DELETED_FILTER);
            it.disable(UNDELETED_FILTER);
        });
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
                    ).variables(false);
                    ctx.statement(1).sql(
                            "select tb_1_.ID " +
                                    "from PERMISSION as tb_1_ " +
                                    "where tb_1_.DELETED = ? and tb_1_.ROLE_ID = ?"
                    ).variables(false, 1L);
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
                    ).variables(false);
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                    "from PERMISSION as tb_1_ " +
                                    "where tb_1_.DELETED = ? and tb_1_.ROLE_ID = ?"
                    ).variables(false, 1L);
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
    public void testQueryUndeletedPermissionAndIdOnlyRole() {
        executeAndExpect(
                sqlClient.createQuery(PermissionTable.class, RootSelectable::select),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.ROLE_ID " +
                                    "from PERMISSION as tb_1_ " +
                                    "where tb_1_.DELETED = ?"
                    ).variables(false);
                    ctx.statement(1).sql(
                            "select tb_1_.ID from ROLE as tb_1_ " +
                                    "where tb_1_.DELETED = ? and " +
                                    "tb_1_.ID in (?, ?)"
                    ).variables(false, 1L, 2L);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"name\":\"p_1\"," +
                                    "--->--->\"deleted\":false," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"role\":{\"id\":1},\"" +
                                    "--->--->id\":1" +
                                    "--->},{" +
                                    "--->--->\"name\":\"p_3\"," +
                                    "--->--->\"deleted\":false," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"role\":null," +
                                    "--->--->\"id\":3" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testQueryUndeletedPermissionAndRole() {
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
                    ).variables(false);
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                    "from ROLE as tb_1_ " +
                                    "where tb_1_.DELETED = ? and tb_1_.ID in (?, ?)"
                    ).variables(false, 1L, 2L);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"name\":\"p_1\"," +
                                    "--->--->\"deleted\":false," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"role\":{" +
                                    "--->--->--->\"name\":\"r_1\"," +
                                    "--->--->--->\"deleted\":false," +
                                    "--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->--->\"id\":1" +
                                    "--->--->}," +
                                    "--->--->\"id\":1" +
                                    "--->},{" +
                                    "--->--->\"name\":\"p_3\"," +
                                    "--->--->\"deleted\":false," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"role\":null," +
                                    "--->--->\"id\":3" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testQueryDeletedRoleWithIdOnlyPermissions() {
        executeAndExpect(
                sqlClientForDeletedData.createQuery(RoleTable.class, (q, role) -> {
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
                    ).variables(true);
                    ctx.statement(1).sql(
                            "select tb_1_.ID " +
                                    "from PERMISSION as tb_1_ " +
                                    "where tb_1_.DELETED = ? and tb_1_.ROLE_ID = ?"
                    ).variables(true, 2L);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"name\":\"r_2\"," +
                                    "--->--->\"deleted\":true," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"permissions\":[" +
                                    "--->--->--->{\"id\":4}" +
                                    "--->--->]," +
                                    "--->--->\"id\":2" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testQueryDeletedRoleWithPermissions() {
        executeAndExpect(
                sqlClientForDeletedData.createQuery(RoleTable.class, (q, role) -> {
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
                    ).variables(true);
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                    "from PERMISSION as tb_1_ " +
                                    "where tb_1_.DELETED = ? and tb_1_.ROLE_ID = ?"
                    ).variables(true, 2L);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"name\":\"r_2\"," +
                                    "--->--->\"deleted\":true," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"permissions\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"name\":\"p_4\"," +
                                    "--->--->--->--->\"deleted\":true," +
                                    "--->--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->--->--->\"id\":4" +
                                    "--->--->--->}" +
                                    "--->--->]," +
                                    "--->--->\"id\":2" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testQueryDeletedPermissionAndIdOnlyRole() {
        executeAndExpect(
                sqlClientForDeletedData.createQuery(PermissionTable.class, RootSelectable::select),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.ROLE_ID " +
                                    "from PERMISSION as tb_1_ " +
                                    "where tb_1_.DELETED = ?"
                    ).variables(true);
                    ctx.statement(1).sql(
                            "select tb_1_.ID from ROLE as tb_1_ " +
                                    "where tb_1_.DELETED = ? and " +
                                    "tb_1_.ID in (?, ?)"
                    ).variables(true, 1L, 2L);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"name\":\"p_2\"," +
                                    "--->--->\"deleted\":true," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"role\":null," +
                                    "--->--->\"id\":2" +
                                    "--->},{" +
                                    "--->--->\"name\":\"p_4\"," +
                                    "--->--->\"deleted\":true," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"role\":{\"id\":2}," +
                                    "--->--->\"id\":4" +
                                    "--->}]"
                    );
                }
        );
    }

    @Test
    public void testQueryDeletedPermissionAndRole() {
        executeAndExpect(
                sqlClientForDeletedData.createQuery(PermissionTable.class, (q, permission) -> {
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
                    ).variables(true);
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                    "from ROLE as tb_1_ " +
                                    "where tb_1_.DELETED = ? and tb_1_.ID in (?, ?)"
                    ).variables(true, 1L, 2L);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"name\":\"p_2\"," +
                                    "--->--->\"deleted\":true," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"role\":null,\"id\":2" +
                                    "--->},{" +
                                    "--->--->\"name\":\"p_4\"," +
                                    "--->--->\"deleted\":true," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"role\":{" +
                                    "--->--->--->\"name\":\"r_2\"," +
                                    "--->--->--->\"deleted\":true," +
                                    "--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->--->\"id\":2" +
                                    "--->--->}," +
                                    "--->--->\"id\":4" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
