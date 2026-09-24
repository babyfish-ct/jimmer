package org.babyfish.jimmer.spring.kotlin

import org.babyfish.jimmer.spring.AbstractTest
import org.babyfish.jimmer.spring.SqlClients
import org.babyfish.jimmer.spring.cfg.JimmerProperties
import org.babyfish.jimmer.spring.datasource.DataSources
import org.babyfish.jimmer.spring.transaction.JimmerTransactionManager
import org.babyfish.jimmer.spring.transaction.TransactionalSqlClients
import org.babyfish.jimmer.sql.kt.KEntities
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurationPackage
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.support.TransactionTemplate
import javax.sql.DataSource

@SpringBootTest(properties = ["jimmer.language=kotlin"])
@SpringBootConfiguration
@AutoConfigurationPackage
@EnableConfigurationProperties(JimmerProperties::class)
open class KTransactionalMultiDataSourceTest : AbstractTest() {

    @Autowired
    private lateinit var sqlClient: KSqlClient

    @Autowired
    @Qualifier("tm1")
    private lateinit var tm1: JimmerTransactionManager

    @Autowired
    @Qualifier("tm2")
    private lateinit var tm2: JimmerTransactionManager

    @Test
    open fun testEntitiesCacheFollowsCurrentDataSource() {
        val first = findRootName(tm1)
        val second = findRootName(tm2)

        Assertions.assertEquals("Home", first.name)
        Assertions.assertEquals("Second", second.name)
        Assertions.assertNotSame(first.entities, second.entities)
    }

    private fun findRootName(tm: JimmerTransactionManager): RootQueryResult =
        TransactionTemplate(tm).execute {
            val entities = sqlClient.entities
            Assertions.assertSame(entities, sqlClient.entities)
            RootQueryResult(
                entities.findById(TreeNode::class, 1L)!!.name,
                entities
            )
        }!!

    private data class RootQueryResult(
        val name: String,
        val entities: KEntities
    )

    @Configuration
    open class Config {

        @Bean("ds1")
        open fun dataSource1(): DataSource =
            DataSources.create("jimmer_spring_test_db_1", null)

        @Bean("ds2")
        open fun dataSource2(): DataSource =
            DataSources.create("jimmer_spring_test_db_2", null)

        @Bean("tm1")
        open fun tm1(
            ctx: ApplicationContext,
            @Qualifier("ds1") dataSource: DataSource
        ): JimmerTransactionManager =
            JimmerTransactionManager(SqlClients.kotlin(ctx, dataSource))

        @Bean("tm2")
        open fun tm2(
            ctx: ApplicationContext,
            @Qualifier("ds2") dataSource: DataSource
        ): JimmerTransactionManager =
            JimmerTransactionManager(SqlClients.kotlin(ctx, dataSource))

        @Bean
        open fun sqlClient(): KSqlClient =
            TransactionalSqlClients.kotlin()
    }

    companion object {

        @BeforeAll
        @JvmStatic
        fun initDatabases() {
            val dataSource1 =
                DataSources.create("jimmer_spring_test_db_1", null)
            val dataSource2 =
                DataSources.create("jimmer_spring_test_db_2", null)
            initDatabase(dataSource1)
            initDatabase(dataSource2)
            dataSource2.connection.use { con ->
                con.createStatement().use { statement ->
                    statement.executeUpdate(
                        "update tree_node set name = 'Second' where node_id = 1"
                    )
                }
            }
        }
    }
}
