package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.embedded.AbstractTypedEmbeddedPropExpression;
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableSelection;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherSelection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class Readers {

    private Readers() {}

    public static Reader<?> createReader(JSqlClient sqlClient, List<Selection<?>> selections) {
        switch (selections.size()) {
            case 1:
                return createSingleReader(sqlClient, selections.get(0));
            case 2:
                return Reader.tuple(
                        createSingleReader(sqlClient, selections.get(0)),
                        createSingleReader(sqlClient, selections.get(1))
                );
            case 3:
                return Reader.tuple(
                        createSingleReader(sqlClient, selections.get(0)),
                        createSingleReader(sqlClient, selections.get(1)),
                        createSingleReader(sqlClient, selections.get(2))
                );
            case 4:
                return Reader.tuple(
                        createSingleReader(sqlClient, selections.get(0)),
                        createSingleReader(sqlClient, selections.get(1)),
                        createSingleReader(sqlClient, selections.get(2)),
                        createSingleReader(sqlClient, selections.get(3))
                );
            case 5:
                return Reader.tuple(
                        createSingleReader(sqlClient, selections.get(0)),
                        createSingleReader(sqlClient, selections.get(1)),
                        createSingleReader(sqlClient, selections.get(2)),
                        createSingleReader(sqlClient, selections.get(3)),
                        createSingleReader(sqlClient, selections.get(4))
                );
            case 6:
                return Reader.tuple(
                        createSingleReader(sqlClient, selections.get(0)),
                        createSingleReader(sqlClient, selections.get(1)),
                        createSingleReader(sqlClient, selections.get(2)),
                        createSingleReader(sqlClient, selections.get(3)),
                        createSingleReader(sqlClient, selections.get(4)),
                        createSingleReader(sqlClient, selections.get(5))
                );
            case 7:
                return Reader.tuple(
                        createSingleReader(sqlClient, selections.get(0)),
                        createSingleReader(sqlClient, selections.get(1)),
                        createSingleReader(sqlClient, selections.get(2)),
                        createSingleReader(sqlClient, selections.get(3)),
                        createSingleReader(sqlClient, selections.get(4)),
                        createSingleReader(sqlClient, selections.get(5)),
                        createSingleReader(sqlClient, selections.get(6))
                );
            case 8:
                return Reader.tuple(
                        createSingleReader(sqlClient, selections.get(0)),
                        createSingleReader(sqlClient, selections.get(1)),
                        createSingleReader(sqlClient, selections.get(2)),
                        createSingleReader(sqlClient, selections.get(3)),
                        createSingleReader(sqlClient, selections.get(4)),
                        createSingleReader(sqlClient, selections.get(5)),
                        createSingleReader(sqlClient, selections.get(6)),
                        createSingleReader(sqlClient, selections.get(7))
                );
            case 9:
                return Reader.tuple(
                        createSingleReader(sqlClient, selections.get(0)),
                        createSingleReader(sqlClient, selections.get(1)),
                        createSingleReader(sqlClient, selections.get(2)),
                        createSingleReader(sqlClient, selections.get(3)),
                        createSingleReader(sqlClient, selections.get(4)),
                        createSingleReader(sqlClient, selections.get(5)),
                        createSingleReader(sqlClient, selections.get(6)),
                        createSingleReader(sqlClient, selections.get(7)),
                        createSingleReader(sqlClient, selections.get(8))
                );
            default:
                throw new IllegalArgumentException("The selection count must between 1 and 9");
        }
    }

    private static Reader<?> createSingleReader(JSqlClient sqlClient, Selection<?> selection) {
        if (selection instanceof TableSelection) {
            ImmutableType immutableType =
                    ((TableSelection)selection).getImmutableType();
            return sqlClient.getReader(immutableType);
        }
        if (selection instanceof Table<?>) {
            ImmutableType immutableType =
                    ((Table<?>)selection).getImmutableType();
            return sqlClient.getReader(immutableType);
        }
        if (selection instanceof FetcherSelection<?>) {
            Fetcher<?> fetcher = ((FetcherSelection<?>) selection).getFetcher();
            ImmutableType type = fetcher.getImmutableType();
            Reader<?> idReader = sqlClient.getReader(type.getIdProp());
            Map<ImmutableProp, Reader<?>> nonIdReaderMap = new LinkedHashMap<>();
            for (Field field : fetcher.getFieldMap().values()) {
                ImmutableProp prop = field.getProp();
                if (!prop.isId()) {
                    Reader<?> subReader = sqlClient.getReader(prop);
                    if (subReader != null) {
                        nonIdReaderMap.put(prop, subReader);
                    }
                }
            }
            return new ObjectReader(type, idReader, nonIdReaderMap);
        }
        return sqlClient.getReader(
                AbstractTypedEmbeddedPropExpression.<ExpressionImplementor<?>>unwrap(selection).getType()
        );
    }
}
