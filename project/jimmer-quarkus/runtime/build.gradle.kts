plugins {
    `java-library`
    id("io.quarkus.extension") version "3.6.4"
}

val quarkusExtension by configurations.creating
quarkusExtension {
    deploymentModule = ":jimmer-quarkus:deployment"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-agroal")
    implementation("io.quarkus:quarkus-jackson")
}