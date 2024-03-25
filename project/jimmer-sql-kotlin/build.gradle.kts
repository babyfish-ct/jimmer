plugins {
    `kotlin-convention`
    `dokka-convention`
    alias(libs.plugins.ksp)
    id("maven-publish")
    id("signing")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    api(projects.jimmerCoreKotlin)
    api(projects.jimmerSql)
    implementation(libs.apache.commons.lang3)

    testImplementation(libs.kotlin.test)
    kspTest(projects.jimmerKsp)
    testAnnotationProcessor(projects.jimmerKsp)

    testImplementation(libs.h2)
    testImplementation(libs.javax.validation.api)
    dokkaHtmlPlugin(libs.dokka.base)
    testImplementation(libs.jackson.datatype.jsr310)
    testImplementation(libs.postgresql)
    testImplementation(libs.spring.jdbc)
    testImplementation(libs.jackson.module.kotlin)
}

kotlin {
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=all")
        jvmTarget = "1.8"
    }
}

tasks.test {
    useJUnit()
}

java {
    withSourcesJar()
    withJavadocJar()
}

ksp {
    arg("jimmer.dto.mutable", "true")
}

tasks {
    withType(Jar::class) {
        if (archiveClassifier.get() == "javadoc") {
            dependsOn(dokkaHtml)
            from("build/dokka/html")
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
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
            artifactId = "jimmer-sql-kotlin"
            from(components["java"])
            pom {
                name.set("jimmer")
                description.set("A revolutionary ORM framework for both java and kotlin")
                url.set("https://github.com/babyfish-ct/jimmer")
                licenses {
                    license {
                        name.set("Apache-2.0")
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
