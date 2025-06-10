package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
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

public abstract class AbstractBaseTable implements BaseTableImplementor {

    protected final ConfigurableBaseQueryImpl<?> query;

    protected final List<Selection<?>> selections;

    protected final RealTable realTable;

    private final AbstractBaseTable parent;

    private final WeakJoinHandle handle;

    protected AbstractBaseTable(TypedBaseQueryImplementor<?> query, List<Selection<?>> selections) {
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
        this.query = (ConfigurableBaseQueryImpl<?>)query;
        this.selections = Collections.unmodifiableList(wrappedSelections);
        this.realTable = new RealTableImpl(this);
        this.parent = null;
        this.handle = null;
    }

    protected AbstractBaseTable(AbstractBaseTable base, AbstractBaseTable parent, WeakJoinHandle handle) {
        this.query = base.query;
        this.selections = base.selections;
        this.realTable = base.realTable;
        this.parent = parent;
        this.handle = handle;
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
    public ImmutableProp getJoinProp() {
        return null;
    }

    @Override
    public RealTable realTable(JoinTypeMergeScope scope) {
        return realTable;
    }

    @Override
    public void accept(AstVisitor visitor) {
        query.accept(visitor);
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        builder.sql(" from ").enter(AbstractSqlBuilder.ScopeType.SUB_QUERY);
        query.renderTo(builder);
        builder.leave().sql(" ").sql(realTable.getAlias());
    }

    @Override
    public AbstractMutableStatementImpl getStatement() {
        return query.getMutableQuery();
    }
}
