package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.cache.CacheDisableConfig;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.sql.Connection;
import java.util.*;

class MutationCache {

    final JSqlClientImplementor sqlClientWithoutCache;

    private final boolean pessimisticLockRequired;

    final Map<TypedId, ImmutableSpi> idObjMap = new HashMap<>();

    final Map<TypedKey, ImmutableSpi> keyObjMap = new HashMap<>();

    private final IdentityHashMap<Object, Object> savedMap = new IdentityHashMap<>();

    private NoFilter noFilter;

    public MutationCache(JSqlClientImplementor sqlClient, boolean pessimisticLockRequired) {
        this.sqlClientWithoutCache = sqlClient.caches(CacheDisableConfig::disableAll);
        this.pessimisticLockRequired = pessimisticLockRequired;
    }

    public boolean hasId(ImmutableType type, Object id) {
        return get(new TypedId(type, id)) != null;
    }

    public ImmutableSpi find(ImmutableSpi example, boolean requiresKey) {
        ImmutableType type = example.__type();
        ImmutableProp idProp = type.getIdProp();
        PropId idPropId = idProp.getId();
        if (example.__isLoaded(idPropId)) {
            Object id = example.__get(idPropId);
            if (id != null) {
                return get(new TypedId(type, id));
            }
        }
        TypedKey key = TypedKey.of(example, keyProps(type), requiresKey);
        if (key == null) {
            return null;
        }
        return get(key);
    }

    @SuppressWarnings("unchecked")
    public List<ImmutableSpi> loadByIds(ImmutableType type, Collection<?> ids, Connection con) {
        if (!(ids instanceof Set<?>)) {
            ids = new HashSet<>(ids);
        }
        List<ImmutableSpi> list = new ArrayList<>(ids.size());
        Collection<Object> missedIds = new ArrayList<>();
        for (Object id : ids) {
            ImmutableSpi spi = get(new TypedId(type, id));
            if (spi != null) {
                list.add(spi);
            } else {
                missedIds.add(id);
            }
        }
        if (!missedIds.isEmpty()) {
            List<ImmutableSpi> rows = Internal.requiresNewDraftContext(ctx -> {
                MutableRootQueryImpl<Table<?>> query = new MutableRootQueryImpl<>(sqlClientWithoutCache, type, ExecutionPurpose.MUTATE, filterLevel());
                TableImplementor<?> table = query.getTableImplementor();
                query.where(table.<Expression<Object>>getId().in(missedIds));
                List<ImmutableSpi> spiList = (List<ImmutableSpi>)
                        query.select(table).forUpdate(isPessimisticLockRequired()).execute(con);
                return ctx.resolveList(spiList);
            });
            for (ImmutableSpi row : rows) {
                save(row, false);
                list.add(row);
            }
        }
        return list;
    }

    public ImmutableSpi save(ImmutableSpi spi, boolean forUserSave) {

        ImmutableType type = spi.__type();
        ImmutableProp idProp = type.getIdProp();
        Set<ImmutableProp> keyProps = keyProps(type);

        ImmutableSpi oldSpi = find(spi, false);
        if (oldSpi != null) {

            TypedId oldTypedId = new TypedId(type, oldSpi.__get(idProp.getId()));
            idObjMap.remove(oldTypedId);

            if (keyProps != null && !keyProps.isEmpty()) {
                TypedKey oldKey = TypedKey.of(oldSpi, keyProps, false);
                if (oldKey != null) {
                    keyObjMap.remove(oldKey);
                }
            }

            ImmutableSpi newSpi = spi;
            spi = (ImmutableSpi) Internal.produce(spi.__type(), oldSpi, draft -> {
                for (ImmutableProp prop : type.getProps().values()) {
                    if (prop.isMutable()) {
                        PropId propId = prop.getId();
                        if (newSpi.__isLoaded(propId)) {
                            ((DraftSpi) draft).__set(propId, newSpi.__get(prop.getId()));
                        }
                    }
                }
            });
        }

        TypedId typedId = new TypedId(type, spi.__get(idProp.getId()));
        idObjMap.put(typedId, spi);

        if (keyProps != null && !keyProps.isEmpty()) {
            TypedKey key = TypedKey.of(spi, keyProps, false);
            if (key != null) {
                keyObjMap.put(key, spi);
            }
        }

        if (forUserSave) {
            savedMap.put(spi, null);
        }

        return spi;
    }

    public boolean isSaved(ImmutableSpi spi) {
        return savedMap.containsKey(spi);
    }

    protected Set<ImmutableProp> keyProps(ImmutableType type) {
        return Collections.emptySet();
    }

    public boolean isPessimisticLockRequired() {
        return pessimisticLockRequired;
    }

    public MutationCache withFilter(boolean withFilter) {
        if (withFilter) {
            return this;
        }
        NoFilter noFilter = this.noFilter;
        if (noFilter == null) {
            this.noFilter = noFilter = new NoFilter(this);
        }
        return noFilter;
    }

    FilterLevel filterLevel() {
        return FilterLevel.DEFAULT;
    }

    ImmutableSpi get(TypedId id) {
        return idObjMap.get(id);
    }

    ImmutableSpi get(TypedKey key) {
        return idObjMap.get(key);
    }

    private static class NoFilter extends MutationCache {

        private final MutationCache parent;

        public NoFilter(MutationCache parent) {
            super(parent.sqlClientWithoutCache, parent.pessimisticLockRequired);
            this.parent = parent;
        }

        @Override
        public List<ImmutableSpi> loadByIds(ImmutableType type, Collection<?> ids, Connection con) {
            if (sqlClientWithoutCache.getFilters().getFilter(type) == null) {
                return parent.loadByIds(type, ids, con);
            }
            return super.loadByIds(type, ids, con);
        }

        @Override
        FilterLevel filterLevel() {
            return FilterLevel.IGNORE_ALL;
        }

        @Override
        public MutationCache withFilter(boolean withFilter) {
            return withFilter ? parent : this;
        }

        @Override
        ImmutableSpi get(TypedId id) {
            ImmutableSpi spi = super.get(id);
            if (spi != null) {
                return spi;
            }
            return parent.get(id);
        }

        @Override
        ImmutableSpi get(TypedKey key) {
            ImmutableSpi spi = super.get(key);
            if (spi != null) {
                return spi;
            }
            return parent.get(key);
        }
    }
}
