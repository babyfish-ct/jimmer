import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-convention`
    antlr
    id("maven-publish")
    id("signing")
}

dependencies {
    api(projects.jimmerCore)
    implementation(libs.slf4j.api)
    implementation(libs.kotlin.stdlib)
    implementation(libs.jetbrains.annotations)
    implementation(libs.apache.commons.lang3)
    implementation(libs.jackson.datatype.jsr310)
    compileOnly(libs.postgresql)
    compileOnly(libs.jackson.module.kotlin)
    antlr(libs.antlr) {
        exclude("com.ibm.icu", "icu4j")
    }

    testAnnotationProcessor(projects.jimmerApt)
    testImplementation(libs.jupiter.api)
    testRuntimeOnly(libs.jupiter.engine)
    testImplementation(libs.spring.jdbc)

    testImplementation(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    testImplementation(libs.h2)
    testImplementation(libs.mysql.connector.java)
    testImplementation(libs.postgresql)
    testImplementation(libs.kafka.connect.api)
    testImplementation(libs.javax.validation.api)
    // testImplementation(files("/Users/chentao/Downloads/ojdbc8-21.9.0.0.jar"))
}

tasks.withType<Jar> {
    dependsOn(tasks.withType<AntlrTask>())
}

tasks.withType<KotlinCompile> {
    dependsOn(tasks.withType<AntlrTask>())
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xmaxerrs")
    options.compilerArgs.add("2000")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=all")
        jvmTarget = "1.8"
    }
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
            artifactId = "jimmer-sql"
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

