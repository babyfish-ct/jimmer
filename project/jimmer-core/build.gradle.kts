plugins {
    `java-library`
    kotlin("jvm") version "1.6.10"
    id("maven-publish")
    id("signing")
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

    api("javax.validation:validation-api:2.0.1.Final")
    api("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    api("org.jetbrains.kotlin:kotlin-reflect:1.7.10")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.3")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.10")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    testAnnotationProcessor(project(":jimmer-apt"))
}

tasks.withType(JavaCompile::class) {
    options.compilerArgs.add("-Ajimmer.source.excludes=org.babyfish.jimmer.invalid")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<Javadoc>{
    options.encoding = "UTF-8"
}

// Publish to maven-----------------------------------------------------
val NEXUS_USERNAME: String by project
val NEXUS_PASSWORD: String by project

publishing {
    repositories {
        maven {
            credentials {
                username = NEXUS_USERNAME
                password = NEXUS_PASSWORD
            }
            name = "MavenCentral"
            url = if (project.version.toString().endsWith("-SNAPSHOT")) {
                uri("https://oss.sonatype.org/content/repositories/snapshots")
            } else {
                uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            }
        }
    }
    publications {
        register("mavenJava", MavenPublication::class) {
            artifactId = "jimmer-core"
            from(components["java"])
            pom {
                name.set("jimmer")
                description.set("immer for java")
                url.set("https://github.com/babyfish-ct/jimmer")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://github.com/babyfish-ct/jimmer/blob/main/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("babyfish-ct")
                        name.set("陈涛")
                        email.set("babyfish.ct@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/babyfish-ct/jimmer.git")
                    developerConnection.set("scm:git:ssh://github.com/babyfish-ct/jimmer.git")
                    url.set("https://github.com//babyfish-ct/jimmer")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}
