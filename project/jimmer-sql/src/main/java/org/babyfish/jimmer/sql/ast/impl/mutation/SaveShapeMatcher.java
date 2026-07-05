package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.ast.mutation.UpsertMask;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImplementor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

class SaveShapeMatcher {

    private final Function<ImmutableType, UpsertMask<?>> upsertMaskProvider;

    private final Map<Fetcher<?>, Boolean> optimizableMap = new HashMap<>();

    SaveShapeMatcher(Function<ImmutableType, UpsertMask<?>> upsertMaskProvider) {
        this.upsertMaskProvider = upsertMaskProvider;
    }

    @SuppressWarnings("unchecked")
    boolean isMatched(DraftSpi draft, @Nullable Fetcher<?> fetcher, boolean trim) {
        return isMatched(draft, fetcher, trim, false);
    }

    @SuppressWarnings("unchecked")
    boolean isMatched(
            DraftSpi draft,
            @Nullable Fetcher<?> fetcher,
            boolean trim,
            boolean rootIdWillBeLoaded
    ) {
        if (draft == null) {
            return true;
        }
        if (!isOptimizable(draft.__type(), fetcher)) {
            return false;
        }
        if (fetcher != null) {
            if (!isMatchedByFetcher(draft, fetcher, trim, rootIdWillBeLoaded)) {
                return false;
            }
            if (trim) {
                trim(draft, fetcher);
            }
        } else {
            for (ImmutableProp prop : draft.__type().getProps().values()) {
                PropId propId = prop.getId();
                if (!draft.__isLoaded(propId)) {
                    return false;
                }
                if (prop.isAssociation(TargetLevel.ENTITY) || prop.isEmbedded(EmbeddedLevel.SCALAR)) {
                    Object associatedValue = draft.__get(propId);
                    if (prop.isReferenceList(TargetLevel.ENTITY)) {
                        List<DraftSpi> list = (List<DraftSpi>) associatedValue;
                        for (DraftSpi e : list) {
                            if (!isMatched(e, null, trim)) {
                                return false;
                            }
                        }
                    } else if (!isMatched((DraftSpi) associatedValue, null, trim)) {
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
    private boolean isMatchedByFetcher(
            DraftSpi draft,
            Fetcher<?> fetcher,
            boolean trim,
            boolean rootIdWillBeLoaded
    ) {
        if (!isOptimizable(draft.__type(), fetcher)) {
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
                Fetcher<?> childFetcher = field.getChildFetcher();
                Object associatedValue = draft.__get(propId);
                if (prop.isReferenceList(TargetLevel.ENTITY)) {
                    List<DraftSpi> list = (List<DraftSpi>) associatedValue;
                    for (DraftSpi e : list) {
                        if (!isMatched(e, childFetcher, trim, false)) {
                            return false;
                        }
                    }
                } else if (!isMatched((DraftSpi) associatedValue, childFetcher, trim, false)) {
                    return false;
                }
            }
        }
        for (Map.Entry<ImmutableType, Fetcher<?>> e :
                ((FetcherImplementor<?>) fetcher).__getTypeBranchFetcherMap().entrySet()) {
            if (e.getKey().isAssignableFrom(draft.__type()) &&
                    !isMatchedByFetcher(draft, e.getValue(), trim, rootIdWillBeLoaded)) {
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

    private boolean isOptimizable(ImmutableType type, @Nullable Fetcher<?> fetcher) {
        if (fetcher != null) {
            return optimizableMap.computeIfAbsent(fetcher, this::isOptimizable);
        }
        return upsertMaskProvider.apply(type) != null;
    }

    private boolean isOptimizable(Fetcher<?> fetcher) {
        if (fetcher.getFieldMap().size() == 1 &&
                ((FetcherImplementor<?>) fetcher).__getTypeBranchFetcherMap().isEmpty() &&
                fetcher.getFieldMap().values().iterator().next().getProp().isId()) {
            return true;
        }
        UpsertMask<?> mask = upsertMaskProvider.apply(fetcher.getImmutableType());
        if (mask == null) {
            return true;
        }
        if (mask.getInsertablePaths() != null) {
            for (Field field : fetcher.getFieldMap().values()) {
                ImmutableProp prop = field.getProp();
                boolean matched = false;
                for (List<ImmutableProp> path : mask.getInsertablePaths()) {
                    if (path.get(0) == prop) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    return false;
                }
            }
        }
        if (mask.getUpdatablePaths() != null) {
            for (Field field : fetcher.getFieldMap().values()) {
                ImmutableProp prop = field.getProp();
                boolean matched = false;
                for (List<ImmutableProp> path : mask.getUpdatablePaths()) {
                    if (path.get(0) == prop) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    return false;
                }
            }
        }
        return true;
    }
}
