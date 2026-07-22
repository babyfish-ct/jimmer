plugins {
    java
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.babyfish.jimmer:jimmer-sql:0")
    annotationProcessor("org.babyfish.jimmer:jimmer-apt:0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")

    implementation("org.openjdk.jmh:jmh-core:1.37")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClass.set("org.babyfish.jimmer.benchmark.BenchmarkApplication")
}
