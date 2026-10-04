# Jimmer KSP

Kotlin symbol processor for Jimmer immutable models, entities, DTOs and client metadata.

```kotlin
dependencies {
    ksp("org.babyfish.jimmer:jimmer-ksp:<jimmer-version>")
}
```

An implemented Kotlin getter without `@Formula` is an ordinary interface implementation. It is excluded from immutable property metadata, generated draft setters, fetchers and SQL columns. Both expression and block getters work, including inherited getters. Load the persistent properties used by the getter before calling it.

Use `@Formula(dependencies = ["firstName", "lastName"])` when the computed property should participate in Jimmer metadata and fetchers with declared loading dependencies. SQL formulas and abstract persistent properties retain their existing behavior. Persistence annotations on implemented getters are rejected.

Run `./gradlew :jimmer-ksp:test :jimmer-sql-kotlin:test` from `project/`.
