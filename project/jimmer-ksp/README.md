# Jimmer KSP

Kotlin symbol processor for Jimmer immutable models, entities, DTOs and client metadata.

For Kotlin/JVM projects using KSP2, add the processor dependency and register DTO files as task inputs in each module that owns them:

```kotlin
import com.google.devtools.ksp.gradle.KspAATask

dependencies {
    ksp("org.babyfish.jimmer:jimmer-ksp:<jimmer-version>")
}

tasks.withType<KspAATask>().configureEach {
    val dtoDirectory = if (name == "kspKotlin") "src/main/dto" else "src/test/dto"
    inputs.files(fileTree(dtoDirectory) { include("**/*.dto") })
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
```

If you configure `jimmer.dto.dirs` or `jimmer.dto.testDirs`, register those directories instead of the defaults above.
The file trees track DTO changes, additions and deletions while allowing unchanged tasks to remain `UP-TO-DATE`.

Run the Gradle build after editing DTO files. An IDE build that does not run Gradle does not use these task inputs.
Without these inputs, changing only a `.dto` file may leave the previous generated DTO unchanged.

An implemented Kotlin getter without `@Formula` is an ordinary interface implementation. It is excluded from immutable property metadata, generated draft setters, fetchers and SQL columns. Both expression and block getters work, including inherited getters. Load the persistent properties used by the getter before calling it.

Use `@Formula(dependencies = ["firstName", "lastName"])` when the computed property should participate in Jimmer metadata and fetchers with declared loading dependencies. SQL formulas and abstract persistent properties retain their existing behavior. Persistence annotations on implemented getters are rejected.

Run `./gradlew :jimmer-ksp:test :jimmer-sql-kotlin:test` from `project/`.
