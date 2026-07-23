package org.babyfish.jimmer.benchmark;

import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

public class BenchmarkApplication {

    public static void main(String[] args) throws RunnerException {
        int threads = argument(args, 0, 20);
        int warmupIterations = argument(args, 1, 5);
        int measurementIterations = argument(args, 2, 5);
        int forks = argument(args, 3, 1);
        String benchmarkMode = benchmarkMode(args);
        String benchmarkMethod = "one-shot".equals(benchmarkMode) ?
                "executeOneShotQuery" :
                "executeRetainedQuery";
        Options options = new OptionsBuilder()
                .include(
                        "\\." +
                                QueryBenchmark.class.getSimpleName() +
                                "\\." +
                                benchmarkMethod +
                                "$"
                )
                .warmupIterations(warmupIterations)
                .warmupTime(TimeValue.seconds(1))
                .measurementIterations(measurementIterations)
                .measurementTime(TimeValue.seconds(1))
                .threads(threads)
                .forks(forks)
                .addProfiler(GCProfiler.class)
                .resultFormat(ResultFormatType.JSON)
                .result(
                        "benchmark-report-" +
                                benchmarkMode +
                                "-" +
                                threads +
                                "t-" +
                                forks +
                                "f.json"
                )
                .shouldFailOnError(true)
                .jvmArgs("-server", "-Xms2g", "-Xmx2g")
                .build();
        new Runner(options).run();
    }

    private static int argument(String[] args, int index, int defaultValue) {
        return args.length > index ? Integer.parseInt(args[index]) : defaultValue;
    }

    private static String benchmarkMode(String[] args) {
        String mode = args.length > 4 ? args[4] : "one-shot";
        if (!"one-shot".equals(mode) && !"retained".equals(mode)) {
            throw new IllegalArgumentException(
                    "Illegal benchmark mode \"" +
                            mode +
                            "\", expected \"one-shot\" or \"retained\""
            );
        }
        return mode;
    }
}
