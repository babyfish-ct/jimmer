package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.Primitive
import kotlin.test.Test

class PrimitiveTest : AbstractQueryTest() {

    @Test
    fun test() {
        executeAndExpect(
            sqlClient.createQuery(Primitive::class) {
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, 
                    |tb_1_.BOOLEAN_VALUE, tb_1_.BOOLEAN_REF, 
                    |tb_1_.CHAR_VALUE, tb_1_.CHAR_REF, 
                    |tb_1_.BYTE_VALUE, tb_1_.BYTE_REF, 
                    |tb_1_.SHORT_VALUE, tb_1_.SHORT_REF, 
                    |tb_1_.INT_VALUE, tb_1_.INT_REF, 
                    |tb_1_.LONG_VALUE, tb_1_.LONG_REF, 
                    |tb_1_.FLOAT_VALUE, tb_1_.FLOAT_REF, 
                    |tb_1_.DOUBLE_VALUE, tb_1_.DOUBLE_REF 
                    |from PRIMITIVE tb_1_""".trimMargin()
            )
            rows("""[
                |--->{
                |--->--->"id":1,
                |--->--->"booleanValue":true,
                |--->--->"booleanRef":true,
                |--->--->"charValue":"X",
                |--->--->"charRef":"X",
                |--->--->"byteValue":3,
                |--->--->"byteRef":3,
                |--->--->"shortValue":4,
                |--->--->"shortRef":4,
                |--->--->"intValue":5,
                |--->--->"intRef":5,
                |--->--->"longValue":6,
                |--->--->"longRef":6,
                |--->--->"floatValue":7.0,
                |--->--->"floatRef":7.0,
                |--->--->"doubleValue":8.0,
                |--->--->"doubleRef":8.0
                |--->},{
                |--->--->"id":2,
                |--->--->"booleanValue":true,
                |--->--->"booleanRef":null,
                |--->--->"charValue":"X",
                |--->--->"charRef":null,
                |--->--->"byteValue":3,
                |--->--->"byteRef":null,
                |--->--->"shortValue":4,
                |--->--->"shortRef":null,
                |--->--->"intValue":5,
                |--->--->"intRef":null,
                |--->--->"longValue":6,
                |--->--->"longRef":null,
                |--->--->"floatValue":7.0,
                |--->--->"floatRef":null,
                |--->--->"doubleValue":8.0,
                |--->--->"doubleRef":null
                |--->}
                |]""".trimMargin())
        }
    }
}