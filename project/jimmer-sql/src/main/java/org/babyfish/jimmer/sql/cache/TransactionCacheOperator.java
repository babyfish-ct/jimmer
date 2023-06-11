package org.babyfish.jimmer.sql.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.spi.AbstractCacheOperator;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

public class TransactionCacheOperator extends AbstractCacheOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionCacheOperator.class);

    public static final String TABLE_NAME = "JIMMER_TRANS_CACHE_OPERATOR";

    private static final String ID = "ID";

    private static final String IMMUTABLE_TYPE = "IMMUTABLE_TYPE";

    private static final String IMMUTABLE_PROP = "IMMUTABLE_PROP";

    private static final String CACHE_KEY = "CACHE_KEY";

    private static final String REASON = "REASON";

    private static final String INSERT =
            "insert into " +
                    TABLE_NAME + "(" +
                    IMMUTABLE_TYPE +
                    ", " +
                    IMMUTABLE_PROP +
                    ", " +
                    CACHE_KEY +
                    ", " +
                    REASON +
                    ") values(?, ?, ?, ?)";

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
                    IMMUTABLE_TYPE +
                    ", " +
                    IMMUTABLE_PROP +
                    ", " +
                    CACHE_KEY +
                    ", " +
                    REASON +
                    " from " +
                    TABLE_NAME +
                    " where " +
                    ID +
                    " in";

    private static final String DELETE_PREFIX =
            "delete from " +
                    TABLE_NAME +
                    " where " +
                    ID +
                    " in";

    private final ObjectMapper mapper;

    private final int batchSize;

    public TransactionCacheOperator() {
        this(null, 32);
    }

    public TransactionCacheOperator(int batchSize) {
        this(null, batchSize);
    }

    public TransactionCacheOperator(ObjectMapper mapper) {
        this(mapper, 32);
    }

    public TransactionCacheOperator(ObjectMapper mapper, int batchSize) {
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

    @Override
    protected void onInitialize(JSqlClientImplementor sqlClient) {
        ConnectionManager connectionManager = sqlClient.getConnectionManager();
        if (connectionManager == null) {
            throw new IllegalArgumentException("The `sqlClient` must support connection manager");
        }
        connectionManager.execute(con -> {
            try {
                try (ResultSet rs = con.getMetaData().getTables(
                        null,
                        null,
                        "JIMMER_TRANS_CACHE_OPERATOR",
                        null
                )) {
                    if (rs.next()) {
                        return null;
                    }
                }
                try (ResultSet rs = con.getMetaData().getTables(
                        null,
                        null,
                        "jimmer_trans_cache_operator",
                        null
                )) {
                    if (rs.next()) {
                        return null;
                    }
                }
                try (Statement statement = con.createStatement()) {
                    statement.execute(sqlClient.getDialect().transCacheOperatorTableDDL());
                }
                return null;
            } catch(SQLException ex) {
                throw new ExecutionException(
                        "Cannot create table `" +
                                TransactionCacheOperator.TABLE_NAME +
                                "`",
                        ex
                );
            }
        });
    }

    @Override
    public void delete(LocatedCache<Object, ?> cache, Object key, Object reason) {
        if (reason != null && !(reason instanceof String)) {
            throw new IllegalArgumentException(
                    "The cache deletion reason can only be null or string when trigger type is `TRANSACTION_ONLY`"
            );
        }
        save(cache.getType(), cache.getProp(), Collections.singleton(key), (String) reason);
    }

    @Override
    public void deleteAll(LocatedCache<Object, ?> cache, Collection<Object> keys, Object reason) {
        if (keys.isEmpty()) {
            return;
        }
        if (reason != null && !(reason instanceof String)) {
            throw new IllegalArgumentException(
                    "The cache deletion reason can only be null or string when trigger type is `TRANSACTION_ONLY`"
            );
        }
        save(cache.getType(), cache.getProp(), keys, (String) reason);
    }

    private void save(
            ImmutableType type,
            ImmutableProp prop,
            Collection<Object> keys,
            String reason
    ) {
        sqlClient().getConnectionManager().execute(con -> {
            try {
                try (PreparedStatement stmt = con.prepareStatement(INSERT)) {
                    for (Object key : keys) {
                        stmt.setString(1, type != null ? type.toString() : null);
                        stmt.setString(2, prop != null ? prop.toString() : null);
                        stmt.setString(3, mapper.writeValueAsString(key));
                        stmt.setString(4, reason);
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }
            } catch (SQLException | JsonProcessingException ex) {
                throw new ExecutionException("Failed to save delayed cache deletion", ex);
            }
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    public void flush() {
        sqlClient().getConnectionManager().execute(con -> {
            flush(con);
            return null;
        });
    }

    private void flush(Connection con) {

        List<Long> ids = selectOperationIds(con);
        if (ids.isEmpty()) {
            return;
        }

        Map<MergedKey, Set<Object>> keyMap = getAndLockOperationKeyMap(ids, con);
        CacheOperator.suspending(() -> {
            executeOperations(keyMap);
        });

        deleteOperations(ids, con);
    }

    private List<Long> selectOperationIds(Connection con) {
        String sql = SELECT_ID_PREFIX + batchSize;
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong(1));
                }
            }
        } catch (SQLException ex) {
            LOGGER.warn("Failed to flush transaction cache operator", ex);
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    private Map<MergedKey, Set<Object>> getAndLockOperationKeyMap(Collection<Long> ids, Connection con) {
        StringBuilder builder = new StringBuilder();
        builder.append(SELECT_PREFIX).append('(');
        for (int i = ids.size(); i > 0; --i) {
            builder.append('?');
            if (i > 1) {
                builder.append(", ");
            }
        }
        builder.append(") for update");
        Map<MergedKey, Set<Object>> keyMap = new LinkedHashMap<>();
        try (PreparedStatement stmt = con.prepareStatement(builder.toString())) {
            int index = 0;
            for (Long id : ids) {
                stmt.setLong(++index, id);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ImmutableType type = typeFromString(rs.getString(2));
                    ImmutableProp prop = propFromString(rs.getString(3));
                    String json = rs.getString(4);
                    Object key = mapper.readValue(
                            json,
                            type != null ?
                                    (Class<Object>)type.getIdProp().getElementClass() :
                                    (Class<Object>)prop.getDeclaringType().getIdProp().getElementClass()
                    );
                    String reason = rs.getString(5);
                    keyMap
                            .computeIfAbsent(new MergedKey(type, prop, reason), it -> new LinkedHashSet<>())
                            .add(key);
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to flush transaction cache operator", ex);
        }
        return keyMap;
    }

    private void executeOperations(Map<MergedKey, Set<Object>> keyMap) {
        for (Map.Entry<MergedKey, Set<Object>> e : keyMap.entrySet()) {
            Cache<Object, ?> cache;
            ImmutableType type = e.getKey().type;
            if (type != null) {
                cache = sqlClient().getCaches().getObjectCache(type);
            } else {
                cache = sqlClient().getCaches().getPropertyCache(e.getKey().prop);
            }
            Object reason = e.getKey().reason;
            Set<Object> keys = e.getValue();
            if (keys.size() == 1) {
                cache.delete(keys.iterator().next(), reason);
            } else {
                cache.deleteAll(keys, reason);
            }
        }
    }

    private void deleteOperations(Collection<Long> ids, Connection con) {
        StringBuilder builder = new StringBuilder();
        builder.append(DELETE_PREFIX).append('(');
        for (int i = ids.size(); i > 0; --i) {
            builder.append('?');
            if (i > 1) {
                builder.append(", ");
            }
        }
        builder.append(')');
        try (PreparedStatement stmt = con.prepareStatement(builder.toString())) {
            int index = 0;
            for (Long id : ids) {
                stmt.setLong(++index, id);
            }
            stmt.executeUpdate();
        } catch (Exception ex) {
            LOGGER.warn("Failed to delete transaction cache operations", ex);
        }
    }

    private static ImmutableType typeFromString(String typeName) throws Exception {
        if (typeName == null) {
            return null;
        }
        Class<?> javaClass = Class.forName(typeName);
        return ImmutableType.get(javaClass);
    }

    private static ImmutableProp propFromString(String propPath) throws Exception {
        if (propPath == null) {
            return null;
        }
        int lastDotIndex = propPath.lastIndexOf('.');
        if (lastDotIndex == -1) {
            throw new IllegalArgumentException(
                    "Illegal property path \"" +
                            propPath +
                            "\""
            );
        }
        return typeFromString(propPath.substring(0, lastDotIndex))
                .getProp(propPath.substring(lastDotIndex + 1));
    }

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
