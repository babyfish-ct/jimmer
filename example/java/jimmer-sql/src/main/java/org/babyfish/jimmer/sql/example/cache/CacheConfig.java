package org.babyfish.jimmer.sql.example.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.CacheFactory;
import org.babyfish.jimmer.sql.cache.chain.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;

@ConditionalOnProperty("spring.redis.host")
@Configuration
public class CacheConfig {

    @Bean
    public RedisTemplate<String, byte[]> rawDataRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, byte[]> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(StringRedisSerializer.UTF_8);
        template.setValueSerializer(
                new RedisSerializer<byte[]>() {
                    @Override
                    public byte[] serialize(byte[] bytes) throws SerializationException {
                        return bytes;
                    }
                    @Override
                    public byte[] deserialize(byte[] bytes) throws SerializationException {
                        return bytes;
                    }
                }
        );
        return template;
    }

    @Bean
    public CacheFactory cacheFactory(RedisTemplate<String, byte[]> redisTemplate) {
        return new CacheFactory() {
            @Override
            public Cache<?, ?> createObjectCache(ImmutableType type) {
                return new ChainCacheBuilder<>()
                        .add(new CaffeineBinder<>(512, Duration.ofSeconds(1)))
                        .add(
                                new RedisBinder<>(redisTemplate, type, Duration.ofHours(10))
                        )
                        .build();
            }

            @Override
            public Cache<?, ?> createAssociatedIdCache(ImmutableProp prop) {
                return new ChainCacheBuilder<>()
                        .add(new CaffeineBinder<>(512, Duration.ofSeconds(1)))
                        .add(
                                new RedisBinder<>(redisTemplate, prop, Duration.ofHours(5))
                        )
                        .build();
            }

            @Override
            public Cache<?, List<?>> createAssociatedIdListCache(ImmutableProp prop) {
                return new ChainCacheBuilder<Object, List<?>>()
                        .add(new CaffeineBinder<>(64, Duration.ofSeconds(1)))
                        .add(
                                new RedisBinder<>(redisTemplate, prop, Duration.ofHours(5))
                        )
                        .build();
            }
        };
    }
}
