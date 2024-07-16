package org.babyfish.jimmer.sql.cache.spi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.CacheTracker;
import org.babyfish.jimmer.sql.cache.ValueSerializer;
import org.babyfish.jimmer.sql.cache.chain.LockableBinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

abstract class AbstractRemoteBinder<K, V> extends AbstractTrackingProducerBinder<K> implements LockableBinder<K, V> {

    final ObjectMapper objectMapper;

    private final String keyPrefix;

    private final long minMills;

    private final long maxMillis;

    final ValueSerializer<V> valueSerializer;

    AbstractRemoteBinder(
            @Nullable ImmutableType type,
            @Nullable ImmutableProp prop,
            @Nullable CacheTracker tracker,
            @Nullable ObjectMapper objectMapper,
            Duration duration,
            int randomPercent
    ) {
        super(type, prop, tracker);
        if (objectMapper != null) {
            if (!objectMapper.getRegisteredModuleIds().contains(ImmutableModule.MODULE_ID)) {
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
            this.keyPrefix = getKeyPrefix(type);
        } else {
            this.keyPrefix = getKeyPrefix(prop);
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

    @Override
    protected final void deleteAllKeys(Collection<K> keys) {
        deleteAllSerializedKeys(serializedKeys(keys));
    }

    protected abstract void deleteAllSerializedKeys(List<String> serializedKeys);

    @Override
    public final @NotNull String keyPrefix() {
        return keyPrefix;
    }

    protected String getKeyPrefix(ImmutableType type) {
        return type.getJavaClass().getSimpleName() + '-';
    }

    protected String getKeyPrefix(ImmutableProp prop) {
        return prop.getDeclaringType().getJavaClass().getSimpleName() + '.' + prop.getName() + '-';
    }

    protected long nextExpireMillis() {
        return ThreadLocalRandom.current().nextLong(minMills, maxMillis);
    }

    String serializedKey(K key) {
        return keyPrefix + key;
    }

    List<String> serializedKeys(Collection<K> keys) {
        if (!(keys instanceof Set<?>)) {
            keys = new LinkedHashSet<>(keys);
        }
        return keys.stream().map(this::serializedKey).collect(Collectors.toList());
    }

    protected static abstract class AbstractBuilder<K, V, B extends AbstractBuilder<K, V, B>> {

        protected final ImmutableType type;
        protected final ImmutableProp prop;
        protected CacheTracker tracker;
        protected ObjectMapper objectMapper;
        protected Duration duration = Duration.ofMinutes(30);
        protected int randomPercent = 30;

        protected AbstractBuilder(ImmutableType type, ImmutableProp prop) {
            this.type = type;
            this.prop = prop;
        }

        @SuppressWarnings("unchecked")
        public B publish(CacheTracker tracker) {
            this.tracker = tracker;
            return (B)this;
        }

        @SuppressWarnings("unchecked")
        public B objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return (B)this;
        }

        @SuppressWarnings("unchecked")
        public B duration(Duration duration) {
            this.duration = duration;
            return (B)this;
        }

        @SuppressWarnings("unchecked")
        public B randomPercent(int randomPercent) {
            this.randomPercent = randomPercent;
            return (B)this;
        }
    }
}
