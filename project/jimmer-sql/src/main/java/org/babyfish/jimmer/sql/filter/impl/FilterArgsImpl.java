package org.babyfish.jimmer.sql.filter.impl;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.query.FilterableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableLikeImplementor;
import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.query.Sortable;
import org.babyfish.jimmer.sql.ast.table.*;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.ast.table.spi.UntypedJoinDisabledTableProxy;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FilterArgsImpl<P extends Props> implements FilterArgs<P> {

    private static final String JOIN_DISABLED_REASON =
            "it is not allowed by in filter";

    private final TableLikeImplementor<?> tableLikeImplementor;

    private final P props;

    private final List<Predicate> predicates = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public FilterArgsImpl(TableLikeImplementor<?> tableLikeImplementor, Props props, boolean forCache) {
        this.tableLikeImplementor = tableLikeImplementor;
        if (forCache) {
            if (props instanceof TableImplementor<?>) {
                props = new UntypedJoinDisabledTableProxy<>((TableImplementor<?>)props, JOIN_DISABLED_REASON);
            } else {
                props = ((TableProxy<?>)props).__disableJoin(JOIN_DISABLED_REASON);
            }
        }
        this.props = (P)props;
    }

    @Override
    public @NotNull P getTable() {
        return props;
    }

    @Override
    @OldChain
    public Sortable where(Predicate... predicates) {
        List<Predicate> list = this.predicates;
        for (Predicate predicate : predicates) {
            if (predicate != null) {
                list.add(predicate);
            }
        }
        return this;
    }

    @Override
    @OldChain
    public Sortable orderBy(Expression<?>... expressions) {
        FilterableImplementor filterable = tableLikeImplementor.getStatement();
        if (filterable instanceof Sortable) {
            ((Sortable) filterable).orderBy(expressions);
        }
        return this;
    }

    @Override
    @OldChain
    public Sortable orderBy(Order... orders) {
        FilterableImplementor filterable = tableLikeImplementor.getStatement();
        if (filterable instanceof Sortable) {
            ((Sortable) filterable).orderBy(orders);
        }
        return this;
    }

    @Override
    @OldChain
    public Sortable orderBy(List<Order> orders) {
        FilterableImplementor filterable = tableLikeImplementor.getStatement();
        if (filterable instanceof Sortable) {
            ((Sortable) filterable).orderBy(orders);
        }
        return this;
    }

    @Override
    public MutableSubQuery createSubQuery(TableProxy<?> table) {
        FilterableImplementor filterable = tableLikeImplementor.getStatement();
        return filterable.createSubQuery(table);
    }

    @Override
    public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>>
    MutableSubQuery createAssociationSubQuery(AssociationTable<SE, ST, TE, TT> table) {
        FilterableImplementor filterable = tableLikeImplementor.getStatement();
        return filterable.createAssociationSubQuery(table);
    }

    public AbstractMutableStatementImpl unwrap() {
        return tableLikeImplementor.getStatement();
    }

    public List<Predicate> toPredicates() {
        return predicates;
    }
}
