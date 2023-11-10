import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "1.6.21"
	kotlin("plugin.spring") version "1.6.21"
}

val jimmerVersion: String by rootProject.extra
val springBootVersion: String by rootProject.extra

java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
	mavenCentral()
}

dependencies {

	implementation(project(":model"))
	implementation(project(":repository"))

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

	implementation("org.springframework.boot:spring-boot-starter-web:${springBootVersion}")
	implementation("org.springframework.data:spring-data-redis:${springBootVersion}")
	implementation("org.springframework.kafka:spring-kafka:${springBootVersion}")
	implementation("org.apache.kafka:connect-api:0.10.0.0")
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
