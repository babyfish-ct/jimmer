import java.net.URLClassLoader

plugins {
    `ksp-convention`
}

dependencies {
    implementation(projects.jimmerSql)
    annotationProcessor(projects.jimmerApt) {
        exclude(module = "jimmer-json-jackson2")
    }
    annotationProcessor(projects.jimmerJsonGson)
    testImplementation(projects.jimmerSqlKotlin)
    kspTest(projects.jimmerKsp) {
        exclude(module = "jimmer-json-jackson2")
    }
    kspTest(projects.jimmerJsonFastjson2)
    testImplementation(projects.jimmerJsonGson)
    testImplementation(projects.jimmerJsonFastjson2)
}

ksp {
    arg("jimmer.json.codec", "org.babyfish.jimmer.json.fastjson2.Fastjson2Codec")
}

tasks.compileJava {
    options.compilerArgs.add("-Ajimmer.json.codec=org.babyfish.jimmer.json.gson.GsonCodec")
    inputs.files(fileTree("src/main/dto") { include("**/*.dto") })
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

tasks.test {
    val checkedClasspaths = mapOf(
        "APT processor classpath" to configurations.annotationProcessor.get(),
        "KSP processor classpath" to configurations.getByName("kspTestKotlinProcessorClasspath"),
        "Java application compile classpath" to sourceSets.main.get().compileClasspath,
        "Kotlin application compile classpath" to sourceSets.test.get().compileClasspath,
        "Application runtime classpath" to classpath
    )
    inputs.files(checkedClasspaths.values).withNormalizer(ClasspathNormalizer::class.java)
    doFirst {
        val jacksonClasses = listOf(
            "com/fasterxml/jackson/annotation/JsonIgnore.class",
            "com/fasterxml/jackson/core/JsonFactory.class",
            "com/fasterxml/jackson/databind/ObjectMapper.class",
            "tools/jackson/core/JsonFactory.class",
            "tools/jackson/databind/ObjectMapper.class"
        )
        for ((name, files) in checkedClasspaths) {
            URLClassLoader(files.map { it.toURI().toURL() }.toTypedArray(), null).use { loader ->
                for (path in jacksonClasses) {
                    val resource = loader.getResource(path)
                    check(resource == null) { "$name contains $resource" }
                }
            }
        }
    }
}
