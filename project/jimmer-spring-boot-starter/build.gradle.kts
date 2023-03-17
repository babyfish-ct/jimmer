plugins {
    `java-library`
    kotlin("jvm") version "1.6.10"
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
    api(project(":jimmer-sql"))
    api(project(":jimmer-sql-kotlin"))
    api(project(":jimmer-client"))
    api("org.springframework.boot:spring-boot-starter-jdbc:2.7.0")
    api("org.springframework.data:spring-data-commons:2.7.0")
    compileOnly("org.springframework.boot:spring-boot-starter-web:2.7.0")
    compileOnly("org.springframework.data:spring-data-redis:2.7.0")
    compileOnly( "com.github.ben-manes.caffeine:caffeine:2.9.1")
    compileOnly("org.springframework.graphql:spring-graphql:1.0.0")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:2.7.0")
    testAnnotationProcessor(project(":jimmer-apt"))
    kspTest(project(":jimmer-ksp"))
    testImplementation("org.springframework.boot:spring-boot-starter-test:2.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testImplementation("com.h2database:h2:2.1.212")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.springframework.boot:spring-boot-starter-web:2.7.0")
}

kotlin {
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

ksp {
    arg("jimmer.source.excludes", "org.babyfish.jimmer.spring.java")
}