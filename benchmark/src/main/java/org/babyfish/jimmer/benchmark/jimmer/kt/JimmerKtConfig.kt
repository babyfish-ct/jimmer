package org.babyfish.jimmer.benchmark.jimmer.kt

import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.newKSqlClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DataSourceUtils
import javax.sql.DataSource

@Configuration
open class JimmerKtConfig {

    @Bean
    open fun kSqlClient(dataSource: DataSource): KSqlClient =
        newKSqlClient {
            setDialect(H2Dialect())
            setConnectionManager {
                val con = DataSourceUtils.getConnection(dataSource)
                try {
                    proceed(con)
                } finally {
                    DataSourceUtils.releaseConnection(con, dataSource)
                }
            }
        }
}