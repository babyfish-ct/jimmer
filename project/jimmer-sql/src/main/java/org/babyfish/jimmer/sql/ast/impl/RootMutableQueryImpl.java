package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.query.ConfigurableTypedRootQuery;
import org.babyfish.jimmer.sql.ast.query.MutableRootQuery;
import org.babyfish.jimmer.sql.ast.query.NullOrderMode;
import org.babyfish.jimmer.sql.ast.query.OrderMode;
import org.babyfish.jimmer.sql.ast.tuple.*;

import java.util.Arrays;

public class RootMutableQueryImpl
        extends AbstractMutableQueryImpl
        implements MutableRootQuery {

    public RootMutableQueryImpl(
            SqlClient sqlClient,
            ImmutableType immutableType
    ) {
        super(new TableAliasAllocator(), sqlClient, immutableType);
    }

    @Override
    public <R> ConfigurableTypedRootQuery<R> select(Selection<R> selection) {
        return new ConfigurableTypedRootQueryImpl<>(
                new TypedQueryData(Arrays.asList(selection)),
                this
        );
    }

    @Override
    public <T1, T2> ConfigurableTypedRootQuery<Tuple2<T1, T2>> select(Selection<T1> selection1, Selection<T2> selection2) {
        return new ConfigurableTypedRootQueryImpl<>(
                new TypedQueryData(Arrays.asList(selection1, selection2)),
                this
        );
    }

    @Override
    public <T1, T2, T3> ConfigurableTypedRootQuery<Tuple3<T1, T2, T3>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3) {
        return new ConfigurableTypedRootQueryImpl<>(
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
    public <T1, T2, T3, T4> ConfigurableTypedRootQuery<Tuple4<T1, T2, T3, T4>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4) {
        return new ConfigurableTypedRootQueryImpl<>(
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
    public <T1, T2, T3, T4, T5> ConfigurableTypedRootQuery<Tuple5<T1, T2, T3, T4, T5>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5) {
        return new ConfigurableTypedRootQueryImpl<>(
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
    public <T1, T2, T3, T4, T5, T6> ConfigurableTypedRootQuery<Tuple6<T1, T2, T3, T4, T5, T6>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6) {
        return new ConfigurableTypedRootQueryImpl<>(
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
    public <T1, T2, T3, T4, T5, T6, T7> ConfigurableTypedRootQuery<Tuple7<T1, T2, T3, T4, T5, T6, T7>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6, Selection<T7> selection7) {
        return new ConfigurableTypedRootQueryImpl<>(
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
    public <T1, T2, T3, T4, T5, T6, T7, T8> ConfigurableTypedRootQuery<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6, Selection<T7> selection7, Selection<T8> selection8) {
        return new ConfigurableTypedRootQueryImpl<>(
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
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> ConfigurableTypedRootQuery<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6, Selection<T7> selection7, Selection<T8> selection8, Selection<T9> selection9) {
        return new ConfigurableTypedRootQueryImpl<>(
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
    public RootMutableQueryImpl where(Predicate... predicates) {
        return (RootMutableQueryImpl) super.where(predicates);
    }

    @Override
    public RootMutableQueryImpl groupBy(Expression<?>... expressions) {
        return (RootMutableQueryImpl) super.groupBy(expressions);
    }

    @Override
    public RootMutableQueryImpl having(Predicate... predicates) {
        return (RootMutableQueryImpl) super.having(predicates);
    }

    @Override
    public RootMutableQueryImpl orderBy(Expression<?> expression) {
        return (RootMutableQueryImpl) super.orderBy(expression);
    }

    @Override
    public RootMutableQueryImpl orderBy(Expression<?> expression, OrderMode orderMode) {
        return (RootMutableQueryImpl) super.orderBy(expression, orderMode);
    }

    @Override
    public RootMutableQueryImpl orderBy(Expression<?> expression, OrderMode orderMode, NullOrderMode nullOrderMode) {
        return (RootMutableQueryImpl) super.orderBy(expression, orderMode, nullOrderMode);
    }
}
