import org.jetbrains.dokka.DokkaVersion

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

dependencies {
    dokkaHtmlPlugin("org.jetbrains.dokka", "dokka-base", DokkaVersion.version)
}