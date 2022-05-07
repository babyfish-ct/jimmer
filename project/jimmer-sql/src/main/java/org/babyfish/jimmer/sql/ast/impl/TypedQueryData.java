package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Selection;

import java.util.Collections;
import java.util.List;

public class TypedQueryData {

    private List<Selection<?>> selections;

    private List<Selection<?>> oldSelections;

    private boolean distinct;

    private int limit;

    private int offset;

    private boolean withoutSortingAndPaging;

    private boolean forUpdate;

    public TypedQueryData(List<Selection<?>> selections) {
        selections = Collections.unmodifiableList(selections);
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
                Collections.unmodifiableList(selections),
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
}
