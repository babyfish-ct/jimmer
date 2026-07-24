plugins {
    `java-convention`
}

dependencies {
    api(projects.jimmerSql)
    annotationProcessor(projects.jimmerApt)
}
