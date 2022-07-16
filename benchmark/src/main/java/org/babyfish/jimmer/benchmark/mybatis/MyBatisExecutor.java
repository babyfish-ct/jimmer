package org.babyfish.jimmer.benchmark.mybatis;

import org.babyfish.jimmer.benchmark.BenchmarkExecutor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MyBatisExecutor extends BenchmarkExecutor {

    private final DataMapper dataMapper;

    public MyBatisExecutor(DataMapper dataMapper) {
        this.dataMapper = dataMapper;
    }

    @Override
    public String name() {
        return "MyBatis";
    }

    @Override
    protected List<?> query() {
        return dataMapper.findAll();
    }
}
