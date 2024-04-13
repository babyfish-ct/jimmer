package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.common.assertContent
import org.babyfish.jimmer.sql.kt.model.embedded.Rect
import org.babyfish.jimmer.sql.kt.model.embedded.dto.RectFlatView
import org.babyfish.jimmer.sql.kt.model.embedded.dto.RectView
import org.babyfish.jimmer.sql.kt.model.embedded.invoke
import kotlin.test.Test

class RectTest {

    @Test
    fun testRectView() {
        val rect = Rect {
            leftTop {
                x = 1
                y = 4
            }
            rightBottom {
                x = 9
                y = 16
            }
        }
        val view = RectView(rect)
        assertContent(
            """RectView(
                |--->leftTop=RectView.TargetOf_leftTop(x=1, y=4), 
                |--->rightBottom=RectView.TargetOf_rightBottom(x=9, y=16)
                |)""".trimMargin(),
            view
        )
        assertContent(
            """{"leftTop":{"x":1,"y":4},"rightBottom":{"x":9,"y":16}}""",
            view.toImmutable()
        )
    }

    @Test
    fun testRectFlatView() {
        val rect = Rect {
            leftTop {
                x = 1
                y = 4
            }
            rightBottom {
                x = 9
                y = 16
            }
        }
        val view = RectFlatView(rect)
        assertContent(
            """RectFlatView(ltX=1, ltY=4, rbX=9, rbY=16)""".trimMargin(),
            view
        )
        assertContent(
            """{"leftTop":{"x":1,"y":4},"rightBottom":{"x":9,"y":16}}""",
            view.toImmutable()
        )
    }
}