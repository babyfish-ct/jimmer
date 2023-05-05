package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.RecursionStrategy;

import java.sql.Connection;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

class FetcherContext {

    private static final ThreadLocal<FetcherContext> FETCHER_CONTEXT_LOCAL = new ThreadLocal<>();

    private JSqlClient sqlClient;

    private Connection con;

    private FetchingCache cache = new FetchingCache();

    private Map<Field, FetcherTask> taskMap = new LinkedHashMap<>();

    public static void using(
            JSqlClient sqlClient,
            Connection con,
            BiConsumer<FetcherContext, Boolean> block
    ) {
        FetcherContext ctx = FETCHER_CONTEXT_LOCAL.get();
        if (ctx != null) {
            block.accept(ctx, false);
        } else {
            ctx = new FetcherContext(sqlClient, con);
            FETCHER_CONTEXT_LOCAL.set(ctx);
            try {
                block.accept(ctx, true);
            } finally {
                FETCHER_CONTEXT_LOCAL.remove();
            }
        }
    }

    private FetcherContext(JSqlClient sqlClient, Connection con) {
        this.sqlClient = sqlClient;
        this.con = con;
    }

    @SuppressWarnings("unchecked")
    public void add(Fetcher<?> fetcher, DraftSpi draft) {
        for (Field field : fetcher.getFieldMap().values()) {
            ImmutableProp prop = field.getProp();
            if (!prop.getDependencies().isEmpty()) {
                draft.__show(field.getProp().getId(), true);
                continue;
            }
            if (field.isImplicit()) {
                draft.__show(field.getProp().getId(), false);
            }
            if (!field.isSimpleField() ||
                    sqlClient.getFilters().getFilter(field.getProp().getTargetType()) != null) {
                RecursionStrategy<?> recursionStrategy = field.getRecursionStrategy();
                if (recursionStrategy != null &&
                        !((RecursionStrategy<Object>)recursionStrategy).isRecursive(
                                new RecursionStrategy.Args<>(draft, 0)
                        )
                ) {
                    return;
                }
                FetcherTask task = taskMap.computeIfAbsent(field, it ->
                        new FetcherTask(
                                cache,
                                sqlClient,
                                con,
                                field
                        )
                );
                task.add(draft);
            }
        }
    }

    public void addAll(Fetcher<?> fetcher, Collection<DraftSpi> drafts) {
        for (DraftSpi draft : drafts) {
            add(fetcher, draft);
        }
    }

    public void execute() {
        while (!taskMap.isEmpty()) {
            Iterator<Map.Entry<Field, FetcherTask>> itr = taskMap.entrySet().iterator();
            Map.Entry<Field, FetcherTask> e = itr.next();
            if (e.getValue().execute()) {
                taskMap.remove(e.getKey());
            }
        }
    }
}
