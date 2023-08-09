plugins {
    kotlin("jvm") version "1.6.10"
    id("com.google.devtools.ksp") version "1.7.10-1.0.6"
}

val jimmerVersion = "0.7.125"

group = "org.babyfish.jimmer.example.save"
version = jimmerVersion

repositories {
    mavenCentral()
}

java.sourceCompatibility = JavaVersion.VERSION_1_8

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("ch.qos.logback:logback-classic:1.4.6")
    implementation("org.babyfish.jimmer:jimmer-sql-kotlin:${jimmerVersion}")
    ksp("org.babyfish.jimmer:jimmer-ksp:${jimmerVersion}")
    testImplementation("com.h2database:h2:2.1.212")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("com.github.jsqlparser:jsqlparser:4.6")
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
