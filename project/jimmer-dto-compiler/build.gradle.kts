import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    kotlin("jvm") version "1.7.10"
    antlr
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:annotations:24.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    antlr("org.antlr:antlr4:4.9.3")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<Javadoc>{
    options.encoding = "UTF-8"
}

tasks.withType<Jar>().configureEach {
    dependsOn(tasks.withType<AntlrTask>())
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(tasks.withType<AntlrTask>())
}