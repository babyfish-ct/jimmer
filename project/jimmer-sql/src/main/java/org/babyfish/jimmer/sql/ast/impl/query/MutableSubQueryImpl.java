package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.ExistsPredicate;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.tuple.*;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherSelection;
import org.babyfish.jimmer.sql.filter.Filter;

import java.util.Arrays;
import java.util.Collections;

public class MutableSubQueryImpl
        extends AbstractMutableQueryImpl
        implements MutableSubQuery {

    private AbstractMutableStatementImpl parent;

    public MutableSubQueryImpl(
            AbstractMutableStatementImpl parent,
            ImmutableType immutableType
    ) {
        super(
                parent.getTableAliasAllocator(),
                parent.getSqlClient(),
                immutableType
        );
        this.parent = parent;
        this.applyFilter();
    }

    public AbstractMutableStatementImpl getParent() {
        return parent;
    }

    @Override
    public MutableSubQueryImpl where(Predicate... predicates) {
        return (MutableSubQueryImpl) super.where(predicates);
    }

    @Override
    public MutableSubQueryImpl groupBy(Expression<?>... expressions) {
        return (MutableSubQueryImpl) super.groupBy(expressions);
    }

    @Override
    public MutableSubQueryImpl having(Predicate... predicates) {
        return (MutableSubQueryImpl) super.having(predicates);
    }

    @Override
    public MutableSubQueryImpl orderBy(Expression<?> ... expressions) {
        return (MutableSubQueryImpl) super.orderBy(expressions);
    }

    @Override
    public MutableSubQueryImpl orderBy(Order... orders) {
        return (MutableSubQueryImpl) super.orderBy(orders);
    }

    @Override
    public <R> ConfigurableSubQuery<R> select(Selection<R> selection) {
        if (selection instanceof FetcherSelection<?>) {
            throw new IllegalArgumentException("Fetcher selection cannot be accepted by sub query");
        }
        return new ConfigurableSubQueryImpl<>(
                new TypedQueryData(Collections.singletonList(selection)),
                this
        );
    }

    @Override
    public <T1, T2> ConfigurableSubQuery<Tuple2<T1, T2>> select(Selection<T1> selection1, Selection<T2> selection2) {
        if (selection1 instanceof FetcherSelection<?> ||
                selection2 instanceof FetcherSelection<?>
        ) {
            throw new IllegalArgumentException("Fetcher selection cannot be accepted by sub query");
        }
        return new ConfigurableSubQueryImpl<>(
                new TypedQueryData(
                        Arrays.asList(selection1, selection2)
                ),
                this
        );
    }

    @Override
    public <T1, T2, T3> ConfigurableSubQuery<Tuple3<T1, T2, T3>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3) {
        if (selection1 instanceof FetcherSelection<?> ||
                selection2 instanceof FetcherSelection<?> ||
                selection3 instanceof FetcherSelection<?>
        ) {
            throw new IllegalArgumentException("Fetcher selection cannot be accepted by sub query");
        }
        return new ConfigurableSubQueryImpl<>(
                new TypedQueryData(
                        Arrays.asList(
                                selection1,
                                selection2,
                                selection3
                        )
                ),
                this
        );
    }

    @Override
    public <T1, T2, T3, T4> ConfigurableSubQuery<Tuple4<T1, T2, T3, T4>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4) {
        if (selection1 instanceof FetcherSelection<?> ||
                selection2 instanceof FetcherSelection<?> ||
                selection3 instanceof FetcherSelection<?> ||
                selection4 instanceof FetcherSelection<?>
        ) {
            throw new IllegalArgumentException("Fetcher selection cannot be accepted by sub query");
        }
        return new ConfigurableSubQueryImpl<>(
                new TypedQueryData(
                        Arrays.asList(
                                selection1,
                                selection2,
                                selection3,
                                selection4
                        )
                ),
                this
        );
    }

    @Override
    public <T1, T2, T3, T4, T5> ConfigurableSubQuery<Tuple5<T1, T2, T3, T4, T5>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5) {
        if (selection1 instanceof FetcherSelection<?> ||
                selection2 instanceof FetcherSelection<?> ||
                selection3 instanceof FetcherSelection<?> ||
                selection4 instanceof FetcherSelection<?> ||
                selection5 instanceof FetcherSelection<?>
        ) {
            throw new IllegalArgumentException("Fetcher selection cannot be accepted by sub query");
        }
        return new ConfigurableSubQueryImpl<>(
                new TypedQueryData(
                        Arrays.asList(
                                selection1,
                                selection2,
                                selection3,
                                selection4,
                                selection5
                        )
                ),
                this
        );
    }

    @Override
    public <T1, T2, T3, T4, T5, T6> ConfigurableSubQuery<Tuple6<T1, T2, T3, T4, T5, T6>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6) {
        if (selection1 instanceof FetcherSelection<?> ||
                selection2 instanceof FetcherSelection<?> ||
                selection3 instanceof FetcherSelection<?> ||
                selection4 instanceof FetcherSelection<?> ||
                selection5 instanceof FetcherSelection<?> ||
                selection6 instanceof FetcherSelection<?>
        ) {
            throw new IllegalArgumentException("Fetcher selection cannot be accepted by sub query");
        }
        return new ConfigurableSubQueryImpl<>(
                new TypedQueryData(
                        Arrays.asList(
                                selection1,
                                selection2,
                                selection3,
                                selection4,
                                selection5,
                                selection6
                        )
                ),
                this
        );
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7> ConfigurableSubQuery<Tuple7<T1, T2, T3, T4, T5, T6, T7>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6, Selection<T7> selection7) {
        if (selection1 instanceof FetcherSelection<?> ||
                selection2 instanceof FetcherSelection<?> ||
                selection3 instanceof FetcherSelection<?> ||
                selection4 instanceof FetcherSelection<?> ||
                selection5 instanceof FetcherSelection<?> ||
                selection6 instanceof FetcherSelection<?> ||
                selection7 instanceof FetcherSelection<?>
        ) {
            throw new IllegalArgumentException("Fetcher selection cannot be accepted by sub query");
        }
        return new ConfigurableSubQueryImpl<>(
                new TypedQueryData(
                        Arrays.asList(
                                selection1,
                                selection2,
                                selection3,
                                selection4,
                                selection5,
                                selection6,
                                selection7
                        )
                ),
                this
        );
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, T8> ConfigurableSubQuery<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6, Selection<T7> selection7, Selection<T8> selection8) {
        if (selection1 instanceof FetcherSelection<?> ||
                selection2 instanceof FetcherSelection<?> ||
                selection3 instanceof FetcherSelection<?> ||
                selection4 instanceof FetcherSelection<?> ||
                selection5 instanceof FetcherSelection<?> ||
                selection6 instanceof FetcherSelection<?> ||
                selection7 instanceof FetcherSelection<?> ||
                selection8 instanceof FetcherSelection<?>
        ) {
            throw new IllegalArgumentException("Fetcher selection cannot be accepted by sub query");
        }
        return new ConfigurableSubQueryImpl<>(
                new TypedQueryData(
                        Arrays.asList(
                                selection1,
                                selection2,
                                selection3,
                                selection4,
                                selection5,
                                selection6,
                                selection7,
                                selection8
                        )
                ),
                this
        );
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> ConfigurableSubQuery<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6, Selection<T7> selection7, Selection<T8> selection8, Selection<T9> selection9) {
        if (selection1 instanceof FetcherSelection<?> ||
            selection2 instanceof FetcherSelection<?> ||
            selection3 instanceof FetcherSelection<?> ||
            selection4 instanceof FetcherSelection<?> ||
            selection5 instanceof FetcherSelection<?> ||
            selection6 instanceof FetcherSelection<?> ||
            selection7 instanceof FetcherSelection<?> ||
            selection8 instanceof FetcherSelection<?> ||
            selection9 instanceof FetcherSelection<?>
        ) {
            throw new IllegalArgumentException("Fetcher selection cannot be accepted by sub query");
        }
        return new ConfigurableSubQueryImpl<>(
                new TypedQueryData(
                        Arrays.asList(
                                selection1,
                                selection2,
                                selection3,
                                selection4,
                                selection5,
                                selection6,
                                selection7,
                                selection8,
                                selection9
                        )
                ),
                this
        );
    }

    @Override
    public Predicate exists() {
        return ExistsPredicate.of(this, false);
    }

    @Override
    public Predicate notExists() {
        return ExistsPredicate.of(this, true);
    }
}

