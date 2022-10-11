package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.table.TableAliasAllocator;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.*;

import java.util.Arrays;
import java.util.Collections;

public class MutableRootQueryImpl<T extends Table<?>>
        extends AbstractMutableQueryImpl
        implements MutableRootQuery<T> {

    public MutableRootQueryImpl(
            JSqlClient sqlClient,
            ImmutableType immutableType
    ) {
        this(null, sqlClient, immutableType);
    }

    @SuppressWarnings("unchecked")
    public MutableRootQueryImpl(
            TableAliasAllocator aliasAllocator,
            JSqlClient sqlClient,
            ImmutableType immutableType
    ) {
        super(
                aliasAllocator != null ? aliasAllocator : new TableAliasAllocator(),
                sqlClient,
                immutableType
        );
        if (aliasAllocator == null) {
            applyFilter();
        }
    }

    @Override
    public <R> ConfigurableRootQuery<T, R> select(Selection<R> selection) {
        return new ConfigurableRootQueryImpl<>(
                new TypedQueryData(Collections.singletonList(selection)),
                this
        );
    }

    @Override
    public <T1, T2> ConfigurableRootQuery<T, Tuple2<T1, T2>> select(Selection<T1> selection1, Selection<T2> selection2) {
        return new ConfigurableRootQueryImpl<>(
                new TypedQueryData(Arrays.asList(selection1, selection2)),
                this
        );
    }

    @Override
    public <T1, T2, T3> ConfigurableRootQuery<T, Tuple3<T1, T2, T3>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3) {
        return new ConfigurableRootQueryImpl<>(
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
    public <T1, T2, T3, T4> ConfigurableRootQuery<T, Tuple4<T1, T2, T3, T4>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4) {
        return new ConfigurableRootQueryImpl<>(
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
    public <T1, T2, T3, T4, T5> ConfigurableRootQuery<T, Tuple5<T1, T2, T3, T4, T5>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5) {
        return new ConfigurableRootQueryImpl<>(
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
    public <T1, T2, T3, T4, T5, T6> ConfigurableRootQuery<T, Tuple6<T1, T2, T3, T4, T5, T6>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6) {
        return new ConfigurableRootQueryImpl<>(
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
    public <T1, T2, T3, T4, T5, T6, T7> ConfigurableRootQuery<T, Tuple7<T1, T2, T3, T4, T5, T6, T7>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6, Selection<T7> selection7) {
        return new ConfigurableRootQueryImpl<>(
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
    public <T1, T2, T3, T4, T5, T6, T7, T8> ConfigurableRootQuery<T, Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6, Selection<T7> selection7, Selection<T8> selection8) {
        return new ConfigurableRootQueryImpl<>(
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
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> ConfigurableRootQuery<T, Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> select(Selection<T1> selection1, Selection<T2> selection2, Selection<T3> selection3, Selection<T4> selection4, Selection<T5> selection5, Selection<T6> selection6, Selection<T7> selection7, Selection<T8> selection8, Selection<T9> selection9) {
        return new ConfigurableRootQueryImpl<>(
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

    @SuppressWarnings("unchecked")
    @Override
    public MutableRootQueryImpl<T> where(Predicate... predicates) {
        return (MutableRootQueryImpl<T>) super.where(predicates);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRootQueryImpl<T> groupBy(Expression<?>... expressions) {
        return (MutableRootQueryImpl<T>) super.groupBy(expressions);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRootQueryImpl<T> having(Predicate... predicates) {
        return (MutableRootQueryImpl<T>) super.having(predicates);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRootQueryImpl<T> orderBy(Expression<?> ... expressions) {
        return (MutableRootQueryImpl<T>) super.orderBy(expressions);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRootQueryImpl<T> orderBy(Order... orders) {
        return (MutableRootQueryImpl<T>) super.orderBy(orders);
    }
}
