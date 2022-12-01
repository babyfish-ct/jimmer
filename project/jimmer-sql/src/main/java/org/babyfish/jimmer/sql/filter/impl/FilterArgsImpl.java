package org.babyfish.jimmer.sql.filter.impl;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.query.AbstractMutableQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.SortableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.query.Sortable;
import org.babyfish.jimmer.sql.ast.table.*;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.ast.table.spi.UntypedJoinDisabledTableProxy;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

public class FilterArgsImpl<P extends Props> implements FilterArgs<P> {

    private static final String JOIN_DISABLED_REASON =
            "it is not allowed by cacheable filter";

    private final SortableImplementor sortable;

    private final P props;

    private boolean sorted;

    @SuppressWarnings("unchecked")
    public FilterArgsImpl(SortableImplementor sortable, Props props, boolean forCache) {
        this.sortable = sortable;
        if (forCache) {
            if (props instanceof TableImplementor<?>) {
                props = new UntypedJoinDisabledTableProxy<>((TableImplementor<?>)props, JOIN_DISABLED_REASON);
            } else {
                props = ((TableProxy<?>)props).__disableJoin(JOIN_DISABLED_REASON);
            }
        }
        this.props = (P)props;
    }

    public boolean isSorted() {
        return sorted;
    }

    @Override
    public @NotNull P getTable() {
        return props;
    }

    @Override
    @OldChain
    public Sortable where(Predicate... predicates) {
        return sortable.where(predicates);
    }

    @Override
    @OldChain
    public Sortable orderBy(Expression<?>... expressions) {
        if (!sorted) {
            sorted = Arrays.stream(expressions).anyMatch(Objects::nonNull);
        }
        return sortable.orderBy(expressions);
    }

    @Override
    @OldChain
    public Sortable orderBy(Order... orders) {
        if (!sorted) {
            sorted = Arrays.stream(orders).anyMatch(Objects::nonNull);
        }
        return sortable.orderBy(orders);
    }

    @Override
    public MutableSubQuery createSubQuery(TableProxy<?> table) {
        return sortable.createSubQuery(table);
    }

    @Override
    public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>>
    MutableSubQuery createAssociationSubQuery(AssociationTable<SE, ST, TE, TT> table) {
        return sortable.createAssociationSubQuery(table);
    }

    public AbstractMutableQueryImpl unwrap() {
        return (AbstractMutableQueryImpl) sortable;
    }
}
