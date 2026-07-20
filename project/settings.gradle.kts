rootProject.name = "jimmer"
include(
    "jimmer-bom",
    "jimmer-core",
    "jimmer-mapstruct-apt",
    "jimmer-apt",
    "jimmer-sql",
    "jimmer-sql-test-model",
    "jimmer-sql-test-support",
    "jimmer-core-kotlin",
    "jimmer-ksp",
    "jimmer-sql-kotlin",
    "jimmer-sql-kotlin-test-model",
    "jimmer-client",
    "jimmer-spring-boot-starter",
    "jimmer-dto-compiler",
    "jimmer-client-swagger",
    "jimmer-client-scalar",
    "jimmer-ddl-compiler",
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    dependencyResolutionManagement {
        repositories {
            mavenCentral()
        }
    }
}
