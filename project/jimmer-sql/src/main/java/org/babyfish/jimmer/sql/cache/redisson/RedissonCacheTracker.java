package org.babyfish.jimmer.sql.cache.redisson;

import org.babyfish.jimmer.json.JsonCodec;
import org.babyfish.jimmer.sql.cache.spi.AbstractCacheTracker;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.BaseStatusListener;
import org.redisson.api.listener.MessageListener;
import org.redisson.client.codec.Codec;

import java.lang.reflect.Method;
import java.util.UUID;

public class RedissonCacheTracker extends AbstractCacheTracker {

    private static final String CHANNEL = "_jimmer_:invalidate";

    private final UUID trackerId = UUID.randomUUID();

    private final RTopic topic;

    public RedissonCacheTracker(RedissonClient redissonClient) {
        this(topic(redissonClient));
    }

    public RedissonCacheTracker(RedissonClient redissonClient, JsonCodec jsonCodec) {
        this(redissonClient.getTopic(CHANNEL, new InvalidateMessageCodec(jsonCodec)));
    }

    private RedissonCacheTracker(RTopic topic) {
        this.topic = topic;
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

    private static RTopic topic(RedissonClient client) {
        Codec original = client.getConfig().getCodec();
        Codec selected = topicCodec(original);
        return selected == original ? client.getTopic(CHANNEL) : client.getTopic(CHANNEL, selected);
    }

    static Codec topicCodec(Codec codec) {
        for (Class<?> type = codec != null ? codec.getClass() : null; type != null; type = type.getSuperclass()) {
            if (type.getName().equals("org.redisson.codec.TypedJsonJacksonCodec")) {
                // Redisson's typed mapper omits null fields. Older listeners require "propName": null.
                try {
                    Method getter = type.getMethod("getObjectMapper");
                    Class<? extends JsonCodec> adapter = Class.forName(
                            "org.babyfish.jimmer.jackson.v2.JsonCodecV2"
                    ).asSubclass(JsonCodec.class);
                    JsonCodec json = adapter.getConstructor(getter.getReturnType()).newInstance(getter.invoke(codec));
                    return new InvalidateMessageCodec(json);
                } catch (ReflectiveOperationException ex) {
                    throw new IllegalStateException("Install jimmer-json-jackson2 for Redisson's TypedJsonJacksonCodec", ex);
                }
            }
        }
        return codec;
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
