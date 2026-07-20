plugins {
    id("java-convention")
    id("publish-convention")
}

extensions.configure<JavaPluginExtension> {
    withSourcesJar()
}
