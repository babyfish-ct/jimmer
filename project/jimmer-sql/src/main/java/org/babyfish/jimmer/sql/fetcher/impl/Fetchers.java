package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

import java.sql.Connection;
import java.util.*;
import java.util.function.Function;

public class Fetchers {

    private Fetchers() {}

    @SuppressWarnings("unchecked")
    public static void fetch(
            JSqlClient sqlClient,
            Connection con,
            List<Selection<?>> selections,
            List<?> rows
    ) {

        if (rows.isEmpty()) {
            return;
        }

        Map<Integer, List<Object>> columnMap = new LinkedHashMap<>();
        for (int i = 0; i < selections.size(); i++) {
            Selection<?> selection = selections.get(i);
            if (selection instanceof FetcherSelection<?>) {
                Fetcher<?> fetcher = ((FetcherSelection<?>)selection).getFetcher();
                if (!fetcher.isSimpleFetcher() ||
                    hasReferenceFilter(fetcher.getImmutableType(), sqlClient)) {
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
                columnValues.add(ColumnAccessors.get(row, columnIndex));
            }
        }

        for (Map.Entry<Integer, List<Object>> e : columnMap.entrySet()) {
            int columnIndex = e.getKey();
            List<Object> columnValues = e.getValue();
            FetcherSelection<?> selection = (FetcherSelection<?>) selections.get(columnIndex);
            List<Object> fetchedList = Internal.produceList(
                    selection.getFetcher().getImmutableType(),
                    columnValues,
                    values -> {
                        fetch(
                                sqlClient,
                                con,
                                selection.getFetcher(),
                                (List<DraftSpi>)values
                        );
                    }
            );
            Function<Object, Object> converter = (Function<Object, Object>) selection.getConverter();
            if (converter != null) {
                List<Object> list = new ArrayList<>(fetchedList.size());
                for (Object fetched : fetchedList) {
                    list.add(converter.apply(fetched));
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
            itr.set(ColumnAccessors.set(itr.next(), indexValueMap));
            rowIndex++;
        }
    }

    private static void fetch(
            JSqlClient sqlClient,
            Connection con,
            Fetcher<?> fetcher,
            List<DraftSpi> drafts
    ) {
        FetcherContext.using(sqlClient, con, (ctx, isRoot) -> {
            ctx.addAll(fetcher, drafts);
            if (isRoot) {
                ctx.execute();
            }
        });
    }

    private static boolean hasReferenceFilter(ImmutableType type, JSqlClient sqlClient) {
        for (ImmutableProp prop : type.getSelectableReferenceProps().values()) {
            if (sqlClient.getFilters().getTargetFilter(prop) != null) {
                return true;
            }
        }
        return false;
    }
}
