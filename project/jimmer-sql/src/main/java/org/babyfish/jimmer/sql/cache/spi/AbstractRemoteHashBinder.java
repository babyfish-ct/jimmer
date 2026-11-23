package org.babyfish.jimmer.sql.cache.spi;

import org.babyfish.jimmer.json.JsonCodec;
import org.babyfish.jimmer.json.JsonWriter;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.CacheTracker;
import org.babyfish.jimmer.sql.cache.RemoteKeyPrefixProvider;
import org.babyfish.jimmer.sql.cache.chain.LockableBinder;
import org.babyfish.jimmer.sql.exception.SerializationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;

public abstract class AbstractRemoteHashBinder<K, V>
        extends AbstractRemoteBinder<K, V>
        implements LockableBinder.Parameterized<K, V> {

    protected AbstractRemoteHashBinder(
            @Nullable ImmutableType type,
            @Nullable ImmutableProp prop,
            @Nullable CacheTracker tracker,
            @NotNull JsonCodec jsonCodec,
            @Nullable RemoteKeyPrefixProvider keyPrefixProvider,
            Duration duration,
            int randomPercent
    ) {
        super(type, prop, tracker, jsonCodec, keyPrefixProvider, duration, randomPercent);
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
        String hashKey = hashKey(parameterMap, jsonCodec.writer());
        List<byte[]> values = read(redisKeys, hashKey);
        return valueSerializer.deserialize(keys, values);
    }

    @Override
    public final void setAll(Map<K, V> map, SortedMap<String, Object> parameterMap) {
        Map<String, byte[]> convertedMap = valueSerializer.serialize(map, this::serializedKey);
        String hashKey = hashKey(parameterMap, jsonCodec.writer());
        write(convertedMap, hashKey);
    }

    protected abstract List<byte[]> read(Collection<String> keys, String hashKey);

    protected abstract void write(Map<String, byte[]> map, String hashKey);

    static String hashKey(SortedMap<String, Object> parameterMap, JsonWriter<Object> parameterWriter) {
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("{");
            boolean addComma = false;
            for (Map.Entry<String, Object> e : parameterMap.entrySet()) {
                if (addComma) {
                    builder.append(",");
                } else {
                    addComma = true;
                }
                builder.append("\"");
                builder.append(e.getKey());
                builder.append("\":");
                appendParameter(builder, parameterWriter.writeAsString(e.getValue()));
            }
            builder.append("}");
            return builder.toString();
        } catch (Exception ex) {
            throw new SerializationException(ex);
        }
    }

    // Existing hash fields use literal line and paragraph separators. Gson escapes both in JSON strings.
    private static void appendParameter(StringBuilder builder, String json) {
        if (!json.contains("\\u202")) {
            builder.append(json);
            return;
        }
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\') {
                if (json.startsWith("u2028", i + 1) || json.startsWith("u2029", i + 1)) {
                    builder.append(json.charAt(i + 5) == '8' ? (char) 0x2028 : (char) 0x2029);
                    i += 5;
                    continue;
                }
                // Treat an escaped backslash as a pair, so the literal text "\\u2028" stays distinct.
                builder.append(c);
                c = json.charAt(++i);
            }
            builder.append(c);
        }
    }
}
