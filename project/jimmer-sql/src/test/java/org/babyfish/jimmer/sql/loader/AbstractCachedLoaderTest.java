package org.babyfish.jimmer.sql.loader;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.CacheFactory;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.CacheImpl;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

public class AbstractCachedLoaderTest extends AbstractQueryTest {

    private SqlClient cachedSqlClient;

    @BeforeEach
    public void initialize() {
        cachedSqlClient = getSqlClient(builder -> {
            builder.setCaches(cfg -> {
                cfg.setCacheFactory(new CacheFactory() {
                    @Override
                    public Cache<?, ?> createObjectCache(ImmutableType type) {
                        return new CacheImpl<>();
                    }

                    @Override
                    public Cache<?, ?> createAssociatedIdCache(ImmutableProp type) {
                        return new CacheImpl<>();
                    }

                    @Override
                    public Cache<?, List<?>> createAssociatedIdListCache(ImmutableProp type) {
                        return new CacheImpl<>();
                    }
                });
            });
        });
    }

    protected SqlClient getCachedSqlClient() {
        return cachedSqlClient;
    }
}
