plugins {
    id("org.jetbrains.dokka")
}

tasks {
    withType(Jar::class) {
        if (archiveClassifier.get() == "javadoc") {
            from(dokkaHtml.flatMap { it.outputDirectory })
        }
    }
}