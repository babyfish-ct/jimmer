package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor;
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl;
import org.babyfish.jimmer.sql.ast.impl.table.FetcherSelectionImpl;
import org.babyfish.jimmer.sql.ast.impl.table.TableSelection;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherSelection;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

class TypedQueryData {

    private static final Package TUPLE_PACKAGE = Tuple2.class.getPackage();

    final List<Selection<?>> selections;

    final List<Selection<?>> oldSelections;

    final boolean distinct;

    final int limit;

    final long offset;

    final boolean withoutSortingAndPaging;

    final boolean reverseSorting;

    final boolean forUpdate;

    final String hint;

    private PropExpressionImplementor<?> idOnlyExpression;

    private boolean idOnlyExpressionResolved;

    public TypedQueryData(List<Selection<?>> selections) {
        this.selections = processSelections(selections);
        oldSelections = null;
        distinct = false;
        limit = Integer.MAX_VALUE;
        offset = 0;
        withoutSortingAndPaging = false;
        reverseSorting = false;
        forUpdate = false;
        hint = null;
    }

    private TypedQueryData(
            List<Selection<?>> selections,
            List<Selection<?>> oldSelections,
            boolean distinct,
            int limit,
            long offset,
            boolean withoutSortingAndPaging,
            boolean reverseSorting,
            boolean forUpdate,
            String hint
    ) {
        this.selections = selections;
        this.oldSelections = oldSelections;
        this.distinct = distinct;
        this.limit = limit;
        this.offset = offset;
        this.withoutSortingAndPaging = withoutSortingAndPaging;
        this.reverseSorting = reverseSorting;
        this.forUpdate = forUpdate;
        this.hint = hint;
    }

    public TypedQueryData reselect(List<Selection<?>> selections) {
        return new TypedQueryData(
                processSelections(selections),
                this.selections,
                distinct,
                limit,
                offset,
                withoutSortingAndPaging,
                reverseSorting,
                forUpdate,
                hint
        );
    }

    public TypedQueryData distinct() {
        return new TypedQueryData(
                selections,
                oldSelections,
                true,
                limit,
                offset,
                withoutSortingAndPaging,
                reverseSorting,
                forUpdate,
                hint
        );
    }

    public TypedQueryData limit(int limit, long offset) {
        return new TypedQueryData(
                selections,
                oldSelections,
                distinct,
                limit,
                offset,
                withoutSortingAndPaging,
                reverseSorting,
                forUpdate,
                hint
        );
    }

    public TypedQueryData withoutSortingAndPaging() {
        return new TypedQueryData(
                selections,
                oldSelections,
                distinct,
                limit,
                offset,
                true,
                reverseSorting,
                forUpdate,
                hint
        );
    }

    public TypedQueryData reverseSorting() {
        return new TypedQueryData(
                selections,
                oldSelections,
                distinct,
                limit,
                offset,
                withoutSortingAndPaging,
                true,
                forUpdate,
                hint
        );
    }

    public TypedQueryData forUpdate() {
        return new TypedQueryData(
                selections,
                oldSelections,
                distinct,
                limit,
                offset,
                withoutSortingAndPaging,
                reverseSorting,
                true,
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
                oldSelections,
                distinct,
                limit,
                offset,
                withoutSortingAndPaging,
                reverseSorting,
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
