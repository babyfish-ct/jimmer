plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<Javadoc>{
    options.encoding = "UTF-8"
}