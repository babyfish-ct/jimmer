package org.babyfish.jimmer.sql.kt.json

import org.babyfish.jimmer.sql.dialect.PostgresDialect
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.model.classic.author.Gender
import org.babyfish.jimmer.sql.kt.model.pg.PgArrayModel
import org.babyfish.jimmer.sql.kt.model.pg.PgEnumArrayModel
import org.babyfish.jimmer.sql.kt.model.pg.Priority
import org.babyfish.jimmer.sql.kt.model.pg.priorities
import org.babyfish.jimmer.sql.kt.model.pg.textList
import org.babyfish.jimmer.sql.kt.model.pg.varcharList
import org.junit.Assume
import org.postgresql.Driver
import org.springframework.jdbc.datasource.SimpleDriverDataSource
import javax.sql.DataSource
import kotlin.test.Test

class QueryArrayTest : AbstractQueryTest() {

    private val _sqlClient: KSqlClient =
        sqlClient {
            setDialect(PostgresDialect())
        }

    @Test
    fun testQuery() {
        Assume.assumeTrue(
            (System.getenv("jimmer-sql-test-native-database") ?: "").let {
                it.isNotEmpty() && it != "false"
            }
        )
        executeAndExpect(
            POSTGRES_DATA_SOURCE,
            _sqlClient.createQuery(PgArrayModel::class) {
                select(table)
            }
        ) {
            sql(
                """select 
                    |tb_1_.ID, 
                    |tb_1_.INT_ARR, tb_1_.INTEGER_ARR, 
                    |tb_1_.TEXT_ARR, tb_1_.TEXT_LIST, tb_1_.VARCHAR_ARR, tb_1_.VARCHAR_LIST 
                    |from PG_ARRAY_MODEL tb_1_""".trimMargin()
            )
            rows(
                """[{
                    |--->"id":1,
                    |--->"intArr":[1,2,3],
                    |--->"integerArr":[4,5,6],
                    |--->"textArr":["a","b","c"],
                    |--->"textList":["d","e","f"],
                    |--->"varcharArr":["g","h","i"],
                    |--->"varcharList":["j","k","l"]
                    |}]""".trimMargin()
            )
        }
    }

    @Test
    fun testQueryEnumArrays() {
        Assume.assumeTrue(
            (System.getenv("jimmer-sql-test-native-database") ?: "").let {
                it.isNotEmpty() && it != "false"
            }
        )
        executeAndExpect(
            POSTGRES_DATA_SOURCE,
            _sqlClient.createQuery(PgEnumArrayModel::class) {
                where(table.textList eq listOf(Gender.FEMALE))
                where(table.varcharList eq listOf(Gender.MALE))
                where(table.priorities eq listOf(Priority.HIGH, Priority.LOW))
                select(table)
            }
        ) {
            sql(
                """select 
                    |tb_1_.ID, tb_1_.GENDER_ARR, tb_1_.TEXT_LIST, tb_1_.VARCHAR_LIST, tb_1_.PRIORITIES 
                    |from PG_ENUM_ARRAY_MODEL tb_1_ 
                    |where tb_1_.TEXT_LIST = ? and tb_1_.VARCHAR_LIST = ? and tb_1_.PRIORITIES = ?""".trimMargin()
            )
            rows(
                """[{
                    |--->"id":1,
                    |--->"genderArr":["MALE","FEMALE"],
                    |--->"textList":["FEMALE"],
                    |--->"varcharList":["MALE"],
                    |--->"priorities":["HIGH","LOW"]
                    |}]""".trimMargin()
            )
        }
    }

    companion object {

        private val POSTGRES_DATA_SOURCE: DataSource = SimpleDriverDataSource(
            Driver(),
            "jdbc:postgresql://localhost:5432/jimmer_test",
            "root",
            "123456"
        )
    }
}