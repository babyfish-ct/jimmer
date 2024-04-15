package org.babyfish.jimmer.sql.kt.mutation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.embedded.Transform
import org.babyfish.jimmer.sql.kt.model.embedded.by
import org.babyfish.jimmer.sql.kt.model.embedded.dto.DynamicRectInput
import kotlin.test.Test

class EmbeddedMutationTest : AbstractMutationTest() {

    @Test
    fun testIssue527() {
        val sourceJson = "{" +
            "    \"leftTop\": {\"x\": 1}, " +
            "    \"rightBottom\": {\"y\": 2} " +
            "}"
        val targetJson = "{" +
            "    \"leftTop\": {\"y\": 3}, " +
            "    \"rightBottom\": {\"x\": 4} " +
            "}"
        val transform = new(Transform::class).by {
            id = 1L
            source = jacksonObjectMapper()
                .readValue(sourceJson, DynamicRectInput::class.java)
                .toImmutable()
            target = jacksonObjectMapper()
                .readValue(targetJson, DynamicRectInput::class.java)
                .toImmutable()
        }
        connectAndExpect({
            sqlClient.entities.forConnection(it).save(transform) {
                setMode(SaveMode.UPDATE_ONLY)
            }.modifiedEntity
        }) {
            statement {
                sql(
                    """update TRANSFORM 
                        |set `LEFT` = ?, BOTTOM = ?, TARGET_TOP = ?, TARGET_RIGHT = ? 
                        |where ID = ?""".trimMargin()
                )
                variables(1L, 2L, 3L, 4L, 1L)
                value(
                    """{
                        |--->"id":1,
                        |--->"source":{"leftTop":{"x":1},"rightBottom":{"y":2}},
                        |--->"target":{"leftTop":{"y":3},"rightBottom":{"x":4}}
                        |}""".trimMargin()
                )
            }
        }
    }
}