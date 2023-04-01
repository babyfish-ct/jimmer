import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.7.0"
	id("io.spring.dependency-management") version "1.0.12.RELEASE"
	kotlin("jvm") version "1.6.21"
	kotlin("kapt") version "1.6.21"
	kotlin("plugin.spring") version "1.6.21"
	id("com.google.devtools.ksp") version "1.7.10-1.0.6"
}

val jimmerVersion = "0.7.15"

group = "org.babyfish.jimmer.example.kt"
version = jimmerVersion
java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
	mavenCentral()
}

dependencies {

	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-graphql")
	implementation("org.springframework.data:spring-data-redis")
	implementation("org.springframework.kafka:spring-kafka:2.9.0")

	implementation("org.babyfish.jimmer:jimmer-spring-boot-starter:${jimmerVersion}")
	ksp("org.babyfish.jimmer:jimmer-ksp:${jimmerVersion}")

	implementation("org.mapstruct:mapstruct:1.5.3.Final")
	kapt("org.mapstruct:mapstruct-processor:1.5.3.Final")
	kapt("org.babyfish.jimmer:jimmer-mapstruct-apt:${jimmerVersion}")

	implementation("com.graphql-java:graphql-java-extended-scalars:18.1")

	runtimeOnly("com.h2database:h2:2.1.212")
	runtimeOnly("mysql:mysql-connector-java:8.0.30")
	runtimeOnly("io.lettuce:lettuce-core:6.2.0.RELEASE")
	runtimeOnly("com.github.ben-manes.caffeine:caffeine:2.9.1")

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

// Without this configuration, gradle command can still run.
// However, Intellij cannot find the generated source.
kotlin {
	sourceSets.main {
		kotlin.srcDir("build/generated/ksp/main/kotlin")
	}
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "1.8"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
