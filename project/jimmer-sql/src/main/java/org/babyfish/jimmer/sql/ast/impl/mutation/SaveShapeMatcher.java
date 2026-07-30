package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode;
import org.babyfish.jimmer.sql.ast.mutation.UpsertMask;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImplementor;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;

class SaveShapeMatcher {

    private final SaveOptions options;

    private final DraftState draftState;

    private final Map<Fetcher<?>, Boolean> upsertMaskCoverageMap;

    private SaveShapeMatcher(SaveOptions options, DraftState draftState) {
        this.options = options;
        this.draftState = draftState;
        this.upsertMaskCoverageMap = draftState == DraftState.SAVE_INPUT ? new HashMap<>() : emptyMap();
    }

    static SaveShapeMatcher forSaveInput(SaveOptions options) {
        return new SaveShapeMatcher(options, DraftState.SAVE_INPUT);
    }

    static SaveShapeMatcher forReturningApplied(SaveOptions options) {
        return new SaveShapeMatcher(options, DraftState.RETURNING_APPLIED);
    }

    static SaveShapeMatcher forFetchedResult(SaveOptions options) {
        return new SaveShapeMatcher(options, DraftState.FETCHED);
    }

    boolean matches(DraftSpi draft, @Nullable Fetcher<?> fetcher, boolean trim) {
        return matches(draft, fetcher, trim, false);
    }

    @SuppressWarnings("unchecked")
    boolean matches(
            DraftSpi draft,
            @Nullable Fetcher<?> fetcher,
            boolean trim,
            boolean rootIdWillBeLoaded
    ) {
        if (draft == null) {
            return true;
        }
        if (fetcher != null) {
            if (!matchesFetcher(draft, fetcher, trim, rootIdWillBeLoaded)) {
                return false;
            }
            if (trim) {
                trim(draft, fetcher);
            }
        } else {
            if (draftState != DraftState.SAVE_INPUT ||
                    options.getUpsertMask(draft.__type()) == null) {
                return false;
            }
            for (ImmutableProp prop : draft.__type().getProps().values()) {
                PropId propId = prop.getId();
                if (!draft.__isLoaded(propId)) {
                    return false;
                }
                if (prop.isAssociation(TargetLevel.ENTITY) || prop.isEmbedded(EmbeddedLevel.SCALAR)) {
                    if (!isAssociationComplete(prop)) {
                        return false;
                    }
                    Object associatedValue = draft.__get(propId);
                    if (prop.isReferenceList(TargetLevel.ENTITY)) {
                        List<DraftSpi> list = (List<DraftSpi>) associatedValue;
                        for (DraftSpi e : list) {
                            if (!matches(e, null, trim)) {
                                return false;
                            }
                        }
                    } else if (!matches((DraftSpi) associatedValue, null, trim)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    void trim(DraftSpi draft, Fetcher<?> fetcher) {
        Map<String, Field> matchedFieldMap = new LinkedHashMap<>();
        collectMatchedFieldMap(draft.__type(), fetcher, matchedFieldMap);
        for (ImmutableProp prop : draft.__type().getProps().values()) {
            PropId propId = prop.getId();
            if (!draft.__isLoaded(propId)) {
                continue;
            }
            Field field = matchedFieldMap.get(prop.getName());
            if (field == null) {
                if (!prop.isView()) {
                    draft.__unload(propId);
                } else {
                    draft.__show(propId, false);
                }
            } else if (field.isImplicit()) {
                draft.__show(propId, false);
            } else {
                draft.__show(propId, true);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private boolean matchesFetcher(
            DraftSpi draft,
            Fetcher<?> fetcher,
            boolean trim,
            boolean rootIdWillBeLoaded
    ) {
        if (!isFetcherCoveredByUpsertMask(fetcher)) {
            return false;
        }
        for (Field field : fetcher.getFieldMap().values()) {
            ImmutableProp prop = field.getProp();
            PropId propId = prop.getId();
            if (!draft.__isLoaded(propId)) {
                if (!rootIdWillBeLoaded || !prop.isId()) {
                    return false;
                }
            }
            if (prop.isAssociation(TargetLevel.ENTITY) || prop.isEmbedded(EmbeddedLevel.SCALAR)) {
                if (!isAssociationComplete(prop)) {
                    return false;
                }
                Fetcher<?> childFetcher = field.getChildFetcher();
                Object associatedValue = draft.__get(propId);
                if (prop.isReferenceList(TargetLevel.ENTITY)) {
                    List<DraftSpi> list = (List<DraftSpi>) associatedValue;
                    for (DraftSpi e : list) {
                        if (!matches(e, childFetcher, trim, false)) {
                            return false;
                        }
                    }
                } else if (!matches((DraftSpi) associatedValue, childFetcher, trim, false)) {
                    return false;
                }
            }
        }
        for (Map.Entry<ImmutableType, Fetcher<?>> e :
                ((FetcherImplementor<?>) fetcher).__getTypeBranchFetcherMap().entrySet()) {
            if (e.getKey().isAssignableFrom(draft.__type()) &&
                    !matchesFetcher(draft, e.getValue(), trim, rootIdWillBeLoaded)) {
                return false;
            }
        }
        return true;
    }

    private static void collectMatchedFieldMap(
            ImmutableType actualType,
            Fetcher<?> fetcher,
            Map<String, Field> matchedFieldMap
    ) {
        for (Field field : fetcher.getFieldMap().values()) {
            addMatchedField(matchedFieldMap, field);
        }
        for (Map.Entry<ImmutableType, Fetcher<?>> e :
                ((FetcherImplementor<?>) fetcher).__getTypeBranchFetcherMap().entrySet()) {
            if (e.getKey().isAssignableFrom(actualType)) {
                collectMatchedFieldMap(actualType, e.getValue(), matchedFieldMap);
            }
        }
    }

    private static void addMatchedField(Map<String, Field> matchedFieldMap, Field field) {
        String name = field.getProp().getName();
        Field oldField = matchedFieldMap.get(name);
        if (oldField == null || oldField.isImplicit() && !field.isImplicit()) {
            matchedFieldMap.put(name, field);
        }
    }

    private boolean isFetcherCoveredByUpsertMask(Fetcher<?> fetcher) {
        if (draftState != DraftState.SAVE_INPUT) {
            return true;
        }
        return upsertMaskCoverageMap.computeIfAbsent(fetcher, this::calculateUpsertMaskCoverage);
    }

    private boolean calculateUpsertMaskCoverage(Fetcher<?> fetcher) {
        if (fetcher.getFieldMap().size() == 1 &&
                ((FetcherImplementor<?>) fetcher).__getTypeBranchFetcherMap().isEmpty() &&
                fetcher.getFieldMap().values().iterator().next().getProp().isId()) {
            return true;
        }
        UpsertMask<?> mask = options.getUpsertMask(fetcher.getImmutableType());
        if (mask == null) {
            return true;
        }
        return areFetcherFieldsCovered(fetcher, mask.getInsertablePaths()) &&
                areFetcherFieldsCovered(fetcher, mask.getUpdatablePaths());
    }

    private static boolean areFetcherFieldsCovered(
            Fetcher<?> fetcher,
            @Nullable List<List<ImmutableProp>> paths
    ) {
        if (paths == null) {
            return true;
        }
        for (Field field : fetcher.getFieldMap().values()) {
            boolean covered = false;
            for (List<ImmutableProp> path : paths) {
                if (path.get(0) == field.getProp()) {
                    covered = true;
                    break;
                }
            }
            if (!covered) {
                return false;
            }
        }
        return true;
    }

    private boolean isAssociationComplete(ImmutableProp prop) {
        if (draftState == DraftState.FETCHED ||
                !prop.isReferenceList(TargetLevel.ENTITY)) {
            return true;
        }
        // Additive modes describe only the list items supplied by the user,
        // not the final association state in the database.
        AssociatedSaveMode mode = options.getAssociatedMode(prop);
        return mode == AssociatedSaveMode.REPLACE ||
                mode == AssociatedSaveMode.VIOLENTLY_REPLACE;
    }

    private enum DraftState {
        SAVE_INPUT,
        RETURNING_APPLIED,
        FETCHED
    }
}
