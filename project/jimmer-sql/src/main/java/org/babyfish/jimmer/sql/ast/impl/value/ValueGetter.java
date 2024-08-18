package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor;
import org.babyfish.jimmer.sql.ast.impl.TupleExpressionImplementor;
import org.babyfish.jimmer.sql.ast.impl.TupleImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface ValueGetter {

    Object get(Object value);

    GetterMetadata metadata();

    @SuppressWarnings("unchecked")
    static <T> List<ValueGetter> valueGetters(
        JSqlClientImplementor sqlClient,
        Expression<T> expression,
        T value
    ) {
        if (expression instanceof PropExpression<?>) {
            return valueGetters(sqlClient, (PropExpression<T>) expression, value);
        }
        if (expression instanceof TupleExpressionImplementor<?>) {
            TupleExpressionImplementor<?> tupleExpressionImplementor =
                    (TupleExpressionImplementor<?>) expression;
            TupleImplementor tupleImplementor =
                    (TupleImplementor) value;
            int size = tupleExpressionImplementor.size();
            if (tupleImplementor != null && tupleExpressionImplementor.size() != tupleImplementor.size()) {
                throw new IllegalArgumentException(
                        "The size of tuple expression is " +
                                size +
                                ", but the size of the tuple value is " +
                                tupleImplementor.size()
                );
            }
            List<ValueGetter>[] gettersArr = new List[size];
            for (int i = 0; i < size; i++) {
                Selection<?> selection = tupleExpressionImplementor.get(i);
                if (!(selection instanceof Expression<?>)) {
                    throw new IllegalArgumentException(
                            "The TupleExpression[" +
                                    i +
                                    "] is not expression"
                    );
                }
                gettersArr[i] = valueGetters(
                        sqlClient,
                        (Expression<Object>) selection,
                        tupleImplementor != null ? tupleImplementor.get(i) : null
                );
            }
            return ValueGetter.tupleGetters(gettersArr);
        }
        ExpressionImplementor<?> expressionImplementor = (ExpressionImplementor<?>) expression;
        if (ImmutableType.tryGet(expressionImplementor.getType()) != null) {
            throw new IllegalArgumentException(
                    "The expression whose type is embeddable type must be prop expression"
            );
        }
        return Collections.singletonList(
                new NonColumnDefinitionValueGetter(sqlClient, expressionImplementor)
        );
    }

    @SuppressWarnings("unchecked")
    static <T> List<ValueGetter> valueGetters(
            JSqlClientImplementor sqlClient,
            PropExpression<T> propExpression,
            T value
    ) {
        PropExpressionImplementor<?> propExpressionImplementor = ((PropExpressionImplementor<T>) propExpression);
        ImmutableProp deepestProp = propExpressionImplementor.getDeepestProp();
        if (!deepestProp.isColumnDefinition()) {
            return Collections.singletonList(
                    new NonColumnDefinitionValueGetter(
                            sqlClient,
                            propExpressionImplementor
                    )
            );
        }
        List<ImmutableProp> props = new ArrayList<>();
        for (PropExpressionImplementor<?> pei = (PropExpressionImplementor<?>) propExpression;
             pei != null;
             pei = pei.getBase()) {
            props.add(0, pei.getDeepestProp());
        }
        Table<?> table = propExpressionImplementor.getTable();
        boolean rawId = propExpressionImplementor.isRawId();
        if (props.get(0).isId()) {
            ImmutableProp joinProp;
            boolean inverse;
            if (table instanceof TableProxy<?>) {
                joinProp = ((TableProxy<?>) table).__prop();
                inverse = ((TableProxy<?>)table).__isInverse();
            } else {
                joinProp = ((TableImplementor<?>) table).getJoinProp();
                inverse = ((TableImplementor<?>)table).isInverse();
            }
            boolean isAddable = false;
            if (joinProp != null) {
                if (!inverse) {
                    isAddable = true;
                } else {
                    if (joinProp.isMiddleTableDefinition()) {
                        isAddable = true;
                    } else {
                        ImmutableProp mappedBy = joinProp.getMappedBy();
                        isAddable = mappedBy != null && mappedBy.isMiddleTableDefinition();
                    }
                }
            }
            if (isAddable) {
                props.add(0, joinProp);
            } else {
                rawId = false;
            }
        }
        return AbstractValueGetter.createValueGetters(
                sqlClient,
                table,
                rawId,
                props,
                value
        );
    }

    static List<ValueGetter> valueGetters(
            JSqlClientImplementor sqlClient,
            ImmutableProp prop
    ) {
        return AbstractValueGetter.createValueGetters(sqlClient, prop, null);
    }

    static List<ValueGetter> tupleGetters(
            List<ValueGetter> leftGetters,
            List<ValueGetter> rightGetters
    ) {
        List<ValueGetter> getters = new ArrayList<>(leftGetters.size() + rightGetters.size());
        for (ValueGetter leftGetter : leftGetters) {
            getters.add(new TupleValueGetter(0, leftGetter));
        }
        for (ValueGetter rightGetter : rightGetters) {
            getters.add(new TupleValueGetter(1, rightGetter));
        }
        return getters;
    }

    static List<ValueGetter> tupleGetters(
            List<ValueGetter>[] gettersArr
    ) {
        List<ValueGetter> getters = new ArrayList<>();
        for (int i = 0; i < gettersArr.length; i++) {
            for (ValueGetter getter : gettersArr[i]) {
                getters.add(new TupleValueGetter(i, getter));
            }
        }
        return getters;
    }

    static List<ValueGetter> alias(String alias, List<ValueGetter> getters) {
        if (alias == null) {
            return getters;
        }
        List<ValueGetter> aliasGetters = new ArrayList<>(getters.size());
        for (int i = 0; i < getters.size(); i++) {
            aliasGetters.add(new AliasValueGetter(alias, getters.get(i)));
        }
        return aliasGetters;
    }
}


