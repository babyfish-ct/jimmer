package org.babyfish.jimmer.sql.example.graphql.cfg;

import org.babyfish.jimmer.sql.example.graphql.entities.JimmerModule;
import org.babyfish.jimmer.sql.runtime.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JimmerConfig {

    /*
     * 1. Jimmer requires this entity manager,
     *    if you do not want to directly create JSqlClient/KSqlClient
     *
     * 2. Kotlin requires `jimmer.language=kotlin` in `application.yml`
     */
    @Bean
    public EntityManager entityManager() {
        return JimmerModule.ENTITY_MANAGER;
    }
}
