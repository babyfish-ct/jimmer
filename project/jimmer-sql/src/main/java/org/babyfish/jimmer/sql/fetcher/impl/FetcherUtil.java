package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.TupleCreator;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public class FetcherUtil {

    private FetcherUtil() {}

    public static <T> T withoutFetcherContext(Supplier<T> supplier) {
        return FetcherContext.withoutContext(supplier);
    }

    private static void visitFetchColumns(
            JSqlClientImplementor sqlClient,
            List<Selection<?>> selections,
            IntFunction<Boolean> block
    ) {
        for (int i = 0; i < selections.size(); i++) {
            Selection<?> selection = selections.get(i);
            if (selection instanceof FetcherSelection<?>) {
                FetcherSelection<?> fetcherSelection = (FetcherSelection<?>) selection;
                Fetcher<?> fetcher = fetcherSelection.getFetcher();
                if (requiresPostFetch(sqlClient, fetcher) ||
                        fetcherSelection.getConverter() != null) {
                    if (Boolean.TRUE.equals(block.apply(i))) {
                        break;
                    }
                }
            }
        }
    }

    public static boolean hasFetchColumns(JSqlClientImplementor sqlClient, List<Selection<?>> selections
    ) {
        boolean[] hasRef = new boolean[1];
        visitFetchColumns(sqlClient, selections, i -> {
            hasRef[0] = true;
            return true;
        });
        return hasRef[0];
    }

    public static boolean hasPostFetchColumns(JSqlClientImplementor sqlClient, List<Selection<?>> selections) {
        for (Selection<?> selection : selections) {
            if (selection instanceof FetcherSelection<?>) {
                Fetcher<?> fetcher = ((FetcherSelection<?>) selection).getFetcher();
                if (requiresPostFetch(sqlClient, fetcher)) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static Object convert(
            List<Selection<?>> selections,
            TupleCreator<?> tupleCreator,
            Object row
    ) {
        Map<Integer, Object> indexValueMap = null;
        for (int i = 0; i < selections.size(); i++) {
            Selection<?> selection = selections.get(i);
            if (selection instanceof FetcherSelection<?>) {
                Function<Object, Object> converter =
                        (Function<Object, Object>) ((FetcherSelection<?>) selection).getConverter();
                if (converter != null) {
                    if (indexValueMap == null) {
                        indexValueMap = new HashMap<>();
                    }
                    Object value = ColumnAccessors.get(row, i, tupleCreator);
                    indexValueMap.put(i, value != null ? converter.apply(value) : null);
                }
            }
        }
        return indexValueMap != null ?
                ColumnAccessors.set(row, indexValueMap, tupleCreator) :
                row;
    }

    @SuppressWarnings("unchecked")
    public static void fetch(
            JSqlClientImplementor sqlClient,
            Connection con,
            List<Selection<?>> selections,
            TupleCreator<?> tupleCreator,
            List<?> rows
    ) {

        if (rows.isEmpty()) {
            return;
        }

        Map<Integer, List<Object>> columnMap = new LinkedHashMap<>();
        visitFetchColumns(sqlClient, selections, i -> {
            columnMap.put(i, new ArrayList<>());
            return false;
        });
        if (columnMap.isEmpty()) {
            return;
        }

        for (Object row : rows) {
            for (Map.Entry<Integer, List<Object>> e : columnMap.entrySet()) {
                int columnIndex = e.getKey();
                List<Object> columnValues = e.getValue();
                columnValues.add(ColumnAccessors.get(row, columnIndex, tupleCreator));
            }
        }

        for (Map.Entry<Integer, List<Object>> e : columnMap.entrySet()) {
            int columnIndex = e.getKey();
            FetcherSelection<?> selection = (FetcherSelection<?>) selections.get(columnIndex);
            e.setValue(
                    fetchColumn(
                            sqlClient,
                            con,
                            selection.getPath(),
                            selection.getFetcher(),
                            (Function<Object, Object>) selection.getConverter(),
                            e.getValue()
                    )
            );
        }

        Map<Integer, Object> indexValueMap = new HashMap<>();
        ListIterator<Object> itr = (ListIterator<Object>) rows.listIterator();
        int rowIndex = 0;
        while (itr.hasNext()) {
            for (Map.Entry<Integer, List<Object>> e : columnMap.entrySet()) {
                int colIndex = e.getKey();
                Object value = e.getValue().get(rowIndex);
                indexValueMap.put(colIndex, value);
            }
            itr.set(ColumnAccessors.set(itr.next(), indexValueMap, tupleCreator));
            rowIndex++;
        }
    }

    @SuppressWarnings("unchecked")
    public static <E> void fetch(
            JSqlClientImplementor sqlClient,
            Connection con,
            Fetcher<?> fetcher,
            @Nullable Function<?, E> converter,
            List<E> entities
    ) {
        List<Object> fetchedList = fetchColumn(
                sqlClient,
                con,
                null,
                fetcher,
                (Function<Object, Object>) converter,
                (List<Object>) entities
        );
        if (fetchedList != entities) {
            Collections.copy(entities, (List<E>) fetchedList);
        }
    }

    private static List<Object> fetchColumn(
            JSqlClientImplementor sqlClient,
            Connection con,
            FetchPath path,
            Fetcher<?> fetcher,
            @Nullable Function<Object, Object> converter,
            List<Object> fetchedList
    ) {
        if (requiresPostFetch(sqlClient, fetcher)) {
            fetchedList = produceList(sqlClient, con, path, fetcher, fetchedList);
        }
        if (converter != null) {
            List<Object> convertedList = new ArrayList<>(fetchedList.size());
            for (Object fetched : fetchedList) {
                convertedList.add(fetched != null ? converter.apply(fetched) : null);
            }
            return convertedList;
        }
        return fetchedList;
    }

    private static void fetch(
            JSqlClientImplementor sqlClient,
            Connection con,
            FetchPath path,
            Fetcher<?> fetcher,
            List<@Nullable DraftSpi> drafts
    ) {
        FetcherContext.using(sqlClient, con, (ctx, isRoot) -> {
            ctx.addAll(path, fetcher, drafts);
            if (isRoot) {
                ctx.execute();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static List<Object> produceList(
            JSqlClientImplementor sqlClient,
            Connection con,
            FetchPath path,
            Fetcher<?> fetcher,
            List<Object> fetchedList
    ) {
        return Internal.produceList(
                fetchedList,
                fetched -> ((ImmutableSpi) fetched).__type(),
                values -> fetch(
                        sqlClient,
                        con,
                        path,
                        fetcher,
                        (List<DraftSpi>) values
                ),
                null
        );
    }

    public static boolean requiresPostFetch(JSqlClientImplementor sqlClient, Fetcher<?> fetcher) {
        return requiresPostFetch(sqlClient, fetcher, 0);
    }

    private static boolean requiresPostFetch(
            JSqlClientImplementor sqlClient,
            Fetcher<?> fetcher,
            int joinFetchDepth
    ) {
        if (hasReferenceFilter(fetcher.getImmutableType(), sqlClient)) {
            return true;
        }
        if (fetcher instanceof FetcherImplementor<?> &&
                ((FetcherImplementor<?>) fetcher).__isSimpleFetcher()) {
            return false;
        }
        for (Field field : fetcher.getFieldMap().values()) {
            if (field.isSimpleField()) {
                continue;
            }
            if (joinFetchDepth < sqlClient.getMaxJoinFetchDepth() &&
                    JoinFetchFieldVisitor.isJoinField(field, sqlClient) &&
                    !requiresPostFetch(sqlClient, field.getChildFetcher(), joinFetchDepth + 1)) {
                continue;
            }
            if (!field.getProp().isEmbedded(EmbeddedLevel.SCALAR)) {
                return true;
            }
        }
        if (fetcher instanceof FetcherImplementor<?>) {
            for (Fetcher<?> typeBranchFetcher : ((FetcherImplementor<?>) fetcher).__getTypeBranchFetcherMap().values()) {
                if (requiresPostFetch(sqlClient, typeBranchFetcher, joinFetchDepth)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasReferenceFilter(ImmutableType type, JSqlClientImplementor sqlClient) {
        for (ImmutableProp prop : type.getSelectableReferenceProps().values()) {
            if (sqlClient.getFilters().getTargetFilter(prop) != null) {
                return true;
            }
        }
        return false;
    }
}
