package org.babyfish.jimmer.benchmark;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BenchmarkApplication {

    private static final int WARMUP_ITERATIONS = 5;

    private static final int MEASUREMENT_ITERATIONS = 5;

    public static void main(String[] args) throws RunnerException {

        // SpringApplication.run will be called in forked process, not here.

        Options opt = new OptionsBuilder()
                // set the class name regex for benchmarks to search for to the current class
                .include("\\." + OrmBenchmark.class.getSimpleName() + "\\.")
                .warmupIterations(WARMUP_ITERATIONS)
                .warmupTime(TimeValue.seconds(1))
                .measurementIterations(MEASUREMENT_ITERATIONS)
                .measurementTime(TimeValue.seconds(1))
                .resultFormat(ResultFormatType.JSON)
                .forks(1)
                .shouldDoGC(true)
                .result("benchmark-report.json") // set this to a valid filename if you want reports
                .shouldFailOnError(true)
                .jvmArgs("-server")
                .build();
        new Runner(opt).run();
    }
}
