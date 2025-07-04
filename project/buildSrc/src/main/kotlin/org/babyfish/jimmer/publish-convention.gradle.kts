plugins {
    id("com.vanniktech.maven.publish")
}

afterEvaluate {
    tasks.findByName("plainJavadocJar")?.let { plainJavadocJarTask ->
        tasks.named("generateMetadataFileForMavenPublication") {
            dependsOn(plainJavadocJarTask)
        }
    }
}

mavenPublishing {

    publishToMavenCentral(automaticRelease = true)

    signAllPublications()
}

mavenPublishing {

    coordinates(project.group.toString(), project.name, project.version.toString())

    pom {
        name.set("Jimmer")
        description.set("The most advanced ORM of JVM, for both java & kotlin")
        inceptionYear.set("2025")
        url.set("https://github.com/babyfish-ct/jimmer")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://github.com/babyfish-ct/jimmer/blob/main/LICENSE")
            }
        }
        developers {
            developer {
                id.set("babyfish-ct")
                name.set("陈涛")
                email.set("babyfish.ct@gmail.com")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/babyfish-ct/jimmer.git")
            developerConnection.set("scm:git:ssh://github.com/babyfish-ct/jimmer.git")
            url.set("https://github.com//babyfish-ct/jimmer")
        }
    }
}
