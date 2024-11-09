package org.babyfish.jimmer.sql.kt.meta

import org.babyfish.jimmer.sql.kt.ast.expression.UnsignedByte.div
import org.babyfish.jimmer.sql.kt.ast.expression.UnsignedByte.minus
import org.babyfish.jimmer.sql.kt.ast.expression.UnsignedByte.plus
import org.babyfish.jimmer.sql.kt.ast.expression.UnsignedByte.times
import org.babyfish.jimmer.sql.kt.ast.expression.UnsignedInt.div
import org.babyfish.jimmer.sql.kt.ast.expression.UnsignedInt.minus
import org.babyfish.jimmer.sql.kt.ast.expression.UnsignedInt.plus
import org.babyfish.jimmer.sql.kt.ast.expression.UnsignedInt.times
import org.babyfish.jimmer.sql.kt.ast.expression.UnsignedLong.div
import org.babyfish.jimmer.sql.kt.ast.expression.UnsignedLong.minus
import org.babyfish.jimmer.sql.kt.ast.expression.UnsignedLong.plus
import org.babyfish.jimmer.sql.kt.ast.expression.UnsignedLong.times
import org.babyfish.jimmer.sql.kt.ast.expression.UnsignedShort.div
import org.babyfish.jimmer.sql.kt.ast.expression.UnsignedShort.minus
import org.babyfish.jimmer.sql.kt.ast.expression.UnsignedShort.plus
import org.babyfish.jimmer.sql.kt.ast.expression.UnsignedShort.times
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.common.assertContent
import org.babyfish.jimmer.sql.kt.model.*
import org.babyfish.jimmer.sql.model.dto.UnsignedNumberInput
import org.babyfish.jimmer.sql.model.dto.UnsignedNumberView
import kotlin.test.Test

class UnsignedNumberTest : AbstractQueryTest() {
    @Test
    fun test() {
        UnsignedNumber {
            id = 1u
            unsignedInt = 1u
            unsignedShort = 1u
            unsignedByte = 1u
        }
    }

    @Test
    fun testInput() {
        assertContent(
            "{\"id\":1,\"unsignedInt\":1,\"unsignedShort\":1,\"unsignedByte\":1}",
            UnsignedNumberInput(
                id = 1u,
                unsignedInt = 1u,
                unsignedShort = 1u,
                unsignedByte = 1u
            ).toEntity()
        )
    }

    @Test
    fun testQueryTuple() {
        executeAndExpect(
            sqlClient.createQuery(UnsignedNumber::class) {
                where(table.id eq 1u)
                select(
                    table.id.plus(1u).minus(1u).times(2u).div(2u),
                    table.unsignedInt.plus(1u).minus(1u).times(2u).div(2u),
                    (table.unsignedShort + 1u - 1u) * 2u / 2u,
                    (table.unsignedByte + 1u - 1u) * 2u / 2u
                )
            }
        ) {
            statement(0)
                .sql("select (tb_1_.ID + ? - ?) * ? / ?, (tb_1_.UNSIGNED_INT + ? - ?) * ? / ?, (tb_1_.UNSIGNED_SHORT + ? - ?) * ? / ?, (tb_1_.UNSIGNED_BYTE + ? - ?) * ? / ? from UNSIGNED_NUMBER tb_1_ where tb_1_.ID = ?")
            rows("[{\"_1\":1,\"_2\":1,\"_3\":1,\"_4\":1}]")
        }
    }

    @Test
    fun testQueryFetch() {
        executeAndExpect(
            sqlClient.createQuery(UnsignedNumber::class) {
                where(table.id eq 1u)
                select(table.fetchBy {
                    unsignedInt()
                    unsignedShort()
                    unsignedByte()
                })
            }
        ) {
            statement(0)
                .sql("select tb_1_.ID, tb_1_.UNSIGNED_INT, tb_1_.UNSIGNED_SHORT, tb_1_.UNSIGNED_BYTE from UNSIGNED_NUMBER tb_1_ where tb_1_.ID = ?")
            rows("[{\"id\":1,\"unsignedInt\":1,\"unsignedShort\":1,\"unsignedByte\":1}]")
        }
    }

    @Test
    fun testQueryView() {
        executeAndExpect(
            sqlClient.createQuery(UnsignedNumber::class) {
                where(table.id eq 1u)
                select(table.fetch(UnsignedNumberView::class))
            }
        ) {
            statement(0)
                .sql("select tb_1_.ID, tb_1_.UNSIGNED_INT, tb_1_.UNSIGNED_SHORT, tb_1_.UNSIGNED_BYTE from UNSIGNED_NUMBER tb_1_ where tb_1_.ID = ?")
            rows("[{\"id\":1,\"unsignedInt\":1,\"unsignedShort\":1,\"unsignedByte\":1}]")
        }
    }

    @Test
    fun testQuery() {
        executeAndExpect(
            sqlClient.createQuery(UnsignedNumber::class) {
                where(table.id eq 1u)
                select(table)
            }
        ) {
            statement(0)
                .sql("select tb_1_.ID, tb_1_.UNSIGNED_INT, tb_1_.UNSIGNED_SHORT, tb_1_.UNSIGNED_BYTE from UNSIGNED_NUMBER tb_1_ where tb_1_.ID = ?")
            rows("[{\"id\":1,\"unsignedInt\":1,\"unsignedShort\":1,\"unsignedByte\":1}]")
        }
    }
}