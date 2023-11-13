package org.babyfish.jimmer.sql.cache.transaction;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.CacheEnvironment;
import org.babyfish.jimmer.sql.cache.CacheFactory;
import org.babyfish.jimmer.sql.cache.TransactionCacheOperator;
import org.babyfish.jimmer.sql.common.AbstractTest;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookProps;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.*;

import static org.babyfish.jimmer.sql.common.Constants.*;
import static org.babyfish.jimmer.sql.common.Constants.learningGraphQLId3;

public abstract class AbstractTransactionCacheOperatorTest extends AbstractTest {

    private final Map<ImmutableType, List<Object>> typeKeyMap = new HashMap<>();

    private final Map<ImmutableProp, List<Object>> propKeyMap = new HashMap<>();

    private JSqlClient sqlClient;

    @BeforeEach
    public void initCacheOperator() {

        assume();

        typeKeyMap.clear();
        propKeyMap.clear();
        sqlClient = getSqlClient(cfg -> {
            cfg.setConnectionManager(
                    ConnectionManager.simpleConnectionManager(
                            dataSource()
                    )
            );
            cfg.setDialect(dialect());
            cfg.setTriggerType(TriggerType.TRANSACTION_ONLY);
            cfg.setCacheOperator(new TransactionCacheOperator());
            cfg.setCacheFactory(new CacheFactory() {
                @Override
                public Cache<?, ?> createObjectCache(@NotNull ImmutableType type) {
                    return new ObjectCacheImpl<>(type);
                }

                @Override
                public Cache<?, ?> createAssociatedIdCache(@NotNull ImmutableProp prop) {
                    return new PropCacheImpl<>(prop);
                }

                @Override
                public Cache<?, List<?>> createAssociatedIdListCache(@NotNull ImmutableProp prop) {
                    return new PropCacheImpl<>(prop);
                }
            });
        });
    }

    @Test
    public void test() {

        getSqlClient().getCaches().getObjectCache(Book.class).delete(learningGraphQLId1);
        getSqlClient().getCaches().getObjectCache(Book.class).deleteAll(Arrays.asList(learningGraphQLId2, learningGraphQLId3));
        assertDeletedKeys(Book.class);
        ((TransactionCacheOperator)((JSqlClientImplementor)sqlClient).getCacheOperator()).flush();
        assertDeletedKeys(Book.class, learningGraphQLId1, learningGraphQLId2, learningGraphQLId3);

        getSqlClient().getCaches().getPropertyCache(BookProps.AUTHORS).delete(learningGraphQLId1);
        getSqlClient().getCaches().getPropertyCache(BookProps.AUTHORS).deleteAll(Arrays.asList(learningGraphQLId2, learningGraphQLId3));
        assertDeletedKeys(BookProps.AUTHORS);
        ((TransactionCacheOperator)((JSqlClientImplementor)sqlClient).getCacheOperator()).flush();
        assertDeletedKeys(BookProps.AUTHORS, learningGraphQLId1, learningGraphQLId2, learningGraphQLId3);
    }

    @Override
    public JSqlClient getSqlClient() {
        return sqlClient;
    }

    protected void assume() {}

    protected abstract DataSource dataSource();

    protected abstract Dialect dialect();

    private void assertDeletedKeys(Class<?> type, Object ... keys) {
        List<?> list = typeKeyMap.get(ImmutableType.get(type));
        Assertions.assertEquals(Arrays.asList(keys), list != null ? list : Collections.emptyList());
    }

    private void assertDeletedKeys(TypedProp<?, ?> prop, Object ... keys) {
        List<?> list = propKeyMap.get(prop.unwrap());
        Assertions.assertEquals(Arrays.asList(keys), list != null ? list : Collections.emptyList());
    }

    private class ObjectCacheImpl<K, V> implements Cache<K, V> {

        private final ImmutableType type;

        private ObjectCacheImpl(ImmutableType type) {
            this.type = type;
        }

        @Override
        public @NotNull Map<K, V> getAll(@NotNull Collection<K> keys, @NotNull CacheEnvironment<K, V> env) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAll(@NotNull Collection<K> keys, @Nullable Object reason) {
            typeKeyMap.computeIfAbsent(type, it -> new ArrayList<>()).addAll(keys);
        }
    }

    private class PropCacheImpl<K, V> implements Cache<K, V> {

        private final ImmutableProp prop;

        private PropCacheImpl(ImmutableProp prop) {
            this.prop = prop;
        }

        @Override
        public @NotNull Map<K, V> getAll(@NotNull Collection<K> keys, @NotNull CacheEnvironment<K, V> env) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAll(@NotNull Collection<K> keys, @Nullable Object reason) {
            propKeyMap.computeIfAbsent(prop, it -> new ArrayList<>()).addAll(keys);
        }
    }
}
