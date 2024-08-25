package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.mutation.LockMode;
import org.babyfish.jimmer.sql.ast.query.MutableQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.SaveException;

import java.util.*;
import java.util.function.BiConsumer;

public class Rows {

    private Rows() {}

    static Map<Object, ImmutableSpi> findMapByIds(
            SaveContext ctx,
            QueryReason queryReason,
            Fetcher<ImmutableSpi> fetcher,
            Collection<DraftSpi> drafts
    ) {
        PropId idPropId = ctx.path.getType().getIdProp().getId();
        Set<Object> ids = new LinkedHashSet<>((drafts.size() * 4 + 2) / 3);
        for (DraftSpi draft : drafts) {
            ids.add(draft.__get(idPropId));
        }
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        List<ImmutableSpi> entities = findRows(ctx, queryReason, fetcher, (q, t) -> {
            q.where(t.getId().in(ids));
        });
        if (entities.isEmpty()) {
            return new HashMap<>();
        }
        Map<Object, ImmutableSpi> map = new LinkedHashMap<>((entities.size() * 4 + 2) / 3);
        for (ImmutableSpi entity : entities) {
            map.put(entity.__get(idPropId), entity);
        }
        return map;
    }

    static Map<Object, ImmutableSpi> findMapByKeys(
            SaveContext ctx,
            QueryReason queryReason,
            Fetcher<ImmutableSpi> fetcher,
            Collection<DraftSpi> drafts
    ) {
        Set<ImmutableProp> keyProps = ctx.options.getKeyProps(ctx.path.getType());
        Set<Object> keys = new LinkedHashSet<>((drafts.size() * 4 + 2) / 3);
        for (DraftSpi draft : drafts) {
            keys.add(Keys.keyOf(draft, keyProps));
        }
        if (keys.isEmpty()) {
            return new HashMap<>();
        }
        List<ImmutableSpi> entities = findRows(ctx, queryReason, fetcher, (q, t) -> {
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
            return new HashMap<>();
        }
        Map<Object, ImmutableSpi> map = new LinkedHashMap<>((entities.size() * 4 + 2) / 3);
        for (ImmutableSpi entity : entities) {
            ImmutableSpi conflictEntity = map.put(Keys.keyOf(entity, keyProps), entity);
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
    private static List<ImmutableSpi> findRows(
            SaveContext ctx,
            QueryReason queryReason,
            Fetcher<ImmutableSpi> fetcher,
            BiConsumer<MutableQuery, Table<?>> block
    ) {
        ImmutableType type = ctx.path.getType();
        SaveOptions options = ctx.options;
        return Internal.requiresNewDraftContext(draftContext -> {
            List<ImmutableSpi> list = Queries.createQuery(
                    options.getSqlClient(),
                    type,
                    ExecutionPurpose.command(queryReason),
                    FilterLevel.IGNORE_USER_FILTERS,
                    (q, table) -> {
                        block.accept(q, table);
                        if (ctx.trigger != null) {
                            return q.select((Table<ImmutableSpi>)table);
                        }
                        return q.select(
                                ((Table<ImmutableSpi>)table).fetch(fetcher)
                        );
                    }
            ).forUpdate(options.getLockMode() == LockMode.PESSIMISTIC).execute(ctx.con);
            return draftContext.resolveList(list);
        });
    }
}
