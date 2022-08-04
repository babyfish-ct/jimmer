package org.babyfish.jimmer.sql.example.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.CacheFactory;
import org.babyfish.jimmer.sql.example.cache.chain.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.List;

@ConditionalOnProperty("spring.redis.host")
@Configuration
public class CacheConfig {

    private final RedisConnectionFactory connectionFactory;

    public CacheConfig(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Bean
    public CacheFactory cacheFactory() {
        return new CacheFactory() {
            @Override
            public Cache<?, ?> createObjectCache(ImmutableType type) {

                return new ChainCacheBuilder<>()
                        .add(new CaffeineBinder<>(512, Duration.ofSeconds(1)))
                        .add(
                                new RedisBinder<>(
                                        RedisTemplates.objectTemplate(type, connectionFactory),
                                        type.getJavaClass().getSimpleName() + "-",
                                        Duration.ofMinutes(10)
                                )
                        )
                        .build();
            }

            @Override
            public Cache<?, ?> createAssociatedIdCache(ImmutableProp prop) {
                return new ChainCacheBuilder<>()
                        .add(new CaffeineBinder<>(512, Duration.ofSeconds(1)))
                        .add(
                                new RedisBinder<>(
                                        RedisTemplates.idRefTemplate(prop, connectionFactory),
                                        prop.getDeclaringType().getJavaClass().getSimpleName() + '.' + prop.getName() + "-",
                                        Duration.ofMinutes(5)
                                )
                        )
                        .build();
            }

            @Override
            public Cache<?, List<?>> createAssociatedIdListCache(ImmutableProp prop) {
                return new ChainCacheBuilder<Object, List<?>>()
                        .add(new CaffeineBinder<>(64, Duration.ofSeconds(1)))
                        .add(
                                new RedisBinder<>(
                                        RedisTemplates.idListTemplate(prop, connectionFactory),
                                        prop.getDeclaringType().getJavaClass().getSimpleName() + '.' + prop.getName() + "-",
                                        Duration.ofMinutes(5)
                                )
                        )
                        .build();
            }
        };
    }
}
