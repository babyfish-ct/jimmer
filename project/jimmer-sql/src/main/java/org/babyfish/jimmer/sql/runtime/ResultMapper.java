package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.association.Association;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableSelection;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.*;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherSelection;
import org.babyfish.jimmer.sql.meta.Column;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

class ResultMapper {

    private JSqlClient sqlClient;

    private List<Selection<?>> selections;

    private ResultSet resultSet;

    private int index;

    public ResultMapper(
            JSqlClient sqlClient,
            List<Selection<?>> selections,
            ResultSet resultSet
    ) {
        if (selections.isEmpty() || selections.size() > 9) {
            throw new IllegalArgumentException("selection count must between 1 and 9");
        }
        this.sqlClient = sqlClient;
        this.selections = selections;
        this.resultSet = resultSet;
    }

    public Object map() throws SQLException {
        index = 1;
        switch (selections.size()) {
            case 1:
                return map(selections.get(0));
            case 2:
                return new Tuple2<>(
                        map(selections.get(0)),
                        map(selections.get(1))
                );
            case 3:
                return new Tuple3<>(
                        map(selections.get(0)),
                        map(selections.get(1)),
                        map(selections.get(2))
                );
            case 4:
                return new Tuple4<>(
                        map(selections.get(0)),
                        map(selections.get(1)),
                        map(selections.get(2)),
                        map(selections.get(3))
                );
            case 5:
                return new Tuple5<>(
                        map(selections.get(0)),
                        map(selections.get(1)),
                        map(selections.get(2)),
                        map(selections.get(3)),
                        map(selections.get(4))
                );
            case 6:
                return new Tuple6<>(
                        map(selections.get(0)),
                        map(selections.get(1)),
                        map(selections.get(2)),
                        map(selections.get(3)),
                        map(selections.get(4)),
                        map(selections.get(5))
                );
            case 7:
                return new Tuple7<>(
                        map(selections.get(0)),
                        map(selections.get(1)),
                        map(selections.get(2)),
                        map(selections.get(3)),
                        map(selections.get(4)),
                        map(selections.get(5)),
                        map(selections.get(6))
                );
            case 8:
                return new Tuple8<>(
                        map(selections.get(0)),
                        map(selections.get(1)),
                        map(selections.get(2)),
                        map(selections.get(3)),
                        map(selections.get(4)),
                        map(selections.get(5)),
                        map(selections.get(6)),
                        map(selections.get(7))
                );
            case 9:
                return new Tuple9<>(
                        map(selections.get(0)),
                        map(selections.get(1)),
                        map(selections.get(2)),
                        map(selections.get(3)),
                        map(selections.get(4)),
                        map(selections.get(5)),
                        map(selections.get(6)),
                        map(selections.get(7)),
                        map(selections.get(8))
                );
            default:
                throw new AssertionError("Internal bug: selection count must between 1 and 9");
        }
    }

    private Object map(Selection<?> selection) throws SQLException {
        if (selection instanceof TableSelection) {
            ImmutableType immutableType =
                    ((TableSelection)selection).getImmutableType();
            if (immutableType instanceof AssociationType) {
                return map((AssociationType)immutableType);
            }
            return map(immutableType, null);
        }
        if (selection instanceof Table<?>) {
            ImmutableType immutableType = ((Table<?>)selection).getImmutableType();
            if (immutableType instanceof AssociationType) {
                return map((AssociationType)immutableType);
            }
            return map(immutableType, null);
        }
        if (selection instanceof FetcherSelection<?>) {
            Fetcher<?> fetcher = ((FetcherSelection<?>) selection).getFetcher();
            return map(fetcher.getImmutableType(), fetcher);
        }
        return read(((ExpressionImplementor<?>)selection).getType());
    }

    private Association<?, ?> map(AssociationType associationType) throws SQLException {

        ImmutableType sourceType = associationType.getSourceType();
        ImmutableType targetType = associationType.getTargetType();
        ImmutableProp sourceIdProp = sourceType.getIdProp();
        ImmutableProp targetIdProp = targetType.getIdProp();

        Object sourceId = read(sourceIdProp.getElementClass());
        Object targetId = read(targetIdProp.getElementClass());

        Object source = Internal.produce(sourceType, null, srcDraft -> {
            ((DraftSpi) srcDraft).__set(sourceIdProp.getId(), sourceId);
        });
        Object target = Internal.produce(targetType, null, tgtDraft -> {
            ((DraftSpi) tgtDraft).__set(targetIdProp.getId(), targetId);
        });
        return new Association<>(source, target);
    }

    private Object map(ImmutableType immutableType, Fetcher<?> fetcher) throws SQLException {
        Object id = read(immutableType.getIdProp().getElementClass());
        Collection<ImmutableProp> props;
        if (fetcher != null) {
            props = fetcher
                    .getFieldMap()
                    .values()
                    .stream()
                    .map(Field::getProp)
                    .filter(it -> it.getStorage() instanceof Column)
                    .collect(Collectors.toList());
        } else {
            props = immutableType.getSelectableProps().values();
        }
        if (id == null) {
            index += props.size() - 1;
            return null;
        }
        return Internal.produce(immutableType, null, draft -> {
            DraftSpi spi = (DraftSpi) draft;
            spi.__set(immutableType.getIdProp().getId(), id);
            for (ImmutableProp prop : props) {
                if (prop.isId()) {
                    continue;
                }
                ImmutableType targetType = prop.getTargetType();
                if (targetType != null) {
                    Object targetId = read(targetType.getIdProp().getElementClass());
                    Object target =
                            targetId != null ?
                                    Internal.produce(targetType, null, targetDraft -> {
                                        DraftSpi targetSpi = (DraftSpi) targetDraft;
                                        targetSpi.__set(targetType.getIdProp().getId(), targetId);
                                    }) :
                                    null;
                    spi.__set(prop.getId(), target);
                } else {
                    spi.__set(prop.getId(), read(prop.getElementClass()));
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private Object read(Class<?> type) throws SQLException {
        Object value = resultSet.getObject(index++);
        ScalarProvider<Object, Object> scalarProvider =
                sqlClient.getScalarProvider((Class<Object>)type);
        Class<?> expectedType = scalarProvider != null ? scalarProvider.getSqlType() : type;
        Object sqlValue;
        if (value == null) {
            sqlValue = null;
        } else {
            sqlValue = Converters.tryConvert(value, expectedType);
            if (sqlValue == null) {
                throw new ExecutionException(
                        "Failed the convert the result value at column $" +
                                (index - 1) +
                                ", the expected type is '" +
                                type.getName() +
                                "', " +
                                "but the actual type is '" +
                                value.getClass().getName() +
                                "'"
                );
            }
        }
        return scalarProvider != null ? scalarProvider.toScalar(sqlValue) : sqlValue;
    }
}
