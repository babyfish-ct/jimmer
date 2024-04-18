package org.babyfish.jimmer.sql.kt.model.embedded

import org.babyfish.jimmer.Formula
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.PropOverride
import org.babyfish.jimmer.sql.kt.model.embedded.p4bug524.Point

@Entity
interface Transform {

    @Id
    val id: Long

    val source: Rect

    @PropOverride(prop = "leftTop.x", columnName = "TARGET_LEFT")
    @PropOverride(prop = "leftTop.y", columnName = "TARGET_TOP")
    @PropOverride(prop = "rightBottom.x", columnName = "TARGET_RIGHT")
    @PropOverride(prop = "rightBottom.y", columnName = "TARGET_BOTTOM")
    val target: Rect

    @Formula(dependencies = ["source.leftTop"])
    val sourceLeftTop: Point
        get() = source.leftTop
}