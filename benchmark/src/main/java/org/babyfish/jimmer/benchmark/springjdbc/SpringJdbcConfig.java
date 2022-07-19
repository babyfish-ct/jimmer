package org.babyfish.jimmer.benchmark.springjdbc;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@Configuration
@EnableJdbcRepositories(basePackageClasses = SpringJdbcDataRepository.class)
public class SpringJdbcConfig {
}
