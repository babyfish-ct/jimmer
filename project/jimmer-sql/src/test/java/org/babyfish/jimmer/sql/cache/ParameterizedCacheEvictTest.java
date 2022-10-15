package org.babyfish.jimmer.sql.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.*;
import java.util.function.Function;

public class ParameterizedCacheEvictTest extends AbstractQueryTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JSqlClient sqlClient;

    private List<String> messages;

    @BeforeEach
    public void initialize() {
        messages = new ArrayList<>();
        ParameterizedCacheEvictTest that = this;
        sqlClient = getSqlClient(it -> {
            it.addFilter(new UndeletedFilter());
            it.setEntityManager(
                    new EntityManager(
                            Administrator.class,
                            AdministratorMetadata.class,
                            Role.class,
                            Permission.class
                    )
            );
            it.setCaches(cfg -> {
                cfg.setCacheFactory(
                        new CacheFactory() {
                            @Override
                            public @Nullable Cache<?, ?> createObjectCache(@NotNull ImmutableType type) {
                                return new CacheImpl<>(type);
                            }

                            @Override
                            public @Nullable Cache<?, ?> createAssociatedIdCache(@NotNull ImmutableProp prop) {
                                return ParameterizedCaches.create(prop, that::onPropCacheDelete);
                            }

                            @Override
                            public @Nullable Cache<?, List<?>> createAssociatedIdListCache(@NotNull ImmutableProp prop) {
                                return ParameterizedCaches.create(prop, that::onPropCacheDelete);
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
    }

    private void onPropCacheDelete(ImmutableProp prop, Collection<?> keys) {
        messages.add(
                "delete " +
                        prop.getDeclaringType().getJavaClass().getSimpleName() +
                        "." +
                        prop.getName() +
                        "-" +
                        keys
        );
    }

    @Test
    public void testAdministratorBinLog() {
        connectAndExpect(
                con -> {
                    try {
                        sqlClient.getCaches().invalidateByBinLog(
                                "administrator",
                                MAPPER.readTree("{\"id\":1, \"deleted\":false}"),
                                MAPPER.readTree("{\"id\":1, \"deleted\":true}")
                        );
                    } catch (JsonProcessingException ex) {
                        Assertions.fail(ex);
                    }
                    return null;
                },
                ctx -> {
                    ctx.sql(
                            "select distinct tb_1_.ID " +
                                    "from ADMINISTRATOR_METADATA as tb_1_ " +
                                    "where tb_1_.ADMINISTRATOR_ID = ?"
                    );
                    ctx.statement(1).sql(
                            "select distinct tb_1_.ID " +
                                    "from ROLE as tb_1_ " +
                                    "inner join ADMINISTRATOR_ROLE_MAPPING as tb_2_ on tb_1_.ID = tb_2_.ROLE_ID " +
                                    "where tb_2_.ADMINISTRATOR_ID = ?"
                    ).variables(1L);
                }
        );
        Assertions.assertEquals(
                "[" +
                        "delete AdministratorMetadata.administrator-[10], " +
                        "delete Role.administrators-[100]" +
                        "]",
                messages.toString()
        );
    }

    @Test
    public void testRoleBinLog() {
        connectAndExpect(
                con -> {
                    try {
                        sqlClient.getCaches().invalidateByBinLog(
                                "role",
                                MAPPER.readTree("{\"id\":100, \"deleted\":false}"),
                                MAPPER.readTree("{\"id\":100, \"deleted\":true}")
                        );
                    } catch (JsonProcessingException ex) {
                        Assertions.fail(ex);
                    }
                    return null;
                },
                ctx -> {
                    ctx.sql(
                            "select distinct tb_1_.ID " +
                                    "from ADMINISTRATOR as tb_1_ " +
                                    "inner join ADMINISTRATOR_ROLE_MAPPING as tb_2_ on tb_1_.ID = tb_2_.ADMINISTRATOR_ID " +
                                    "where tb_2_.ROLE_ID = ?"
                    ).variables(100L);
                    ctx.statement(1).sql(
                            "select distinct tb_1_.ID " +
                                    "from PERMISSION as tb_1_ " +
                                    "where tb_1_.ROLE_ID = ?"
                    ).variables(100L);
                }
        );
        Assertions.assertEquals(
                "[" +
                        "delete Administrator.roles-[1, 2, 3], " +
                        "delete Permission.role-[1000, 2000]" +
                        "]",
                messages.toString()
        );
    }

    @Test
    public void testPermissionBinLog() {
        connectAndExpect(
                con -> {
                    try {
                        sqlClient.getCaches().invalidateByBinLog(
                                "permission",
                                MAPPER.readTree("{\"id\":1000, \"deleted\":false, \"role_id\": 100}"),
                                MAPPER.readTree("{\"id\":1000, \"deleted\":true}")
                        );
                    } catch (JsonProcessingException ex) {
                        Assertions.fail(ex);
                    }
                    return null;
                },
                ctx -> {}
        );
        Assertions.assertEquals(
                "[" +
                        "delete Role.permissions-[100]" +
                        "]",
                messages.toString()
        );
    }

    /*
     * When Foreign key is changed, use classic trigger,
     * not new trigger for parameterized cache only.
     */
    @Test
    public void testPermissionBinLogWithChangedForeignKey() {
        connectAndExpect(
                con -> {
                    try {
                        sqlClient.getCaches().invalidateByBinLog(
                                "permission",
                                MAPPER.readTree("{\"id\":1000, \"deleted\":false, \"role_id\": 100}"),
                                MAPPER.readTree("{\"id\":1000, \"deleted\":true, \"role_id\": 200}")
                        );
                    } catch (JsonProcessingException ex) {
                        Assertions.fail(ex);
                    }
                    return null;
                },
                ctx -> {}
        );
        Assertions.assertEquals(
                "[" +
                        "delete Role.permissions-[100], " +
                        "delete Role.permissions-[200], " +
                        "delete Permission.role-[1000]" +
                        "]",
                messages.toString()
        );
    }

    private static class UndeletedFilter implements CacheableFilter<NamedEntityProps> {

        @Override
        public void filter(FilterArgs<NamedEntityProps> args) {
            args.where(args.getTable().deleted().eq(false));
        }

        @Override
        public NavigableMap<String, Object> getParameters() {
            NavigableMap<String, Object> map = new TreeMap<>();
            map.put("deleted", false);
            return map;
        }

        @Override
        public boolean isAffectedBy(EntityEvent<?> e) {
            return e.getUnchangedFieldRef(NamedEntityProps.DELETED) == null;
        }
    }
}
