package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.ExistsPredicate;
import org.babyfish.jimmer.sql.ast.impl.table.SubQueryTableImplementor;
import org.babyfish.jimmer.sql.ast.query.ConfigurableTypedSubQuery;
import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.query.NullOrderMode;
import org.babyfish.jimmer.sql.ast.query.OrderMode;
import org.babyfish.jimmer.sql.ast.tuple.*;

import javax.persistence.criteria.JoinType;
import java.util.Arrays;
import java.util.Collections;

public class SubMutableQueryImpl
        extends AbstractMutableQueryImpl
        implements MutableSubQuery {

    private AbstractMutableQueryImpl parentQuery;

    public SubMutableQueryImpl(
            AbstractMutableQueryImpl parentQuery,
            ImmutableType immutableType
    ) {
        super(
                parentQuery.getTableAliasAllocator(),
                parentQuery.getSqlClient(),
                immutableType
        );
        this.parentQuery = parentQuery;
    }

    @Override
    protected SubQueryTableImplementor<?> createTableImpl(ImmutableType immutableType) {
        return SubQueryTableImplementor.create(this, immutableType);
    }

    @Override
    public SubMutableQueryImpl where(Predicate... predicates) {
        return (SubMutableQueryImpl) super.where(predicates);
    }

    @Override
    public SubMutableQueryImpl groupBy(Expression<?>... expressions) {
        return (SubMutableQueryImpl) super.groupBy(expressions);
    }

    @Override
    public SubMutableQueryImpl having(Predicate... predicates) {
        return (SubMutableQueryImpl) super.having(predicates);
    }

    @Override
    public SubMutableQueryImpl orderBy(Expression<?> expression) {
        return (SubMutableQueryImpl) super.orderBy(expression);
    }

    @Override
    public SubMutableQueryImpl orderBy(Expression<?> expression, OrderMode orderMode) {
        return (SubMutableQueryImpl) super.orderBy(expression, orderMode);
    }

    @Override
    public SubMutableQueryImpl orderBy(Expression<?> expression, OrderMode orderMode, NullOrderMode nullOrderMode) {
        return (SubMutableQueryImpl) super.orderBy(expression, orderMode, nullOrderMode);
    }

    @Override
    public <R> ConfigurableTypedSubQuery<R> select(Selection<R> selection) {
        return new ConfigurableTypedSubQueryImpl<>(
                new TypedQueryData(Collections.singletonList(selection)),
                this
        );
    }

    @Override
    public <T1, T2> ConfigurableTypedSubQuery<Tuple2<T1, T2>> select(Selection<T1> selection1, Selection<T2> selection2) {
        return new ConfigurableTypedSubQueryImpl<>(
                new TypedQueryData(
                        Arrays.asList(selection1, selection2)
                ),
                this
        );
    }

    @Override
    public <T1, T2, T3> ConfigurableTypedSubQuery<Tuple3<T1, T2, T3>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3) {
        return new ConfigurableTypedSubQueryImpl<>(
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
    public <T1, T2, T3, T4> ConfigurableTypedSubQuery<Tuple4<T1, T2, T3, T4>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4) {
        return new ConfigurableTypedSubQueryImpl<>(
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
    public <T1, T2, T3, T4, T5> ConfigurableTypedSubQuery<Tuple5<T1, T2, T3, T4, T5>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5) {
        return new ConfigurableTypedSubQueryImpl<>(
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
    public <T1, T2, T3, T4, T5, T6> ConfigurableTypedSubQuery<Tuple6<T1, T2, T3, T4, T5, T6>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6) {
        return new ConfigurableTypedSubQueryImpl<>(
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
    public <T1, T2, T3, T4, T5, T6, T7> ConfigurableTypedSubQuery<Tuple7<T1, T2, T3, T4, T5, T6, T7>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6, Selection<T7> selection7) {
        return new ConfigurableTypedSubQueryImpl<>(
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
    public <T1, T2, T3, T4, T5, T6, T7, T8> ConfigurableTypedSubQuery<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6, Selection<T7> selection7, Selection<T8> selection8) {
        return new ConfigurableTypedSubQueryImpl<>(
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
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> ConfigurableTypedSubQuery<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6, Selection<T7> selection7, Selection<T8> selection8, Selection<T9> selection9) {
        return new ConfigurableTypedSubQueryImpl<>(
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

