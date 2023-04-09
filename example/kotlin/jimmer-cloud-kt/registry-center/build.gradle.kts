import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.7.0"
    id("io.spring.dependency-management") version "1.0.12.RELEASE"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
}

val jimmerVersion: String by rootProject.extra

repositories {
    mavenCentral()
}

dependencies {

    implementation(kotlin("stdlib"))

    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2021.0.6"))

    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-server")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}