package org.babyfish.jimmer.sql.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.CacheImpl;
import org.babyfish.jimmer.sql.common.ParameterizedCaches;
import org.babyfish.jimmer.sql.filter.common.CacheableFileFilter;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class FilterCacheEvictTest extends AbstractQueryTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JSqlClient sqlClient;

    private List<String> deleteMessages;

    @BeforeEach
    public void initialize() {
        deleteMessages = new ArrayList<>();
        FilterCacheEvictTest that = this;
        sqlClient = getSqlClient(it -> {
            it.addFilters(new CacheableFileFilter());
            it.setCaches(cfg -> {
                cfg.setCacheFactory(
                        new CacheFactory() {
                            @Override
                            public Cache<?, ?> createObjectCache(@NotNull ImmutableType type) {
                                return new CacheImpl<>(type);
                            }

                            @Override
                            public Cache<?, ?> createAssociatedIdCache(@NotNull ImmutableProp prop) {
                                return ParameterizedCaches.create(prop, that::onPropCacheDelete);
                            }

                            @Override
                            public Cache<?, List<?>> createAssociatedIdListCache(@NotNull ImmutableProp prop) {
                                return ParameterizedCaches.create(prop, that::onPropCacheDelete);
                            }

                            @Override
                            public Cache<?, ?> createResolverCache(@NotNull ImmutableProp prop) {
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

    private void onPropCacheDelete(Collection<String> keys) {
        deleteMessages.addAll(keys);
    }

    @Test
    public void testChangeForeignKey() {
        connectAndExpect(
                con -> {
                    try {
                        sqlClient.getBinLog().accept(
                                "file",
                                MAPPER.readTree("{\"id\":9, \"parent_id\":8}"),
                                MAPPER.readTree("{\"id\":9, \"parent_id\":2}")
                        );
                    } catch (JsonProcessingException ex) {
                        Assertions.fail(ex);
                    }
                    return null;
                },
                ctx -> {}
        );
        Assertions.assertEquals(
                "[File.parent-9, File.childFiles-8, File.childFiles-2]",
                deleteMessages.toString()
        );
    }

    @Test
    public void testChangeUserAssociations() {
        connectAndExpect(
                con -> {
                    try {
                        sqlClient.getBinLog().accept(
                                "file_user_mapping",
                                MAPPER.readTree("{\"file_id\":28, \"user_id\":2}"),
                                null
                        );
                    } catch (JsonProcessingException ex) {
                        Assertions.fail(ex);
                    }
                    return null;
                },
                ctx -> {
                    ctx.sql(
                            "select tb_1_.PARENT_ID " +
                                    "from FILE tb_1_ " +
                                    "where tb_1_.ID = ? " +
                                    "and tb_1_.PARENT_ID is not null"
                    ).variables(28L);
                    ctx.statement(1).sql(
                            "select distinct tb_1_.ID " +
                                    "from FILE tb_1_ " +
                                    "where tb_1_.PARENT_ID = ?"
                    ).variables(28L);
                }
        );
        Assertions.assertEquals(
                "[" +
                        "File.childFiles-27, " +
                        "File.parent-29, " +
                        "File.parent-30, " +
                        "File.parent-31, " +
                        "File.parent-32, " +
                        "File.parent-33, " +
                        "File.users-28, " +
                        "User.files-2" +
                        "]",
                deleteMessages.toString()
        );
    }
}
