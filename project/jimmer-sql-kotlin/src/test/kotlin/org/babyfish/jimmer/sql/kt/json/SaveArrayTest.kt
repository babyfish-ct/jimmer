package org.babyfish.jimmer.sql.kt.json

import org.babyfish.jimmer.sql.dialect.PostgresDialect
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.classic.author.Gender
import org.babyfish.jimmer.sql.kt.model.pg.PgArrayModel
import org.babyfish.jimmer.sql.kt.model.pg.PgEnumArrayModel
import org.babyfish.jimmer.sql.kt.model.pg.Priority
import org.babyfish.jimmer.sql.kt.model.pg.genderArr
import org.babyfish.jimmer.sql.kt.model.pg.priorities
import org.babyfish.jimmer.sql.kt.model.pg.textList
import org.babyfish.jimmer.sql.kt.model.pg.intArr
import org.babyfish.jimmer.sql.kt.model.pg.integerArr
import org.junit.Assume
import org.junit.Test
import org.postgresql.Driver
import org.springframework.jdbc.datasource.SimpleDriverDataSource
import javax.sql.DataSource

class SaveArrayTest : AbstractMutationTest() {

    private val _sqlClient: KSqlClient =
        sqlClient {
            setDialect(PostgresDialect())
        }

    @Test
    fun testDML() {

        Assume.assumeTrue(
            (System.getenv("jimmer-sql-test-native-database") ?: "").let {
                it.isNotEmpty() && it != "false"
            }
        )

        executeAndExpectRowCount(
            POSTGRES_DATA_SOURCE,
            _sqlClient.createUpdate(PgArrayModel::class) {
                set(table.intArr, intArrayOf(1, 2, 3))
                set(table.integerArr, arrayOf(1, 2, 3))
            }
        ) {
            statement {
                sql(
                    """update PG_ARRAY_MODEL tb_1_ 
                        |set INT_ARR = ?, INTEGER_ARR = ?""".trimMargin()
                )
                rowCount(1)
            }
        }
    }

    @Test
    fun testEnumArrayDML() {

        Assume.assumeTrue(
            (System.getenv("jimmer-sql-test-native-database") ?: "").let {
                it.isNotEmpty() && it != "false"
            }
        )

        executeAndExpectRowCount(
            POSTGRES_DATA_SOURCE,
            _sqlClient.createUpdate(PgEnumArrayModel::class) {
                set(table.genderArr, arrayOf(Gender.FEMALE))
                set(table.textList, listOf(Gender.FEMALE, Gender.MALE))
                set(table.priorities, listOf(Priority.LOW))
            }
        ) {
            statement {
                sql(
                    """update PG_ENUM_ARRAY_MODEL tb_1_ 
                        |set GENDER_ARR = ?, TEXT_LIST = ?, PRIORITIES = ?""".trimMargin()
                )
                rowCount(1)
            }
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