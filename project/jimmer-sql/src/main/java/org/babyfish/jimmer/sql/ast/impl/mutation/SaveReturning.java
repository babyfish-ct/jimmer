package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImplementor;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.impl.FilterManager;
import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

class SaveReturning {

    private final SaveContext ctx;

    private final Shape shape;

    private final PropertyGetter idGetter;

    private final List<PropertyGetter> sourceGetters;

    private final List<PropertyGetter> updatedGetters;

    @Nullable
    private final PropertyGetter versionGetter;

    private final List<PropertyGetter> returningGetters;

    private final List<ImmutableProp> returningProps;

    private final int logicalDeletedIndex;

    @Nullable
    private final LogicalDeletedInfo logicalDeletedInfo;

    private SaveReturning(
            SaveContext ctx,
            Shape shape,
            PropertyGetter idGetter,
            List<PropertyGetter> sourceGetters,
            List<PropertyGetter> updatedGetters,
            @Nullable PropertyGetter versionGetter,
            List<PropertyGetter> returningGetters,
            List<ImmutableProp> returningProps,
            int logicalDeletedIndex,
            @Nullable LogicalDeletedInfo logicalDeletedInfo
    ) {
        this.ctx = ctx;
        this.shape = shape;
        this.idGetter = idGetter;
        this.sourceGetters = sourceGetters;
        this.updatedGetters = updatedGetters;
        this.versionGetter = versionGetter;
        this.returningGetters = returningGetters;
        this.returningProps = returningProps;
        this.logicalDeletedIndex = logicalDeletedIndex;
        this.logicalDeletedInfo = logicalDeletedInfo;
    }

    static @Nullable SaveReturning forUpdate(
            SaveContext ctx,
            Shape shape,
            EntityCollection<DraftSpi> entities,
            List<PropertyGetter> updatedGetters,
            @Nullable Set<ImmutableProp> keyProps,
            @Nullable Predicate userOptimisticLockPredicate,
            @Nullable PropertyGetter versionGetter,
            boolean fakeUpdate,
            boolean forceOneByOne
    ) {
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        if (!sqlClient.getDialect().isUpdateByValuesReturningSupported()) {
            return null;
        }
        if (ctx.path.getParent() != null || ctx.trigger != null || ctx.fetcher == null) {
            return null;
        }
        Filter<?> filter = sqlClient.getFilters().getFilter(shape.getType());
        if (FilterManager.hasUserFilter(filter)) {
            return null;
        }
        LogicalDeletedInfo logicalDeletedInfo = filter != null ?
                shape.getType().getLogicalDeletedInfo() :
                null;
        if (shape.getType().getInheritanceInfo() != null ||
                keyProps != null ||
                userOptimisticLockPredicate != null ||
                fakeUpdate ||
                forceOneByOne ||
                (updatedGetters.isEmpty() && versionGetter == null)) {
            return null;
        }
        Fetcher<?> fetcher = ctx.fetcher;
        if (!((FetcherImplementor<?>) fetcher).__getTypeBranchFetcherMap().isEmpty()) {
            return null;
        }
        if (!isFetchRequired(ctx, fetcher, entities)) {
            return null;
        }
        List<PropertyGetter> idGetters = Shape.fullOf(sqlClient, shape.getType().getJavaClass()).getIdGetters();
        if (idGetters.size() != 1) {
            return null;
        }
        PropertyGetter idGetter = idGetters.get(0);
        if (!isSingleColumn(idGetter)) {
            return null;
        }
        if (hasDuplicateIds(idGetter, entities)) {
            return null;
        }
        List<PropertyGetter> sourceGetters = new ArrayList<>(updatedGetters.size() + 1);
        sourceGetters.add(idGetter);
        if (versionGetter != null) {
            if (!isSingleColumn(versionGetter)) {
                return null;
            }
            sourceGetters.add(versionGetter);
        }
        for (PropertyGetter getter : updatedGetters) {
            if (!isSingleColumn(getter)) {
                return null;
            }
            sourceGetters.add(getter);
        }
        List<ImmutableProp> returningProps = new ArrayList<>();
        returningProps.add(shape.getType().getIdProp());
        if (versionGetter != null) {
            returningProps.add(versionGetter.prop());
        }
        for (Field field : fetcher.getFieldMap().values()) {
            ImmutableProp prop = field.getProp();
            if (returningProps.contains(prop)) {
                continue;
            }
            if (!isReturningProp(prop)) {
                return null;
            }
            returningProps.add(prop);
        }
        int logicalDeletedIndex = -1;
        if (logicalDeletedInfo != null) {
            ImmutableProp prop = logicalDeletedInfo.getProp();
            if (!returningProps.contains(prop)) {
                if (!isReturningProp(prop)) {
                    return null;
                }
                returningProps.add(prop);
            }
            logicalDeletedIndex = returningProps.indexOf(prop);
        }
        List<PropertyGetter> returningGetters = new ArrayList<>(returningProps.size());
        for (ImmutableProp prop : returningProps) {
            List<PropertyGetter> getters = PropertyGetter.propertyGetters(sqlClient, prop);
            if (getters.size() != 1 || !isSingleColumn(getters.get(0))) {
                return null;
            }
            returningGetters.add(getters.get(0));
        }
        return new SaveReturning(
                ctx,
                shape,
                idGetter,
                Collections.unmodifiableList(sourceGetters),
                Collections.unmodifiableList(new ArrayList<>(updatedGetters)),
                versionGetter,
                Collections.unmodifiableList(returningGetters),
                Collections.unmodifiableList(returningProps),
                logicalDeletedIndex,
                logicalDeletedInfo
        );
    }

    private static boolean hasDuplicateIds(PropertyGetter idGetter, EntityCollection<DraftSpi> entities) {
        Set<Object> ids = new HashSet<>((entities.size() * 4 + 2) / 3);
        for (DraftSpi draft : entities) {
            if (!ids.add(idGetter.get(draft))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFetchRequired(
            SaveContext ctx,
            Fetcher<?> fetcher,
            EntityCollection<DraftSpi> entities
    ) {
        SaveShapeMatcher shapeMatcher = new SaveShapeMatcher(ctx.options::getUpsertMask);
        for (DraftSpi draft : entities) {
            if (!shapeMatcher.isMatched(draft, fetcher, false)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isReturningProp(ImmutableProp prop) {
        return prop.isColumnDefinition() &&
                !prop.isAssociation(TargetLevel.ENTITY) &&
                !prop.isEmbedded(EmbeddedLevel.SCALAR) &&
                !prop.isFormula() &&
                !prop.isTransient() &&
                !prop.isView();
    }

    private static boolean isSingleColumn(PropertyGetter getter) {
        return getter.metadata().getColumnName() != null;
    }

    int[] executeUpdate(EntityCollection<DraftSpi> entities) {
        if (entities.isEmpty()) {
            return new int[0];
        }
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        sqlClient.getDialect().updateByValues(new UpdateByValuesContextImpl(builder, entities));
        Tuple3<String, List<Object>, List<Integer>> tuple = builder.build();
        return sqlClient.getExecutor().execute(
                new Executor.Args<>(
                        sqlClient,
                        ctx.con,
                        tuple.get_1(),
                        tuple.get_2(),
                        tuple.get_3(),
                        ExecutionPurpose.MUTATE,
                        ctx.options.getExceptionTranslator(),
                        null,
                        (stmt, args) -> {
                            stmt.execute();
                            try (ResultSet rs = stmt.getResultSet()) {
                                return read(rs, entities);
                            }
                        }
                )
        );
    }

    private int[] read(ResultSet rs, EntityCollection<DraftSpi> entities) throws SQLException {
        Map<Object, EntityCollection.Item<DraftSpi>> itemMap = new LinkedHashMap<>();
        Map<Object, Integer> indexMap = new LinkedHashMap<>();
        int index = 0;
        for (EntityCollection.Item<DraftSpi> item : entities.items()) {
            Object id = idGetter.get(item.getEntity());
            itemMap.put(id, item);
            indexMap.put(id, index++);
        }

        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        List<Reader<?>> readers = new ArrayList<>(returningProps.size());
        for (ImmutableProp prop : returningProps) {
            readers.add(sqlClient.getReader(prop));
        }
        int[] rowCounts = new int[entities.size()];
        Reader.Context readerContext = new Reader.Context(null, sqlClient);
        SaveShapeMatcher shapeMatcher = new SaveShapeMatcher(ctx.options::getUpsertMask);
        while (rs.next()) {
            readerContext.resetCol();
            Object[] values = new Object[returningProps.size()];
            for (int i = 0; i < returningProps.size(); i++) {
                values[i] = readers.get(i).read(rs, readerContext);
            }
            Object id = values[0];
            EntityCollection.Item<DraftSpi> item = itemMap.get(id);
            if (item == null) {
                throw new ExecutionException(
                        "The update returning statement returned unexpected id \"" +
                                id +
                                "\" for \"" +
                                shape.getType() +
                                "\""
                );
            }
            rowCounts[indexMap.get(id)] = 1;
            if (logicalDeletedIndex != -1 && isLogicalDeleted(values)) {
                continue;
            }
            apply(item.getEntity(), values, shapeMatcher);
            for (DraftSpi draft : item.getOriginalEntities()) {
                if (draft != item.getEntity()) {
                    apply(draft, values, shapeMatcher);
                }
            }
        }
        return rowCounts;
    }

    private boolean isLogicalDeleted(Object[] values) {
        return logicalDeletedInfo != null && logicalDeletedInfo.isDeleted(values[logicalDeletedIndex]);
    }

    private void apply(DraftSpi draft, Object[] values, SaveShapeMatcher shapeMatcher) {
        for (int i = 0; i < returningProps.size(); i++) {
            ImmutableProp prop = returningProps.get(i);
            PropId propId = prop.getId();
            draft.__set(propId, values[i]);
            draft.__show(propId, true);
        }
        shapeMatcher.isMatched(draft, ctx.fetcher, true);
    }

    private class UpdateByValuesContextImpl implements Dialect.UpdateByValuesContext {

        private final SqlBuilder builder;

        private final EntityCollection<DraftSpi> entities;

        private UpdateByValuesContextImpl(SqlBuilder builder, EntityCollection<DraftSpi> entities) {
            this.builder = builder;
            this.entities = entities;
        }

        @Override
        public Dialect.UpdateByValuesContext sql(String sql) {
            builder.sql(sql);
            return this;
        }

        @Override
        public Dialect.UpdateByValuesContext enter(AbstractSqlBuilder.ScopeType type) {
            builder.enter(type);
            return this;
        }

        @Override
        public Dialect.UpdateByValuesContext separator() {
            builder.separator();
            return this;
        }

        @Override
        public Dialect.UpdateByValuesContext leave() {
            builder.leave();
            return this;
        }

        @Override
        public Dialect.UpdateByValuesContext appendTableName() {
            builder.sql(shape.getType().getTableName(ctx.options.getSqlClient().getMetadataStrategy()));
            return this;
        }

        @Override
        public Dialect.UpdateByValuesContext appendSource() {
            builder
                    .enter(AbstractSqlBuilder.ScopeType.LIST)
                    .enter(AbstractSqlBuilder.ScopeType.VALUES);
            for (DraftSpi draft : entities) {
                builder.separator()
                        .enter(AbstractSqlBuilder.ScopeType.TUPLE);
                for (PropertyGetter getter : sourceGetters) {
                    builder.separator();
                    Object value = getter.get(draft);
                    if (value != null) {
                        builder.variable(value);
                    } else {
                        builder.nullVariable(getter.prop());
                    }
                }
                builder.leave();
            }
            builder
                    .leave()
                    .leave();
            return this;
        }

        @Override
        public Dialect.UpdateByValuesContext appendSourceColumns() {
            for (PropertyGetter getter : sourceGetters) {
                builder.separator().sql(getter);
            }
            return this;
        }

        @Override
        public Dialect.UpdateByValuesContext appendAssignments(String targetPrefix, String sourcePrefix) {
            for (PropertyGetter getter : updatedGetters) {
                builder.separator()
                        .sql(getter)
                        .sql(" = ")
                        .sql(sourcePrefix)
                        .sql(getter);
            }
            if (versionGetter != null) {
                builder.separator()
                        .sql(versionGetter)
                        .sql(" = ")
                        .sql(targetPrefix)
                        .sql(versionGetter)
                        .sql(" + 1");
            }
            return this;
        }

        @Override
        public Dialect.UpdateByValuesContext appendPredicates(String targetPrefix, String sourcePrefix) {
            builder
                    .sql(targetPrefix)
                    .sql(idGetter)
                    .sql(" = ")
                    .sql(sourcePrefix)
                    .sql(idGetter);
            if (versionGetter != null) {
                builder
                        .sql(" and ")
                        .sql(targetPrefix)
                        .sql(versionGetter)
                        .sql(" = ")
                        .sql(sourcePrefix)
                        .sql(versionGetter);
            }
            return this;
        }

        @Override
        public Dialect.UpdateByValuesContext appendReturning(String targetPrefix) {
            boolean addComma = false;
            for (PropertyGetter getter : returningGetters) {
                if (addComma) {
                    builder.sql(", ");
                } else {
                    addComma = true;
                }
                builder.sql(targetPrefix).sql(getter);
            }
            return this;
        }
    }
}
