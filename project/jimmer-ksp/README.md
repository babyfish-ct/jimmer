# Jimmer KSP

Kotlin symbol processor for Jimmer immutable models, entities, DTOs and client metadata.

```kotlin
dependencies {
    ksp("org.babyfish.jimmer:jimmer-ksp:<jimmer-version>")
}

tasks.withType<com.google.devtools.ksp.gradle.KspAATask>().configureEach {
    val dtoDirectory = if (name == "kspTestKotlin") "src/test/dto" else "src/main/dto"
    inputs.files(fileTree(dtoDirectory) { include("**/*.dto") })
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
```

This configures KSP2 tasks lazily; compilations without a KSP task need no special handling.

For Kotlin/JVM Gradle projects, declare DTO files as KSP task inputs in each module that owns them.
Otherwise, changing only a `.dto` file can leave `kspKotlin` up to date and preserve an older generated DTO,
including one without newly selected inherited properties. Use the directories configured by
`jimmer.dto.dirs` and `jimmer.dto.testDirs` if they differ from the defaults above.
The file trees also track additions, renames and deletions, and allow a DTO directory to be absent.
Unchanged inputs still allow Gradle to skip generation.

Run the Gradle build after editing DTO files. An IDE build that does not run Gradle does not use these task inputs.
A `gradle clean build` can diagnose stale output, but repeated clean builds are unnecessary with the inputs configured.
Changing JDK or KSP versions can also invalidate build outputs; successful generation after an upgrade or downgrade
alone does not establish a version compatibility problem.

An implemented Kotlin getter without `@Formula` is an ordinary interface implementation. It is excluded from immutable property metadata, generated draft setters, fetchers and SQL columns. Both expression and block getters work, including inherited getters. Load the persistent properties used by the getter before calling it.

Use `@Formula(dependencies = ["firstName", "lastName"])` when the computed property should participate in Jimmer metadata and fetchers with declared loading dependencies. SQL formulas and abstract persistent properties retain their existing behavior. Persistence annotations on implemented getters are rejected.

Run `./gradlew :jimmer-ksp:test :jimmer-sql-kotlin:test` from `project/`.
