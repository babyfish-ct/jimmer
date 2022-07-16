package org.babyfish.jimmer.benchmark;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class BenchmarkRunner implements ApplicationRunner {

    private final DatabaseInitializer databaseInitializer;

    private final List<BenchmarkExecutor> executors;

    public BenchmarkRunner(
            DatabaseInitializer databaseInitializer,
            List<BenchmarkExecutor> executors
    ) {
        this.databaseInitializer = databaseInitializer;
        this.executors = executors;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        databaseInitializer.initialize();
        Map<String, Long> map = new LinkedHashMap<>();
        for (BenchmarkExecutor executor : executors) {
            long time = executor.execute();
            map.put(executor.name(), time);
        }
        for (Map.Entry<String, Long> entry : map.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue() + "ms");
        }
    }
}
