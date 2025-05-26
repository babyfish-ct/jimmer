package org.babyfish.jimmer.sql.ast.query.selectable;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.babyfish.jimmer.sql.ast.tuple.*;

public interface RootSelectable<T extends TableLike<?>> {

    <R> ConfigurableRootQuery<T, R> select(
            Selection<R> selection
    );

    <T1, T2> ConfigurableRootQuery<T, Tuple2<T1, T2>> select(
            Selection<T1> selection1,
            Selection<T2> selection2
    );

    <T1, T2, T3> ConfigurableRootQuery<T, Tuple3<T1, T2, T3>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3
    );

    <T1, T2, T3, T4> ConfigurableRootQuery<T, Tuple4<T1, T2, T3, T4>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4
    );

    <T1, T2, T3, T4, T5> ConfigurableRootQuery<T, Tuple5<T1, T2, T3, T4, T5>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5
    );

    <T1, T2, T3, T4, T5, T6> ConfigurableRootQuery<T, Tuple6<T1, T2, T3, T4, T5, T6>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6
    );

    <T1, T2, T3, T4, T5, T6, T7> ConfigurableRootQuery<T, Tuple7<T1, T2, T3, T4, T5, T6, T7>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6,
            Selection<T7> selection7
    );

    <T1, T2, T3, T4, T5, T6, T7, T8> ConfigurableRootQuery<T, Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6,
            Selection<T7> selection7,
            Selection<T8> selection8
    );

    <T1, T2, T3, T4, T5, T6, T7, T8, T9> ConfigurableRootQuery<T, Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> select(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6,
            Selection<T7> selection7,
            Selection<T8> selection8,
            Selection<T9> selection9
    );

    default ConfigurableRootQuery<T, Long> selectCount() {
        return select(Expression.rowCount());
    }
}
