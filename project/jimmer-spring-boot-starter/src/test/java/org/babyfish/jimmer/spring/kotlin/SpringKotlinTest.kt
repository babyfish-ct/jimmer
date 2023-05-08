package org.babyfish.jimmer.spring.kotlin

import org.babyfish.jimmer.client.meta.Metadata
import org.babyfish.jimmer.spring.AbstractTest
import org.babyfish.jimmer.spring.cfg.JimmerProperties
import org.babyfish.jimmer.spring.cfg.MetadataCondition
import org.babyfish.jimmer.spring.cfg.SqlClientConfig
import org.babyfish.jimmer.spring.client.MetadataFactoryBean
import org.babyfish.jimmer.spring.client.TypeScriptController
import org.babyfish.jimmer.spring.datasource.DataSources
import org.babyfish.jimmer.spring.datasource.TxCallback
import org.babyfish.jimmer.spring.repository.EnableJimmerRepositories
import org.babyfish.jimmer.sql.runtime.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurationPackage
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterNameDiscoverer
import org.springframework.data.domain.Sort
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.sql.Connection
import java.sql.PreparedStatement
import javax.sql.DataSource

@SpringBootTest(properties = ["jimmer.client.ts.path=/my-ts.zip", "jimmer.language=kotlin"])
@SpringBootConfiguration
@AutoConfigurationPackage
@EnableJimmerRepositories
@EnableConfigurationProperties(JimmerProperties::class)
@Import(SqlClientConfig::class)
open class SpringKotlinTest : AbstractTest() {

    @BeforeEach
    fun beforeEach() {
        TRANSACTION_EVENTS.clear()
        SQL_STATEMENTS.clear()
    }

    @Configuration
    open class DataSourceConfig {

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
        open fun executor(): Executor? {
            return object : Executor {
                override fun <R> execute(args: Executor.Args<R>): R {
                    SQL_STATEMENTS.add(args.sql)
                    return DefaultExecutor.INSTANCE.execute(args)
                }
            }
        }

        @Bean
        open fun mockMvc(ctx: WebApplicationContext): MockMvc {
            return MockMvcBuilders.webAppContextSetup(ctx).build()
        }

        @ConditionalOnProperty("jimmer.client.ts.path")
        @ConditionalOnMissingBean(TypeScriptController::class)
        @Bean
        open fun typeScriptController(metadata: Metadata, properties: JimmerProperties): TypeScriptController {
            return TypeScriptController(metadata, properties)
        }

        @Conditional(MetadataCondition::class)
        @ConditionalOnMissingBean(Metadata::class)
        @Bean
        open fun metadataFactoryBean(
            ctx: ApplicationContext,
            @Autowired(required = false) parameterNameDiscoverer: ParameterNameDiscoverer?
        ): MetadataFactoryBean {
            return MetadataFactoryBean(ctx, parameterNameDiscoverer)
        }
    }

    @Autowired
    private lateinit var jimmerProperties: JimmerProperties

    @Autowired
    private lateinit var treeNodeRepository: TreeNodeRepository

    @Autowired
    private lateinit var mvc: MockMvc

    @Test
    fun testProperties() {
        Assertions.assertEquals("/my-ts.zip", jimmerProperties.client.ts.path)
    }

    @Test
    fun testFindSort() {
        val page = treeNodeRepository.findAll(0, 1) {
            asc(TreeNode::name)
        }
        Assertions.assertEquals(
            Sort.by(
                Sort.Order(Sort.Direction.ASC, "name")
            ),
            page.pageable.sort,
        )
    }

    @Test
    fun testFindRootNodes() {
        Assertions.assertEquals(
            "{\"id\":2,\"name\":\"Food\",\"parent\":{\"id\":1}}",
            treeNodeRepository.findNullable(2L)?.toString()
        );
        assertSQLs(
            """select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID 
                |from TREE_NODE tb_1_ 
                |where tb_1_.NODE_ID = ?""".trimMargin()
        )
        Assertions.assertEquals(
            "[{\"id\":1,\"name\":\"Home\",\"parent\":null}]",
            treeNodeRepository.findRootNodes().toString()
        );
        assertSQLs(
            """select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID 
                |from TREE_NODE tb_1_ 
                |where tb_1_.PARENT_ID is null""".trimMargin()
        )
    }

    @Test
    fun testByParentIsNullAndNameOrderByIdAsc() {
        treeNodeRepository.findByParentIsNullAndNameOrderByIdAsc(null, null)
        assertSQLs(
            "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                "from TREE_NODE tb_1_ " +
                "where tb_1_.PARENT_ID is null " +
                "order by tb_1_.NODE_ID asc"
        )

        treeNodeRepository.findByParentIsNullAndNameOrderByIdAsc("X", null)
        assertSQLs(
            "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                "from TREE_NODE tb_1_ " +
                "where tb_1_.PARENT_ID is null " +
                "and tb_1_.NAME = ? " +
                "order by tb_1_.NODE_ID asc"
        )
    }

    @Test
    fun testByNameAndParentId() {
        val treeNode = treeNodeRepository.findByNameAndParentId("Food", 1L)
        assertSQLs(
            "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                "from TREE_NODE tb_1_ " +
                "where tb_1_.NAME = ? and tb_1_.PARENT_ID = ?"
        )
        Assertions.assertEquals(
            "{\"id\":2,\"name\":\"Food\",\"parent\":{\"id\":1}}",
            treeNode?.toString()
        )

        val treeNode2 = treeNodeRepository.findByNameAndParentId("Food", 2L)
        assertSQLs(
            "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                "from TREE_NODE tb_1_ " +
                "where tb_1_.NAME = ? and tb_1_.PARENT_ID = ?"
        )
        Assertions.assertNull(treeNode2)
    }

    @Test
    open fun testDownloadTypescript() {
        mvc.perform(MockMvcRequestBuilders.get("/my-ts.zip"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith("application/zip"))
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