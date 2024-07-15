package org.babyfish.jimmer.sql.cache.redisson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.spi.AbstractCacheTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.BaseStatusListener;
import org.redisson.api.listener.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class RedissonCacheTracker extends AbstractCacheTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedissonCacheTracker.class);

    private static final ObjectMapper MAPPER =
            new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .registerModule(new ImmutableModule());

    private static final String CHANNEL = "_jimmer_:invalidate";

    private final UUID trackerId = UUID.randomUUID();

    private final RTopic topic;

    public RedissonCacheTracker(RedissonClient redisson) {
        topic = redisson.getTopic(CHANNEL);
        topic.addListener(InvalidationMessage.class, new MessageListener<InvalidationMessage>() {
            @Override
            public void onMessage(CharSequence channel, InvalidationMessage msg) {
                if (!msg.trackerId.equals(trackerId)) { // "Eq" means same JVM
                    firer().invalidate(msg.toEvent());
                }
            }
        });
        topic.addListener(new BaseStatusListener() {
            @Override
            public void onSubscribe(String channel) {
                firer().reconnect();
            }
        });
    }

    @Override
    protected void publishInvalidationEvent(InvalidationEvent event) {
        topic.publish(
                new InvalidationMessage(
                        trackerId,
                        event.getType().toString(),
                        event.getProp() != null ? event.getProp().getName() : null,
                        event.getId().toString()
                )
        );
    }

    private static class InvalidationMessage {

        @NotNull
        final UUID trackerId;

        @NotNull
        final String typeName;

        @Nullable
        final String propName;

        @NotNull
        final String id;

        InvalidationMessage(
                @NotNull UUID trackerId,
                @NotNull String typeName,
                @Nullable String propName,
                @NotNull String id
        ) {
            this.trackerId = trackerId;
            this.typeName = typeName;
            this.propName = propName;
            this.id = id;
        }

        InvalidationEvent toEvent() {
            Class<?> javaType;
            try {
                javaType = Class.forName(typeName, true, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(
                        "Cannot resolve the type name \"" +
                                typeName +
                                "\""
                );
            }
            ImmutableType type = ImmutableType.get(javaType);
            Object id;
            try {
                id = MAPPER.readValue(this.id, type.getIdProp().getReturnClass());
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(
                        "Can not parse \"" +
                                "id\" to the type \"" +
                                type.getIdProp().getReturnClass().getName() +
                                "\""
                );
            }
            if (propName != null) {
                ImmutableProp prop = type.getProp(propName);
                return new InvalidationEvent(prop, id);
            }
            return new InvalidationEvent(type, id);
        }
    }
}
