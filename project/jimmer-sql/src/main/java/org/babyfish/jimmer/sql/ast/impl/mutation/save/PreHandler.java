package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.babyfish.jimmer.sql.DraftPreProcessor;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.TupleImplementor;
import org.babyfish.jimmer.sql.ast.impl.mutation.IdAndKeyFetchers;
import org.babyfish.jimmer.sql.ast.impl.mutation.SaveOptions;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.mutation.LockMode;
import org.babyfish.jimmer.sql.ast.query.MutableQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.IdOnlyFetchType;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImplementor;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.SaveException;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

abstract class PreHandler {

    final SaveContext ctx;

    final DraftPreProcessor<DraftSpi> processor;

    final DraftInterceptor<Object, DraftSpi> interceptor;

    @SuppressWarnings("unchecked")
    PreHandler(SaveContext ctx) {
        this.ctx = ctx;
        this.processor = (DraftPreProcessor<DraftSpi>)
                ctx.options.getSqlClient().getDraftPreProcessor(ctx.path.getType());
        this.interceptor = (DraftInterceptor<Object, DraftSpi>)
                ctx.options.getSqlClient().getDraftInterceptor(ctx.path.getType());
    }

    abstract void add(DraftSpi draft);
}

class InsertPreHandler extends PreHandler {

    InsertPreHandler(SaveContext ctx) {
        super(ctx);
    }

    private ShapedEntityMap<DraftSpi> entityMap = new ShapedEntityMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public void add(DraftSpi draft) {
        if (processor != null) {
            processor.beforeSave(draft);
        }
        if (interceptor != null) {
            interceptor.beforeSave(draft, null);
        }
        entityMap.add(draft);
    }
}

abstract class UpdateablePreHandler extends PreHandler {

    final ImmutableProp idProp;

    final Set<ImmutableProp> keyProps;

    final List<DraftSpi> draftsWithId = new ArrayList<>();

    final List<DraftSpi> draftsWithKey = new ArrayList<>();

    private Map<Object, ImmutableSpi> oldWithId;

    private Map<Object, ImmutableSpi> oldWithKey;

    private Fetcher<ImmutableSpi> oldFetcher;

    UpdateablePreHandler(SaveContext ctx) {
        super(ctx);
        idProp = ctx.path.getType().getIdProp();
        keyProps = ctx.path.getType().getKeyProps();
    }

    @Override
    void add(DraftSpi draft) {
        if (draft.__isLoaded(idProp.getId())) {
            draftsWithId.add(draft);
        } else if (keyProps.isEmpty()) {
            throw new SaveException.NoKeyProps(
                    ctx.path,
                    "Cannot save \"" +
                            ctx.path.getType() +
                            "\" that have no properties decorated by \"@" +
                            Key.class.getName() +
                            "\""
            );
        } else {
            for (ImmutableProp keyProp : keyProps) {
                if (!draft.__isLoaded(keyProp.getId())) {
                    throw new SaveException.NoKeyProp(
                            ctx.path,
                            "Cannot save \"" +
                                    ctx.path.getType() +
                                    "\" with the unloaded key property \"" +
                                    keyProp +
                                    "\""
                    );
                }
            }
            draftsWithKey.add(draft);
        }
    }

    Map<Object, ImmutableSpi> findOldMapByIds() {
        List<Object> ids = new ArrayList<>(draftsWithId.size());
        for (DraftSpi draft : draftsWithKey) {
            ids.add(draft.__get(idProp.getId()));
        }
        List<ImmutableSpi> entities = findOldList((q, t) -> {
            q.where(t.get(idProp).in(ids));
        });
        if (entities.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Object, ImmutableSpi> map = new LinkedHashMap<>((entities.size() * 4 + 2) / 3);
        for (ImmutableSpi entity : entities) {
            map.put(entity.__get(idProp.getId()), entity);
        }
        return map;
    }

    Map<Object, ImmutableSpi> findOldMapByKeys() {
        Collection<ImmutableProp> keyProps = this.keyProps;
        List<Object> keys = new ArrayList<>(draftsWithKey.size());
        for (DraftSpi draft : draftsWithKey) {
            keys.add(keyOf(draft));
        }

        List<ImmutableSpi> entities = findOldList((q, t) -> {
            Expression<Object> keyExpr;
            if (keyProps.size() == 1) {
                keyExpr = t.get(keyProps.iterator().next());
            } else {
                Expression<?>[] arr = new Expression[keyProps.size()];
                int index = 0;
                for (ImmutableProp keyProp : keyProps) {
                    Expression<Object> expr;
                    if (keyProp.isReference(TargetLevel.PERSISTENT)) {
                        expr = t.join(keyProp).get(keyProp.getTargetType().getIdProp());
                    } else {
                        expr = t.get(keyProp);
                    }
                    arr[index++] = expr;
                }
                keyExpr = Tuples.expressionOf(arr);
            }
            q.where(keyExpr.nullableIn(keys));
        });
        if (entities.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Object, ImmutableSpi> map = new LinkedHashMap<>((entities.size() * 4 + 2) / 3);
        for (ImmutableSpi entity : entities) {
            ImmutableSpi conflictEntity = map.put(keyOf(entity), entity);
            if (conflictEntity != null) {
                throw new SaveException.KeyNotUnique(
                        ctx.path,
                        "Key properties " +
                                keyProps +
                                " cannot guarantee uniqueness under that path, " +
                                "do you forget to add unique constraint for that key?"
                );
            }
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private List<ImmutableSpi> findOldList(BiConsumer<MutableQuery, Table<?>> block) {
        if (draftsWithId.isEmpty()) {
            return null;
        }
        ImmutableType type = ctx.path.getType();
        SaveOptions options = ctx.options;
        return Internal.requiresNewDraftContext(draftContext -> {
            List<ImmutableSpi> list = Queries.createQuery(options.getSqlClient(), type, ExecutionPurpose.MUTATE, FilterLevel.DEFAULT, (q, table) -> {
                block.accept(q, table);
                if (ctx.trigger != null) {
                    return q.select((Table<ImmutableSpi>)table);
                }
                return q.select(
                        ((Table<ImmutableSpi>)table).fetch(
                                IdAndKeyFetchers.getFetcher(options.getSqlClient(), type)
                        )
                );
            }).forUpdate(options.getLockMode() == LockMode.PESSIMISTIC).execute(ctx.con);
            return draftContext.resolveList(list);
        });
    }

    @SuppressWarnings("unchecked")
    private Fetcher<ImmutableSpi> oldFetcher() {
        Fetcher<ImmutableSpi> oldFetcher = this.oldFetcher;
        if (oldFetcher == null) {
            ImmutableType type = ctx.path.getType();
            FetcherImplementor<ImmutableSpi> fetcherImplementor =
                    new FetcherImpl<>((Class<ImmutableSpi>)ctx.path.getType().getJavaClass());
            fetcherImplementor = fetcherImplementor.add(idProp.getName());
            for (ImmutableProp keyProp : keyProps) {
                fetcherImplementor = fetcherImplementor.add(keyProp.getName(), IdOnlyFetchType.RAW);
            }
            DraftInterceptor<?, ?> interceptor = ctx.options.getSqlClient().getDraftInterceptor(type);
            if (interceptor != null) {
                Collection<? extends TypedProp<?, ?>> typedProps = interceptor.dependencies();
                if (typedProps != null) {
                    for (TypedProp<?, ?> typedProp : typedProps) {
                        fetcherImplementor = fetcherImplementor.add(typedProp.unwrap().getName(), IdOnlyFetchType.RAW);
                    }
                }
            }
            if (ctx.backReferenceFrozen) {
                fetcherImplementor = fetcherImplementor.add(ctx.backReferenceProp.getName(), IdOnlyFetchType.RAW);
            }
            this.oldFetcher = oldFetcher = fetcherImplementor;
        }
        return oldFetcher;
    }

    private Object keyOf(ImmutableSpi spi) {
        if (keyProps.size() == 1) {
            PropId propId = keyProps.iterator().next().getId();
            return spi.__get(propId);
        }
        Object[] arr = new Object[keyProps.size()];
        int index = 0;
        for (ImmutableProp keyProp : keyProps) {
            Object o = spi.__get(keyProp.getId());
            if (o != null && keyProp.isReference(TargetLevel.PERSISTENT)) {
                o = ((ImmutableSpi)o).__get(keyProp.getTargetType().getIdProp().getId());
            }
            arr[index++] = o;
        }
        return Tuples.valueOf(arr);
    }
}

class UpdatePreHandler extends UpdateablePreHandler {

    UpdatePreHandler(SaveContext ctx) {
        super(ctx);
    }
}
