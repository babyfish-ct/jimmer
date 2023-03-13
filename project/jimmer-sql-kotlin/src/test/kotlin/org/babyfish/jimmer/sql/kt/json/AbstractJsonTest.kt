package org.babyfish.jimmer.sql.kt.json

import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.model.ENTITY_MANAGER
import org.babyfish.jimmer.sql.kt.model.pg.PointProvider
import org.babyfish.jimmer.sql.kt.model.pg.ScoresProvider
import org.babyfish.jimmer.sql.kt.model.pg.TagsProvider
import org.babyfish.jimmer.sql.kt.newKSqlClient
import org.junit.Assume
import org.postgresql.Driver
import org.springframework.jdbc.datasource.SimpleDriverDataSource
import java.sql.Connection
import javax.sql.DataSource
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

abstract class AbstractJsonTest {

    private var con: Connection? = null

    private lateinit var _sqlClient: KSqlClient

    @BeforeTest
    fun initialize() {
        Assume.assumeTrue("true" == System.getenv("jimmer-sql-test-native-database"))
        con = POSTGRES_DATA_SOURCE.connection
        con?.autoCommit = false
        _sqlClient = newKSqlClient {
            setEntityManager(ENTITY_MANAGER)
            setConnectionManager {
                proceed(con!!)
            }
            addScalarProvider(PointProvider())
            addScalarProvider(TagsProvider())
            addScalarProvider(ScoresProvider())
        }
    }

    @AfterTest
    fun uninitialize() {
        val c = con
        if (c !== null) {
            con = null
            c.rollback()
            c.close()
        }
    }

    protected val sqlClient: KSqlClient
        get() = _sqlClient

    companion object {

        private val POSTGRES_DATA_SOURCE: DataSource = SimpleDriverDataSource(
            Driver(),
            "jdbc:postgresql://localhost:5432/db",
            "sa",
            "123456"
        )
    }
}