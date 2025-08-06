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
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    dependencyResolutionManagement {
        repositories {
            mavenCentral()
        }
    }
}
include("jimmer-ksp-jdbc2entity")