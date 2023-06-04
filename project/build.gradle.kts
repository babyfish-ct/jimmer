plugins {
    java
    `maven-publish`
}

allprojects {
    group = "org.babyfish.jimmer"
    version = "0.7.77"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
    }
}
