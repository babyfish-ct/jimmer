package org.babyfish.jimmer.sql.cache.redisson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.sql.cache.spi.AbstractCacheTracker;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.BaseStatusListener;
import org.redisson.api.listener.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class RedissonCacheTracker extends AbstractCacheTracker {

    private static final String CHANNEL = "_jimmer_:invalidate";

    private final UUID trackerId = UUID.randomUUID();

    private final RTopic topic;

    public RedissonCacheTracker(RedissonClient redisson) {
        topic = redisson.getTopic(CHANNEL);
        topic.addListener(InvalidateMessage.class, new MessageListener<InvalidateMessage>() {
            @Override
            public void onMessage(CharSequence channel, InvalidateMessage msg) {
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
        String ids;
        try {
            ids = InvalidateMessage.MAPPER.writeValueAsString(event.getIds());
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException(
                    "Cannot serialize the ids of InvalidationEvent", ex
            );
        }
        topic.publish(
                new InvalidateMessage(
                        trackerId,
                        event.getType().toString(),
                        event.getProp() != null ? event.getProp().getName() : null,
                        ids
                )
        );
    }

}
