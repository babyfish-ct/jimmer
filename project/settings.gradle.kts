rootProject.name = "jimmer"
include(
    "jimmer-bom",
    "jimmer-core",
    "jimmer-mapstruct-apt",
    "jimmer-apt",
    "jimmer-sql",
    "jimmer-core-kotlin",
    "jimmer-ksp",
    "jimmer-sql-kotlin",
    "jimmer-client",
    "jimmer-spring-boot-starter",
    "jimmer-dto-compiler",
    "jimmer-client-swagger",
    "jimmer-client-scalar",
    "jimmer-ddl-compiler",
    "jimmer-sql-test:jimmer-sql-test-model-base",
    "jimmer-sql-test:jimmer-sql-test-model",
    "jimmer-sql-test:jimmer-sql-test-model-kotlin",
    "jimmer-sql-test:jimmer-sql-test-support",
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    dependencyResolutionManagement {
        repositories {
            mavenCentral()
        }
    }
}
