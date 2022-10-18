package org.babyfish.jimmer.sql.cache.spi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.SerializationException;
import org.babyfish.jimmer.sql.cache.chain.SimpleBinder;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;
import java.util.*;

public abstract class AbstractRemoteHashBinder<K, V>
        extends AbstractRemoteBinder<K, V>
        implements SimpleBinder.Parameterized<K, V> {

    protected AbstractRemoteHashBinder(
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
        return getAll(keys, Collections.emptySortedMap());
    }

    @Override
    public final void setAll(Map<K, V> map) {
        setAll(map, Collections.emptySortedMap());
    }

    @Override
    public final Map<K, V> getAll(Collection<K> keys, SortedMap<String, Object> parameterMap) {
        Collection<String> redisKeys = redisKeys(keys);
        String hashKey = hashKey(parameterMap);
        List<byte[]> values = read(redisKeys, hashKey);
        return valueSerializer.deserialize(keys, values);
    }

    @Override
    public final void setAll(Map<K, V> map, SortedMap<String, Object> parameterMap) {
        Map<String, byte[]> convertedMap = valueSerializer.serialize(map, this::redisKey);
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
                    objectMapper.writeValue(writer, e.getValue());
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
