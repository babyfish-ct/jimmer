package org.babyfish.jimmer.benchmark.jdbc;

import org.babyfish.jimmer.benchmark.BenchmarkExecutor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JdbcExecutor extends BenchmarkExecutor {

    private final JdbcDataRepository jdbcDataRepository;

    public JdbcExecutor(JdbcDataRepository jdbcDataRepository) {
        this.jdbcDataRepository = jdbcDataRepository;
    }

    @Override
    public String name() {
        return "spring-data-jdbc";
    }

    @Override
    protected List<?> query() {
        return (List<?>)jdbcDataRepository.findAll();
    }
}
