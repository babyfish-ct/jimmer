package org.babyfish.jimmer.sql.trigger;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.CacheFactory;
import org.babyfish.jimmer.sql.cache.CacheOperator;
import org.babyfish.jimmer.sql.cache.UsedCache;
import org.babyfish.jimmer.sql.common.CacheImpl;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.OrganizationDraft;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.OrganizationProps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JoinedInheritanceMutationWithTriggerTest extends AbstractTriggerTest {

    private final List<String> cacheOpRecords = new ArrayList<>();

    @Test
    public void testRejectedSubtypeUpdateDoesNotFireTriggerOrEvictCache() {
        cacheOpRecords.clear();
        connectAndExpect(
                con -> {
                    sqlClientWithCacheOperator()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(201L);
                                        organization.setTaxCode("SHOULD-NOT-WRITE");
                                        organization.addIntoProjects(project -> {
                                            project.setId(2401L);
                                            project.setName("Should not be saved");
                                        });
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setAssociatedMode(OrganizationProps.PROJECTS, AssociatedSaveMode.APPEND)
                            .execute(con);
                    return cacheOpRecords.toString();
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME, tb_1__sub.TAX_CODE " +
                                        "from JOINED_CLIENT tb_1_ " +
                                        "inner join JOINED_ORGANIZATION tb_1__sub " +
                                        "on tb_1_.ID = tb_1__sub.ID " +
                                        "where tb_1_.ID = ? and tb_1_.CLIENT_TYPE = ?"
                        );
                        it.variables(201L, "ORG");
                    });
                    ctx.value("[]");
                }
        );
        assertEvents();
        Assertions.assertEquals("[]", cacheOpRecords.toString());
    }

    @Test
    public void testRejectedSubtypeUpsertDoesNotFireTriggerOrEvictCache() {
        cacheOpRecords.clear();
        connectAndExpect(
                con -> {
                    sqlClientWithCacheOperator()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(201L);
                                        organization.setName("Should not update");
                                        organization.setTaxCode("SHOULD-NOT-WRITE");
                                        organization.addIntoProjects(project -> {
                                            project.setId(2402L);
                                            project.setName("Should not be saved");
                                        });
                                    })
                            )
                            .setAssociatedMode(OrganizationProps.PROJECTS, AssociatedSaveMode.APPEND)
                            .execute(con);
                    return cacheOpRecords.toString();
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME, tb_1__sub.TAX_CODE " +
                                        "from JOINED_CLIENT tb_1_ " +
                                        "inner join JOINED_ORGANIZATION tb_1__sub " +
                                        "on tb_1_.ID = tb_1__sub.ID " +
                                        "where tb_1_.ID = ? and tb_1_.CLIENT_TYPE = ?"
                        );
                        it.variables(201L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.CLIENT_TYPE " +
                                        "from JOINED_CLIENT tb_1_ " +
                                        "where tb_1_.ID = ?"
                        );
                        it.variables(201L);
                    });
                    ctx.value("[]");
                }
        );
        assertEvents();
        Assertions.assertEquals("[]", cacheOpRecords.toString());
    }

    private JSqlClient sqlClientWithCacheOperator() {
        return getSqlClient(builder -> builder.setCaches(cfg ->
                cfg.setCacheFactory(
                        new CacheFactory() {

                            @Override
                            public Cache<?, ?> createObjectCache(ImmutableType type) {
                                return new CacheImpl<>(type);
                            }

                            @Override
                            public Cache<?, ?> createAssociatedIdCache(ImmutableProp prop) {
                                return new CacheImpl<>(prop);
                            }

                            @Override
                            public Cache<?, List<?>> createAssociatedIdListCache(ImmutableProp prop) {
                                return new CacheImpl<>(prop);
                            }
                        }
                ).setCacheOperator(
                        new CacheOperator() {
                            @Override
                            public void delete(UsedCache<Object, ?> cache, Object key, Object reason) {
                                cacheOpRecords.add(cacheName(cache, key));
                                CacheOperator.suspending(() -> cache.delete(key));
                            }

                            @Override
                            public void deleteAll(UsedCache<Object, ?> cache, Collection<Object> keys, Object reason) {
                                for (Object key : keys) {
                                    cacheOpRecords.add(cacheName(cache, key));
                                }
                                CacheOperator.suspending(() -> cache.deleteAll(keys));
                            }
                        }
                )
        ));
    }

    private static String cacheName(UsedCache<Object, ?> cache, Object key) {
        ImmutableType type = cache.type();
        ImmutableProp prop = cache.prop();
        if (prop == null) {
            return type.getJavaClass().getSimpleName() + '-' + key;
        }
        return prop.getDeclaringType().getJavaClass().getSimpleName() + '.' + prop.getName() + '-' + key;
    }
}
