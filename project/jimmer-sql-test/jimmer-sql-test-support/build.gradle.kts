import org.gradle.api.attributes.java.TargetJvmVersion

plugins {
    `java-convention`
}

dependencies {
    testImplementation(projects.jimmerSqlTest.jimmerSqlTestModel)
    testImplementation(projects.jimmerSqlTest.jimmerSqlTestModelKotlin)
    testImplementation(projects.jimmerJsonJackson2)
    testImplementation(projects.jimmerJsonJackson3)
    testImplementation(projects.jimmerJsonGson)
    testImplementation(projects.jimmerJsonFastjson2)
}

tasks.compileTestJava {
    options.release.set(17)
}

configurations.testRuntimeClasspath {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
}
