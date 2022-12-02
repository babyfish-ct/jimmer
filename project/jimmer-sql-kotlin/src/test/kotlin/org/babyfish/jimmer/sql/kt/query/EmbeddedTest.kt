package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.embedded.*
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
                    |from TRANSFORM as tb_1_ 
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
                    |from TRANSFORM as tb_1_ 
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
                    |from TRANSFORM as tb_1_ 
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
                    |from TRANSFORM as tb_1_""".trimMargin()
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
                    |from TRANSFORM as tb_1_""".trimMargin()
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
                    |from TRANSFORM as tb_1_""".trimMargin()
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
}