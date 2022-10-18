package org.babyfish.jimmer.sql.example.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.spi.AbstractRemoteHashBinder;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RedisHashBinder<K, V> extends AbstractRemoteHashBinder<K, V> {

    private final RedisOperations<String, byte[]> operations;

    public RedisHashBinder(
            RedisOperations<String, byte[]> operations,
            ObjectMapper objectMapper,
            ImmutableType type,
            Duration duration
    ) {
        super(objectMapper, type, null, duration, 30);
        this.operations = operations;
    }

    public RedisHashBinder(
            RedisOperations<String, byte[]> operations,
            ObjectMapper objectMapper,
            ImmutableProp prop,
            Duration duration
    ) {
        super(objectMapper, null, prop, duration, 30);
        this.operations = operations;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<byte[]> read(Collection<String> keys, String hashKey) {
        return (List<byte[]>)(List<?>)operations.executePipelined(
                new SessionCallback<Void>() {
                    @Override
                    public <XK, XV> Void execute(RedisOperations<XK, XV> pops) throws DataAccessException {
                        RedisOperations<String, byte[]> pipelinedOps = (RedisOperations<String, byte[]>)pops;
                        for (String key : keys) {
                            pipelinedOps.opsForHash().get(key, hashKey);
                        }
                        return null;
                    }
                }
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void write(Map<String, byte[]> map, String hashKey) {
        operations.executePipelined(
                new SessionCallback<Void>() {
                    @Override
                    public <XK, XV> Void execute(RedisOperations<XK, XV> pops) throws DataAccessException {
                        RedisOperations<String, byte[]> pipelinedOps = (RedisOperations<String, byte[]>)pops;
                        for (Map.Entry<String, byte[]> e : map.entrySet()) {
                            pipelinedOps.opsForHash().put(e.getKey(), hashKey, e.getValue());
                            pipelinedOps.expire(
                                    e.getKey(),
                                    randomMillis(),
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
        operations.delete(keys);
    }

    @Override
    protected String reason() {
        return "redis";
    }
}
