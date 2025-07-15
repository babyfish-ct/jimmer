package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.MergedBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.TypedBaseQueryImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.*;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class AbstractBaseTableSymbol implements BaseTableSymbol {

    private final TypedBaseQueryImplementor<?> query;

    protected final List<Selection<?>> selections;

    protected final Object ref;

    protected final TableLike<?> parent;

    private final WeakJoinHandle handle;

    private final JoinType joinType;

    protected AbstractBaseTableSymbol(
            TypedBaseQueryImplementor<?> query,
            List<Selection<?>> selections,
            Object ref
    ) {
        this.query = query;
        this.selections = wrapSelections(selections);
        this.ref = ref;
        this.parent = null;
        this.handle = null;
        this.joinType = JoinType.INNER;
    }

    protected AbstractBaseTableSymbol(
            BaseTableSymbol base,
            TableLike<?> parent,
            WeakJoinHandle handle,
            JoinType joinType
    ) {
        this.query = base.getQuery();
        this.selections = wrapSelections(base.getSelections());
        this.ref = ((AbstractBaseTableSymbol) base).ref;
        this.parent = Objects.requireNonNull(parent, "parent cannot be null");
        this.handle = Objects.requireNonNull(handle, "handle cannot be null");
        this.joinType = joinType;
    }

    private List<Selection<?>> wrapSelections(List<Selection<?>> selections) {
        return wrapSelections(selections, this);
    }

    public static List<Selection<?>> wrapSelections(List<Selection<?>> selections, BaseTable baseTable) {
        int size = selections.size();
        List<Selection<?>> wrappedSelections = new ArrayList<>(selections.size());
        for (int i = 0; i < size; i++) {
            Selection<?> wrappedSelection = BaseTableSelections.of(
                    selections.get(i),
                    baseTable,
                    i
            );
            wrappedSelections.add(wrappedSelection);
        }
        return Collections.unmodifiableList(wrappedSelections);
    }

    @Override
    public ConfigurableBaseQueryImpl<?> getQuery() {
        if (query instanceof MergedBaseQueryImpl<?>) {
            return ((MergedBaseQueryImpl<?>) query).getExpandedQueries()[0];
        }
        return (ConfigurableBaseQueryImpl<?>) query;
    }

    @Override
    public List<Selection<?>> getSelections() {
        return selections;
    }

    @Override
    public TableLike<?> getParent() {
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

    public abstract AbstractBaseTableSymbol query(TypedBaseQueryImplementor<?> query);

    public Object getRef() {
        return ref;
    }

    protected final String suffix() {
        if (ref == null) {
            return "";
        }
        if (Boolean.TRUE.equals(ref)) {
            return "(CTE)";
        }
        return "(RecursiveCTE)";
    }

    public static <T extends BaseTable> T validateRef(T baseTable, Object ref) {
        if (!Objects.equals(((AbstractBaseTableSymbol) baseTable).ref, ref)) {
            throw new IllegalStateException(
                    "BaseQuery does not support calling " +
                            "`asBaseTable`/`asCteBaseTable`/`asRecursiveBaseTable` " +
                            "multiple times with different parameters."
            );
        }
        return baseTable;
    }
}
