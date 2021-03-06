package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.table.TableAliasAllocator;
import org.babyfish.jimmer.sql.ast.query.ConfigurableTypedRootQuery;
import org.babyfish.jimmer.sql.ast.query.MutableRootQuery;
import org.babyfish.jimmer.sql.ast.query.NullOrderMode;
import org.babyfish.jimmer.sql.ast.query.OrderMode;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.*;

import java.util.Arrays;
import java.util.Collections;

public class MutableRootQueryImpl<T extends Table<?>>
        extends AbstractMutableQueryImpl
        implements MutableRootQuery<T> {

    public MutableRootQueryImpl(
            SqlClient sqlClient,
            ImmutableType immutableType
    ) {
        super(new TableAliasAllocator(), sqlClient, immutableType);
    }

    @Override
    public <R> ConfigurableTypedRootQuery<T, R> select(Selection<R> selection) {
        return new ConfigurableTypedRootQueryImpl<>(
                new TypedQueryData(Collections.singletonList(selection)),
                this
        );
    }

    @Override
    public <T1, T2> ConfigurableTypedRootQuery<T, Tuple2<T1, T2>> select(Selection<T1> selection1, Selection<T2> selection2) {
        return new ConfigurableTypedRootQueryImpl<>(
                new TypedQueryData(Arrays.asList(selection1, selection2)),
                this
        );
    }

    @Override
    public <T1, T2, T3> ConfigurableTypedRootQuery<T, Tuple3<T1, T2, T3>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3) {
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
    public <T1, T2, T3, T4> ConfigurableTypedRootQuery<T, Tuple4<T1, T2, T3, T4>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4) {
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
    public <T1, T2, T3, T4, T5> ConfigurableTypedRootQuery<T, Tuple5<T1, T2, T3, T4, T5>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5) {
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
    public <T1, T2, T3, T4, T5, T6> ConfigurableTypedRootQuery<T, Tuple6<T1, T2, T3, T4, T5, T6>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6) {
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
    public <T1, T2, T3, T4, T5, T6, T7> ConfigurableTypedRootQuery<T, Tuple7<T1, T2, T3, T4, T5, T6, T7>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6, Selection<T7> selection7) {
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
    public <T1, T2, T3, T4, T5, T6, T7, T8> ConfigurableTypedRootQuery<T, Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6, Selection<T7> selection7, Selection<T8> selection8) {
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
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> ConfigurableTypedRootQuery<T, Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6, Selection<T7> selection7, Selection<T8> selection8, Selection<T9> selection9) {
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
    public MutableRootQueryImpl where(Predicate... predicates) {
        return (MutableRootQueryImpl) super.where(predicates);
    }

    @Override
    public MutableRootQueryImpl groupBy(Expression<?>... expressions) {
        return (MutableRootQueryImpl) super.groupBy(expressions);
    }

    @Override
    public MutableRootQueryImpl having(Predicate... predicates) {
        return (MutableRootQueryImpl) super.having(predicates);
    }

    @Override
    public MutableRootQueryImpl orderBy(Expression<?> expression) {
        return (MutableRootQueryImpl) super.orderBy(expression);
    }

    @Override
    public MutableRootQueryImpl orderBy(Expression<?> expression, OrderMode orderMode) {
        return (MutableRootQueryImpl) super.orderBy(expression, orderMode);
    }

    @Override
    public MutableRootQueryImpl orderBy(Expression<?> expression, OrderMode orderMode, NullOrderMode nullOrderMode) {
        return (MutableRootQueryImpl) super.orderBy(expression, orderMode, nullOrderMode);
    }
}
