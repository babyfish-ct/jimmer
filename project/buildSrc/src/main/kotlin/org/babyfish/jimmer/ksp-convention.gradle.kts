import com.google.devtools.ksp.gradle.KspAATask
import org.babyfish.jimmer.DtoDirectories

plugins {
    id("kotlin-convention")
    id("com.google.devtools.ksp")
}

val projectDirectory = layout.projectDirectory
val dtoDirectories = extensions.create<DtoDirectories>("dtoDirectories").apply {
    main.convention(listOf("src/main/dto"))
    test.convention(listOf("src/test/dto"))
}

ksp {
    arg("jimmer.dto.dirs", dtoDirectories.main.map { it.joinToString(",") })
    arg("jimmer.dto.testDirs", dtoDirectories.test.map { it.joinToString(",") })
}

tasks.withType<KspAATask>().configureEach {
    val directories = if (name == "kspKotlin") dtoDirectories.main else dtoDirectories.test
    val dtoFiles = directories.map { directories ->
        directories.map { directory ->
            projectDirectory.dir(directory).asFileTree.matching { include("**/*.dto") }
        }
    }
    inputs.files(dtoFiles).withPropertyName("jimmerDtoFiles").withPathSensitivity(PathSensitivity.RELATIVE)
}
