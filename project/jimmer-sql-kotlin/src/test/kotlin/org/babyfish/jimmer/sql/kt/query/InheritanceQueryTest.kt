package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.inheritance.Role
import org.babyfish.jimmer.sql.kt.model.inheritance.fetchBy
import kotlin.test.Test

class InheritanceQueryTest : AbstractQueryTest() {

    @Test
    fun testFetchLonely() {
        executeAndExpect(
            sqlClient.createQuery(Role::class) {
                select(
                    table.fetchBy {
                        allScalarFields()
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME from ROLE as tb_1_"""
            )
            rows(
                """[{"name":"r_1","id":1},{"name":"r_2","id":2}]"""
            )
        }
    }

    @Test
    fun testFetchIdOnlyChildren() {
        executeAndExpect(
            sqlClient.createQuery(Role::class) {
                select(
                    table.fetchBy {
                        allScalarFields()
                        permissions()
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME from ROLE as tb_1_"""
            )
            statement(1).sql(
                """select tb_1_.ROLE_ID, tb_1_.ID 
                    |from PERMISSION as tb_1_ 
                    |where tb_1_.ROLE_ID in (?, ?)""".trimMargin()
            )
            rows(
                """[
                    |--->{"name":"r_1","permissions":[{"id":1},{"id":2}],"id":1},
                    |--->{"name":"r_2","permissions":[{"id":3},{"id":4}],"id":2}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testFetchChildren() {
        executeAndExpect(
            sqlClient.createQuery(Role::class) {
                select(
                    table.fetchBy {
                        allScalarFields()
                        permissions {
                            allScalarFields()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME from ROLE as tb_1_"""
            )
            statement(1).sql(
                """select tb_1_.ROLE_ID, tb_1_.ID, tb_1_.NAME 
                    |from PERMISSION as tb_1_ 
                    |where tb_1_.ROLE_ID in (?, ?)""".trimMargin()
            )
            rows(
                """[
                    |--->{
                    |--->--->"name":"r_1",
                    |--->--->"permissions":[
                    |--->--->--->{"name":"p_1","id":1},
                    |--->--->--->{"name":"p_2","id":2}
                    |--->--->],
                    |--->--->"id":1
                    |--->},{
                    |--->--->"name":"r_2",
                    |--->--->"permissions":[
                    |--->--->--->{"name":"p_3","id":3},
                    |--->--->--->{"name":"p_4","id":4}
                    |--->--->],
                    |--->--->"id":2
                    |--->}
                    |]""".trimMargin()
            )
        }
    }
}