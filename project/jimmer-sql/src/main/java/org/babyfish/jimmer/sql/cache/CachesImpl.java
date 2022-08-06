package org.babyfish.jimmer.sql.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.impl.DatabaseIdentifiers;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.Triggers;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.event.binlog.BinLogParser;
import org.babyfish.jimmer.sql.meta.MiddleTable;

import java.util.*;

public class CachesImpl implements Caches {

    private final Triggers triggers;

    private final Map<ImmutableType, LocatedCacheImpl<?, ?>> objectCacheMap;

    private final Map<ImmutableProp, LocatedCacheImpl<?, ?>> associationCacheMap;

    private final Map<String, ImmutableType> tableNameTypeMap;

    private final CacheOperator operator;

    private final BinLogParser binLogParser;

    private final boolean disableAll;

    private final Set<ImmutableType> disabledTypes;

    private final Set<ImmutableProp> disabledProps;

    public CachesImpl(
            Triggers triggers,
            Map<ImmutableType, Cache<?, ?>> objectCacheMap,
            Map<ImmutableProp, Cache<?, ?>> associationCacheMap,
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
        for (ImmutableProp prop : associationCacheMap.keySet()) {
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
        Map<ImmutableProp, LocatedCacheImpl<?, ?>> associationCacheWrapperMap = new LinkedHashMap<>();
        for (Map.Entry<ImmutableProp, Cache<?, ?>> e : associationCacheMap.entrySet()) {
            ImmutableProp prop = e.getKey();
            associationCacheWrapperMap.put(prop, wrapAssociationCache(triggers, e.getValue(), prop));
        }
        this.triggers = triggers;
        this.objectCacheMap = objectCacheWrapperMap;
        this.associationCacheMap = associationCacheWrapperMap;
        this.tableNameTypeMap = tableNameTypeMap;
        this.operator = operator;
        this.binLogParser = binLogParser;
        disableAll = false;
        disabledTypes = Collections.emptySet();
        disabledProps = Collections.emptySet();
    }

    public CachesImpl(
            CachesImpl base,
            CacheDisableConfig cfg
    ) {
        triggers = base.triggers;
        objectCacheMap = base.objectCacheMap;
        associationCacheMap = base.associationCacheMap;
        tableNameTypeMap = base.tableNameTypeMap;
        operator = base.operator;
        binLogParser = base.binLogParser;
        disableAll = cfg.isDisableAll();
        disabledTypes = cfg.getDisabledTypes();
        disabledProps = cfg.getDisabledProps();
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
    public <K, V> LocatedCache<K, V> getAssociationCache(ImmutableProp prop) {
        if (disableAll ||
                disabledProps.contains(prop) ||
                disabledTypes.contains(prop.getTargetType())
        ) {
            return null;
        }
        return LocatedCacheImpl.export((LocatedCache<K, V>)associationCacheMap.get(prop));
    }

    @Override
    public void invalidateByBinData(String tableName, JsonNode oldData, JsonNode newData) {
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
                triggers.fireMiddleTableInsert(associationType.getBaseProp(), idPair.get_1(), idPair.get_2());
            } else {
                AssociationType associationType = (AssociationType) type;
                Tuple2<?, ?> idPair = binLogParser.parseIdPair(associationType, oldData);
                triggers.fireMiddleTableDelete(associationType.getBaseProp(), idPair.get_1(), idPair.get_2());
            }
        } else {
            triggers.fireEntityTableChange(
                    binLogParser.parseEntity(type, oldData),
                    binLogParser.parseEntity(type, newData)
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
                Object id = oldEntity.__get(type.getIdProp().getId());
                if (operator != null) {
                    operator.delete(wrapper, id);
                } else {
                    wrapper.delete(id);
                }
            }
        });
        return wrapper;
    }

    @SuppressWarnings("unchecked")
    private LocatedCacheImpl<?, ?> wrapAssociationCache(
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
        triggers.addAssociationListener(prop, e -> {
            Object id = e.getSourceId();
            if (operator != null) {
                operator.delete(wrapper, id);
            } else {
                wrapper.delete(id);
            }
        });
        return wrapper;
    }
}
