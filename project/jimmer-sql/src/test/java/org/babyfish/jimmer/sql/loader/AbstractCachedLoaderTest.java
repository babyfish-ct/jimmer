package org.babyfish.jimmer.sql.loader;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.CacheFactory;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.CacheImpl;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

public class AbstractCachedLoaderTest extends AbstractQueryTest {

    private JSqlClient cachedSqlClient;

    @BeforeEach
    public void initialize() {
        cachedSqlClient = getSqlClient(builder -> {
            builder.setEntityManager(
                    new EntityManager(
                            BookStore.class,
                            Book.class,
                            Author.class,
                            Country.class
                    )
            );
            builder.setCaches(cfg -> {
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

                            @Override
                            public Cache<?, ?> createResolverCache(ImmutableProp prop) {
                                return new CacheImpl<>(prop);
                            }
                        }
                );
            });
        });
    }

    protected JSqlClient getCachedSqlClient() {
        return cachedSqlClient;
    }
}
