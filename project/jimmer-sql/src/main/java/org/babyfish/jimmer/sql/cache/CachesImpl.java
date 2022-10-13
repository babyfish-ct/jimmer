package org.babyfish.jimmer.sql.cache;

import com.fasterxml.jackson.databind.JsonNode;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.impl.DatabaseIdentifiers;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.babyfish.jimmer.sql.Triggers;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.event.binlog.BinLogParser;
import org.babyfish.jimmer.sql.meta.MiddleTable;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;

import java.util.*;
import java.util.function.Consumer;

public class CachesImpl implements Caches {

    private final Triggers triggers;

    private final Map<ImmutableType, LocatedCacheImpl<?, ?>> objectCacheMap;

    private final Map<ImmutableProp, LocatedCacheImpl<?, ?>> propCacheMap;

    private final Map<String, ImmutableType> tableNameTypeMap;

    private final CacheOperator operator;
    
    private final BinLogParser binLogParser;

    private final boolean disableAll;

    private final Set<ImmutableType> disabledTypes;

    private final Set<ImmutableProp> disabledProps;

    public CachesImpl(
            Triggers triggers,
            Map<ImmutableType, Cache<?, ?>> objectCacheMap,
            Map<ImmutableProp, Cache<?, ?>> propCacheMap,
            CacheOperator operator,
            BinLogParser binLogParser
    ) {
        Map<String, ImmutableType> tableNameTypeMap = new HashMap<>();
        for (ImmutableType type : objectCacheMap.keySet()) {
            String tableName = DatabaseIdentifiers.standardIdentifier(type.getTableName());
            ImmutableType oldType = tableNameTypeMap.put(tableName, type);
            if (oldType != null) {
                throw new IllegalArgumentException(
                        "Illegal mapping, the table \"" +
                                tableName +
                                "\" is shared by both \"" +
                                oldType +
                                "\" and \"" +
                                type +
                                "\""
                );
            }
        }
        for (ImmutableProp prop : propCacheMap.keySet()) {
            if (prop.getMappedBy() != null) {
                prop = prop.getMappedBy();
            }
            if (prop.getStorage() instanceof MiddleTable) {
                AssociationType type = AssociationType.of(prop);
                String tableName = DatabaseIdentifiers.standardIdentifier(type.getTableName());
                ImmutableType oldType = tableNameTypeMap.put(tableName, type);
                if (oldType != null && oldType != type) {
                    throw new IllegalArgumentException(
                            "Illegal mapping, the table \"" +
                                    tableName +
                                    "\" is shared by both \"" +
                                    oldType +
                                    "\" and \"" +
                                    type +
                                    "\""
                    );
                }
            }
        }
        Map<ImmutableType, LocatedCacheImpl<?, ?>> objectCacheWrapperMap = new LinkedHashMap<>();
        for (Map.Entry<ImmutableType, Cache<?, ?>> e : objectCacheMap.entrySet()) {
            ImmutableType type = e.getKey();
            objectCacheWrapperMap.put(type, wrapObjectCache(triggers, e.getValue(), type));
        }
        Map<ImmutableProp, LocatedCacheImpl<?, ?>> propCacheWrapperMap = new LinkedHashMap<>();
        for (Map.Entry<ImmutableProp, Cache<?, ?>> e : propCacheMap.entrySet()) {
            ImmutableProp prop = e.getKey();
            propCacheWrapperMap.put(prop, wrapPropCache(triggers, e.getValue(), prop));
        }
        this.triggers = triggers;
        this.objectCacheMap = objectCacheWrapperMap;
        this.propCacheMap = propCacheWrapperMap;
        this.tableNameTypeMap = tableNameTypeMap;
        this.operator = operator;
        this.binLogParser = binLogParser;
        this.disableAll = false;
        this.disabledTypes = Collections.emptySet();
        this.disabledProps = Collections.emptySet();
    }

    public CachesImpl(
            CachesImpl base,
            CacheDisableConfig cfg
    ) {
        triggers = base.triggers;
        objectCacheMap = base.objectCacheMap;
        propCacheMap = base.propCacheMap;
        tableNameTypeMap = base.tableNameTypeMap;
        operator = base.operator;
        binLogParser = base.binLogParser;
        disableAll = cfg.isDisableAll();
        disabledTypes = cfg.getDisabledTypes();
        disabledProps = cfg.getDisabledProps();
    }

    public Map<ImmutableType, LocatedCache<?, ?>> getObjectCacheMap() {
        return Collections.unmodifiableMap(objectCacheMap);
    }

    public Map<ImmutableProp, LocatedCache<?, ?>> getPropCacheMap() {
        return Collections.unmodifiableMap(propCacheMap);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K, V> LocatedCache<K, V> getObjectCache(ImmutableType type) {
        if (disableAll || disabledTypes.contains(type)) {
            return null;
        }
        return LocatedCacheImpl.export((LocatedCache<K, V>)objectCacheMap.get(type));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K, V> LocatedCache<K, V> getPropertyCache(ImmutableProp prop) {
        if (disableAll ||
                disabledProps.contains(prop) ||
                disabledTypes.contains(prop.getTargetType())
        ) {
            return null;
        }
        return LocatedCacheImpl.export((LocatedCache<K, V>) propCacheMap.get(prop));
    }

    @Override
    public boolean isAffectedBy(String tableName) {
        return tableNameTypeMap.containsKey(
                DatabaseIdentifiers.databaseIdentifier(tableName)
        );
    }

    @Override
    public void invalidateByBinLog(String tableName, JsonNode oldData, JsonNode newData, Object reason) {
        boolean isOldNull = oldData == null || oldData.isNull();
        boolean isNewNull = newData == null || newData.isNull();
        if (isOldNull && isNewNull) {
            return;
        }
        ImmutableType type = tableNameTypeMap.get(
                DatabaseIdentifiers.databaseIdentifier(tableName)
        );
        if (type == null) {
            throw new IllegalArgumentException(
                    "Illegal table name \"" +
                            tableName +
                            "\", it is not managed by cache"
            );
        }
        if (type instanceof AssociationType) {
            if (isOldNull) {
                AssociationType associationType = (AssociationType) type;
                Tuple2<?, ?> idPair = binLogParser.parseIdPair(associationType, newData);
                triggers.fireMiddleTableInsert(
                        associationType.getBaseProp(),
                        idPair.get_1(),
                        idPair.get_2(),
                        reason
                );
            } else {
                AssociationType associationType = (AssociationType) type;
                Tuple2<?, ?> idPair = binLogParser.parseIdPair(associationType, oldData);
                triggers.fireMiddleTableDelete(
                        associationType.getBaseProp(),
                        idPair.get_1(),
                        idPair.get_2(),
                        reason
                );
            }
        } else {
            triggers.fireEntityTableChange(
                    binLogParser.parseEntity(type, oldData),
                    binLogParser.parseEntity(type, newData),
                    reason
            );
        }
    }

    @SuppressWarnings("unchecked")
    private LocatedCacheImpl<?, ?> wrapObjectCache(
            Triggers triggers,
            Cache<?, ?> cache,
            ImmutableType type
    ) {
        if (cache == null) {
            return null;
        }
        LocatedCacheImpl<Object, Object> wrapper = LocatedCacheImpl.wrap(
                (Cache<Object, Object>) cache,
                type
        );
        triggers.addEntityListener(type, e -> {
            ImmutableSpi oldEntity = e.getOldEntity();
            if (oldEntity != null) {
                Object id = e.getId();
                if (operator != null) {
                    operator.delete(wrapper, id, e.getReason());
                } else {
                    wrapper.delete(id, e.getReason());
                }
            }
        });
        return wrapper;
    }

    @SuppressWarnings("unchecked")
    private LocatedCacheImpl<?, ?> wrapPropCache(
            Triggers triggers,
            Cache<?, ?> cache,
            ImmutableProp prop
    ) {
        if (cache == null) {
            return null;
        }
        LocatedCacheImpl<Object, Object> wrapper = LocatedCacheImpl.wrap(
                (Cache<Object, Object>) cache,
                prop
        );
        if (prop.isAssociation(TargetLevel.ENTITY)) {
            triggers.addAssociationListener(prop, e -> {
                Object id = e.getSourceId();
                if (operator != null) {
                    operator.delete(wrapper, id, e.getReason());
                } else {
                    wrapper.delete(id, e.getReason());
                }
            });
        }
        return wrapper;
    }

    public static Caches of(
            Triggers triggers,
            Map<Class<?>, ScalarProvider<?, ?>> scalarProviderMap,
            EntityManager entityManager,
            Consumer<CacheConfig> block
    ) {
        CacheConfig cfg = new CacheConfig(entityManager);
        if (block != null) {
            block.accept(cfg);
        }
        return cfg.build(triggers, scalarProviderMap);
    }
}
