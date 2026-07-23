# Jimmer internal benchmarks

This project measures Jimmer's runtime hot paths against the current sources in
`../project`. Database and driver costs are excluded by a fixed-row JDBC stub;
SQL rendering, parameter binding, `DefaultExecutor` and entity materialization
remain in the measured path.

Run the normal 20-thread benchmark in a forked JVM:

```shell
gradle run --args="20 5 5 1 one-shot"
```

The arguments are thread count, warmup iterations, measurement iterations and
fork count, followed by the optional benchmark mode. `one-shot` is the default
and covers the normal production path from query construction through result
materialization. For example, a short smoke run is:

```shell
gradle run --args="1 1 1 1 one-shot"
```

The narrower retained-query path can be measured separately:

```shell
gradle run --args="20 5 5 1 retained"
```

## IDEA profiler

Open `benchmark-internal` as a Gradle project and profile
`BenchmarkApplication.main` directly with these program arguments:

```text
20 5 5 0 one-shot
```

Use `-Xms2g -Xmx2g` as VM options. Zero forks are intentional for profiling:
IDEA attaches to the application JVM, while a normal JMH fork would execute the
measured code in a child process. Use this mode to inspect hot spots, not to
publish benchmark numbers; use the forked command above for measurements.
