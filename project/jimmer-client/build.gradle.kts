plugins {
    `java-library`
    kotlin("jvm") version "1.7.10"
    id("com.google.devtools.ksp") version "1.7.10-1.0.6"
}

group = "org.babyfish.jimmer"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    implementation(project(":jimmer-sql"))
    testAnnotationProcessor(project(":jimmer-apt"))
    kspTest(project(":jimmer-ksp"))
    testImplementation(project(":jimmer-sql-kotlin"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType(JavaCompile::class) {
    options.compilerArgs.add("-Ajimmer.dtoDirs=src/test/dto")
    inputs.files("src/test/dto")
}

ksp {
    arg("jimmer.source.excludes", "org.babyfish.jimmer.client.java.model.")
}

kotlin {
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=all")
        jvmTarget = "1.8"
    }
}