package org.babyfish.jimmer.kt.model

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.Scalar

@Immutable
interface Data {

    @Scalar
    val list: List<Long>

    @Scalar
    val nestedList: List<List<Long>>

    @Scalar
    val arr: LongArray

    @Scalar
    val nestedArr: Array<LongArray>
}