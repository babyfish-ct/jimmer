package org.babyfish.jimmer.sql.cache.redisson;

import org.babyfish.jimmer.sql.cache.spi.AbstractCacheTracker;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.BaseStatusListener;
import org.redisson.api.listener.MessageListener;

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
    protected void publishInvalidationEvent(InvalidateEvent event) {
        topic.publish(
                new InvalidateMessage(
                        trackerId,
                        event
                )
        );
    }

}
