package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.TupleCreator;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.*;
import java.util.function.Function;

public class FetcherUtil {

    private FetcherUtil() {}

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
        for (int i = 0; i < selections.size(); i++) {
            Selection<?> selection = selections.get(i);
            if (selection instanceof FetcherSelection<?>) {
                FetcherSelection<?> fetcherSelection = (FetcherSelection<?>) selection;
                Fetcher<?> fetcher = fetcherSelection.getFetcher();
                if (!((FetcherImplementor<?>)fetcher).__isSimpleFetcher() ||
                        hasReferenceFilter(fetcher.getImmutableType(), sqlClient) ||
                        fetcherSelection.getConverter() != null) {
                    columnMap.put(i, new ArrayList<>());
                }
            }
        }
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
            if (!((FetcherImplementor<?>)fetcher).__isSimpleFetcher() || hasReferenceFilter(fetcher.getImmutableType(), sqlClient)) {
                fetchedList = Internal.produceList(
                        selection.getFetcher().getImmutableType(),
                        fetchedList,
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

    private static boolean hasReferenceFilter(ImmutableType type, JSqlClientImplementor sqlClient) {
        for (ImmutableProp prop : type.getSelectableReferenceProps().values()) {
            if (sqlClient.getFilters().getTargetFilter(prop) != null) {
                return true;
            }
        }
        return false;
    }
}