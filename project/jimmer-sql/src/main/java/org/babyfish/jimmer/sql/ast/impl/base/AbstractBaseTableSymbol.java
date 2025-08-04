package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.MergedBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.TypedBaseQueryImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.*;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class AbstractBaseTableSymbol implements BaseTableSymbol {

    private final TypedBaseQueryImplementor<?> query;

    protected final List<Selection<?>> selections;

    protected final byte[] kotlinSelectionTypes;

    protected final boolean cte;

    protected final BaseTableSymbol recursive;

    protected final TableLike<?> parent;

    private final WeakJoinHandle handle;

    private final JoinType joinType;

    protected AbstractBaseTableSymbol(
            TypedBaseQueryImplementor<?> query,
            List<Selection<?>> selections,
            byte[] kotlinSelectionTypes,
            boolean cte
    ) {
        this.query = query;
        this.selections = wrapSelections(selections);
        this.kotlinSelectionTypes = kotlinSelectionTypes;
        this.cte = cte;
        this.parent = null;
        this.handle = null;
        this.joinType = JoinType.INNER;
        this.recursive = null;
    }

    protected AbstractBaseTableSymbol(
            BaseTableSymbol base,
            TableLike<?> parent,
            WeakJoinHandle handle,
            JoinType joinType,
            BaseTableSymbol recursive
    ) {
        this.query = base.getQuery();
        this.selections = wrapSelections(base.getSelections());
        this.kotlinSelectionTypes = ((AbstractBaseTableSymbol) base).kotlinSelectionTypes;
        this.cte = ((AbstractBaseTableSymbol) base).cte;
        this.parent = Objects.requireNonNull(parent, "parent cannot be null");
        this.handle = Objects.requireNonNull(handle, "handle cannot be null");
        this.joinType = joinType;
        this.recursive = recursive;
    }

    private List<Selection<?>> wrapSelections(List<Selection<?>> selections) {
        return wrapSelections(selections, this);
    }

    public byte[] getKotlinSelectionTypes() {
        return kotlinSelectionTypes;
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
            return ((MergedBaseQueryImpl<?>) query).firstQuery();
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

    @Override
    public boolean isCte() {
        return cte;
    }

    @Override
    public BaseTableSymbol getRecursive() {
        return recursive;
    }

    public abstract AbstractBaseTableSymbol query(TypedBaseQueryImplementor<?> query);

    protected final String suffix() {
        return recursive != null ?
                "(RecursiveCTE)" :
                cte ? "(CTE)" : "";
    }

    public static <T extends BaseTable> T validateCte(T baseTable, boolean cte) {
        if (((AbstractBaseTableSymbol) baseTable).cte != cte) {
            throw new IllegalStateException(
                    "BaseQuery does not support calling " +
                            "`asBaseTable`/`asCteBaseTable` " +
                            "multiple times with different parameters."
            );
        }
        return baseTable;
    }
}
