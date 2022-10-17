package org.babyfish.jimmer.sql.cache.spi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.chain.SimpleBinder;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class AbstractRemoteValueBinder<K, V> extends AbstractRemoteBinder<K, V> implements SimpleBinder<K, V> {

    AbstractRemoteValueBinder(
            ObjectMapper objectMapper,
            ImmutableType type,
            ImmutableProp prop,
            Duration duration,
            int randomPercent
    ) {
        super(objectMapper, type, prop, duration, randomPercent);
    }

    @Override
    public final Map<K, V> getAll(Collection<K> keys) {
        Collection<String> redisKeys = redisKeys(keys);
        List<byte[]> values = read(redisKeys);
        return valueSerializer.deserialize(keys, values);
    }

    @Override
    public final void setAll(Map<K, V> map) {
        Map<String, byte[]> convertedMap = valueSerializer.serialize(map, this::redisKey);
        write(convertedMap);
    }

    public void deleteAll(Collection<K> keys, Object reason) {
        if (reason == null || reason.equals(this.reason())) {
            Collection<String> redisKeys = redisKeys(keys);
            delete(redisKeys);
        }
    }

    protected abstract List<byte[]> read(Collection<String> keys);

    protected abstract void write(Map<String, byte[]> map);

    protected abstract void delete(Collection<String> keys);
}
