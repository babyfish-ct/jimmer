import org.gradle.api.JavaVersion
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

plugins {
    `java-library`
}

extensions.configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()
}

tasks.test {
    useJUnitPlatform()
}
tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}
tasks.withType<Javadoc>{
    options.encoding = "UTF-8"
}