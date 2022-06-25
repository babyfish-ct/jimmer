plugins {
    `java-library`
    id("maven-publish")
    id("signing")
}

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

    api(project(":jimmer-core"))
    api("javax.persistence:javax.persistence-api:2.2")
    implementation("org.slf4j:slf4j-api:1.7.36")

    testAnnotationProcessor(project(":jimmer-apt"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    testImplementation("org.springframework:spring-jdbc:5.3.20")
    testImplementation("com.fasterxml.uuid:java-uuid-generator:4.0.1")

    testImplementation("com.h2database:h2:2.1.212")
    testImplementation("mysql:mysql-connector-java:8.0.29")
    testImplementation("org.postgresql:postgresql:42.3.4")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
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
