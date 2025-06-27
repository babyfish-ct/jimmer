package org.babyfish.jimmer.sql.cache.redis.spring;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * framework-related classes should not be included in the jimmer-sql module.<br>
 * <br>
 * Redis-related caching should be implemented through framework-specific extensions.
 * @see "org.babyfish.jimmer.spring.cache.RedisCaches(Provided by jimmer-spring-boot-starter)"
 */
@Deprecated
public class RedisCaches {

    private static final RedisSerializer<byte[]> NOP_SERIALIZER =
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

    private RedisCaches() {}

    public static RedisTemplate<String, byte[]> cacheRedisTemplate(
            RedisConnectionFactory connectionFactory
    ) {
        RedisTemplate<String, byte[]> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(StringRedisSerializer.UTF_8);
        template.setValueSerializer(NOP_SERIALIZER);
        template.setHashKeySerializer(StringRedisSerializer.UTF_8);
        template.setHashValueSerializer(NOP_SERIALIZER);
        template.afterPropertiesSet();
        return template;
    }
}

