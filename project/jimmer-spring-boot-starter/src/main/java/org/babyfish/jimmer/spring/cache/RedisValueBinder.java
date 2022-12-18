package org.babyfish.jimmer.spring.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.spi.AbstractRemoteValueBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class RedisValueBinder<K, V> extends AbstractRemoteValueBinder<K, V> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisValueBinder.class);

    private final RedisOperations<String, byte[]> operations;

    public RedisValueBinder(
            RedisOperations<String, byte[]> operations,
            ObjectMapper objectMapper,
            ImmutableType type,
            Duration duration
    ) {
        super(objectMapper,type, null, duration, 30);
        this.operations = operations;
    }

    public RedisValueBinder(
            RedisOperations<String, byte[]> operations,
            ObjectMapper objectMapper,
            ImmutableProp prop,
            Duration duration
    ) {
        super(objectMapper,null, prop, duration, 30);
        this.operations = operations;
    }

    @Override
    protected List<byte[]> read(Collection<String> keys) {
        return operations.opsForValue().multiGet(keys);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void write(Map<String, byte[]> map) {
        operations.executePipelined(
                new SessionCallback<Void>() {
                    @Override
                    public <XK, XV> Void execute(RedisOperations<XK, XV> pops) throws DataAccessException {
                        RedisOperations<String, byte[]> pipelinedOps = (RedisOperations<String, byte[]>)pops;
                        pipelinedOps.opsForValue().multiSet(map);
                        for (String key : map.keySet()) {
                            pipelinedOps.expire(
                                    key,
                                    nextExpireMillis(),
                                    TimeUnit.MILLISECONDS
                            );
                        }
                        return null;
                    }
                }
        );
    }

    @Override
    protected void delete(Collection<String> keys) {
        LOGGER.info("Delete object data from redis: {}", keys);
        operations.delete(keys);
    }

    @Override
    protected String reason() {
        return "redis";
    }
}

