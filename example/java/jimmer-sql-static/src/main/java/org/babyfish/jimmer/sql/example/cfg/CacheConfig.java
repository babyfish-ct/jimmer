package org.babyfish.jimmer.sql.example.cfg;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.spring.cache.CaffeineBinder;
import org.babyfish.jimmer.spring.cache.RedisHashBinder;
import org.babyfish.jimmer.spring.cache.RedisValueBinder;
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

        RedisSerializer<byte[]> nopSerializer =
                new RedisSerializer<byte[]>() {
                    @Override
                    public byte[] serialize(byte[] bytes) throws SerializationException {
                        return bytes;
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

    @Bean
    public CacheFactory cacheFactory(
            RedisTemplate<String, byte[]> redisTemplate,
            ObjectMapper objectMapper
    ) {
        return new CacheFactory() {

            // Id -> Object
            @Override
            public Cache<?, ?> createObjectCache(ImmutableType type) {
                return new ChainCacheBuilder<>()
                        .add(new CaffeineBinder<>(512, Duration.ofSeconds(1)))
                        .add(new RedisValueBinder<>(redisTemplate, objectMapper, type, Duration.ofMinutes(10)))
                        .build();
            }

            // Id -> TargetId, for one-to-one/many-to-one
            @Override
            public Cache<?, ?> createAssociatedIdCache(ImmutableProp prop) {
                return new ChainCacheBuilder<>()
                        .add(new RedisHashBinder<>(redisTemplate, objectMapper, prop, Duration.ofMinutes(5)))
                        .build();
            }

            // Id -> TargetId list, for one-to-many/many-to-many
            @Override
            public Cache<?, List<?>> createAssociatedIdListCache(ImmutableProp prop) {
                return new ChainCacheBuilder<Object, List<?>>()
                        .add(new RedisHashBinder<>(redisTemplate, objectMapper, prop, Duration.ofMinutes(5)))
                        .build();
            }

            // Id -> computed value, for transient properties with resolver
            @Override
            public Cache<?, ?> createResolverCache(ImmutableProp prop) {
                return new ChainCacheBuilder<Object, List<?>>()
                        .add(new RedisHashBinder<>(redisTemplate, objectMapper, prop, Duration.ofHours(1)))
                        .build();
            }
        };
    }
}
