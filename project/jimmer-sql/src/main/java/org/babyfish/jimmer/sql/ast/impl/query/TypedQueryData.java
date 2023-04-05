package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableSelection;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;

import java.util.Collections;
import java.util.List;

class TypedQueryData {

    private static final Package TUPLE_PACKAGE = Tuple2.class.getPackage();

    private List<Selection<?>> selections;

    private List<Selection<?>> oldSelections;

    private boolean distinct;

    private int limit;

    private int offset;

    private boolean withoutSortingAndPaging;

    private boolean forUpdate;

    public TypedQueryData(List<Selection<?>> selections) {
        this.selections = processSelections(selections);
        limit = Integer.MAX_VALUE;
    }

    private TypedQueryData(
            List<Selection<?>> selections,
            List<Selection<?>> oldSelections,
            boolean distinct,
            int limit,
            int offset,
            boolean withoutSortingAndPaging,
            boolean forUpdate
    ) {
        this.selections = selections;
        this.oldSelections = oldSelections;
        this.distinct = distinct;
        this.limit = limit;
        this.offset = offset;
        this.withoutSortingAndPaging = withoutSortingAndPaging;
        this.forUpdate = forUpdate;
    }

    public List<Selection<?>> getSelections() {
        return selections;
    }

    public List<Selection<?>> getOldSelections() {
        return oldSelections;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public boolean isWithoutSortingAndPaging() {
        return withoutSortingAndPaging;
    }

    public boolean isForUpdate() {
        return forUpdate;
    }

    public TypedQueryData reselect(List<Selection<?>> selections) {
        return new TypedQueryData(
                processSelections(selections),
                this.selections,
                distinct,
                limit,
                offset,
                withoutSortingAndPaging,
                forUpdate
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
                forUpdate
        );
    }

    public TypedQueryData limit(int limit, int offset) {
        return new TypedQueryData(
                selections,
                oldSelections,
                distinct,
                limit,
                offset,
                withoutSortingAndPaging,
                forUpdate
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
                forUpdate
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
                true
        );
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
