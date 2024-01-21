plugins {
    `java-library`
}

subprojects {
    apply(plugin = "java-library")

    repositories {
        mavenCentral()
    }
    dependencies {
        implementation(enforcedPlatform("${project.property("quarkusPluginId")}:${project.property("quarkusPlatformArtifactId")}:${project.property("quarkusVersion")}"))
        implementation(project(mapOf("path" to ":jimmer-sql")))
    }
}