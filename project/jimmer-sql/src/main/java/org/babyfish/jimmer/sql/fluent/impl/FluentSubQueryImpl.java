package org.babyfish.jimmer.sql.fluent.impl;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.tuple.*;
import org.babyfish.jimmer.sql.fluent.FluentSubQuery;

class FluentSubQueryImpl implements FluentSubQuery {

    private final MutableSubQuery raw;

    private final Runnable onTerminate;

    public FluentSubQueryImpl(MutableSubQuery raw, Runnable onTerminate) {
        this.raw = raw;
        this.onTerminate = onTerminate;
    }

    @Override
    @OldChain
    public FluentSubQuery where(Predicate... predicates) {
        raw.where(predicates);
        return this;
    }

    @Override
    @OldChain
    public FluentSubQuery orderBy(Expression<?> ... expressions) {
        raw.orderBy(expressions);
        return this;
    }

    @Override
    @OldChain
    public FluentSubQuery orderBy(Order... orders) {
        raw.orderBy(orders);
        return this;
    }

    @Override
    @OldChain
    public FluentSubQuery groupBy(Expression<?>... expressions) {
        raw.groupBy(expressions);
        return this;
    }

    @Override
    @OldChain
    public FluentSubQuery having(Predicate... predicates) {
        raw.having(predicates);
        return this;
    }

    @Override
    public Predicate exists() {
        Predicate result = raw.exists();
        onTerminate.run();
        return result;
    }

    @Override
    public Predicate notExists() {
        Predicate result = raw.notExists();
        onTerminate.run();
        return result;
    }

    @Override
    public <R> ConfigurableSubQuery<R> select(Selection<R> selection) {
        ConfigurableSubQuery<R> result = raw.select(selection);
        onTerminate.run();
        return result;
    }

    @Override
    public <T1, T2> ConfigurableSubQuery<Tuple2<T1, T2>> select(
            Selection<T1> selection1,
            Selection<T2> selection2
    ) {
        ConfigurableSubQuery<Tuple2<T1, T2>> result = raw.select(selection1, selection2);
        onTerminate.run();
        return result;
    }

    @Override
    public <T1, T2, T3> ConfigurableSubQuery<Tuple3<T1, T2, T3>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3
    ) {
        ConfigurableSubQuery<Tuple3<T1, T2, T3>> result =  raw.select(selection1, selection2, selection3);
        onTerminate.run();
        return result;
    }

    @Override
    public <T1, T2, T3, T4> ConfigurableSubQuery<Tuple4<T1, T2, T3, T4>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4
    ) {
        ConfigurableSubQuery<Tuple4<T1, T2, T3, T4>> result =
                raw.select(selection1, selection2, selection3, selection4);
        onTerminate.run();
        return result;
    }

    @Override
    public <T1, T2, T3, T4, T5> ConfigurableSubQuery<Tuple5<T1, T2, T3, T4, T5>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5
    ) {
        ConfigurableSubQuery<Tuple5<T1, T2, T3, T4, T5>> result =
                raw.select(selection1, selection2, selection3, selection4, selection5);
        onTerminate.run();
        return result;
    }

    @Override
    public <T1, T2, T3, T4, T5, T6> ConfigurableSubQuery<Tuple6<T1, T2, T3, T4, T5, T6>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6
    ) {
        ConfigurableSubQuery<Tuple6<T1, T2, T3, T4, T5, T6>> result =
                raw.select(selection1, selection2, selection3, selection4, selection5, selection6);
        onTerminate.run();
        return result;
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7> ConfigurableSubQuery<Tuple7<T1, T2, T3, T4, T5, T6, T7>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6,
            Selection<T7> selection7
    ) {
        ConfigurableSubQuery<Tuple7<T1, T2, T3, T4, T5, T6, T7>> result =
                raw.select(selection1, selection2, selection3, selection4, selection5, selection6, selection7);
        onTerminate.run();
        return result;
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, T8> ConfigurableSubQuery<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6,
            Selection<T7> selection7,
            Selection<T8> selection8
    ) {
        ConfigurableSubQuery<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> result = raw.select(
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
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> ConfigurableSubQuery<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> select(
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
        ConfigurableSubQuery<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> result =
                raw.select(
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
