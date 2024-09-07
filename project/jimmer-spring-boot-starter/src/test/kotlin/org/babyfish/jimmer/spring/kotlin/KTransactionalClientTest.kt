package org.babyfish.jimmer.spring.kotlin

import org.babyfish.jimmer.spring.SqlClients
import org.babyfish.jimmer.spring.cfg.JimmerProperties
import org.babyfish.jimmer.spring.datasource.DataSources
import org.babyfish.jimmer.spring.transaction.JimmerTransactionManager
import org.babyfish.jimmer.spring.transaction.TransactionalSqlClients
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.isNull
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurationPackage
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.annotation.Transactional
import javax.sql.DataSource


@SpringBootTest(properties = ["jimmer.client.ts.path=/my-ts.zip", "jimmer.language=kotlin"])
@SpringBootConfiguration
@AutoConfigurationPackage
@EnableConfigurationProperties(
    JimmerProperties::class
)
open class KTransactionalClientTest {

    @Autowired
    private lateinit var sqlClient: KSqlClient

    @Transactional("tm")
    @Test
    open fun test() {
        val treeNodes = sqlClient
            .createQuery(TreeNode::class) {
                where(table.parentId.isNull())
                select(table)
            }
            .execute()
        Assertions.assertEquals(
            "[{\"id\":1,\"name\":\"Home\",\"parent\":null}]",
            treeNodes.toString()
        )
    }

    @Configuration
    open class Config {
        @Bean
        open fun dataSource(): DataSource {
            return DataSources.create(null)
        }

        @Bean
        open fun tm(ctx: ApplicationContext): JimmerTransactionManager {
            return JimmerTransactionManager(SqlClients.kotlin((ctx), dataSource()))
        }

        @Bean
        open fun sqlClient(): KSqlClient {
            return TransactionalSqlClients.kotlin()
        }
    }
}

