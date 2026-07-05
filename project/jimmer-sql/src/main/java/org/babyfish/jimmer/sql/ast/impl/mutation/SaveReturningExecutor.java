package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

class SaveReturningExecutor {

    private SaveReturningExecutor() {}

    static int[] executeInsert(SaveReturning returning, EntityCollection<DraftSpi> entities) {
        if (entities.isEmpty()) {
            return new int[0];
        }
        JSqlClientImplementor sqlClient = returning.ctx.options.getSqlClient();
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        sqlClient.getDialect().insertReturning(new SaveReturningInsertContext(returning, builder, entities));
        return execute(returning, builder, entities);
    }

    static int[] executeUpdate(SaveReturning returning, EntityCollection<DraftSpi> entities) {
        if (entities.isEmpty()) {
            return new int[0];
        }
        JSqlClientImplementor sqlClient = returning.ctx.options.getSqlClient();
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        sqlClient.getDialect().updateByValues(new SaveReturningUpdateContext(returning, builder, entities));
        return execute(returning, builder, entities);
    }

    static int[] executeUpsert(SaveReturning returning, EntityCollection<DraftSpi> entities) {
        if (entities.isEmpty()) {
            return new int[0];
        }
        JSqlClientImplementor sqlClient = returning.ctx.options.getSqlClient();
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        sqlClient.getDialect().upsert(new SaveReturningUpsertContext(returning, builder, entities));
        return execute(returning, builder, entities);
    }

    private static int[] execute(
            SaveReturning returning,
            SqlBuilder builder,
            EntityCollection<DraftSpi> entities
    ) {
        JSqlClientImplementor sqlClient = returning.ctx.options.getSqlClient();
        Tuple3<String, List<Object>, List<Integer>> tuple = builder.build();
        return sqlClient.getExecutor().execute(
                new Executor.Args<>(
                        sqlClient,
                        returning.ctx.con,
                        tuple.get_1(),
                        tuple.get_2(),
                        tuple.get_3(),
                        ExecutionPurpose.MUTATE,
                        returning.ctx.options.getExceptionTranslator(),
                        null,
                        (stmt, args) -> {
                            stmt.execute();
                            try (ResultSet rs = stmt.getResultSet()) {
                                return read(returning, rs, entities);
                            }
                        }
                )
        );
    }

    private static int[] read(
            SaveReturning returning,
            ResultSet rs,
            EntityCollection<DraftSpi> entities
    ) throws SQLException {
        int[] rowCounts = new int[entities.size()];
        Reader.Context readerContext = new Reader.Context(null, returning.ctx.options.getSqlClient());
        SaveShapeMatcher shapeMatcher = new SaveShapeMatcher(returning.ctx.options::getUpsertMask);
        switch (returning.matchMode) {
            case ORDER:
                readByOrder(returning, rs, entities, rowCounts, readerContext, shapeMatcher);
                break;
            case ID:
            case KEY:
                readByKey(returning, rs, entities, rowCounts, readerContext, shapeMatcher);
                break;
            default:
                throw new AssertionError("Internal bug: Unexpected match mode: " + returning.matchMode);
        }
        if (returning.kind != SaveReturningKind.INSERT) {
            markNotAcceptedRows(returning, entities, rowCounts);
        }
        if (returning.upsert != null && returning.upsert.ignoreUpdate) {
            unloadAssociationsOfNotAcceptedRows(returning, entities, rowCounts);
        }
        return rowCounts;
    }

    private static void readByOrder(
            SaveReturning returning,
            ResultSet rs,
            EntityCollection<DraftSpi> entities,
            int[] rowCounts,
            Reader.Context readerContext,
            SaveShapeMatcher shapeMatcher
    ) throws SQLException {
        Iterator<EntityCollection.Item<DraftSpi>> itr = entities.items().iterator();
        int index = 0;
        while (rs.next()) {
            if (!itr.hasNext()) {
                throw unexpectedReturningRow(returning, null);
            }
            Object[] values = readValues(returning, rs, readerContext);
            rowCounts[index++] = 1;
            EntityCollection.Item<DraftSpi> item = itr.next();
            if (!isRejectedByLogicalDeleted(returning, values)) {
                apply(returning, item, values, shapeMatcher);
            }
        }
        if (itr.hasNext() && returning.kind == SaveReturningKind.INSERT) {
            throw new ExecutionException(
                    "The insert-returning statement for \"" +
                            returning.shape.getType() +
                            "\" returned fewer rows than inserted"
            );
        }
    }

    private static void readByKey(
            SaveReturning returning,
            ResultSet rs,
            EntityCollection<DraftSpi> entities,
            int[] rowCounts,
            Reader.Context readerContext,
            SaveShapeMatcher shapeMatcher
    ) throws SQLException {
        Map<List<Object>, EntityCollection.Item<DraftSpi>> itemMap = new LinkedHashMap<>();
        Map<List<Object>, Integer> indexMap = new LinkedHashMap<>();
        int index = 0;
        for (EntityCollection.Item<DraftSpi> item : entities.items()) {
            List<Object> key = keyOf(item.getEntity(), returning.matchGetters);
            itemMap.put(key, item);
            indexMap.put(key, index++);
        }
        while (rs.next()) {
            Object[] values = readValues(returning, rs, readerContext);
            List<Object> key = keyOf(returning, values);
            EntityCollection.Item<DraftSpi> item = itemMap.get(key);
            if (item == null) {
                throw unexpectedReturningRow(returning, key);
            }
            rowCounts[indexMap.get(key)] = 1;
            if (!isRejectedByLogicalDeleted(returning, values)) {
                apply(returning, item, values, shapeMatcher);
            }
        }
    }

    private static Object[] readValues(
            SaveReturning returning,
            ResultSet rs,
            Reader.Context readerContext
    ) throws SQLException {
        JSqlClientImplementor sqlClient = returning.ctx.options.getSqlClient();
        Object[] values = new Object[returning.returningProps.size()];
        readerContext.resetCol();
        for (int i = 0; i < returning.returningProps.size(); i++) {
            values[i] = sqlClient.getReader(returning.returningProps.get(i)).read(rs, readerContext);
        }
        return values;
    }

    private static List<Object> keyOf(DraftSpi draft, List<PropertyGetter> getters) {
        List<Object> key = new ArrayList<>(getters.size());
        for (PropertyGetter getter : getters) {
            key.add(getter.get(draft));
        }
        return key;
    }

    private static List<Object> keyOf(SaveReturning returning, Object[] values) {
        List<Object> key = new ArrayList<>(returning.matchIndexes.size());
        for (Integer index : returning.matchIndexes) {
            key.add(values[index]);
        }
        return key;
    }

    private static ExecutionException unexpectedReturningRow(SaveReturning returning, @Nullable Object key) {
        return new ExecutionException(
                "The " +
                        returning.kind.name().toLowerCase(Locale.ROOT) +
                        "-returning statement returned unexpected row" +
                        (key != null ? " with key \"" + key + "\"" : "") +
                        " for \"" +
                        returning.shape.getType() +
                        "\""
        );
    }

    private static boolean isRejectedByLogicalDeleted(SaveReturning returning, Object[] values) {
        if (returning.logicalDeletedInfo == null || returning.logicalDeletedIndex == -1) {
            return false;
        }
        boolean deleted = returning.logicalDeletedInfo.isDeleted(values[returning.logicalDeletedIndex]);
        switch (returning.logicalDeletedBehavior) {
            case REVERSED:
                return !deleted;
            case DEFAULT:
                return deleted;
            default:
                return false;
        }
    }

    private static void apply(
            SaveReturning returning,
            EntityCollection.Item<DraftSpi> item,
            Object[] values,
            SaveShapeMatcher shapeMatcher
    ) {
        apply(returning, item.getEntity(), values, shapeMatcher);
        for (DraftSpi draft : item.getOriginalEntities()) {
            if (draft != item.getEntity()) {
                apply(returning, draft, values, shapeMatcher);
            }
        }
    }

    private static void apply(
            SaveReturning returning,
            DraftSpi draft,
            Object[] values,
            SaveShapeMatcher shapeMatcher
    ) {
        for (int i = 0; i < returning.returningProps.size(); i++) {
            ImmutableProp prop = returning.returningProps.get(i);
            PropId propId = prop.getId();
            draft.__set(propId, values[i]);
            draft.__show(propId, true);
        }
        returning.ctx.markSaveReturningApplied(draft);
        shapeMatcher.isMatched(draft, returning.ctx.fetcher, true);
    }

    private static void unloadAssociationsOfNotAcceptedRows(
            SaveReturning returning,
            EntityCollection<DraftSpi> entities,
            int[] rowCounts
    ) {
        List<PropId> unloadedPropIds = new ArrayList<>();
        for (ImmutableProp prop : returning.ctx.path.getType().getProps().values()) {
            if (!prop.isMiddleTableDefinition() && prop.isAssociation(TargetLevel.PERSISTENT)) {
                unloadedPropIds.add(prop.getId());
            }
        }
        if (unloadedPropIds.isEmpty()) {
            return;
        }
        int index = 0;
        for (EntityCollection.Item<DraftSpi> item : entities.items()) {
            if (index < rowCounts.length && rowCounts[index++] != 0) {
                continue;
            }
            for (PropId propId : unloadedPropIds) {
                for (DraftSpi draft : item.getOriginalEntities()) {
                    draft.__unload(propId);
                }
            }
        }
    }

    private static void markNotAcceptedRows(
            SaveReturning returning,
            EntityCollection<DraftSpi> entities,
            int[] rowCounts
    ) {
        int index = 0;
        for (EntityCollection.Item<DraftSpi> item : entities.items()) {
            if (index < rowCounts.length && rowCounts[index++] != 0) {
                continue;
            }
            returning.ctx.markSaveReturningNotAccepted(item.getEntity());
            for (DraftSpi draft : item.getOriginalEntities()) {
                returning.ctx.markSaveReturningNotAccepted(draft);
            }
        }
    }
}
