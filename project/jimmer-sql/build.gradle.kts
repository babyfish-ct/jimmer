plugins {
    java
}

group = "org.babyfish.jimmer"
version = "0.0.2"

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

    implementation(project(":jimmer-core"))
    implementation("javax.validation:validation-api:2.0.1.Final")
    implementation("javax.persistence:javax.persistence-api:2.2")
    implementation("org.apache.commons:commons-lang3:3.12.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("com.h2database:h2:2.1.212")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    testAnnotationProcessor(project(":jimmer-apt"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}