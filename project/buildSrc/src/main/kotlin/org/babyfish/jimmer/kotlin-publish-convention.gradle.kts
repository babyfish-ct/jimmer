plugins {
    id("kotlin-convention")
    id("publish-convention")
}

extensions.configure<JavaPluginExtension> {
    withSourcesJar()
}
