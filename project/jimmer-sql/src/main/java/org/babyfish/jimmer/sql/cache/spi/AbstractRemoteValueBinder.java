package org.babyfish.jimmer.sql.cache.spi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.CacheTracker;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class AbstractRemoteValueBinder<K, V>
        extends AbstractRemoteBinder<K, V> {

    protected AbstractRemoteValueBinder(
            @Nullable ImmutableType type,
            @Nullable ImmutableProp prop,
            @Nullable CacheTracker tracker,
            @Nullable ObjectMapper objectMapper,
            Duration duration,
            int randomPercent
    ) {
        super(type, prop, tracker, objectMapper, duration, randomPercent);
    }

    @Override
    public final Map<K, V> getAll(Collection<K> keys) {
        Collection<String> redisKeys = remoteKeys(keys);
        List<byte[]> values = read(redisKeys);
        return valueSerializer.deserialize(keys, values);
    }

    @Override
    public final void setAll(Map<K, V> map) {
        Map<String, byte[]> convertedMap = valueSerializer.serialize(map, this::redisKey);
        write(convertedMap);
    }

    protected abstract List<byte[]> read(Collection<String> keys);

    protected abstract void write(Map<String, byte[]> map);
}
