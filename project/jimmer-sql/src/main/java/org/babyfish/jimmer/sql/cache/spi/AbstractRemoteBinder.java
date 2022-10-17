package org.babyfish.jimmer.sql.cache.spi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.ValueSerializer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public abstract class AbstractRemoteBinder<K, V> {

    final ObjectMapper objectMapper;

    private final String keyPrefix;

    private final long minMills;

    private final long maxMillis;

    final ValueSerializer<V> valueSerializer;

    AbstractRemoteBinder(
            ObjectMapper objectMapper,
            ImmutableType type,
            ImmutableProp prop,
            Duration duration,
            int randomPercent
    ) {
        if (objectMapper != null) {
            if (!objectMapper.getRegisteredModuleIds().contains(ImmutableModule.class.getName())) {
                throw new IllegalArgumentException("There is no ImmutableModule in object mapper");
            }
        } else {
            objectMapper = new ObjectMapper()
                    .registerModule(new ImmutableModule())
                    .registerModule(new JavaTimeModule());
        }
        this.objectMapper = objectMapper;
        if ((type == null) == (prop == null)) {
            throw new IllegalArgumentException("The nullity of type and prop cannot be same");
        }
        if (randomPercent < 0 || randomPercent > 99) {
            throw new IllegalArgumentException("randomPercent must between 0 and 99");
        }
        if (type != null) {
            this.keyPrefix = type.getJavaClass().getSimpleName() + '-';
        } else {
            this.keyPrefix = prop.getDeclaringType().getJavaClass().getSimpleName() + '.' + prop.getName() + '-';
        }
        long millis = duration.toMillis();
        minMills = millis - randomPercent * millis / 100;
        maxMillis = millis + randomPercent * millis / 100;
        if (type != null) {
            valueSerializer = new ValueSerializer<>(type, objectMapper);
        } else {
            valueSerializer = new ValueSerializer<>(prop, objectMapper);
        }
    }

    protected long randomMillis() {
        return ThreadLocalRandom.current().nextLong(minMills, maxMillis);
    }

    public final void deleteAll(Collection<K> keys, Object reason) {
        if (reason == null || reason.equals(this.reason())) {
            Collection<String> redisKeys = redisKeys(keys);
            delete(redisKeys);
        }
    }
    protected abstract void delete(Collection<String> keys);

    protected abstract String reason();

    String redisKey(K key) {
        return keyPrefix + key;
    }

    List<String> redisKeys(Collection<K> keys) {
        if (!(keys instanceof Set<?>)) {
            keys = new LinkedHashSet<>(keys);
        }
        return keys.stream().map(this::redisKey).collect(Collectors.toList());
    }
}
