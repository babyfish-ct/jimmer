plugins {
    `java-library`
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

    implementation("javax.validation:validation-api:2.0.1.Final")
    implementation("org.springframework:spring-core:5.3.20")
    implementation("com.intellij:annotations:12.0")
    implementation("com.squareup:javapoet:1.13.0")
    implementation(project(":jimmer-core"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<Javadoc>{
    options.encoding = "UTF-8"
}
