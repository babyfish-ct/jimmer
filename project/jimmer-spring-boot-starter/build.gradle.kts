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
    api(project(":jimmer-sql"))
    api(project(":jimmer-sql-kotlin"))
    api(project(":jimmer-client"))
    api("org.springframework.boot:spring-boot-starter-web:2.7.0")
    api("org.springframework:spring-jdbc:5.3.20")
    api("org.springframework.data:spring-data-commons:2.7.0")
    api("org.springframework.data:spring-data-redis:2.7.0")
    api( "com.github.ben-manes.caffeine:caffeine:2.9.1")
    testAnnotationProcessor(project(":jimmer-apt"))
    testImplementation("org.springframework.boot:spring-boot-starter-test:2.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testImplementation("com.h2database:h2:2.1.212")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}