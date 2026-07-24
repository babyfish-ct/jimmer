plugins {
    `java-convention`
}

dependencies {
    api(projects.jimmerSql)
    api(projects.jimmerSqlTest.jimmerSqlTestModelBase)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    annotationProcessor(projects.jimmerApt)
    annotationProcessor(files("src/main/dto-bundle"))

    compileOnly(libs.bundles.jackson)
    compileOnly(libs.hibernate.validation)
    compileOnly(libs.javax.validation.api)
    compileOnly(libs.jetbrains.annotations)
    compileOnly(libs.postgresql)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xmaxerrs")
    options.compilerArgs.add("2000")
    options.compilerArgs.add("-Ajimmer.dto.hibernateValidatorEnhancement=true")
    options.compilerArgs.add("-Ajimmer.dto.fieldVisibility=protected")
    options.compilerArgs.add("-Ajimmer.jackson3=true")
}
