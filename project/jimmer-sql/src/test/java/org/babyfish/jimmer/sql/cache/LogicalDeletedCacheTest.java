package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.CacheImpl;
import org.babyfish.jimmer.sql.common.ParameterizedCaches;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.filter.CacheableFilter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.babyfish.jimmer.sql.model.inheritance.*;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.babyfish.jimmer.sql.runtime.LogicalDeletedBehavior;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

public class LogicalDeletedCacheTest extends AbstractQueryTest {

    private JSqlClient sqlClient;

    private JSqlClient sqlClientForAllData;

    private JSqlClient sqlClientForDeletedData;

    @BeforeEach
    public void initialize() {
        sqlClient = getSqlClient(it -> {
            it.setCaches(cfg -> {
                cfg.setCacheFactory(
                        new CacheFactory() {
                            @Override
                            public @Nullable Cache<?, ?> createObjectCache(@NotNull ImmutableType type) {
                                return new CacheImpl<>(type);
                            }

                            @Override
                            public @Nullable Cache<?, ?> createAssociatedIdCache(@NotNull ImmutableProp prop) {
                                return new CacheImpl<>(prop);
                            }

                            @Override
                            public @Nullable Cache<?, List<?>> createAssociatedIdListCache(@NotNull ImmutableProp prop) {
                                return new CacheImpl<>(prop);
                            }

                            @Override
                            public @Nullable Cache<?, ?> createResolverCache(@NotNull ImmutableProp prop) {
                                return new CacheImpl<>(prop);
                            }
                        }
                );
            });
            it.setConnectionManager(
                    new ConnectionManager() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public <R> R execute(Function<Connection, R> block) {
                            R[] resultBox = (R[])new Object[1];
                            jdbc(con -> {
                                resultBox[0] = block.apply(con);
                            });
                            return resultBox[0];
                        }
                    }
            );
        });
        sqlClientForAllData = sqlClient
                .filters(cfg -> cfg.setBehavior(LogicalDeletedBehavior.IGNORED));
        sqlClientForDeletedData = sqlClient
                .filters(cfg -> cfg.setBehavior(LogicalDeletedBehavior.REVERSED));
    }

    @Test
    public void testRoleWithPermissions() {
        RoleTable role = RoleTable.$;
        for (int i = 0; i < 2; i++) {
            boolean useSql = i == 0;
            executeAndExpect(
                    sqlClient
                            .createQuery(role)
                            .select(
                                    role.fetch(
                                            RoleFetcher.$
                                                    .allScalarFields()
                                                    .deleted()
                                                    .permissions(
                                                            PermissionFetcher.$
                                                                    .allScalarFields()
                                                                    .deleted()
                                                    )
                                    )
                            ),
                    ctx -> {
                        ctx.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED " +
                                        "from ROLE tb_1_ " +
                                        "where tb_1_.DELETED <> ?"
                        ).variables(true);
                        if (useSql) {
                            ctx.statement(1).sql(
                                    "select tb_1_.ID " +
                                            "from PERMISSION tb_1_ " +
                                            "where tb_1_.ROLE_ID = ? " +
                                            "and tb_1_.DELETED <> ?"
                            ).variables(100L, true);
                            ctx.statement(2).sql(
                                    "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.ROLE_ID " +
                                            "from PERMISSION tb_1_ " +
                                            "where tb_1_.ID = ?"
                            ).variables(1000L);
                        }
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
            executeAndExpect(
                    sqlClientForAllData
                            .createQuery(role)
                            .select(
                                    role.fetch(
                                            RoleFetcher.$
                                                    .allScalarFields()
                                                    .deleted()
                                                    .permissions(
                                                            PermissionFetcher.$
                                                                    .allScalarFields()
                                                                    .deleted()
                                                    )
                                    )
                            ),
                    ctx -> {
                        ctx.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED " +
                                        "from ROLE tb_1_"
                        ).variables();
                        ctx.statement(1).sql(
                                "select " +
                                        "--->tb_1_.ROLE_ID, " +
                                        "--->tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED " +
                                        "from PERMISSION tb_1_ " +
                                        "where tb_1_.ROLE_ID in (?, ?)"
                        ).variables(100L, 200L);
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
                                        "--->--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\",\"id\":1000" +
                                        "--->--->--->},{" +
                                        "--->--->--->--->\"name\":\"p_2\"," +
                                        "--->--->--->--->\"deleted\":true," +
                                        "--->--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                        "--->--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                        "--->--->--->--->\"id\":2000" +
                                        "--->--->--->}" +
                                        "--->--->]," +
                                        "--->--->\"id\":100" +
                                        "--->},{" +
                                        "--->--->\"name\":\"r_2\"," +
                                        "--->--->\"deleted\":true," +
                                        "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                        "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                        "--->--->\"permissions\":[" +
                                        "--->--->--->{" +
                                        "--->--->--->--->\"name\":\"p_3\"," +
                                        "--->--->--->--->\"deleted\":false," +
                                        "--->--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                        "--->--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                        "--->--->--->--->\"id\":3000" +
                                        "--->--->--->},{" +
                                        "--->--->--->--->\"name\":\"p_4\"," +
                                        "--->--->--->--->\"deleted\":true," +
                                        "--->--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                        "--->--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\",\"id\":4000" +
                                        "--->--->--->}" +
                                        "--->--->]," +
                                        "--->--->\"id\":200" +
                                        "--->}" +
                                        "]"
                        );
                    }
            );
            executeAndExpect(
                    sqlClientForDeletedData
                            .createQuery(role)
                            .select(
                                    role.fetch(
                                            RoleFetcher.$
                                                    .allScalarFields()
                                                    .deleted()
                                                    .permissions(
                                                            PermissionFetcher.$
                                                                    .allScalarFields()
                                                                    .deleted()
                                                    )
                                    )
                            ),
                    ctx -> {
                        ctx.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED " +
                                        "from ROLE tb_1_ " +
                                        "where tb_1_.DELETED = ?"
                        ).variables(true);
                        ctx.statement(1).sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED " +
                                        "from PERMISSION tb_1_ " +
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
    }

    @Test
    public void testPermissionWithRole() {
        PermissionTable permission = PermissionTable.$;
        for (int i = 0; i < 2; i++) {
            boolean useSql = i == 0;
            executeAndExpect(
                    sqlClient
                            .createQuery(permission)
                            .select(
                                    permission.fetch(
                                            PermissionFetcher.$
                                                    .allScalarFields()
                                                    .deleted()
                                                    .role(
                                                            RoleFetcher.$
                                                                    .allScalarFields()
                                                                    .deleted()
                                                    )
                                    )
                            ),
                    ctx -> {
                        ctx.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED, tb_1_.ROLE_ID " +
                                        "from PERMISSION tb_1_ " +
                                        "where tb_1_.DELETED <> ?"
                        ).variables(true);
                        if (useSql) {
                            ctx.statement(1).sql(
                                    "select tb_1_.ID, tb_1_.ROLE_ID " +
                                            "from PERMISSION tb_1_ " +
                                            "inner join ROLE tb_2_ on tb_1_.ROLE_ID = tb_2_.ID " +
                                            "where tb_1_.ID in (?, ?) and tb_1_.ROLE_ID is not null " +
                                            "and tb_2_.DELETED <> ?"
                            ).variables(1000L, 3000L, true);
                            ctx.statement(2).sql(
                                    "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                            "from ROLE tb_1_ where tb_1_.ID = ?"
                            ).variables(100L);
                        }
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
            executeAndExpect(
                    sqlClientForAllData.createQuery(permission)
                            .select(
                                    permission.fetch(
                                            PermissionFetcher.$
                                                    .allScalarFields()
                                                    .deleted()
                                                    .role(
                                                            RoleFetcher.$
                                                                    .allScalarFields()
                                                                    .deleted()
                                                    )
                                    )
                            ),
                    ctx -> {
                        ctx.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED, tb_1_.ROLE_ID " +
                                        "from PERMISSION tb_1_"
                        ).variables();
                        ctx.statement(1).sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED " +
                                        "from ROLE tb_1_ where tb_1_.ID in (?, ?)"
                        ).variables(100L, 200L);
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
                                        "--->--->\"name\":\"p_2\"," +
                                        "--->--->\"deleted\":true," +
                                        "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                        "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                        "--->--->\"role\":{" +
                                        "--->--->--->\"name\":\"r_1\"," +
                                        "--->--->--->\"deleted\":false," +
                                        "--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                        "--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                        "--->--->--->\"id\":100" +
                                        "--->--->}," +
                                        "--->--->\"id\":2000" +
                                        "--->},{" +
                                        "--->--->\"name\":\"p_3\"," +
                                        "--->--->\"deleted\":false," +
                                        "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                        "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                        "--->--->\"role\":{" +
                                        "--->--->--->\"name\":\"r_2\"," +
                                        "--->--->--->\"deleted\":true," +
                                        "--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                        "--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                        "--->--->--->\"id\":200" +
                                        "--->--->}," +
                                        "--->--->\"id\":3000" +
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
            executeAndExpect(
                    sqlClientForDeletedData.createQuery(permission)
                            .select(
                                    permission.fetch(
                                            PermissionFetcher.$
                                                    .allScalarFields()
                                                    .deleted()
                                                    .role(
                                                            RoleFetcher.$
                                                                    .allScalarFields()
                                                                    .deleted()
                                                    )
                                    )
                            ),
                    ctx -> {
                        ctx.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED, tb_1_.ROLE_ID " +
                                        "from PERMISSION tb_1_ " +
                                        "where tb_1_.DELETED = ?"
                        ).variables(true);
                        ctx.statement(1).sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED " +
                                        "from ROLE tb_1_ where tb_1_.ID in (?, ?) and tb_1_.DELETED = ?"
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
    }

    @Test
    public void testAdministratorWithRoles() {
        AdministratorTable administrator = AdministratorTable.$;
        for (int i = 0; i < 2; i++) {
            boolean useSql = i == 0;
            executeAndExpect(
                    sqlClient
                            .createQuery(administrator)
                            .select(
                                    administrator.fetch(
                                            AdministratorFetcher.$
                                                    .allScalarFields()
                                                    .deleted()
                                                    .roles(
                                                            RoleFetcher.$
                                                                    .allScalarFields()
                                                                    .deleted()
                                                    )
                                    )
                            ),
                    ctx -> {
                        ctx.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED " +
                                        "from ADMINISTRATOR tb_1_ " +
                                        "where tb_1_.DELETED <> ?"
                        ).variables(true);
                        if (useSql) {
                            ctx.statement(1).sql(
                                    "select tb_1_.ADMINISTRATOR_ID, tb_1_.ROLE_ID " +
                                            "from ADMINISTRATOR_ROLE_MAPPING tb_1_ " +
                                            "inner join ROLE tb_3_ on tb_1_.ROLE_ID = tb_3_.ID " +
                                            "where tb_1_.ADMINISTRATOR_ID in (?, ?) and tb_3_.DELETED <> ?"
                            ).variables(1L, 3L, true);
                            ctx.statement(2).sql(
                                    "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                            "from ROLE tb_1_ " +
                                            "where tb_1_.ID = ?"
                            ).variables(100L);
                        }
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
            executeAndExpect(
                    sqlClientForAllData
                            .createQuery(administrator)
                            .select(
                                    administrator.fetch(
                                            AdministratorFetcher.$
                                                    .allScalarFields()
                                                    .deleted()
                                                    .roles(
                                                            RoleFetcher.$
                                                                    .allScalarFields()
                                                                    .deleted()
                                                    )
                                    )
                            ),
                    ctx -> {
                        ctx.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED " +
                                        "from ADMINISTRATOR tb_1_"
                        ).variables();
                        ctx.statement(1).sql(
                                "select " +
                                        "--->tb_2_.ADMINISTRATOR_ID, " +
                                        "--->tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED " +
                                        "from ROLE tb_1_ inner join ADMINISTRATOR_ROLE_MAPPING tb_2_ " +
                                        "--->on tb_1_.ID = tb_2_.ROLE_ID " +
                                        "where tb_2_.ADMINISTRATOR_ID in (?, ?, ?, ?, ?)"
                        ).variables(-1L, 1L, 2L, 3L, 4L);
                        ctx.rows(
                                "[" +
                                        "--->{" +
                                        "--->--->\"name\":\"a_-1\"," +
                                        "--->--->\"deleted\":true," +
                                        "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                        "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                        "--->--->\"roles\":[]," +
                                        "--->--->\"id\":-1" +
                                        "--->},{" +
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
                                        "--->--->\"name\":\"a_2\"," +
                                        "--->--->\"deleted\":true," +
                                        "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                        "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                        "--->--->\"roles\":[" +
                                        "--->--->--->{" +
                                        "--->--->--->--->\"name\":\"r_1\"," +
                                        "--->--->--->--->\"deleted\":false," +
                                        "--->--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                        "--->--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                        "--->--->--->--->\"id\":100" +
                                        "--->--->--->}," +
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
                                        "--->--->--->}," +
                                        "--->--->--->{" +
                                        "--->--->--->--->\"name\":\"r_2\"," +
                                        "--->--->--->--->\"deleted\":true," +
                                        "--->--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                        "--->--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                        "--->--->--->--->\"id\":200" +
                                        "--->--->--->}" +
                                        "--->--->]," +
                                        "--->--->\"id\":3" +
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
            executeAndExpect(
                    sqlClientForDeletedData
                            .createQuery(administrator)
                            .select(
                                    administrator.fetch(
                                            AdministratorFetcher.$
                                                    .allScalarFields()
                                                    .deleted()
                                                    .roles(
                                                            RoleFetcher.$
                                                                    .allScalarFields()
                                                                    .deleted()
                                                    )
                                    )
                            ),
                    ctx -> {
                        ctx.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED " +
                                        "from ADMINISTRATOR tb_1_ " +
                                        "where tb_1_.DELETED = ?"
                        ).variables(true);
                        ctx.statement(1).sql(
                                "select " +
                                        "--->tb_2_.ADMINISTRATOR_ID, " +
                                        "--->tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED " +
                                        "from ROLE tb_1_ inner join ADMINISTRATOR_ROLE_MAPPING tb_2_ " +
                                        "--->on tb_1_.ID = tb_2_.ROLE_ID " +
                                        "where tb_2_.ADMINISTRATOR_ID in (?, ?, ?) and tb_1_.DELETED = ?"
                        ).variables(-1L, 2L, 4L, true);
                        ctx.rows(
                                "[" +
                                        "--->{" +
                                        "--->--->\"name\":\"a_-1\"," +
                                        "--->--->\"deleted\":true," +
                                        "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                        "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                        "--->--->\"roles\":[]," +
                                        "--->--->\"id\":-1" +
                                        "--->}," +
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
    }

    @Test
    public void testRoleAndAdministrators() {
        RoleTable role = RoleTable.$;
        for (int i = 0; i < 2; i++) {
            boolean useSql = i == 0;
            executeAndExpect(
                    sqlClient
                            .createQuery(role)
                            .select(
                                    role.fetch(
                                            RoleFetcher.$
                                                    .allScalarFields()
                                                    .deleted()
                                                    .administrators(
                                                            AdministratorFetcher.$
                                                                    .allScalarFields()
                                                                    .deleted()
                                                    )
                                    )
                            ),
                    ctx -> {
                        ctx.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED " +
                                        "from ROLE tb_1_ " +
                                        "where tb_1_.DELETED <> ?"
                        ).variables(true);
                        if (useSql) {
                            ctx.statement(1).sql(
                                    "select tb_1_.ADMINISTRATOR_ID " +
                                            "from ADMINISTRATOR_ROLE_MAPPING tb_1_ " +
                                            "inner join ADMINISTRATOR tb_3_ on tb_1_.ADMINISTRATOR_ID = tb_3_.ID " +
                                            "where tb_1_.ROLE_ID = ? and tb_3_.DELETED <> ?"
                            ).variables(100L, true);
                            ctx.statement(2).sql(
                                    "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                            "from ADMINISTRATOR tb_1_ " +
                                            "where tb_1_.ID in (?, ?)"
                            ).variables(1L, 3L);
                        }
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
            executeAndExpect(
                    sqlClientForAllData
                            .createQuery(role)
                            .select(
                                    role.fetch(
                                            RoleFetcher.$
                                                    .allScalarFields()
                                                    .deleted()
                                                    .administrators(
                                                            AdministratorFetcher.$
                                                                    .allScalarFields()
                                                                    .deleted()
                                                    )
                                    )
                            ),
                    ctx -> {
                        ctx.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED " +
                                        "from ROLE tb_1_"
                        ).variables();
                        ctx.statement(1).sql(
                                "select " +
                                        "--->tb_2_.ROLE_ID, " +
                                        "--->tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED " +
                                        "from ADMINISTRATOR tb_1_ inner join ADMINISTRATOR_ROLE_MAPPING tb_2_ " +
                                        "--->on tb_1_.ID = tb_2_.ADMINISTRATOR_ID " +
                                        "where tb_2_.ROLE_ID in (?, ?)"
                        ).variables(100L, 200L);
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
                                        "--->--->--->}," +
                                        "--->--->--->{" +
                                        "--->--->--->--->\"name\":\"a_2\"," +
                                        "--->--->--->--->\"deleted\":true," +
                                        "--->--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                        "--->--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                        "--->--->--->--->\"id\":2" +
                                        "--->--->--->}," +
                                        "--->--->--->{" +
                                        "--->--->--->--->\"name\":\"a_3\"," +
                                        "--->--->--->--->\"deleted\":false," +
                                        "--->--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                        "--->--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                        "--->--->--->--->\"id\":3" +
                                        "--->--->--->}" +
                                        "--->--->]," +
                                        "--->--->\"id\":100" +
                                        "--->},{" +
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
                                        "--->--->--->}," +
                                        "--->--->--->{" +
                                        "--->--->--->--->\"name\":\"a_3\"," +
                                        "--->--->--->--->\"deleted\":false," +
                                        "--->--->--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                        "--->--->--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                        "--->--->--->--->\"id\":3" +
                                        "--->--->--->}," +
                                        "--->--->--->{" +
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
            executeAndExpect(
                    sqlClientForDeletedData
                            .createQuery(role)
                            .select(
                                    role.fetch(
                                            RoleFetcher.$
                                                    .allScalarFields()
                                                    .deleted()
                                                    .administrators(
                                                            AdministratorFetcher.$
                                                                    .allScalarFields()
                                                                    .deleted()
                                                    )
                                    )
                            ),
                    ctx -> {
                        ctx.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED " +
                                        "from ROLE tb_1_ " +
                                        "where tb_1_.DELETED = ?"
                        ).variables(true);
                        ctx.statement(1).sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED " +
                                        "from ADMINISTRATOR tb_1_ inner join ADMINISTRATOR_ROLE_MAPPING tb_2_ " +
                                        "--->on tb_1_.ID = tb_2_.ADMINISTRATOR_ID " +
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
}
