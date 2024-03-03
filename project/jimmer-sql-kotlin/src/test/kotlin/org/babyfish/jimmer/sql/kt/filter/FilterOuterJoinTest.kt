package org.babyfish.jimmer.sql.kt.filter

import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.hr.Department
import org.babyfish.jimmer.sql.kt.model.hr.`employees?`
import org.babyfish.jimmer.sql.kt.model.hr.fetchBy
import org.babyfish.jimmer.sql.kt.model.hr.name
import kotlin.test.Test

class FilterOuterJoinTest : AbstractQueryTest() {

    @Test
    fun test() {
        executeAndExpect(
            sqlClient
                .createQuery(Department::class) {
                    where(table.asTableEx().`employees?`.name eq "Jessica")
                    select(
                        table.fetchBy {
                            name()
                            employeeCount()
                        }
                    )
                }
        ) {
            sql(
                "select " +
                    "tb_1_.ID, " +
                    "tb_1_.NAME, " +
                    "(select count(*) from employee where department_id = tb_1_.id) " +
                    "from DEPARTMENT tb_1_ " +
                    "left join EMPLOYEE tb_2_ " +
                    "--->on tb_1_.ID = tb_2_.DEPARTMENT_ID " +
                    "--->and tb_2_.DELETED_UUID is null " +
                    "where " +
                    "--->tb_2_.NAME = ? " +
                    "--->and " +
                    "--->tb_1_.DELETED_TIME is null"
            )
            rows(
                "[{\"id\":\"1\",\"name\":\"Market\",\"employeeCount\":2}]"
            )
        }
    }
}