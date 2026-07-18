rootProject.name = "jimmer"
include(
    "jimmer-bom",
    "jimmer-core",
    "jimmer-kotlinx-serialization",
    "jimmer-jackson2",
    "jimmer-jackson3",
    "jimmer-mapstruct-apt",
    "jimmer-apt",
    "jimmer-sql",
    "jimmer-core-kotlin",
    "jimmer-ksp",
    "jimmer-sql-kotlin",
    "jimmer-client",
    "jimmer-spring-boot-starter",
    "jimmer-spring-boot-starter-jackson",
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
