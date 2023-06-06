package org.babyfish.jimmer.spring.cache;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;

public class RedisCaches {

    private RedisCaches() {}

    public static RedisTemplate<String, byte[]> rawValueRedisTemplate(
            RedisConnectionFactory connectionFactory
    ) {
        RedisSerializer<byte[]> nopSerializer =
                new RedisSerializer<byte[]>() {
                    @Override
                    public byte[] serialize(byte[] t) throws SerializationException {
                        return t;
                    }
                    @Override
                    public byte[] deserialize(byte[] bytes) throws SerializationException {
                        return bytes;
                    }
                };

        RedisTemplate<String, byte[]> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(StringRedisSerializer.UTF_8);
        template.setValueSerializer(nopSerializer);
        template.setHashKeySerializer(StringRedisSerializer.UTF_8);
        template.setHashValueSerializer(nopSerializer);
        return template;
    }
}
