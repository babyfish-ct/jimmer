package org.babyfish.jimmer.spring.kotlin

import org.babyfish.jimmer.spring.AbstractTest
import org.babyfish.jimmer.spring.cfg.JimmerProperties
import org.babyfish.jimmer.spring.datasource.DataSources
import org.babyfish.jimmer.spring.datasource.TxCallback
import org.babyfish.jimmer.spring.repository.EnableJimmerRepositories
import org.babyfish.jimmer.spring.repository.SpringConnectionManager
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.newKSqlClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurationPackage
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@SpringBootTest(properties = ["jimmer.ts.path=/my-ts.zip"])
@SpringBootConfiguration
@AutoConfigurationPackage
@EnableJimmerRepositories
@EnableConfigurationProperties(JimmerProperties::class)
open class SpringKotlinTest : AbstractTest() {

    @BeforeEach
    fun beforeEach() {
        TRANSACTION_EVENTS.clear()
        SQL_STATEMENTS.clear()
    }

    @Configuration
    open class SqlClientConfig {
        @Bean
        open fun dataSource(): DataSource {
            return DataSources.create(
                object : TxCallback {
                    override fun open() {
                        TRANSACTION_EVENTS.add("connect")
                    }

                    override fun commit() {
                        TRANSACTION_EVENTS.add("commit")
                    }

                    override fun rollback() {
                        TRANSACTION_EVENTS.add("rollback")
                    }
                }
            )
        }

        @Bean
        open fun sqlClient(dataSource: DataSource): KSqlClient =
            newKSqlClient {
                setConnectionManager(SpringConnectionManager(dataSource))
                setEntityManager(ENTITY_MANAGER)
                setExecutor {
                    SQL_STATEMENTS.add(sql)
                    proceed()
                }
            }
    }

    @Autowired
    private lateinit var jimmerProperties: JimmerProperties

    @Autowired
    private lateinit var treeNodeRepository: TreeNodeRepository

    @Test
    fun testProperties() {
        Assertions.assertEquals("/my-ts.zip", jimmerProperties.ts.path)
    }

    @Test
    fun testRepository() {
        Assertions.assertEquals(
            "{\"id\":2,\"name\":\"Food\",\"parent\":{\"id\":1}}",
            treeNodeRepository.findNullable(2L)?.toString()
        );
        assertSQLs(
            """select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID 
                |from TREE_NODE as tb_1_ 
                |where tb_1_.NODE_ID = ?""".trimMargin()
        )
        Assertions.assertEquals(
            "[{\"id\":1,\"name\":\"Home\",\"parent\":null}]",
            treeNodeRepository.findRootNodes().toString()
        );
        assertSQLs(
            """select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID 
                |from TREE_NODE as tb_1_ 
                |where tb_1_.PARENT_ID is null""".trimMargin()
        )
    }

    companion object {

        private val TRANSACTION_EVENTS = mutableListOf<String>()

        private val SQL_STATEMENTS = mutableListOf<String>()

        @BeforeAll
        fun beforeAll() {
            initDatabase(DataSources.create(null))
        }

        private fun assertSQLs(vararg statements: String) {
            try {
                for (i in 0 until Math.min(statements.size, SQL_STATEMENTS.size)) {
                    Assertions.assertEquals(
                        statements[i]
                            .replace("\r", "")
                            .replace("\n", ""),
                        SQL_STATEMENTS[i],
                        "sql[$i]"
                    )
                }
                Assertions.assertEquals(statements.size, SQL_STATEMENTS.size, "sql count")
            } finally {
                SQL_STATEMENTS.clear()
            }
        }
    }
}