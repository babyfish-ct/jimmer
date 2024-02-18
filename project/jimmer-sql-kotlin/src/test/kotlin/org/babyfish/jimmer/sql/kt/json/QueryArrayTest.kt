package org.babyfish.jimmer.sql.kt.json

import org.babyfish.jimmer.sql.dialect.PostgresDialect
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.pg.PgArrayModel
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

    companion object {

        private val POSTGRES_DATA_SOURCE: DataSource = SimpleDriverDataSource(
            Driver(),
            "jdbc:postgresql://localhost:5432/jimmer_test",
            "root",
            "123456"
        )
    }
}