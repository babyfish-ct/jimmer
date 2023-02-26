plugins {
    kotlin("jvm") version "1.6.10"
    id("com.google.devtools.ksp") version "1.7.10-1.0.6"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java.sourceCompatibility = JavaVersion.VERSION_1_8

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.babyfish.jimmer:jimmer-sql-kotlin:0.6.56-0.7-preview")
    ksp("org.babyfish.jimmer:jimmer-ksp:0.6.56-0.7-preview")
    testImplementation("com.h2database:h2:2.1.212")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<Javadoc>{
    options.encoding = "UTF-8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}