package org.babyfish.jimmer.sql.cache.spi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.CacheTracker;
import org.babyfish.jimmer.sql.cache.SerializationException;
import org.babyfish.jimmer.sql.cache.chain.LockableBinder;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;
import java.util.*;

public abstract class AbstractRemoteHashBinder<K, V>
        extends AbstractRemoteBinder<K, V>
        implements LockableBinder.Parameterized<K, V> {

    protected AbstractRemoteHashBinder(
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
        return getAll(keys, Collections.emptySortedMap());
    }

    @Override
    public final void setAll(Map<K, V> map) {
        setAll(map, Collections.emptySortedMap());
    }

    @Override
    public final Map<K, V> getAll(Collection<K> keys, SortedMap<String, Object> parameterMap) {
        Collection<String> redisKeys = serializedKeys(keys);
        String hashKey = hashKey(parameterMap);
        List<byte[]> values = read(redisKeys, hashKey);
        return valueSerializer.deserialize(keys, values);
    }

    @Override
    public final void setAll(Map<K, V> map, SortedMap<String, Object> parameterMap) {
        Map<String, byte[]> convertedMap = valueSerializer.serialize(map, this::serializedKey);
        String hashKey = hashKey(parameterMap);
        write(convertedMap, hashKey);
    }

    protected abstract List<byte[]> read(Collection<String> keys, String hashKey);

    protected abstract void write(Map<String, byte[]> map, String hashKey);

    private String hashKey(SortedMap<String, Object> parameterMap) {
        try {
            StringWriter writer = new StringWriter();
            try {
                writer.write("{");
                boolean addComma = false;
                for (Map.Entry<String, Object> e : parameterMap.entrySet()) {
                    if (addComma) {
                        writer.write(",");
                    } else {
                        addComma = true;
                    }
                    writer.write("\"");
                    writer.write(e.getKey());
                    writer.write("\":");
                    writer.write(objectMapper.writeValueAsString(e.getValue()));
                }
                writer.write("}");
                writer.flush();
                return writer.toString();
            } finally {
                writer.close();
            }
        } catch (IOException ex) {
            throw new SerializationException(ex);
        }
    }
}
