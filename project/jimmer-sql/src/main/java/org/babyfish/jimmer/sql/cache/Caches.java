package org.babyfish.jimmer.sql.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.Triggers;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Caches {

    default <K, V> Cache<K, V> getObjectCache(Class<V> type) {
        return getObjectCache(ImmutableType.get(type));
    }

    <K, V> Cache<K, V> getObjectCache(ImmutableType type);

    default <K, V, ST extends Table<?>> Cache<K, V> getAssociationCache(
            Class<ST> sourceTableType,
            Function<ST, ? extends Table<?>> targetTableGetter
    ) {
        return getAssociationCache(
                ImmutableProps.join(sourceTableType, targetTableGetter)
        );
    }

    <K, V> Cache<K, V> getAssociationCache(ImmutableProp prop);

    void invalidateByBinData(String table, JsonNode jsonNode) throws JsonProcessingException;

    static Caches of(Triggers triggers, Consumer<CacheConfig> block) {
        CacheConfig cfg = new CacheConfig();
        if (block != null) {
            block.accept(cfg);
        }
        return cfg.build(triggers);
    }
}
