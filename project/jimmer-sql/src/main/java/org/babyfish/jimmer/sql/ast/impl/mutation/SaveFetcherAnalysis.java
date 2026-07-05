package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImplementor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class SaveFetcherAnalysis {

    private final boolean hasTypeBranches;

    private final boolean scalarOnly;

    private final List<ImmutableProp> returningProps;

    private final List<ImmutableProp> databaseDefaultProps;

    private final List<ImmutableProp> completableProps;

    private SaveFetcherAnalysis(
            boolean hasTypeBranches,
            boolean scalarOnly,
            List<ImmutableProp> returningProps,
            List<ImmutableProp> databaseDefaultProps,
            List<ImmutableProp> completableProps
    ) {
        this.hasTypeBranches = hasTypeBranches;
        this.scalarOnly = scalarOnly;
        this.returningProps = Collections.unmodifiableList(returningProps);
        this.databaseDefaultProps = Collections.unmodifiableList(databaseDefaultProps);
        this.completableProps = Collections.unmodifiableList(completableProps);
    }

    static SaveFetcherAnalysis of(Fetcher<?> fetcher) {
        return of(fetcher, null);
    }

    static SaveFetcherAnalysis of(Fetcher<?> fetcher, @Nullable ImmutableType stageType) {
        boolean hasTypeBranches = !((FetcherImplementor<?>) fetcher).__getTypeBranchFetcherMap().isEmpty();
        boolean scalarOnly = !hasTypeBranches;
        List<ImmutableProp> returningProps = new ArrayList<>();
        List<ImmutableProp> databaseDefaultProps = new ArrayList<>();
        List<ImmutableProp> completableProps = new ArrayList<>();
        InheritanceInfo inheritanceInfo = stageType != null ? stageType.getInheritanceInfo() : null;
        for (Field field : fetcher.getFieldMap().values()) {
            ImmutableProp prop = field.getProp();
            if (!isScalarColumnProp(prop)) {
                scalarOnly = false;
                continue;
            }
            if (inheritanceInfo != null && !inheritanceInfo.isPropAvailableInTable(prop, stageType)) {
                continue;
            }
            if (!containsProp(returningProps, prop)) {
                returningProps.add(prop);
            }
            if (prop.hasDatabaseDefaultValue()) {
                if (!containsProp(databaseDefaultProps, prop)) {
                    databaseDefaultProps.add(prop);
                }
            } else if (!prop.isId()) {
                Ref<Object> defaultRef = prop.getDefaultValueRef();
                if ((defaultRef != null || prop.isNullable()) && !containsProp(completableProps, prop)) {
                    completableProps.add(prop);
                }
            }
        }
        return new SaveFetcherAnalysis(
                hasTypeBranches,
                scalarOnly,
                returningProps,
                databaseDefaultProps,
                completableProps
        );
    }

    boolean hasTypeBranches() {
        return hasTypeBranches;
    }

    boolean isScalarOnly() {
        return scalarOnly;
    }

    List<ImmutableProp> getReturningProps() {
        return returningProps;
    }

    List<ImmutableProp> getDatabaseDefaultProps() {
        return databaseDefaultProps;
    }

    List<ImmutableProp> getCompletableProps() {
        return completableProps;
    }

    boolean isUnmatchedOnlyByDatabaseDefaultProps(DraftSpi draft) {
        for (ImmutableProp prop : returningProps) {
            if (!draft.__isLoaded(prop.getId()) && !containsProp(databaseDefaultProps, prop)) {
                return false;
            }
        }
        return true;
    }

    boolean areReturningPropsLoaded(DraftSpi draft) {
        for (ImmutableProp prop : returningProps) {
            if (!draft.__isLoaded(prop.getId())) {
                return false;
            }
        }
        return true;
    }

    static boolean isScalarColumnProp(ImmutableProp prop) {
        return prop.isColumnDefinition() &&
                !prop.isAssociation(TargetLevel.ENTITY) &&
                !prop.isEmbedded(EmbeddedLevel.SCALAR) &&
                !prop.isFormula() &&
                !prop.isTransient() &&
                !prop.isView();
    }

    private static boolean containsProp(List<ImmutableProp> props, ImmutableProp prop) {
        ImmutableProp originalProp = prop.toOriginal();
        for (ImmutableProp existingProp : props) {
            if (existingProp.toOriginal() == originalProp) {
                return true;
            }
        }
        return false;
    }
}
