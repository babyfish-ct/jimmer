plugins {
    `java-convention`
}

dependencies {
    api(projects.jimmerSpringBootStarter)
    api(projects.jimmerJackson2)
    api(projects.jimmerJackson3)

    annotationProcessor(libs.spring.boot.configurationProcessor)
}
