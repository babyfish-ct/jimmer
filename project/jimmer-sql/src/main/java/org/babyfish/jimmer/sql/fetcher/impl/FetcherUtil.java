package org.babyfish.jimmer.sql.fetcher.impl;

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

public class FetcherUtil {

    private FetcherUtil() {}

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
            List<Object> fetchedList = e.getValue();
            FetcherSelection<?> selection = (FetcherSelection<?>) selections.get(columnIndex);
            Fetcher<?> fetcher = selection.getFetcher();
            if (requiresPostFetch(sqlClient, fetcher)) {
                fetchedList = produceList(sqlClient, con, selection, fetchedList);
            }
            Function<Object, Object> converter = (Function<Object, Object>) selection.getConverter();
            if (converter != null) {
                List<Object> list = new ArrayList<>(fetchedList.size());
                for (Object fetched : fetchedList) {
                    list.add(fetched != null ? converter.apply(fetched) : null);
                }
                fetchedList = list;
            }
            e.setValue(fetchedList);
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
            FetcherSelection<?> selection,
            List<Object> fetchedList
    ) {
        Map<ImmutableType, TypeGroup> groupMap = new LinkedHashMap<>();
        Object[] arr = new Object[fetchedList.size()];
        for (int i = 0; i < fetchedList.size(); i++) {
            Object fetched = fetchedList.get(i);
            if (fetched == null) {
                continue;
            }
            TypeGroup group = groupMap.computeIfAbsent(
                    ((ImmutableSpi) fetched).__type(),
                    it -> new TypeGroup()
            );
            group.indices.add(i);
            group.values.add(fetched);
        }
        for (Map.Entry<ImmutableType, TypeGroup> e : groupMap.entrySet()) {
            TypeGroup group = e.getValue();
            List<Object> producedList = Internal.produceList(
                    e.getKey(),
                    group.values,
                    values -> {
                        fetch(
                                sqlClient,
                                con,
                                selection.getPath(),
                                selection.getFetcher(),
                                (List<DraftSpi>) values
                        );
                    }
            );
            for (int i = 0; i < producedList.size(); i++) {
                arr[group.indices.get(i)] = producedList.get(i);
            }
        }
        return Arrays.asList(arr);
    }

    private static boolean requiresPostFetch(JSqlClientImplementor sqlClient, Fetcher<?> fetcher) {
        if (hasReferenceFilter(fetcher.getImmutableType(), sqlClient)) {
            return true;
        }
        for (Field field : fetcher.getFieldMap().values()) {
            if (!field.isSimpleField()) {
                return true;
            }
        }
        if (fetcher instanceof FetcherImplementor<?>) {
            for (Fetcher<?> typeBranchFetcher : ((FetcherImplementor<?>) fetcher).__getTypeBranchFetcherMap().values()) {
                if (requiresPostFetch(sqlClient, typeBranchFetcher)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static class TypeGroup {

        final List<Integer> indices = new ArrayList<>();

        final List<Object> values = new ArrayList<>();
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
