package org.babyfish.jimmer.spring.kotlin

import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.spring.AbstractTest
import org.babyfish.jimmer.spring.SqlClients
import org.babyfish.jimmer.spring.cfg.JimmerProperties
import org.babyfish.jimmer.spring.datasource.DataSources
import org.babyfish.jimmer.spring.repository.support.KRepositoryImpl
import org.babyfish.jimmer.spring.transaction.JimmerTransactionManager
import org.babyfish.jimmer.spring.transaction.TransactionalSqlClients
import org.babyfish.jimmer.sql.JSqlClient
import org.babyfish.jimmer.sql.kt.KEntities
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.junit.jupiter.api.Assertions.*
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
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

@SpringBootTest(properties = ["jimmer.language=kotlin"])
@SpringBootConfiguration
@AutoConfigurationPackage
@EnableConfigurationProperties(JimmerProperties::class)
open class KTransactionalMultiDataSourceTest : AbstractTest() {

    @Autowired
    @Qualifier("sqlClient")
    private lateinit var sqlClient: KSqlClient

    @Autowired
    @Qualifier("client1")
    private lateinit var client1: KSqlClient

    @Autowired
    @Qualifier("client2")
    private lateinit var client2: JSqlClient

    @Autowired
    @Qualifier("tm1")
    private lateinit var tm1: JimmerTransactionManager

    @Autowired
    @Qualifier("tm2")
    private lateinit var tm2: JimmerTransactionManager

    @Test
    fun testEntitiesFollowCurrentDataSourceAndReuseClients() {
        val first = findRoot(tm1)
        val second = findRoot(tm2)
        val firstAgain = findRoot(tm1)
        val secondAgain = findRoot(tm2)

        assertEquals("Home", first.name)
        assertEquals("Second", second.name)
        assertEquals("Home", firstAgain.name)
        assertEquals("Second", secondAgain.name)
        assertNotSame(first.entities, second.entities)
        assertSame(client1.entities, first.entities)
        assertSame(first.entities, firstAgain.entities)
        assertSame(second.entities, secondAgain.entities)
    }

    @Test
    fun testNestedTransactionRestoresOuterClient() {
        TransactionTemplate(tm1).executeWithoutResult {
            val outerEntities = sqlClient.entities
            assertEquals("Home", rootName(outerEntities))
            TransactionTemplate(tm2).executeWithoutResult { status ->
                assertEquals("Second", rootName(sqlClient.entities))
                assertNotSame(outerEntities, sqlClient.entities)
                status.setRollbackOnly()
            }
            assertSame(outerEntities, sqlClient.entities)
            assertSame(client1.javaClient, JimmerTransactionManager.sqlClient())
            assertEquals("Home", rootName(sqlClient.entities))
        }
        assertNull(JimmerTransactionManager.sqlClient())
        assertThrows(IllegalStateException::class.java) { sqlClient.entities }
    }

    @Test
    fun testJavaClientRemainsTransactional() {
        val javaClient = TransactionTemplate(tm1).execute { sqlClient.javaClient }!!
        assertSame(TransactionalSqlClients.java(), javaClient)
        TransactionTemplate(tm2).executeWithoutResult {
            assertSame(javaClient, sqlClient.javaClient)
            assertEquals("Second", javaClient.entities.findById(TreeNode::class.java, 1L)!!.name)
        }
        assertThrows(IllegalStateException::class.java) { javaClient.entities }
    }

    @Test
    fun testRepositoryInitializationOutsideTransaction() {
        assertSame(TransactionalSqlClients.java(), sqlClient.javaClient)
        assertFalse(sqlClient.entityManager.getAllTypes(null).contains(ImmutableType.get(TreeNode::class.java)))
        val repository = KRepositoryImpl<TreeNode, Long>(sqlClient, TreeNode::class)
        assertThrows(IllegalStateException::class.java) { repository.findNullable(1L, null) }
        TransactionTemplate(tm1).executeWithoutResult {
            assertEquals("Home", repository.findNullable(1L, null)!!.name)
        }
        TransactionTemplate(tm2).executeWithoutResult {
            assertEquals("Second", repository.findNullable(1L, null)!!.name)
        }
        assertThrows(IllegalStateException::class.java) { repository.findNullable(1L, null) }
    }

    @Test
    fun testConcurrentFirstAccessToJavaClient() {
        val manager = JimmerTransactionManager(client2)
        val executor = Executors.newFixedThreadPool(4)
        val barrier = CyclicBarrier(4)
        try {
            val futures = (1..4).map {
                executor.submit<RootQueryResult> {
                    val result = TransactionTemplate(manager).execute {
                        barrier.await(10, TimeUnit.SECONDS)
                        val entities = sqlClient.entities
                        RootQueryResult(rootName(entities), entities)
                    }!!
                    assertNull(JimmerTransactionManager.sqlClient())
                    result
                }
            }
            val results = futures.map { it.get(20, TimeUnit.SECONDS) }
            results.forEach {
                assertEquals("Second", it.name)
                assertSame(results.first().entities, it.entities)
            }
            assertSame(results.first().entities, findRoot(manager).entities)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun findRoot(tm: JimmerTransactionManager): RootQueryResult =
        TransactionTemplate(tm).execute {
            val entities = sqlClient.entities
            assertSame(entities, sqlClient.entities)
            RootQueryResult(rootName(entities), entities)
        }!!

    private fun rootName(entities: KEntities): String =
        entities.findById(TreeNode::class, 1L)!!.name

    private data class RootQueryResult(val name: String, val entities: KEntities)

    @Configuration
    open class Config {

        @Bean("ds1")
        open fun dataSource1(): DataSource = DataSources.create("jimmer_transactional_kotlin_1", null)

        @Bean("ds2")
        open fun dataSource2(): DataSource = DataSources.create("jimmer_transactional_kotlin_2", null)

        @Bean("client1")
        open fun client1(ctx: ApplicationContext, @Qualifier("ds1") dataSource: DataSource): KSqlClient =
            SqlClients.kotlin(ctx, dataSource)

        @Bean("client2")
        open fun client2(ctx: ApplicationContext, @Qualifier("ds2") dataSource: DataSource): JSqlClient =
            SqlClients.kotlin(ctx, dataSource).javaClient

        @Bean("tm1")
        open fun tm1(@Qualifier("client1") client: KSqlClient): JimmerTransactionManager = JimmerTransactionManager(client)

        @Bean("tm2")
        open fun tm2(@Qualifier("client2") client: JSqlClient): JimmerTransactionManager = JimmerTransactionManager(client)

        @Bean
        open fun sqlClient(): KSqlClient = TransactionalSqlClients.kotlin()
    }

    companion object {

        @BeforeAll
        @JvmStatic
        fun initDatabases() {
            val dataSource1 = DataSources.create("jimmer_transactional_kotlin_1", null)
            val dataSource2 = DataSources.create("jimmer_transactional_kotlin_2", null)
            initDatabase(dataSource1)
            initDatabase(dataSource2)
            dataSource2.connection.use { con ->
                con.createStatement().use { statement ->
                    statement.executeUpdate("update tree_node set name = 'Second' where node_id = 1")
                }
            }
        }
    }
}
