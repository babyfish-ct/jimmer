plugins {
    `java-library`
    kotlin("jvm") version "1.6.10"
}

group = "org.babyfish.jimmer"
version = "0.5.14"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_9
    targetCompatibility = JavaVersion.VERSION_1_9
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    implementation(project(":jimmer-sql"))
    implementation(project(":jimmer-sql-kotlin"))
    implementation(project(":jimmer-client"))
    implementation("org.springframework.boot:spring-boot-starter-web:2.7.0")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc:2.7.0")
    testAnnotationProcessor(project(":jimmer-apt"))
    testImplementation("org.springframework.boot:spring-boot-starter-test:2.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}