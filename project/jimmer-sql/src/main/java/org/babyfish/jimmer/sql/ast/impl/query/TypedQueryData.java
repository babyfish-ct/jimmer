package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor;
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl;
import org.babyfish.jimmer.sql.ast.impl.table.FetcherSelectionImpl;
import org.babyfish.jimmer.sql.ast.impl.table.TableSelection;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.KTable;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherSelection;
import org.babyfish.jimmer.sql.runtime.TupleCreator;

import java.util.Collections;
import java.util.List;

class TypedQueryData {

    private static final Package TUPLE_PACKAGE = Tuple2.class.getPackage();

    final List<Selection<?>> selections;

    final TupleCreator<?> tupleCreator;

    final List<Selection<?>> oldSelections;

    final TupleCreator<?> oldTupleCreator;

    final boolean distinct;

    final int limit;

    final long offset;

    final boolean withoutSortingAndPaging;

    final boolean reverseSorting;

    final Boolean reverseSortOptimizationEnabled;

    final ForUpdate forUpdate;

    final String hint;

    private PropExpressionImplementor<?> idOnlyExpression;

    private boolean idOnlyExpressionResolved;

    public TypedQueryData(List<Selection<?>> selections, TupleCreator<?> tupleCreator) {
        this.selections = processSelections(selections);
        this.tupleCreator = tupleCreator;
        oldSelections = null;
        oldTupleCreator = null;
        distinct = false;
        limit = Integer.MAX_VALUE;
        offset = 0;
        withoutSortingAndPaging = false;
        reverseSorting = false;
        reverseSortOptimizationEnabled = null;
        forUpdate = null;
        hint = null;
    }

    private TypedQueryData(
            List<Selection<?>> selections,
            TupleCreator<?> tupleCreator,
            List<Selection<?>> oldSelections,
            TupleCreator<?> oldTupleCreator,
            boolean distinct,
            int limit,
            long offset,
            boolean withoutSortingAndPaging,
            boolean reverseSorting,
            Boolean reverseSortOptimizationEnabled,
            ForUpdate forUpdate,
            String hint
    ) {
        this.selections = selections;
        this.tupleCreator = tupleCreator;
        this.oldSelections = oldSelections;
        this.oldTupleCreator = oldTupleCreator;
        this.distinct = distinct;
        this.limit = limit;
        this.offset = offset;
        this.withoutSortingAndPaging = withoutSortingAndPaging;
        this.reverseSorting = reverseSorting;
        this.reverseSortOptimizationEnabled = reverseSortOptimizationEnabled;
        this.forUpdate = forUpdate;
        this.hint = hint;
    }

    public TypedQueryData reselect(List<Selection<?>> selections, TupleCreator<?> tupleCreator) {
        return new TypedQueryData(
                processSelections(selections),
                tupleCreator,
                this.selections,
                this.tupleCreator,
                distinct,
                limit,
                offset,
                withoutSortingAndPaging,
                reverseSorting,
                reverseSortOptimizationEnabled,
                forUpdate,
                hint
        );
    }

    public TypedQueryData distinct() {
        return new TypedQueryData(
                selections,
                tupleCreator,
                oldSelections,
                oldTupleCreator,
                true,
                limit,
                offset,
                withoutSortingAndPaging,
                reverseSorting,
                reverseSortOptimizationEnabled,
                forUpdate,
                hint
        );
    }

    public TypedQueryData limit(int limit, long offset) {
        return new TypedQueryData(
                selections,
                tupleCreator,
                oldSelections,
                oldTupleCreator,
                distinct,
                limit,
                offset,
                withoutSortingAndPaging,
                reverseSorting,
                reverseSortOptimizationEnabled,
                forUpdate,
                hint
        );
    }

    public TypedQueryData withoutSortingAndPaging() {
        return new TypedQueryData(
                selections,
                tupleCreator,
                oldSelections,
                oldTupleCreator,
                distinct,
                limit,
                offset,
                true,
                reverseSorting,
                reverseSortOptimizationEnabled,
                forUpdate,
                hint
        );
    }

    public TypedQueryData reverseSorting() {
        return new TypedQueryData(
                selections,
                tupleCreator,
                oldSelections,
                oldTupleCreator,
                distinct,
                limit,
                offset,
                withoutSortingAndPaging,
                true,
                reverseSortOptimizationEnabled,
                forUpdate,
                hint
        );
    }

    public TypedQueryData reverseSortOptimizationEnabled(Boolean enabled) {
        return new TypedQueryData(
                selections,
                tupleCreator,
                oldSelections,
                oldTupleCreator,
                distinct,
                limit,
                offset,
                withoutSortingAndPaging,
                reverseSorting,
                enabled,
                forUpdate,
                hint
        );
    }

    public TypedQueryData forUpdate(ForUpdate forUpdate) {
        return new TypedQueryData(
                selections,
                tupleCreator,
                oldSelections,
                oldTupleCreator,
                distinct,
                limit,
                offset,
                withoutSortingAndPaging,
                reverseSorting,
                reverseSortOptimizationEnabled,
                forUpdate,
                hint
        );
    }

    public TypedQueryData hint(String hint) {
        if (hint != null) {
            hint = hint.trim();
            if (hint.isEmpty()) {
                hint = null;
            } else {
                if (!hint.startsWith("/*+")) {
                    hint = "/*+ " + hint;
                }
                if (!hint.endsWith("*/")) {
                    hint = hint + " */";
                }
            }
        }
        return new TypedQueryData(
                selections,
                tupleCreator,
                oldSelections,
                oldTupleCreator,
                distinct,
                limit,
                offset,
                withoutSortingAndPaging,
                reverseSorting,
                reverseSortOptimizationEnabled,
                forUpdate,
                hint
        );
    }

    public PropExpressionImplementor<?> getIdOnlyExpression() {
        if (idOnlyExpressionResolved) {
            return idOnlyExpression;
        }
        List<Selection<?>> selections = this.selections;
        if (selections.size() == 1) {
            Selection<?> selection = selections.get(0);
            Table<?> table = null;
            if (selection instanceof FetcherSelection<?>) {
                Fetcher<?> fetcher = ((FetcherSelection<?>) selection).getFetcher();
                if (fetcher.getFieldMap().size() > 1) {
                    table = ((FetcherSelectionImpl<?>) selection).getTable();
                }
            } else if (selection instanceof Table<?>){
                table = (Table<?>) selection;
            } else if (selection instanceof KTable) {
                table = ((KTable<?>) selection).getImplementor();
            }
            if (table != null && table.getImmutableType().getSelectableProps().size() > 1) {
                idOnlyExpression = (PropExpressionImpl<?>)table.get(table.getImmutableType().getIdProp());
            }
        }
        idOnlyExpressionResolved = true;
        return idOnlyExpression;
    }

    private static List<Selection<?>> processSelections(List<Selection<?>> selections) {
        for (Selection<?> selection : selections) {
            if (selection instanceof ExpressionImplementor<?>) {
                Class<?> type = ((ExpressionImplementor<?>)selection).getType();
                if (TUPLE_PACKAGE.equals(type.getPackage())) {
                    throw new IllegalArgumentException("Tuple expression cannot be selected");
                }
            } else if (selection instanceof TableSelection && ((TableSelection)selection).isRemote()) {
                throw new IllegalArgumentException("Remote table cannot be selected");
            }
        }
        return Collections.unmodifiableList(selections);
    }
}
