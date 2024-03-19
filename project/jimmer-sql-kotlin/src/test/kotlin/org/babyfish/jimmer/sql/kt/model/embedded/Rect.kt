package org.babyfish.jimmer.sql.kt.model.embedded

import org.babyfish.jimmer.Formula
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.PropOverride

@Embeddable
interface Rect {

    @PropOverride(prop = "x", columnName = "`LEFT`")
    @PropOverride(prop = "y", columnName = "TOP")
    val leftTop: Point

    @PropOverride(prop = "x", columnName = "`RIGHT`")
    @PropOverride(prop = "y", columnName = "BOTTOM")
    val rightBottom: Point?

    @Formula(dependencies = ["leftTop.x", "leftTop.y", "rightBottom.x", "rightBottom.y"])
    val area: Long?
        get() = rightBottom?.let {
            (it.x - leftTop.x) * (it.y - leftTop.y)
        }
}