plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(mapOf("path" to ":jimmer-quarkus:runtime")))
    implementation("io.quarkus:quarkus-arc-deployment")
    implementation("io.quarkus:quarkus-agroal-deployment")
    testImplementation("io.quarkus:quarkus-junit5-internal")
}