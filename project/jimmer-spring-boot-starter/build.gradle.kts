plugins {
    `java-library`
    kotlin("jvm") version "1.6.10"
}

group = "org.babyfish.jimmer"
version = "0.5.14"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":jimmer-sql"))
    implementation(project(":jimmer-sql-kotlin"))
    implementation(project(":jimmer-client"))
    testAnnotationProcessor(project(":jimmer-apt"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}