package org.babyfish.jimmer.sql.kt.ast.mutation.impl

import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.ast.query.selectable.ReturningSelectable
import org.babyfish.jimmer.sql.ast.tuple.*
import org.babyfish.jimmer.sql.kt.ast.KSelectionExecutable
import org.babyfish.jimmer.sql.kt.ast.mutation.KReturningSelectable
import org.babyfish.jimmer.sql.kt.impl.KSelectionExecutableImpl
import org.babyfish.jimmer.sql.runtime.TupleMapper

internal class KReturningSelectableImpl(
    private val javaSelectable: ReturningSelectable
) : KReturningSelectable {

    override fun <T> returning(selection: Selection<T>): KSelectionExecutable<T> =
        KSelectionExecutableImpl(javaSelectable.returning(selection))

    override fun <T1, T2> returning(
        s1: Selection<T1>,
        s2: Selection<T2>
    ): KSelectionExecutable<Tuple2<T1, T2>> =
        KSelectionExecutableImpl(javaSelectable.returning(s1, s2))

    override fun <T1, T2, T3> returning(
        s1: Selection<T1>,
        s2: Selection<T2>,
        s3: Selection<T3>
    ): KSelectionExecutable<Tuple3<T1, T2, T3>> =
        KSelectionExecutableImpl(javaSelectable.returning(s1, s2, s3))

    override fun <T1, T2, T3, T4> returning(
        s1: Selection<T1>,
        s2: Selection<T2>,
        s3: Selection<T3>,
        s4: Selection<T4>
    ): KSelectionExecutable<Tuple4<T1, T2, T3, T4>> =
        KSelectionExecutableImpl(javaSelectable.returning(s1, s2, s3, s4))

    override fun <T1, T2, T3, T4, T5> returning(
        s1: Selection<T1>,
        s2: Selection<T2>,
        s3: Selection<T3>,
        s4: Selection<T4>,
        s5: Selection<T5>
    ): KSelectionExecutable<Tuple5<T1, T2, T3, T4, T5>> =
        KSelectionExecutableImpl(javaSelectable.returning(s1, s2, s3, s4, s5))

    override fun <T1, T2, T3, T4, T5, T6> returning(
        s1: Selection<T1>,
        s2: Selection<T2>,
        s3: Selection<T3>,
        s4: Selection<T4>,
        s5: Selection<T5>,
        s6: Selection<T6>
    ): KSelectionExecutable<Tuple6<T1, T2, T3, T4, T5, T6>> =
        KSelectionExecutableImpl(javaSelectable.returning(s1, s2, s3, s4, s5, s6))

    override fun <T1, T2, T3, T4, T5, T6, T7> returning(
        s1: Selection<T1>,
        s2: Selection<T2>,
        s3: Selection<T3>,
        s4: Selection<T4>,
        s5: Selection<T5>,
        s6: Selection<T6>,
        s7: Selection<T7>
    ): KSelectionExecutable<Tuple7<T1, T2, T3, T4, T5, T6, T7>> =
        KSelectionExecutableImpl(javaSelectable.returning(s1, s2, s3, s4, s5, s6, s7))

    override fun <T1, T2, T3, T4, T5, T6, T7, T8> returning(
        s1: Selection<T1>,
        s2: Selection<T2>,
        s3: Selection<T3>,
        s4: Selection<T4>,
        s5: Selection<T5>,
        s6: Selection<T6>,
        s7: Selection<T7>,
        s8: Selection<T8>
    ): KSelectionExecutable<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> =
        KSelectionExecutableImpl(javaSelectable.returning(s1, s2, s3, s4, s5, s6, s7, s8))

    override fun <T1, T2, T3, T4, T5, T6, T7, T8, T9> returning(
        s1: Selection<T1>,
        s2: Selection<T2>,
        s3: Selection<T3>,
        s4: Selection<T4>,
        s5: Selection<T5>,
        s6: Selection<T6>,
        s7: Selection<T7>,
        s8: Selection<T8>,
        s9: Selection<T9>
    ): KSelectionExecutable<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> =
        KSelectionExecutableImpl(javaSelectable.returning(s1, s2, s3, s4, s5, s6, s7, s8, s9))

    override fun <T> returning(mapper: TupleMapper<T>): KSelectionExecutable<T> =
        KSelectionExecutableImpl(javaSelectable.returning(mapper))
}
