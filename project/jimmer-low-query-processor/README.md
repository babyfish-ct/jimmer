# Jimmer Low Query

Jimmer Low Query is a KSP processor that generates typed Kotlin query helpers from Jimmer entity annotations.
It is intended for admin/back-office filter pages where many endpoints share the same dynamic `where` and `orderBy` rules.

## Modules

- `jimmer-low-query-annotations`: query annotations and the optional runtime provider SPI.
- `jimmer-low-query-processor`: KSP processor that reads Jimmer entities and writes generated Kotlin query extensions.

## Gradle usage

```kotlin
dependencies {
    implementation("org.babyfish.jimmer:jimmer-low-query-annotations:<jimmer-version>")
    ksp("org.babyfish.jimmer:jimmer-low-query-processor:<jimmer-version>")
}

ksp {
    // Optional. Defaults to <entity-package>.generated.lowquery
    arg("jimmerLowQuery.generatedPackage", "com.example.generated.lowquery")
}
```

## Entity annotations

```kotlin
import org.babyfish.jimmer.lowquery.annotation.Eq
import org.babyfish.jimmer.lowquery.annotation.Keyword
import org.babyfish.jimmer.lowquery.annotation.Like
import org.babyfish.jimmer.lowquery.annotation.OrderByDesc
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id

@Entity
interface Book {
    @Id
    @OrderByDesc
    val id: Long

    @Like
    @Keyword
    val name: String

    @Eq
    val storeId: Long?
}
```

The generated function is a `KMutableRootQuery.ForEntity<Book>` extension:

```kotlin
sqlClient.createQuery(Book::class) {
    query(name = "GraphQL", storeId = 1L)
}
```

It also generates a `KSqlClient.createLowQuery(entity)` overload. The entity overload uses `ImmutableObjects.isLoaded(entity, "property")`, so callers can pass a Jimmer draft containing only the properties that should participate in filtering.

## Runtime provider

When Spring `@Component` is visible on the compilation classpath, the processor also emits a `JimmerLowQueryProvider<E>` bean per entity. Generic controller code can use those providers without statically importing generated extension functions from downstream entity packages.

## Supported annotations

- `@Eq`, `@Ne`
- `@Like`, `@StartsWith`, `@EndsWith`
- `@Gt`, `@Ge`, `@Lt`, `@Le`
- `@In`, `@NotIn`
- `@Between`, `@TimeRange`
- `@Keyword`
- `@OrderByAsc`, `@OrderByDesc`
- `@JimmerFindBy`

If an entity has at least one low-query annotation, scalar business properties without explicit query annotations are generated as nullable equality parameters. Audit and infrastructure fields such as `id`, `createTime`, `updateTime`, `deleted`, and `tenantId` are excluded from implicit equality generation unless explicitly annotated.

## Tests

Run the focused test suite from the `project` directory:

```bash
./gradlew :jimmer-low-query-processor:test
```
