package org.babyfish.jimmer.sql.example.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.SimpleType;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

public class RedisTemplates {

    private static final ObjectMapper IMMUTABLE_MAPPER =
            new ObjectMapper()
                    .registerModule(new ImmutableModule());

    private RedisTemplates() {}

    @SuppressWarnings("unchecked")
    public static <V> RedisTemplate<String, V> objectTemplate(
            ImmutableType type,
            RedisConnectionFactory connectionFactory
    ) {
        RedisTemplate<String, V> template = new RedisTemplate<>();
        template.setKeySerializer(new StringRedisSerializer());
        Jackson2JsonRedisSerializer<V> valueSerializer =
                new Jackson2JsonRedisSerializer<>((Class<V>) type.getJavaClass());
        valueSerializer.setObjectMapper(IMMUTABLE_MAPPER);
        template.setValueSerializer(valueSerializer);
        template.setConnectionFactory(connectionFactory);
        template.afterPropertiesSet();
        return template;
    }

    @SuppressWarnings("unchecked")
    public static <ID> RedisTemplate<String, ID> idRefTemplate(
            ImmutableProp prop,
            RedisConnectionFactory connectionFactory
    ) {
        RedisTemplate<String, ID> template = new RedisTemplate<>();
        template.setKeySerializer(new StringRedisSerializer());
        Jackson2JsonRedisSerializer<ID> valueSerializer =
                new Jackson2JsonRedisSerializer<>(
                        (Class<ID>)prop.getTargetType().getIdProp().getElementClass()
                );
        template.setValueSerializer(valueSerializer);
        template.setConnectionFactory(connectionFactory);
        template.afterPropertiesSet();
        return template;
    }

    public static <ID> RedisTemplate<String, List<ID>> idListTemplate(
            ImmutableProp prop,
            RedisConnectionFactory connectionFactory
    ) {
        RedisTemplate<String, List<ID>> template = new RedisTemplate<>();
        template.setKeySerializer(new StringRedisSerializer());
        Jackson2JsonRedisSerializer<List<ID>> valueSerializer =
                new Jackson2JsonRedisSerializer<>(
                    CollectionType.construct(
                            List.class,
                            null,
                            null,
                            null,
                            SimpleType.constructUnsafe(
                                    prop.getTargetType().getIdProp().getElementClass()
                            )
                    )
                );
        template.setValueSerializer(valueSerializer);
        template.setConnectionFactory(connectionFactory);
        template.afterPropertiesSet();
        return template;
    }
}
