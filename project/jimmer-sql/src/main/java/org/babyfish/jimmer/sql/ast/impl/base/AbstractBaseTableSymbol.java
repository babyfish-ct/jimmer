package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.TypedBaseQueryImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AbstractBaseTableSymbol implements BaseTableSymbol {

    private final ConfigurableBaseQueryImpl<?> query;

    protected final List<Selection<?>> selections;

    private final BaseTableSymbol parent;

    private final WeakJoinHandle handle;

    private final JoinType joinType;

    // Only not null when parent is null
    private RealTable rootRealTable;

    protected AbstractBaseTableSymbol(TypedBaseQueryImplementor<?> query, List<Selection<?>> selections) {
        this.query = (ConfigurableBaseQueryImpl<?>)query;
        this.selections = wrapSelections(selections);
        this.parent = null;
        this.handle = null;
        this.joinType = JoinType.INNER;
    }

    protected AbstractBaseTableSymbol(
            BaseTableSymbol base,
            BaseTableSymbol parent,
            WeakJoinHandle handle,
            JoinType joinType
    ) {
        this.query = (ConfigurableBaseQueryImpl<?>) base.getQuery();
        this.selections = wrapSelections(base.getSelections());
        this.parent = Objects.requireNonNull(parent, "parent cannot be null");
        this.handle = Objects.requireNonNull(handle, "handle cannot be null");
        this.joinType = joinType;
    }

    private List<Selection<?>> wrapSelections(List<Selection<?>> selections) {
        int size = selections.size();
        List<Selection<?>> wrappedSelections = new ArrayList<>(selections.size());
        for (int i = 0; i < size; i++) {
            Selection<?> wrappedSelection = BaseTableSelections.of(
                    selections.get(i),
                    this,
                    i
            );
            wrappedSelections.add(wrappedSelection);
        }
        return Collections.unmodifiableList(wrappedSelections);
    }

    @Override
    public ConfigurableBaseQueryImpl<?> getQuery() {
        return query;
    }

    @Override
    public List<Selection<?>> getSelections() {
        return selections;
    }

    @Override
    public BaseTableSymbol getParent() {
        return parent;
    }

    @Override
    public WeakJoinHandle getWeakJoinHandle() {
        return handle;
    }

    @Override
    public JoinType getJoinType() {
        return joinType;
    }
}
