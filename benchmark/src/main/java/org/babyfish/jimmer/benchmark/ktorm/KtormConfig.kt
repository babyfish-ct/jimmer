package org.babyfish.jimmer.benchmark.ktorm

import org.ktorm.database.Database
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
open class KtormConfig {

    @Bean
    open fun database(dataSource: DataSource): Database =
        Database.connectWithSpringSupport(dataSource)
}