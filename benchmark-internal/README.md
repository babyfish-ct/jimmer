# Jimmer internal benchmarks

This project measures Jimmer's runtime hot paths against the current sources in
`../project`. Database and driver costs are excluded by a fixed-row JDBC stub;
SQL rendering, parameter binding, `DefaultExecutor` and entity materialization
remain in the measured path.

Run the default 20-thread benchmark:

```shell
gradle run
```

The optional arguments are thread count, warmup iterations and measurement
iterations. For example, a short smoke run is:

```shell
gradle run --args="1 1 1"
```
