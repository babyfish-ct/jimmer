package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.babyfish.jimmer.sql.model.inheritance.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GlobalFilterTest extends AbstractQueryTest {

    private static final Filter<NamedEntityProps> UNDELETED_FILTER =
            new Filter<NamedEntityProps>() {
                @Override
                public void filter(FilterArgs<NamedEntityProps> args) {
                    args.where(args.getTable().deleted().eq(false));
                }
            };

    private static final Filter<NamedEntityProps> DELETED_FILTER =
            new Filter<NamedEntityProps>() {
                @Override
                public void filter(FilterArgs<NamedEntityProps> args) {
                    args.where(args.getTable().deleted().eq(true));
                }
            };

    private JSqlClient sqlClient;

    private JSqlClient sqlClientForDeletedData;

    @BeforeEach
    public void initialize() {
        this.sqlClient = getSqlClient(it -> {
           it.addFilters(UNDELETED_FILTER);
           it.addDisabledFilters(DELETED_FILTER);
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
                                    "where tb_1_.ROLE_ID = ? " +
                                    "and tb_1_.DELETED = ?"
                    ).variables(100L, false);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"name\":\"r_1\"," +
                                    "--->--->\"deleted\":false," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"permissions\":[" +
                                    "--->--->--->{\"id\":1000}" +
                                    "--->--->]," +
                                    "--->--->\"id\":100" +
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
                                    "where tb_1_.ROLE_ID = ? " +
                                    "and tb_1_.DELETED = ?"
                    ).variables(100L, false);
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
                                    "--->--->--->--->\"id\":1000" +
                                    "--->--->--->}" +
                                    "--->--->]," +
                                    "--->--->\"id\":100" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testQueryUndeletedPermissionAndIdOnlyRole() {
        executeAndExpect(
                sqlClient.createQuery(PermissionTable.class, (q, permisson) -> {
                    return q.select(
                            permisson.fetch(
                                    PermissionFetcher.$
                                            .allScalarFields()
                                            .role()
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
                            "select tb_1_.ID from ROLE as tb_1_ " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "and tb_1_.DELETED = ?"
                    ).variables(100L, 200L, false);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"name\":\"p_1\"," +
                                    "--->--->\"deleted\":false," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"role\":{\"id\":100},\"" +
                                    "--->--->id\":1000" +
                                    "--->},{" +
                                    "--->--->\"name\":\"p_3\"," +
                                    "--->--->\"deleted\":false," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"role\":null," +
                                    "--->--->\"id\":3000" +
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
                                    "where tb_1_.ID in (?, ?) and tb_1_.DELETED = ?"
                    ).variables(100L, 200L, false);
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
                                    "--->--->--->\"id\":100" +
                                    "--->--->}," +
                                    "--->--->\"id\":1000" +
                                    "--->},{" +
                                    "--->--->\"name\":\"p_3\"," +
                                    "--->--->\"deleted\":false," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"role\":null," +
                                    "--->--->\"id\":3000" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testQueryUndeletedAdministratorWithIdOnlyRoles() {
        executeAndExpect(
                sqlClient.createQuery(AdministratorTable.class, (q, administrator) -> {
                    return q.select(
                            administrator.fetch(
                                    AdministratorFetcher.$
                                            .allScalarFields()
                                            .roles()
                            )
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                    "from ADMINISTRATOR as tb_1_ " +
                                    "where tb_1_.DELETED = ?"
                    ).variables(false);
                    ctx.statement(1).sql(
                            "select tb_2_.ADMINISTRATOR_ID, tb_1_.ID " +
                                    "from ROLE as tb_1_ " +
                                    "inner join ADMINISTRATOR_ROLE_MAPPING as tb_2_ on tb_1_.ID = tb_2_.ROLE_ID " +
                                    "where tb_2_.ADMINISTRATOR_ID in (?, ?) " +
                                    "and tb_1_.DELETED = ?"
                    ).variables(1L, 3L, false);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"name\":\"a_1\"," +
                                    "--->--->\"deleted\":false," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"roles\":[" +
                                    "--->--->--->{\"id\":100}" +
                                    "--->--->]," +
                                    "--->--->\"id\":1" +
                                    "--->},{" +
                                    "--->--->\"name\":\"a_3\"," +
                                    "--->--->\"deleted\":false," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"roles\":[" +
                                    "--->--->--->{\"id\":100}" +
                                    "--->--->]," +
                                    "--->--->\"id\":3" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testQueryUndeletedAdministratorWithRoles() {
        executeAndExpect(
                sqlClient.createQuery(AdministratorTable.class, (q, administrator) -> {
                    return q.select(
                            administrator.fetch(
                                    AdministratorFetcher.$
                                            .allScalarFields()
                                            .roles(
                                                    RoleFetcher.$
                                                            .allScalarFields()
                                            )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                    "from ADMINISTRATOR as tb_1_ " +
                                    "where tb_1_.DELETED = ?"
                    ).variables(false);
                    ctx.statement(1).sql(
                            "select tb_2_.ADMINISTRATOR_ID, tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                    "from ROLE as tb_1_ " +
                                    "inner join ADMINISTRATOR_ROLE_MAPPING as tb_2_ on tb_1_.ID = tb_2_.ROLE_ID " +
                                    "where tb_2_.ADMINISTRATOR_ID in (?, ?) " +
                                    "and tb_1_.DELETED = ?"
                    ).variables(1L, 3L, false);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"name\":\"a_1\"," +
                                    "--->--->\"deleted\":false," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"roles\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"name\":\"r_1\"," +
                                    "--->--->--->--->\"deleted\":false," +
                                    "--->--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->--->--->\"id\":100" +
                                    "--->--->--->}" +
                                    "--->--->]," +
                                    "--->--->\"id\":1" +
                                    "--->},{" +
                                    "--->--->\"name\":\"a_3\"," +
                                    "--->--->\"deleted\":false," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"roles\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"name\":\"r_1\"," +
                                    "--->--->--->--->\"deleted\":false," +
                                    "--->--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->--->--->\"id\":100" +
                                    "--->--->--->}" +
                                    "--->--->]," +
                                    "--->--->\"id\":3" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testQueryUndeletedRoleAndIdOnlyAdministrators() {
        executeAndExpect(
                sqlClient.createQuery(RoleTable.class, (q, role) -> {
                    return q.select(
                            role.fetch(
                                    RoleFetcher.$
                                            .allScalarFields()
                                            .administrators()
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
                                    "from ADMINISTRATOR as tb_1_ " +
                                    "inner join ADMINISTRATOR_ROLE_MAPPING as tb_2_ on tb_1_.ID = tb_2_.ADMINISTRATOR_ID " +
                                    "where tb_2_.ROLE_ID = ? and tb_1_.DELETED = ?"
                    ).variables(100L, false);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"name\":\"r_1\"," +
                                    "--->--->\"deleted\":false," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"administrators\":[" +
                                    "--->--->--->{\"id\":1}," +
                                    "--->--->--->{\"id\":3}" +
                                    "--->--->]," +
                                    "--->--->\"id\":100" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testQueryUndeletedRoleAndAdministrators() {
        executeAndExpect(
                sqlClient.createQuery(RoleTable.class, (q, role) -> {
                    return q.select(
                            role.fetch(
                                    RoleFetcher.$
                                            .allScalarFields()
                                            .administrators(
                                                    AdministratorFetcher.$
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
                                    "from ADMINISTRATOR as tb_1_ " +
                                    "inner join ADMINISTRATOR_ROLE_MAPPING as tb_2_ on tb_1_.ID = tb_2_.ADMINISTRATOR_ID " +
                                    "where tb_2_.ROLE_ID = ? and tb_1_.DELETED = ?"
                    ).variables(100L, false);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"name\":\"r_1\"," +
                                    "--->--->\"deleted\":false," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"administrators\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"name\":\"a_1\"," +
                                    "--->--->--->--->\"deleted\":false," +
                                    "--->--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->--->--->\"id\":1" +
                                    "--->--->--->},{" +
                                    "--->--->--->--->\"name\":\"a_3\"," +
                                    "--->--->--->--->\"deleted\":false," +
                                    "--->--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->--->--->\"id\":3" +
                                    "--->--->--->}" +
                                    "--->--->]," +
                                    "--->--->\"id\":100" +
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
                                    "where tb_1_.ROLE_ID = ? and tb_1_.DELETED = ?"
                    ).variables(200L, true);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"name\":\"r_2\"," +
                                    "--->--->\"deleted\":true," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"permissions\":[" +
                                    "--->--->--->{\"id\":4000}" +
                                    "--->--->]," +
                                    "--->--->\"id\":200" +
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
                                    "where tb_1_.ROLE_ID = ? and tb_1_.DELETED = ?"
                    ).variables(200L, true);
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
                                    "--->--->--->--->\"id\":4000" +
                                    "--->--->--->}" +
                                    "--->--->]," +
                                    "--->--->\"id\":200" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testQueryDeletedPermissionAndIdOnlyRole() {
        executeAndExpect(
                sqlClientForDeletedData.createQuery(PermissionTable.class, (q, permission) -> {
                    return q.select(
                            permission.fetch(
                                    PermissionFetcher.$
                                            .allScalarFields()
                                            .role()
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
                            "select tb_1_.ID from ROLE as tb_1_ " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "and tb_1_.DELETED = ?"
                    ).variables(100L, 200L, true);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"name\":\"p_2\"," +
                                    "--->--->\"deleted\":true," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"role\":null," +
                                    "--->--->\"id\":2000" +
                                    "--->},{" +
                                    "--->--->\"name\":\"p_4\"," +
                                    "--->--->\"deleted\":true," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"role\":{\"id\":200}," +
                                    "--->--->\"id\":4000" +
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
                                    "where tb_1_.ID in (?, ?) and tb_1_.DELETED = ?"
                    ).variables(100L, 200L, true);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"name\":\"p_2\"," +
                                    "--->--->\"deleted\":true," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"role\":null," +
                                    "--->--->\"id\":2000" +
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
                                    "--->--->--->\"id\":200" +
                                    "--->--->}," +
                                    "--->--->\"id\":4000" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testQueryDeletedAdministratorWithIdOnlyRoles() {
        executeAndExpect(
                sqlClientForDeletedData.createQuery(AdministratorTable.class, (q, administrator) -> {
                    return q.select(
                            administrator.fetch(
                                    AdministratorFetcher.$
                                            .allScalarFields()
                                            .roles()
                            )
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                    "from ADMINISTRATOR as tb_1_ " +
                                    "where tb_1_.DELETED = ?"
                    ).variables(true);
                    ctx.statement(1).sql(
                            "select tb_2_.ADMINISTRATOR_ID, tb_1_.ID " +
                                    "from ROLE as tb_1_ " +
                                    "inner join ADMINISTRATOR_ROLE_MAPPING as tb_2_ on tb_1_.ID = tb_2_.ROLE_ID " +
                                    "where tb_2_.ADMINISTRATOR_ID in (?, ?) " +
                                    "and tb_1_.DELETED = ?"
                    ).variables(2L, 4L, true);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"name\":\"a_2\"," +
                                    "--->--->\"deleted\":true," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"roles\":[" +
                                    "--->--->--->{\"id\":200}" +
                                    "--->--->]," +
                                    "--->--->\"id\":2" +
                                    "--->},{" +
                                    "--->--->\"name\":\"a_4\"," +
                                    "--->--->\"deleted\":true," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"roles\":[" +
                                    "--->--->--->{\"id\":200}" +
                                    "--->--->]," +
                                    "--->--->\"id\":4" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testQueryDeletedAdministratorWithRoles() {
        executeAndExpect(
                sqlClientForDeletedData.createQuery(AdministratorTable.class, (q, administrator) -> {
                    return q.select(
                            administrator.fetch(
                                    AdministratorFetcher.$
                                            .allScalarFields()
                                            .roles(
                                                    RoleFetcher.$
                                                            .allScalarFields()
                                            )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                    "from ADMINISTRATOR as tb_1_ " +
                                    "where tb_1_.DELETED = ?"
                    ).variables(true);
                    ctx.statement(1).sql(
                            "select tb_2_.ADMINISTRATOR_ID, tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                    "from ROLE as tb_1_ " +
                                    "inner join ADMINISTRATOR_ROLE_MAPPING as tb_2_ on tb_1_.ID = tb_2_.ROLE_ID " +
                                    "where tb_2_.ADMINISTRATOR_ID in (?, ?) " +
                                    "and tb_1_.DELETED = ?"
                    ).variables(2L, 4L, true);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"name\":\"a_2\"," +
                                    "--->--->\"deleted\":true," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"roles\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"name\":\"r_2\"," +
                                    "--->--->--->--->\"deleted\":true," +
                                    "--->--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->--->--->\"id\":200" +
                                    "--->--->--->}" +
                                    "--->--->]," +
                                    "--->--->\"id\":2" +
                                    "--->},{" +
                                    "--->--->\"name\":\"a_4\"," +
                                    "--->--->\"deleted\":true," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"roles\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"name\":\"r_2\"," +
                                    "--->--->--->--->\"deleted\":true," +
                                    "--->--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->--->--->\"id\":200" +
                                    "--->--->--->}" +
                                    "--->--->]," +
                                    "--->--->\"id\":4" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testQueryDeletedRoleAndIdOnlyAdministrators() {
        executeAndExpect(
                sqlClientForDeletedData.createQuery(RoleTable.class, (q, role) -> {
                    return q.select(
                            role.fetch(
                                    RoleFetcher.$
                                            .allScalarFields()
                                            .administrators()
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
                                    "from ADMINISTRATOR as tb_1_ " +
                                    "inner join ADMINISTRATOR_ROLE_MAPPING as tb_2_ on tb_1_.ID = tb_2_.ADMINISTRATOR_ID " +
                                    "where tb_2_.ROLE_ID = ? and tb_1_.DELETED = ?"
                    ).variables(200L, true);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"name\":\"r_2\"," +
                                    "--->--->\"deleted\":true," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"administrators\":[" +
                                    "--->--->--->{\"id\":2}," +
                                    "--->--->--->{\"id\":4}" +
                                    "--->--->]," +
                                    "--->--->\"id\":200" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testQueryDeletedRoleAndAdministrators() {
        executeAndExpect(
                sqlClientForDeletedData.createQuery(RoleTable.class, (q, role) -> {
                    return q.select(
                            role.fetch(
                                    RoleFetcher.$
                                            .allScalarFields()
                                            .administrators(
                                                    AdministratorFetcher.$
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
                                    "from ADMINISTRATOR as tb_1_ " +
                                    "inner join ADMINISTRATOR_ROLE_MAPPING as tb_2_ on tb_1_.ID = tb_2_.ADMINISTRATOR_ID " +
                                    "where tb_2_.ROLE_ID = ? and tb_1_.DELETED = ?"
                    ).variables(200L, true);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"name\":\"r_2\"," +
                                    "--->--->\"deleted\":true," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"administrators\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"name\":\"a_2\"," +
                                    "--->--->--->--->\"deleted\":true," +
                                    "--->--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->--->--->\"id\":2" +
                                    "--->--->--->},{" +
                                    "--->--->--->--->\"name\":\"a_4\"," +
                                    "--->--->--->--->\"deleted\":true," +
                                    "--->--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->--->--->\"id\":4" +
                                    "--->--->--->}" +
                                    "--->--->]," +
                                    "--->--->\"id\":200" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
