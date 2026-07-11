# Jimmer DDL Compiler

Jimmer DDL Compiler is a compile-time DDL generator for Jimmer entities. It can run as either a Kotlin KSP processor or a Java APT processor, convert Jimmer entity metadata into the LSI model, and render dialect-specific schema SQL.

## Module

- `jimmer-ddl-compiler`: processor artifact containing the KSP provider, APT provider, DDL compiler, schema snapshot handling, and tests.

## Gradle usage

Kotlin/KSP:

```kotlin
dependencies {
    ksp("org.babyfish.jimmer:jimmer-ddl-compiler:<jimmer-version>")
}

ksp {
    arg("jimmerDdl.enabled", "true")
    arg("jimmerDdl.databaseType", "postgresql")
    arg("jimmerDdl.outputFormat", "flyway")
    arg("jimmerDdl.outputDir", "$projectDir/build/generated/jimmer-ddl/main/resources/db/migration")
    arg("jimmerDdl.version", "1001")
    arg("jimmerDdl.description", "jimmer_auto_ddl_generated")
}
```

Java/APT:

```kotlin
dependencies {
    annotationProcessor("org.babyfish.jimmer:jimmer-ddl-compiler:<jimmer-version>")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-AjimmerDdl.enabled=true")
    options.compilerArgs.add("-AjimmerDdl.databaseType=postgresql")
}
```

## Options

| Option | Default | Description |
| --- | --- | --- |
| `jimmerDdl.enabled` | `true` | Enables or disables generation. |
| `jimmerDdl.profiles` | empty | Comma-separated profile names. Each profile can override options through `jimmerDdl.profile.<name>.<option>`. |
| `jimmerDdl.databaseType` | `auto` | Dialect code such as `postgresql`, `mysql`, `h2`, `sqlite`, `sqlserver`, `oracle`, `dm`, `kingbase`, or `taos`. `auto` resolves from JDBC URL when available. |
| `jimmerDdl.outputFormat` | `flyway` | `flyway` writes `V<version>__<description>.sql`; `plain` writes `<description>.sql`. |
| `jimmerDdl.outputDir` | `build/generated/jimmer-ddl/main/resources/db/migration` | Output directory for generated SQL. |
| `jimmerDdl.version` | `1001` | Flyway version prefix. |
| `jimmerDdl.description` | `jimmer_auto_ddl_generated` | Output file description. |
| `jimmerDdl.includePackages` | empty | Optional comma-separated package allow-list. |
| `jimmerDdl.excludePackages` | empty | Optional comma-separated package deny-list. |
| `jimmerDdl.includeForeignKeys` | `true` | Generates foreign key statements when the dialect supports them. |
| `jimmerDdl.includeIndexes` | `true` | Generates index statements. |
| `jimmerDdl.includeComments` | `true` | Generates comments. |
| `jimmerDdl.includeSequences` | `true` | Generates sequences. |
| `jimmerDdl.includeManyToManyTables` | `true` | Generates Jimmer many-to-many junction tables. |
| `jimmerDdl.compareDatabase` | `true` | Reads the configured database and emits diff SQL. If the database cannot be read, it falls back to offline DDL. |
| `jimmerDdl.nullabilityRepairOnly` | `false` | Limits risky offline alteration planning to nullability repair. |
| `jimmerDdl.sourceFingerprint` | empty | Optional source fingerprint stored in the snapshot. |
| `jimmerDdl.jdbcUrl` / `jdbcUsername` / `jdbcPassword` / `jdbcSchema` / `jdbcDriver` | empty | Explicit JDBC settings used by database comparison. |
| `jimmerDdl.springResourcePath` | empty | Optional Spring resource path used to discover datasource settings. |
| `jimmerDdl.springProfile` | `local` | Spring profile used when reading YAML datasource settings. |

## Snapshot model

The compiler writes a generated snapshot to `build/generated/jimmer-ddl/main/resources/.jimmer-ddl/entity-table-snapshot.properties`. The snapshot records entity-to-table mappings and table hashes so subsequent builds can emit incremental SQL, including table renames caused by `@Table(name = ...)` changes.

## Tests

Run the focused test suite from the `project` directory:

```bash
./gradlew :jimmer-ddl-compiler:test
```
