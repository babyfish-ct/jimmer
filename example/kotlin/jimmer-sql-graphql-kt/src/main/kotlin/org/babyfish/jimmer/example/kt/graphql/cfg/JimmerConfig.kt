package org.babyfish.jimmer.example.kt.graphql.cfg

import org.babyfish.jimmer.example.kt.graphql.entities.ENTITY_MANAGER
import org.babyfish.jimmer.sql.runtime.EntityManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JimmerConfig {

    /*
     * 1. Jimmer requires this entity manager,
     *    if you do not want to directly create JSqlClient/KSqlClient
     *
     * 2. Kotlin requires `jimmer.language=kotlin` in `application.yml`
     */
    @Bean
    fun entityManager(): EntityManager =
        ENTITY_MANAGER
}