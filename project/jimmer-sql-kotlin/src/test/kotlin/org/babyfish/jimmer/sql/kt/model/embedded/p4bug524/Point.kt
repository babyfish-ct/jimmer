package org.babyfish.jimmer.sql.kt.model.embedded.p4bug524

import org.babyfish.jimmer.Formula
import org.babyfish.jimmer.sql.Embeddable
import kotlin.math.sqrt

@Embeddable
interface Point {

    val x: Long

    val y: Long

    @Formula(dependencies = ["x", "y"])
    val distance: Double
        get() = sqrt((x * x + y * y).toDouble())
}