package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.ast.tuple.*
import org.babyfish.jimmer.sql.kt.ast.KExecutable
import org.babyfish.jimmer.sql.runtime.TupleMapper

interface KReturningSelectable {

    fun <T> returning(selection: Selection<T>): KExecutable<List<T>>

    fun <T1, T2> returning(
        selection1: Selection<T1>,
        selection2: Selection<T2>
    ): KExecutable<List<Tuple2<T1, T2>>>

    fun <T1, T2, T3> returning(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>
    ): KExecutable<List<Tuple3<T1, T2, T3>>>

    fun <T1, T2, T3, T4> returning(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>,
        selection4: Selection<T4>
    ): KExecutable<List<Tuple4<T1, T2, T3, T4>>>

    fun <T1, T2, T3, T4, T5> returning(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>,
        selection4: Selection<T4>,
        selection5: Selection<T5>
    ): KExecutable<List<Tuple5<T1, T2, T3, T4, T5>>>

    fun <T1, T2, T3, T4, T5, T6> returning(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>,
        selection4: Selection<T4>,
        selection5: Selection<T5>,
        selection6: Selection<T6>
    ): KExecutable<List<Tuple6<T1, T2, T3, T4, T5, T6>>>

    fun <T1, T2, T3, T4, T5, T6, T7> returning(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>,
        selection4: Selection<T4>,
        selection5: Selection<T5>,
        selection6: Selection<T6>,
        selection7: Selection<T7>
    ): KExecutable<List<Tuple7<T1, T2, T3, T4, T5, T6, T7>>>

    fun <T1, T2, T3, T4, T5, T6, T7, T8> returning(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>,
        selection4: Selection<T4>,
        selection5: Selection<T5>,
        selection6: Selection<T6>,
        selection7: Selection<T7>,
        selection8: Selection<T8>
    ): KExecutable<List<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>>>

    fun <T1, T2, T3, T4, T5, T6, T7, T8, T9> returning(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>,
        selection4: Selection<T4>,
        selection5: Selection<T5>,
        selection6: Selection<T6>,
        selection7: Selection<T7>,
        selection8: Selection<T8>,
        selection9: Selection<T9>
    ): KExecutable<List<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>>>

    fun <T> returning(mapper: TupleMapper<T>): KExecutable<List<T>>
}
