package org.babyfish.jimmer.sql.fluent.impl;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.*;
import org.babyfish.jimmer.sql.fluent.FluentRootQuery;

class FluentRootQueryImpl<T extends Table<?>> implements FluentRootQuery<T> {

    private final MutableRootQuery<T> raw;

    private final Runnable onTerminate;

    public FluentRootQueryImpl(MutableRootQuery<T> raw, Runnable onTerminate) {
        this.raw = raw;
        this.onTerminate = onTerminate;
    }

    @Override
    @OldChain
    public FluentRootQuery<T> where(Predicate... predicates) {
        raw.where(predicates);
        return this;
    }

    @Override
    @OldChain
    public FluentRootQuery<T> orderBy(Expression<?> ... expressions) {
        raw.orderBy(expressions);
        return this;
    }

    @Override
    @OldChain
    public FluentRootQuery<T> orderBy(Order... orders) {
        raw.orderBy(orders);
        return this;
    }

    @Override
    @OldChain
    public FluentRootQuery<T> groupBy(Expression<?>... expressions) {
        raw.groupBy(expressions);
        return this;
    }

    @Override
    @OldChain
    public FluentRootQuery<T> having(Predicate... predicates) {
        raw.having(predicates);
        return this;
    }

    @Override
    public <R> ConfigurableRootQuery<T, R> select(Selection<R> selection) {
        ConfigurableRootQuery<T, R> result = raw.select(selection);
        onTerminate.run();
        return result;
    }

    @Override
    public <T1, T2> ConfigurableRootQuery<T, Tuple2<T1, T2>> select(
            Selection<T1> selection1,
            Selection<T2> selection2
    ) {
        ConfigurableRootQuery<T, Tuple2<T1, T2>> result =
                raw.select(selection1, selection2);
        onTerminate.run();
        return result;
    }

    @Override
    public <T1, T2, T3> ConfigurableRootQuery<T, Tuple3<T1, T2, T3>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3
    ) {
        ConfigurableRootQuery<T, Tuple3<T1, T2, T3>> result =
                raw.select(selection1, selection2, selection3);
        onTerminate.run();
        return result;
    }

    @Override
    public <T1, T2, T3, T4> ConfigurableRootQuery<T, Tuple4<T1, T2, T3, T4>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4
    ) {
        ConfigurableRootQuery<T, Tuple4<T1, T2, T3, T4>> result =
                raw.select(selection1, selection2, selection3, selection4);
        onTerminate.run();
        return result;
    }

    @Override
    public <T1, T2, T3, T4, T5> ConfigurableRootQuery<T, Tuple5<T1, T2, T3, T4, T5>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5
    ) {
        ConfigurableRootQuery<T, Tuple5<T1, T2, T3, T4, T5>> result =
                raw.select(selection1, selection2, selection3, selection4, selection5);
        onTerminate.run();
        return result;
    }

    @Override
    public <T1, T2, T3, T4, T5, T6> ConfigurableRootQuery<T, Tuple6<T1, T2, T3, T4, T5, T6>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6
    ) {
        ConfigurableRootQuery<T, Tuple6<T1, T2, T3, T4, T5, T6>> result =
                raw.select(selection1, selection2, selection3, selection4, selection5, selection6);
        onTerminate.run();
        return result;
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7> ConfigurableRootQuery<T, Tuple7<T1, T2, T3, T4, T5, T6, T7>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6,
            Selection<T7> selection7
    ) {
        ConfigurableRootQuery<T, Tuple7<T1, T2, T3, T4, T5, T6, T7>> result =
                raw.select(selection1, selection2, selection3, selection4, selection5, selection6, selection7);
        onTerminate.run();
        return result;
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, T8> ConfigurableRootQuery<T, Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6,
            Selection<T7> selection7,
            Selection<T8> selection8
    ) {
        ConfigurableRootQuery<T, Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> result =
                raw.select(
                        selection1,
                        selection2,
                        selection3,
                        selection4,
                        selection5,
                        selection6,
                        selection7,
                        selection8
                );
        onTerminate.run();
        return result;
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> ConfigurableRootQuery<T, Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6,
            Selection<T7> selection7,
            Selection<T8> selection8,
            Selection<T9> selection9
    ) {
        ConfigurableRootQuery<T, Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> result = raw.select(
                selection1,
                selection2,
                selection3,
                selection4,
                selection5,
                selection6,
                selection7,
                selection8,
                selection9
        );
        onTerminate.run();
        return result;
    }
}
