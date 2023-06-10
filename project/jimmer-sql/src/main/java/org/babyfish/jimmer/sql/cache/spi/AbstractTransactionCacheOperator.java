package org.babyfish.jimmer.sql.cache.spi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.CacheOperator;
import org.babyfish.jimmer.sql.cache.LocatedCache;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class AbstractTransactionCacheOperator implements CacheOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTransactionCacheOperator.class);

    private static final String TABLE_NAME = "JIMMER_TRANS_CACHE_OPERATOR";

    private static final String ID = "ID";

    private static final String TYPE = "TYPE";

    private static final String PROP = "PROP";

    private static final String REASON = "REASON";

    private static final String KEYS = "KEYS";

    private static final String INSERT =
            "insert into " +
                    TABLE_NAME + "(" +
                    TYPE +
                    ", " +
                    PROP +
                    ", " +
                    REASON +
                    ", " +
                    KEYS +
                    ")";

    private static final String SELECT_ID_PREFIX =
            "select " +
                    ID +
                    " from " +
                    TABLE_NAME +
                    " order by " +
                    ID +
                    " limit ";

    private static final String SELECT_PREFIX =
            "select " +
                    ID +
                    ", " +
                    TYPE +
                    ", " +
                    PROP +
                    ", " +
                    REASON +
                    ", " +
                    KEYS +
                    " from " +
                    TABLE_NAME +
                    " where " +
                    ID +
                    " in";

    private final ThreadLocal<Map<MergedKey, Set<Object>>> mergedMapLocal = new ThreadLocal<>();

    private final ObjectMapper mapper;

    private final int batchSize;

    protected AbstractTransactionCacheOperator(ObjectMapper mapper, int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("`batchSize` cannot be less than 1");
        }
        this.mapper = mapper != null ?
                mapper :
                new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .registerModule(new ImmutableModule());
        this.batchSize = batchSize;
    }

    public void begin() {
        mergedMapLocal.set(new LinkedHashMap<>());
    }

    @Override
    public void delete(LocatedCache<Object, ?> cache, Object key, Object reason) {
        keys(cache, reason).add(key);
    }

    @Override
    public void deleteAll(LocatedCache<Object, ?> cache, Collection<Object> keys, Object reason) {
        if (!keys.isEmpty()) {
            keys(cache, reason).addAll(keys);
        }
    }

    private Set<Object> keys(LocatedCache<?, ?> cache, Object reason) {
        if (reason != null && !(reason instanceof String)) {
            throw new IllegalArgumentException(
                    "The cache deletion reason can only be null or string when trigger type is `TRANSACTION_ONLY`"
            );
        }
        return mergedMapLocal
                .get()
                .computeIfAbsent(
                        new MergedKey(cache.getType(), cache.getProp(), (String)reason),
                        it -> new LinkedHashSet<>()
                );
    }

    public void beforeCommit() {
        Map<MergedKey, Set<Object>> map = mergedMapLocal.get();
        jdbc(con -> {
            try {
                try (PreparedStatement stmt = con.prepareStatement(INSERT)) {
                    for (Map.Entry<MergedKey, Set<Object>> e : map.entrySet()) {
                        ImmutableType type = e.getKey().type;
                        ImmutableProp prop = e.getKey().prop;
                        String reason = e.getKey().reason;
                        Set<Object> keys = e.getValue();
                        int keyCountInCount = type != null ? keyCountInRow(type) : keyCountInRow(prop);
                        if (keys.size() <= keyCountInCount) {
                            save(stmt, type, prop, reason, keys);
                        } else {
                            List<Object> list = new ArrayList<>(keyCountInCount);
                            for (Object key : keys) {
                                list.add(key);
                                if (list.size() == keyCountInCount) {
                                    save(stmt, type, prop, reason, list);
                                    list.clear();
                                }
                            }
                            if (!list.isEmpty()) {
                                save(stmt, type, prop, reason, list);
                            }
                        }
                    }
                    stmt.executeBatch();
                }
                mergedMapLocal.remove();
            } catch (SQLException | JsonProcessingException ex) {
                mergedMapLocal.remove();
                throw new ExecutionException("Failed to save delayed cache deletion", ex);
            }
        });
    }

    private void save(
            PreparedStatement stmt,
            ImmutableType type,
            ImmutableProp prop,
            String reason,
            Collection<Object> keys
    ) throws SQLException, JsonProcessingException {
        stmt.setString(1, type != null ? type.toString() : null);
        stmt.setString(2, prop != null ? prop.toString() : null);
        stmt.setString(3, REASON);
        stmt.setString(4, mapper.writeValueAsString(keys));
        stmt.addBatch();
    }

    public void rollback() {
        mergedMapLocal.remove();
    }

    public void flush() {
        String sql = SELECT_ID_PREFIX + batchSize;
        List<Long> ids = new ArrayList<>();
        jdbc(con -> {
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ids.add(rs.getLong(1));
                    }
                }
            } catch (SQLException ex) {
                LOGGER.warn("Failed to flush transaction cache operator", ex);
                return;
            }
        });

        if (ids.isEmpty()) {
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append(SELECT_PREFIX).append('(');
        for (int i = ids.size(); i > 0; --i) {
            if (i > 1) {
                builder.append(", ");
            }
            builder.append('?');
        }
        builder.append(") for update");
        jdbc(con -> {
            try(PreparedStatement stmt = con.prepareStatement(builder.toString())) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {

                    }
                }
            } catch (SQLException ex) {
                LOGGER.warn("Failed to flush transaction cache operator", ex);
            }
        });
    }

    protected int keyCountInRow(ImmutableType type) {
        Class<?> idClass = type.getIdProp().getElementClass();
        if (idClass.isPrimitive()) {
            return 8;
        }
        return 4;
    }

    protected int keyCountInRow(ImmutableProp prop) {
        Class<?> idClass = prop.getDeclaringType().getIdProp().getElementClass();
        if (idClass.isPrimitive()) {
            return 8;
        }
        return 4;
    }

    protected abstract void jdbc(Consumer<Connection> con);

    private static class MergedKey {
        final ImmutableType type;
        final ImmutableProp prop;
        final String reason;

        private MergedKey(ImmutableType type, ImmutableProp prop, String reason) {
            this.type = type;
            this.prop = prop;
            this.reason = reason;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MergedKey mergedKey = (MergedKey) o;
            return Objects.equals(type, mergedKey.type) && Objects.equals(prop, mergedKey.prop) && Objects.equals(reason, mergedKey.reason);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, prop, reason);
        }

        @Override
        public String toString() {
            return "MergedKey{" +
                    "type=" + type +
                    ", prop=" + prop +
                    ", reason=" + reason +
                    '}';
        }
    }
}
