plugins {
    `kotlin-convention`
    alias(libs.plugins.ksp)
    id("maven-publish")
    id("signing")
}

group = "org.babyfish.jimmer"

repositories {
    mavenCentral()
}

dependencies {
    api(projects.jimmerSql)
    api(projects.jimmerSqlKotlin)
    api(projects.jimmerClient)
    api(libs.spring.boot.starter.jdbc)
    api(libs.spring.data.commons)
    api(libs.jackson.module.kotlin)

    compileOnly(libs.spring.boot.starter.web)
    compileOnly(libs.spring.data.redis)
    compileOnly(libs.caffeine)
    compileOnly(libs.spring.graphql)
    compileOnly(libs.jakartaee.api)
    compileOnly(libs.springdoc.openapi.common)

    annotationProcessor(libs.spring.boot.configurationProcessor)
    testAnnotationProcessor(projects.jimmerApt)
    kspTest(projects.jimmerKsp)

    testImplementation(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.jupiter.api)
    testImplementation(libs.h2)
    testRuntimeOnly(libs.jupiter.engine)
    testImplementation(libs.spring.boot.starter.web)
}

kotlin {
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}

ksp {
    arg("jimmer.source.excludes", "org.babyfish.jimmer.spring.java")
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
            artifactId = "jimmer-spring-boot-starter"
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
