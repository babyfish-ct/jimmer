package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.common.assertContentEquals
import org.babyfish.jimmer.sql.kt.model.embedded.*
import org.babyfish.jimmer.sql.kt.model.embedded.dto.TransformFlatView
import org.babyfish.jimmer.sql.kt.model.embedded.dto.TransformSpecification
import org.babyfish.jimmer.sql.kt.model.embedded.dto.TransformView
import org.babyfish.jimmer.sql.kt.model.embedded.dto.TransformView2
import kotlin.test.Test

class EmbeddedTest : AbstractQueryTest() {

    @Test
    fun testFindBySourceLeftTopX() {
        executeAndExpect(
            sqlClient.createQuery(Transform::class) {
                where(table.source.leftTop.x eq 100)
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, 
                    |tb_1_.BOTTOM, tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM 
                    |from TRANSFORM tb_1_ 
                    |where tb_1_.`LEFT` = ?""".trimMargin()
            )
            variables(100L)
            rows(ROWS)
        }
    }

    @Test
    fun testFindBySourceLeftTop() {
        executeAndExpect(
            sqlClient.createQuery(Transform::class) {
                where(table.source.leftTop eq new(Point::class).by {
                    x = 100
                    y = 120
                })
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, 
                    |tb_1_.BOTTOM, tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM 
                    |from TRANSFORM tb_1_ 
                    |where (tb_1_.`LEFT`, tb_1_.TOP) = (?, ?)""".trimMargin()
            )
            variables(100L, 120L)
            rows(ROWS)
        }
    }

    @Test
    fun testFindBySource() {
        executeAndExpect(
            sqlClient.createQuery(Transform::class) {
                where(table.source eq new(Rect::class).by {
                    leftTop().apply {
                        x = 100
                        y = 120
                    }
                    rightBottom().apply {
                        x = 400
                        y = 320
                    }
                })
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, 
                    |tb_1_.BOTTOM, tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM 
                    |from TRANSFORM tb_1_ 
                    |where (tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM) = (?, ?, ?, ?)""".trimMargin()
            )
            variables(100L, 120L, 400L, 320L)
            rows(ROWS)
        }
    }

    @Test
    fun testSelectLevel1() {
        executeAndExpect(
            sqlClient.createQuery(Transform::class) {
                select(
                    table.id,
                    table.source,
                    table.target
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, 
                    |tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM 
                    |from TRANSFORM tb_1_""".trimMargin()
            )
            rows(
                """[
                    |--->{
                    |--->--->"_1":1,
                    |--->--->"_2":{
                    |--->--->--->"leftTop":{"x":100,"y":120},
                    |--->--->--->"rightBottom":{"x":400,"y":320}
                    |--->--->},
                    |--->--->"_3":{
                    |--->--->--->"leftTop":{"x":800,"y":600},
                    |--->--->--->"rightBottom":{"x":1400,"y":1000}
                    |--->--->}
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testSelectLevel2() {
        executeAndExpect(
            sqlClient.createQuery(Transform::class) {
                select(
                    table.id,
                    table.source.leftTop,
                    table.source.rightBottom,
                    table.target.leftTop,
                    table.target.rightBottom
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, 
                    |tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM 
                    |from TRANSFORM tb_1_""".trimMargin()
            )
            rows(
                """[
                    |--->{
                    |--->--->"_1":1,
                    |--->--->"_2":{"x":100,"y":120},
                    |--->--->"_3":{"x":400,"y":320},
                    |--->--->"_4":{"x":800,"y":600},
                    |--->--->"_5":{"x":1400,"y":1000}
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testSelectLevel3() {
        executeAndExpect(
            sqlClient.createQuery(Transform::class) {
                select(
                    table.id,
                    table.source.leftTop.x,
                    table.source.leftTop.y,
                    table.source.rightBottom.x,
                    table.source.rightBottom.y,
                    table.target.leftTop.x,
                    table.target.leftTop.y,
                    table.target.rightBottom.x,
                    table.target.rightBottom.y
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, 
                    |tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM 
                    |from TRANSFORM tb_1_""".trimMargin()
            )
            rows(
                """[
                    |--->{
                    |--->--->"_1":1,
                    |--->--->"_2":100,
                    |--->--->"_3":120,
                    |--->--->"_4":400,
                    |--->--->"_5":320,
                    |--->--->"_6":800,
                    |--->--->"_7":600,
                    |--->--->"_8":1400,
                    |--->--->"_9":1000
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testObjectFetcher() {
        executeAndExpect(
            sqlClient.createQuery(Transform::class) {
                select(
                    table.fetchBy {
                        source {
                            leftTop {
                                x()
                            }
                        }
                        target {
                            rightBottom {
                                y()
                            }
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.`LEFT`, tb_1_.TARGET_BOTTOM 
                    |from TRANSFORM tb_1_""".trimMargin()
            )
            rows(
                """[{"id":1,"source":{"leftTop":{"x":100}},"target":{"rightBottom":{"y":1000}}}]"""
            )
        }
    }

    @Test
    fun testDto() {
        executeAndExpect(
            sqlClient.createQuery(Transform::class) {
                select(
                    table.fetch(TransformView::class)
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.`LEFT`, tb_1_.TARGET_BOTTOM 
                    |from TRANSFORM tb_1_""".trimMargin()
            )
            rows {
                assertContentEquals(
                    """[
                        |--->TransformView(
                        |--->--->id=1, 
                        |--->--->source=TransformView.TargetOf_source(
                        |--->--->--->leftTop=TransformView.TargetOf_source.TargetOf_leftTop(x=100)
                        |--->--->), 
                        |--->--->target=TransformView.TargetOf_target(
                        |--->--->--->rightBottom=TransformView.TargetOf_target.TargetOf_rightBottom(
                        |--->--->--->--->y=1000
                        |--->--->--->)
                        |--->--->)
                        |--->)
                        |]""".trimMargin(),
                    it
                )
            }
        }
    }

    @Test
    fun testDtoWithFormula() {
        executeAndExpect(
            sqlClient.createQuery(Transform::class) {
                select(
                    table.fetch(TransformView2::class)
                )
            }
        ) {
            sql(
                """select tb_1_.ID, 
                    |tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, 
                    |tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM, tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP 
                    |from TRANSFORM tb_1_""".trimMargin()
            )
            rows {
                assertContentEquals(
                    "[" +
                        "--->TransformView2(" +
                        "--->--->id=1, " +
                        "--->--->source=TransformView2.TargetOf_source(" +
                        "--->--->--->area=60000, " +
                        "--->--->--->leftTop=TransformView2.TargetOf_source.TargetOf_leftTop(x=100)" +
                        "--->--->), " +
                        "--->--->target=TransformView2.TargetOf_target(" +
                        "--->--->--->area=240000, " +
                        "--->--->--->rightBottom=TransformView2.TargetOf_target.TargetOf_rightBottom(y=1000)" +
                        "--->--->)" +
                        "--->)" +
                        "]",
                    it
                )
            }
        }
    }

    @Test
    fun testFlatDto() {
        executeAndExpect(
            sqlClient.createQuery(Transform::class) {
                select(
                    table.fetch(TransformFlatView::class)
                )
            }
        ) {
            sql(
                """select tb_1_.ID, 
                    |tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, 
                    |tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM 
                    |from TRANSFORM tb_1_""".trimMargin()
            )
            rows {
                assertContentEquals(
                    """[
                        |--->TransformFlatView(
                        |--->--->id=1, 
                        |--->--->sourceX1=100, 
                        |--->--->sourceY1=120, 
                        |--->--->sourceX2=400, 
                        |--->--->sourceY2=320, 
                        |--->--->targetX1=800, 
                        |--->--->targetY1=600, 
                        |--->--->targetX2=1400, 
                        |--->--->targetY2=1000
                        |--->)
                        |]""".trimMargin(),
                    it
                )
            }
        }
    }

    @Test
    fun testFormulaDependsOnEmbeddable() {
        executeAndExpect(
            sqlClient.createQuery(Machine::class) {
                select(
                    table.fetchBy {
                        factoryCount()
                        factoryNames()
                        detail {
                            patents()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.patent_map, tb_1_.factory_map from MACHINE tb_1_"""
            )
            rows(
                """[
                    |--->{
                    |--->--->"id":1,
                    |--->--->"detail":{"patents":{"p-1":"patent-1","p-2":"patent-2"}},
                    |--->--->"factoryCount":2,
                    |--->--->"factoryNames":["f-1","f-2"]
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testFormulaDependsOnDuplicatedEmbeddable() {
        executeAndExpect(
            sqlClient.createQuery(Machine::class) {
                select(
                    table.fetchBy {
                        factoryCount()
                        detail {
                            patents()
                            factories()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.patent_map, tb_1_.factory_map from MACHINE tb_1_"""
            )
            rows(
                """[
                    |--->{
                    |--->--->"id":1,
                    |--->--->"detail":{
                    |--->--->--->"factories":{"f-1":"factory-1","f-2":"factory-2"},
                    |--->--->--->"patents":{"p-1":"patent-1","p-2":"patent-2"}
                    |--->--->},
                    |--->--->"factoryCount":2
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testFormulaInEmbeddable() {
        executeAndExpect(
            sqlClient.createQuery(Transform::class) {
                select(
                    table.fetchBy {
                        source {
                            area()
                        }
                        target {
                            area()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, 
                    |tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, 
                    |tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM 
                    |from TRANSFORM tb_1_""".trimMargin()
            )
            rows(
                """[{"id":1,"source":{"area":60000},"target":{"area":240000}}]"""
            )
        }
    }

    @Test
    fun testFormulaInEmbeddableWithDuplicatedFetching() {
        executeAndExpect(
            sqlClient.createQuery(Transform::class) {
                select(
                    table.fetchBy {
                        source {
                            area()
                            leftTop {
                                x()
                            }
                        }
                        target {
                            area()
                            rightBottom {
                                y()
                            }
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, 
                    |tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, 
                    |tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM, tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP 
                    |from TRANSFORM tb_1_""".trimMargin()
            )
            rows(
                """[
                    |--->{
                    |--->--->"id":1,
                    |--->--->"source":{
                    |--->--->--->"leftTop":{"x":100},
                    |--->--->--->"area":60000
                    |--->--->},
                    |--->--->"target":{
                    |--->--->--->"rightBottom":{"y":1000},
                    |--->--->--->"area":240000
                    |--->--->}
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    companion object {
        private val ROWS = """[
            |--->{
            |--->--->"id":1,
            |--->--->"source":{
            |--->--->--->"leftTop":{"x":100,"y":120},
            |--->--->--->"rightBottom":{"x":400,"y":320}
            |--->--->},
            |--->--->"target":{
            |--->--->--->"leftTop":{"x":800,"y":600},
            |--->--->--->"rightBottom":{"x":1400,"y":1000}
            |--->--->}
            |--->}
            |]""".trimMargin()
    }

    @Test
    fun testSpecification() {
        executeAndExpect(
            sqlClient.createQuery(Transform::class) {
                where(
                    TransformSpecification(
                        minX = 100L,
                        maxX = 2000L
                    )
                )
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, 
                    |tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, 
                    |tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM 
                    |from TRANSFORM tb_1_ 
                    |where tb_1_.`LEFT` >= ? and tb_1_.TARGET_RIGHT <= ?""".trimMargin()
            )
            rows(
                """[{
                    |--->"id":1,
                    |--->"source":{
                    |--->--->"leftTop":{"x":100,"y":120},
                    |--->--->"rightBottom":{"x":400,"y":320}
                    |--->},
                    |--->"target":{
                    |--->--->"leftTop":{"x":800,"y":600},
                    |--->--->"rightBottom":{"x":1400,"y":1000}
                    |--->}
                    |}]""".trimMargin()
            )
        }
    }
}