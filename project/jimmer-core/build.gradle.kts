plugins {
    `kotlin-convention`
    `jvm-test-suite`
    alias(libs.plugins.buildconfig)
}

dependencies {
    api(libs.jspecify)
    api(libs.kotlin.reflect)
    implementation(libs.javax.validation.api)
    implementation(libs.kotlin.stdlib)
    compileOnly(libs.bundles.jackson)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Ajimmer.source.excludes=org.babyfish.jimmer.invalid")
    options.compilerArgs.add("-Ajimmer.generate.dynamic.pojo=true")
}

buildConfig {
    val versionParts = (project.version as String).split('.')
    packageName("org.babyfish.jimmer")
    className("JimmerVersion")
    buildConfigField("int", "major", versionParts[0])
    buildConfigField("int", "minor", versionParts[1])
    buildConfigField("int", "patch", versionParts[2])
    useKotlinOutput {
        internalVisibility = false
    }
}

testing {
    suites {
        withType<JvmTestSuite> {
            useJUnitJupiter()
            dependencies {
                implementation(libs.mapstruct)
                implementation(libs.javax.validation.api)
                compileOnly(libs.lombok)
                annotationProcessor(projects.jimmerApt)
                annotationProcessor(libs.lombok)
                annotationProcessor(libs.mapstruct.processor)
            }
        }
        val test by getting(JvmTestSuite::class) {
            dependencies {
                implementation(libs.jackson2.databind)
                implementation(libs.jackson2.datatype.jsr310)
            }
        }
        val testJackson3 by registering(JvmTestSuite::class) {
            sources {
                java { setSrcDirs(test.sources.java.srcDirs) }
                resources { setSrcDirs(test.sources.resources.srcDirs) }
            }
            dependencies {
                implementation(project())
                implementation(libs.jackson3.databind)
            }
        }
        tasks.check { dependsOn(testJackson3) }
    }
}
