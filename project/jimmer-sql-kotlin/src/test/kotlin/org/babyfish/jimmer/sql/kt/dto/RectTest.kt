package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.common.assertContent
import org.babyfish.jimmer.sql.kt.model.embedded.Rect
import org.babyfish.jimmer.sql.kt.model.embedded.Transform
import org.babyfish.jimmer.sql.kt.model.embedded.dto.RectFlatView
import org.babyfish.jimmer.sql.kt.model.embedded.dto.RectView
import org.babyfish.jimmer.sql.kt.model.embedded.source
import org.babyfish.jimmer.sql.kt.model.embedded.target
import kotlin.test.Test

class RectTest : AbstractQueryTest() {

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

    @Test
    fun testQueryRectView() {
        executeAndExpect(
            sqlClient.createQuery(Transform::class) {
                select(
                    table.source.fetch(RectView::class),
                    table.target.fetch(RectFlatView::class)
                )
            }
        ) {
            sql(
                """select 
                    |tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, 
                    |tb_1_.BOTTOM, tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM 
                    |from TRANSFORM tb_1_""".trimMargin()
            )
            rows {
                assertContent(
                    """[
                        |--->Tuple2(
                        |--->--->_1=RectView(
                        |--->--->--->leftTop=RectView.TargetOf_leftTop(x=100, y=120), 
                        |--->--->--->rightBottom=RectView.TargetOf_rightBottom(x=400, y=320)
                        |--->--->), 
                        |--->--->_2=RectFlatView(ltX=800, ltY=600, rbX=1400, rbY=1000)
                        |--->)
                        |]""".trimMargin(),
                    it
                )
            }
        }
    }
}