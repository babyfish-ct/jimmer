package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSelections;
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.TypedBaseQueryImplementor;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class AbstractBaseTableImpl implements BaseTableImplementor {

    private final ConfigurableBaseQueryImpl<?> query;

    protected final List<Selection<?>> selections;

    private final AbstractBaseTableImpl parent;

    private final WeakJoinHandle handle;

    private final JoinType joinType;

    // Only not null when parent is null
    private RealTable rootRealTable;

    protected AbstractBaseTableImpl(TypedBaseQueryImplementor<?> query, List<Selection<?>> selections) {
        this.query = (ConfigurableBaseQueryImpl<?>)query;
        this.selections = wrapSelections(selections);
        this.parent = null;
        this.handle = null;
        this.joinType = JoinType.INNER;
    }

    protected AbstractBaseTableImpl(BaseTableImplementor base, AbstractBaseTableImpl parent, WeakJoinHandle handle, JoinType joinType) {
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
    public TypedBaseQueryImplementor<?> getQuery() {
        return query;
    }

    @Override
    public List<Selection<?>> getSelections() {
        return selections;
    }

    @Override
    public AbstractBaseTableImpl getParent() {
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
    public RealTable realTable(JoinTypeMergeScope scope) {
        if (parent == null) {
            RealTable rrt = this.rootRealTable;
            if (rrt == null) {
                this.rootRealTable = rrt = new RealTableImpl(this);
            }
            return rrt;
        }
        RealTableImpl parentRealTable = (RealTableImpl) parent.realTable(scope);
        return parentRealTable.child(scope, this);
    }

    @Override
    public void accept(AstVisitor visitor) {
        query.accept(visitor);
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        builder.sql(" from ").enter(AbstractSqlBuilder.ScopeType.SUB_QUERY);
        query.renderTo(builder);
        builder.leave().sql(" ").sql(realTable(builder.assertSimple().getAstContext().getJoinTypeMergeScope()).getAlias());
    }

    @Override
    public AbstractMutableStatementImpl getStatement() {
        return query.getMutableQuery();
    }
}
