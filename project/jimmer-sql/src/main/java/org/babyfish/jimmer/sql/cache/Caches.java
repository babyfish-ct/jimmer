package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Caches {

    default <K, V> Cache<K, V> getObjectCache(Class<V> type) {
        return getObjectCache(ImmutableType.get(type));
    }

    <K, V> Cache<K, V> getObjectCache(ImmutableType type);

    default <K, V, ST extends Table<?>> Cache<K, V> getAssociatedIdCache(
            Class<ST> sourceTableType,
            Function<ST, ? extends Table<?>> targetTableGetter
    ) {
        return getAssociatedIdCache(
                ImmutableProps.join(sourceTableType, targetTableGetter)
        );
    }

    <K, V> Cache<K, V> getAssociatedIdCache(
            ImmutableProp prop
    );

    default <K, V, ST extends Table<?>> Cache<K, List<V>> getAssociatedIdListCache(
            Class<ST> sourceTableType,
            Function<ST, Table<?>> targetTableGetter
    ) {
        return getAssociatedIdListCache(
                ImmutableProps.join(sourceTableType, targetTableGetter)
        );
    }

    <K, V> Cache<K, List<V>> getAssociatedIdListCache(
            ImmutableProp prop
    );

    static Caches of(Consumer<CacheConfig> block) {
        CacheConfig cfg = new CacheConfig();
        block.accept(cfg);
        return cfg.build();
    }
}
