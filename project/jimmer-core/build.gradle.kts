plugins {
    `kotlin-convention`
    alias(libs.plugins.buildconfig)
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
    implementation(libs.javax.validation.api)
    api(libs.jackson.databind)
    api(libs.kotlin.reflect)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.apache.commons.lang3)
    implementation(libs.kotlin.stdlib)
    compileOnly(libs.mapstruct)

    testImplementation(libs.jupiter.api)
    testImplementation(libs.mapstruct)
    testImplementation(libs.lombok)
    testRuntimeOnly(libs.jupiter.engine)

    testAnnotationProcessor(projects.jimmerApt)
    testAnnotationProcessor(libs.lombok)
    testAnnotationProcessor(libs.mapstruct.processor)
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Ajimmer.source.excludes=org.babyfish.jimmer.invalid")
    options.compilerArgs.add("-Ajimmer.generate.dynamic.pojo=true")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<Javadoc>{
    options.encoding = "UTF-8"
}

buildConfig {
    val versionParts = (project.version as String).split('.')
    packageName(project.group as String)
    className("JimmerVersion")
    buildConfigField("int", "major", versionParts[0])
    buildConfigField("int", "minor", versionParts[1])
    buildConfigField("int", "patch", versionParts[2])
    useKotlinOutput {
        internalVisibility = false
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
            artifactId = "jimmer-core"
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