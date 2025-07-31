package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.bug1126.WorkUser
import org.babyfish.jimmer.sql.kt.model.bug1126.dto.UserSpec
import kotlin.test.Test

class Issue1126Test : AbstractQueryTest() {

    @Test
    fun test() {
        executeAndExpect(
            sqlClient.createQuery(WorkUser::class) {
                where(UserSpec())
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME from WORK_USER tb_1_"""
            )
        }
    }
}