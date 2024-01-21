plugins {
    `java-library`
    id("io.quarkus") version "3.6.4"
}

repositories {
    mavenCentral()
}

dependencies{
    api(project(mapOf("path" to ":jimmer-quarkus:runtime")))
    implementation("io.quarkus:quarkus-resteasy-reactive")
    implementation("io.quarkus:quarkus-resteasy-reactive-jackson")
    implementation("io.quarkus:quarkus-config-yaml")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-agroal")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation(project(mapOf("path" to ":jimmer-core")))
    annotationProcessor(project(mapOf("path" to ":jimmer-apt")))
}