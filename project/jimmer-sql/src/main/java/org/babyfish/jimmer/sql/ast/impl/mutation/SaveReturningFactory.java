package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImplementor;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.impl.FilterManager;
import org.babyfish.jimmer.sql.meta.impl.SequenceIdGenerator;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

class SaveReturningFactory {

    private SaveReturningFactory() {}

    static @Nullable SaveReturning forInsert(
            SaveContext ctx,
            Shape shape,
            EntityCollection<DraftSpi> entities,
            ImmutableType tableType,
            @Nullable SequenceIdGenerator sequenceIdGenerator,
            boolean generatedId,
            List<PropertyGetter> insertedGetters,
            @Nullable PropertyGetter discriminatorGetter,
            List<PropertyGetter> defaultGetters
    ) {
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        if (!sqlClient.getDialect().isInsertReturningSupported()) {
            return null;
        }
        if (entities.size() > 1 && generatedId && !sqlClient.getDialect().isInsertBatchReturningByOrderSupported()) {
            return null;
        }
        SaveReturningBasic basic = basicForInsert(ctx, shape);
        if (basic == null) {
            return null;
        }
        SaveFetcherAnalysis fetcherAnalysis = SaveFetcherAnalysis.of(basic.fetcher, tableType);
        if (fetcherAnalysis.hasTypeBranches() || fetcherAnalysis.getDatabaseDefaultProps().isEmpty()) {
            return null;
        }
        List<PropertyGetter> idGetters = Shape.fullOf(sqlClient, tableType.getJavaClass()).getIdGetters();
        if (idGetters.size() != 1 || !isSingleColumn(idGetters.get(0))) {
            return null;
        }
        PropertyGetter idGetter = idGetters.get(0);
        SaveReturningMatchMode matchMode;
        List<PropertyGetter> matchGetters;
        if (generatedId) {
            matchMode = SaveReturningMatchMode.ORDER;
            matchGetters = Collections.emptyList();
        } else {
            matchMode = SaveReturningMatchMode.ID;
            matchGetters = Collections.singletonList(idGetter);
            if (hasDuplicateKeys(matchGetters, entities)) {
                return null;
            }
        }
        List<SaveReturningColumnValue> sourceValues = insertSourceValues(
                sqlClient,
                tableType,
                sequenceIdGenerator,
                insertedGetters,
                discriminatorGetter,
                defaultGetters
        );
        SaveReturningColumns returning = returningColumns(
                sqlClient,
                idGetter,
                insertReturningProps(shape.getType(), fetcherAnalysis.getDatabaseDefaultProps()),
                basic.logicalDeletedInfo,
                matchGetters
        );
        if (returning == null) {
            return null;
        }
        return new SaveReturning(
                ctx,
                shape,
                tableType,
                SaveReturningKind.INSERT,
                matchMode,
                sourceValues,
                Collections.emptyList(),
                null,
                null,
                idGetter,
                matchGetters,
                returning.matchIndexes,
                returning.getters,
                returning.props,
                returning.logicalDeletedIndex,
                basic.logicalDeletedInfo,
                null
        );
    }

    static @Nullable SaveReturning forUpdate(
            SaveContext ctx,
            Shape shape,
            EntityCollection<DraftSpi> entities,
            List<PropertyGetter> updatedGetters,
            @Nullable ImmutableProp discriminatorProp,
            @Nullable ImmutableProp discriminatorGuardProp,
            @Nullable Object discriminatorGuardValue,
            List<PropertyGetter> nullGetters,
            @Nullable SaveReturningUpdateCondition updateCondition,
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
        SaveReturningBasic basic = basic(ctx, shape, entities, false);
        if (basic == null ||
                discriminatorProp != null ||
                !nullGetters.isEmpty() ||
                keyProps != null ||
                userOptimisticLockPredicate != null ||
                fakeUpdate ||
                forceOneByOne ||
                (updatedGetters.isEmpty() && versionGetter == null)) {
            return null;
        }
        SaveFetcherAnalysis fetcherAnalysis = SaveFetcherAnalysis.of(basic.fetcher, shape.getType());
        if (fetcherAnalysis.hasTypeBranches() ||
                (fetcherAnalysis.getReturningProps().isEmpty() && versionGetter == null) ||
                (!isFetchRequired(fetcherAnalysis.getReturningProps(), entities, false) && versionGetter == null)) {
            return null;
        }
        List<PropertyGetter> idGetters = Shape.fullOf(sqlClient, shape.getType().getJavaClass()).getIdGetters();
        if (idGetters.size() != 1 || !isSingleColumn(idGetters.get(0))) {
            return null;
        }
        PropertyGetter idGetter = idGetters.get(0);
        List<PropertyGetter> matchGetters = Collections.singletonList(idGetter);
        if (hasDuplicateKeys(matchGetters, entities)) {
            return null;
        }
        List<SaveReturningColumnValue> sourceValues = new ArrayList<>(updatedGetters.size() + 2);
        sourceValues.add(new SaveReturningColumnValue(idGetter, SaveReturningValueMode.VALUE, null));
        if (versionGetter != null) {
            if (!isSingleColumn(versionGetter)) {
                return null;
            }
            sourceValues.add(new SaveReturningColumnValue(versionGetter, SaveReturningValueMode.VALUE, null));
        }
        for (PropertyGetter getter : updatedGetters) {
            if (!isSingleColumn(getter)) {
                return null;
            }
            sourceValues.add(new SaveReturningColumnValue(getter, SaveReturningValueMode.VALUE, null));
        }
        if (updateCondition == null && discriminatorGuardProp != null) {
            updateCondition = SaveReturningUpdateCondition.discriminatorGuard(
                    sqlClient,
                    shape.getType(),
                    discriminatorGuardValue
            );
        }
        if (updateCondition != null) {
            updateCondition.addSourceValues(sourceValues);
        }
        SaveReturningColumns returning = returningColumns(
                sqlClient,
                idGetter,
                updateReturningProps(
                        shape.getType(),
                        versionGetter != null ? Collections.singletonList(versionGetter) : Collections.emptyList(),
                        fetcherAnalysis.getReturningProps()
                ),
                basic.logicalDeletedInfo,
                matchGetters
        );
        if (returning == null) {
            return null;
        }
        return new SaveReturning(
                ctx,
                shape,
                shape.getType(),
                SaveReturningKind.UPDATE,
                SaveReturningMatchMode.ID,
                Collections.unmodifiableList(sourceValues),
                Collections.unmodifiableList(new ArrayList<>(updatedGetters)),
                updateCondition,
                versionGetter,
                idGetter,
                matchGetters,
                returning.matchIndexes,
                returning.getters,
                returning.props,
                returning.logicalDeletedIndex,
                basic.logicalDeletedInfo,
                null
        );
    }

    static @Nullable SaveReturning forUpsert(
            SaveContext ctx,
            Batch<DraftSpi> batch,
            ImmutableType tableType,
            @Nullable ImmutableProp generatedIdProp,
            @Nullable SequenceIdGenerator sequenceIdGenerator,
            List<PropertyGetter> insertedGetters,
            @Nullable ImmutableProp discriminatorProp,
            boolean updateDiscriminator,
            @Nullable ImmutableProp discriminatorGuardProp,
            List<PropertyGetter> nullGetters,
            List<PropertyGetter> conflictGetters,
            @Nullable LogicalDeletedInfo conflictPredicate,
            List<PropertyGetter> updatedGetters,
            boolean ignoreUpdate,
            @Nullable Predicate userOptimisticLockPredicate,
            @Nullable PropertyGetter versionGetter,
            boolean fakeUpdate,
            boolean forceOneByOne
    ) {
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        if (!sqlClient.getDialect().isUpsertSupported()) {
            return null;
        }
        SaveReturningBasic basic = basic(ctx, batch.shape(), batch.entities(), generatedIdProp != null);
        if (basic == null ||
                userOptimisticLockPredicate != null ||
                versionGetter != null ||
                forceOneByOne) {
            return null;
        }
        SaveFetcherAnalysis fetcherAnalysis = SaveFetcherAnalysis.of(basic.fetcher, tableType);
        if (fetcherAnalysis.hasTypeBranches() ||
                (fetcherAnalysis.getReturningProps().isEmpty() && generatedIdProp == null) ||
                (!isFetchRequired(fetcherAnalysis.getReturningProps(), batch.entities(), generatedIdProp != null) &&
                        generatedIdProp == null)) {
            return null;
        }
        if (generatedIdProp != null && generatedIdProp.isEmbedded(EmbeddedLevel.SCALAR)) {
            return null;
        }
        PropertyGetter idGetter = Shape.fullOf(sqlClient, tableType.getJavaClass()).getIdGetters().get(0);
        if (!isSingleColumn(idGetter)) {
            return null;
        }
        List<PropertyGetter> matchGetters;
        SaveReturningMatchMode matchMode;
        if (batch.shape().getIdGetters().isEmpty()) {
            matchMode = SaveReturningMatchMode.KEY;
            matchGetters = conflictGetters;
        } else {
            matchMode = SaveReturningMatchMode.ID;
            matchGetters = Collections.singletonList(idGetter);
        }
        if (matchGetters.isEmpty() || hasDuplicateKeys(matchGetters, batch.entities())) {
            return null;
        }
        PropertyGetter discriminatorGetter = discriminatorProp != null ?
                PropertyGetter.propertyGetters(sqlClient, discriminatorProp).get(0) :
                null;
        SaveReturningUpsert upsert = new SaveReturningUpsert(
                generatedIdProp,
                sequenceIdGenerator,
                discriminatorGetter,
                updateDiscriminator,
                discriminatorGuardProp != null ?
                        PropertyGetter.propertyGetters(sqlClient, discriminatorGuardProp).get(0) :
                        null,
                Collections.unmodifiableList(new ArrayList<>(nullGetters)),
                Collections.unmodifiableList(new ArrayList<>(conflictGetters)),
                conflictPredicate,
                ignoreUpdate,
                fakeUpdate
        );
        List<SaveReturningColumnValue> sourceValues = upsertSourceValues(
                sqlClient,
                tableType,
                sequenceIdGenerator,
                insertedGetters,
                discriminatorGetter,
                nullGetters
        );
        SaveReturningColumns returning = returningColumns(
                sqlClient,
                idGetter,
                updateReturningProps(
                        batch.shape().getType(),
                        Collections.emptyList(),
                        fetcherAnalysis.getReturningProps()
                ),
                basic.logicalDeletedInfo,
                matchGetters
        );
        if (returning == null) {
            return null;
        }
        return new SaveReturning(
                ctx,
                batch.shape(),
                tableType,
                SaveReturningKind.UPSERT,
                matchMode,
                sourceValues,
                Collections.unmodifiableList(new ArrayList<>(updatedGetters)),
                null,
                null,
                idGetter,
                Collections.unmodifiableList(new ArrayList<>(matchGetters)),
                returning.matchIndexes,
                returning.getters,
                returning.props,
                returning.logicalDeletedIndex,
                basic.logicalDeletedInfo,
                upsert
        );
    }

    private static @Nullable SaveReturningBasic basic(
            SaveContext ctx,
            Shape shape,
            EntityCollection<DraftSpi> entities,
            boolean idWillBeLoadedByDml
    ) {
        if (ctx.path.getParent() != null || ctx.trigger != null || ctx.fetcher == null) {
            return null;
        }
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        Filter<?> filter = sqlClient.getFilters().getFilter(shape.getType());
        if (FilterManager.hasUserFilter(filter)) {
            return null;
        }
        LogicalDeletedInfo logicalDeletedInfo = filter != null ?
                shape.getType().getLogicalDeletedInfo() :
                null;
        Fetcher<?> fetcher = ctx.fetcher;
        if (!((FetcherImplementor<?>) fetcher).__getTypeBranchFetcherMap().isEmpty()) {
            return null;
        }
        if (!isFetchRequired(ctx, fetcher, entities, idWillBeLoadedByDml)) {
            return null;
        }
        return new SaveReturningBasic(fetcher, logicalDeletedInfo);
    }

    private static @Nullable SaveReturningBasic basicForInsert(
            SaveContext ctx,
            Shape shape
    ) {
        if (ctx.path.getParent() != null || ctx.trigger != null || ctx.fetcher == null) {
            return null;
        }
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        Filter<?> filter = sqlClient.getFilters().getFilter(shape.getType());
        if (FilterManager.hasUserFilter(filter)) {
            return null;
        }
        Fetcher<?> fetcher = ctx.fetcher;
        if (!((FetcherImplementor<?>) fetcher).__getTypeBranchFetcherMap().isEmpty()) {
            return null;
        }
        LogicalDeletedInfo logicalDeletedInfo = filter != null ?
                shape.getType().getLogicalDeletedInfo() :
                null;
        return new SaveReturningBasic(fetcher, logicalDeletedInfo);
    }

    private static List<SaveReturningColumnValue> insertSourceValues(
            JSqlClientImplementor sqlClient,
            ImmutableType tableType,
            @Nullable SequenceIdGenerator sequenceIdGenerator,
            List<PropertyGetter> insertedGetters,
            @Nullable PropertyGetter discriminatorGetter,
            List<PropertyGetter> defaultGetters
    ) {
        List<SaveReturningColumnValue> sourceValues = new ArrayList<>();
        if (sequenceIdGenerator != null) {
            sourceValues.add(new SaveReturningColumnValue(
                    Shape.fullOf(sqlClient, tableType.getJavaClass()).getIdGetters().get(0),
                    SaveReturningValueMode.SEQUENCE,
                    sequenceIdGenerator
            ));
        }
        for (PropertyGetter getter : insertedGetters) {
            sourceValues.add(new SaveReturningColumnValue(getter, SaveReturningValueMode.VALUE, null));
        }
        if (discriminatorGetter != null) {
            sourceValues.add(new SaveReturningColumnValue(discriminatorGetter, SaveReturningValueMode.VALUE, null));
        }
        for (PropertyGetter getter : defaultGetters) {
            sourceValues.add(new SaveReturningColumnValue(getter, SaveReturningValueMode.DEFAULT, null));
        }
        return Collections.unmodifiableList(sourceValues);
    }

    private static List<SaveReturningColumnValue> upsertSourceValues(
            JSqlClientImplementor sqlClient,
            ImmutableType tableType,
            @Nullable SequenceIdGenerator sequenceIdGenerator,
            List<PropertyGetter> insertedGetters,
            @Nullable PropertyGetter discriminatorGetter,
            List<PropertyGetter> nullGetters
    ) {
        List<SaveReturningColumnValue> sourceValues = new ArrayList<>();
        if (sequenceIdGenerator != null) {
            sourceValues.add(new SaveReturningColumnValue(
                    Shape.fullOf(sqlClient, tableType.getJavaClass()).getIdGetters().get(0),
                    SaveReturningValueMode.SEQUENCE,
                    sequenceIdGenerator
            ));
        }
        for (PropertyGetter getter : insertedGetters) {
            sourceValues.add(new SaveReturningColumnValue(getter, SaveReturningValueMode.VALUE, null));
        }
        if (discriminatorGetter != null) {
            sourceValues.add(new SaveReturningColumnValue(discriminatorGetter, SaveReturningValueMode.VALUE, null));
        }
        for (PropertyGetter getter : nullGetters) {
            sourceValues.add(new SaveReturningColumnValue(getter, SaveReturningValueMode.NULL, null));
        }
        return Collections.unmodifiableList(sourceValues);
    }

    private static @Nullable List<ImmutableProp> updateReturningProps(
            ImmutableType type,
            List<PropertyGetter> requiredGetters,
            List<ImmutableProp> fetcherProps
    ) {
        List<ImmutableProp> props = new ArrayList<>();
        addProp(props, type.getIdProp());
        for (PropertyGetter getter : requiredGetters) {
            addProp(props, getter.prop());
        }
        for (ImmutableProp prop : fetcherProps) {
            if (!addScalarProp(props, prop)) {
                return null;
            }
        }
        return props;
    }

    private static @Nullable List<ImmutableProp> insertReturningProps(
            ImmutableType type,
            List<ImmutableProp> databaseDefaultProps
    ) {
        List<ImmutableProp> props = new ArrayList<>();
        addProp(props, type.getIdProp());
        for (ImmutableProp prop : databaseDefaultProps) {
            if (!addScalarProp(props, prop)) {
                return null;
            }
        }
        return props;
    }

    private static @Nullable SaveReturningColumns returningColumns(
            JSqlClientImplementor sqlClient,
            PropertyGetter idGetter,
            @Nullable List<ImmutableProp> props,
            @Nullable LogicalDeletedInfo logicalDeletedInfo,
            List<PropertyGetter> matchGetters
    ) {
        if (props == null) {
            return null;
        }
        int logicalDeletedIndex = -1;
        if (logicalDeletedInfo != null) {
            ImmutableProp prop = logicalDeletedInfo.getProp();
            if (!addScalarProp(props, prop)) {
                return null;
            }
            logicalDeletedIndex = indexOfProp(props, prop);
        }
        List<Integer> matchIndexes = new ArrayList<>(matchGetters.size());
        for (PropertyGetter getter : matchGetters) {
            ImmutableProp prop = getter.prop();
            if (!addScalarProp(props, prop)) {
                return null;
            }
            matchIndexes.add(indexOfProp(props, prop));
        }
        List<PropertyGetter> getters = new ArrayList<>(props.size());
        for (ImmutableProp prop : props) {
            List<PropertyGetter> propGetters = PropertyGetter.propertyGetters(sqlClient, prop);
            if (propGetters.size() != 1 || !isSingleColumn(propGetters.get(0))) {
                return null;
            }
            getters.add(propGetters.get(0));
        }
        if (!props.get(0).isId() || !idGetter.prop().isId()) {
            return null;
        }
        return new SaveReturningColumns(
                Collections.unmodifiableList(getters),
                Collections.unmodifiableList(props),
                Collections.unmodifiableList(matchIndexes),
                logicalDeletedIndex
        );
    }

    private static void addProp(List<ImmutableProp> props, ImmutableProp prop) {
        if (!containsProp(props, prop)) {
            props.add(prop);
        }
    }

    private static boolean addScalarProp(List<ImmutableProp> props, ImmutableProp prop) {
        if (containsProp(props, prop)) {
            return true;
        }
        if (!SaveFetcherAnalysis.isScalarColumnProp(prop)) {
            return false;
        }
        props.add(prop);
        return true;
    }

    private static boolean containsProp(List<ImmutableProp> props, ImmutableProp prop) {
        return indexOfProp(props, prop) != -1;
    }

    private static int indexOfProp(List<ImmutableProp> props, ImmutableProp prop) {
        ImmutableProp originalProp = prop.toOriginal();
        for (int i = 0; i < props.size(); i++) {
            if (props.get(i).toOriginal() == originalProp) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isFetchRequired(
            SaveContext ctx,
            Fetcher<?> fetcher,
            EntityCollection<DraftSpi> entities,
            boolean idWillBeLoadedByDml
    ) {
        SaveShapeMatcher shapeMatcher = new SaveShapeMatcher(ctx.options::getUpsertMask);
        for (DraftSpi draft : entities) {
            if (!shapeMatcher.isMatched(draft, fetcher, false, idWillBeLoadedByDml)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFetchRequired(
            List<ImmutableProp> props,
            EntityCollection<DraftSpi> entities,
            boolean idWillBeLoadedByDml
    ) {
        for (DraftSpi draft : entities) {
            for (ImmutableProp prop : props) {
                if (!draft.__isLoaded(prop.getId()) && (!idWillBeLoadedByDml || !prop.isId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isSingleColumn(PropertyGetter getter) {
        return getter.metadata().getColumnName() != null;
    }

    private static boolean hasDuplicateKeys(
            List<PropertyGetter> getters,
            EntityCollection<DraftSpi> entities
    ) {
        Set<List<Object>> keys = new HashSet<>((entities.size() * 4 + 2) / 3);
        for (DraftSpi draft : entities) {
            if (!keys.add(keyOf(draft, getters))) {
                return true;
            }
        }
        return false;
    }

    private static List<Object> keyOf(DraftSpi draft, List<PropertyGetter> getters) {
        List<Object> key = new ArrayList<>(getters.size());
        for (PropertyGetter getter : getters) {
            key.add(getter.get(draft));
        }
        return key;
    }
}
