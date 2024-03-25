plugins {
    id("org.jetbrains.dokka")
}

tasks {
    withType<Jar> {
        if (archiveClassifier.get() == "javadoc") {
            from(dokkaHtml.flatMap { it.outputDirectory })
        }
    }
}