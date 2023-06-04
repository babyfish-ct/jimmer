package cn.enaium.jimmer

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.create
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.SimpleRemapper

/**
 * @author Enaium
 */
class JakartaTransformPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val jakartaTransformExtension = project.extensions.create<JakartaTransformExtension>("jakartaTransform")

        project.afterEvaluate {
            project.dependencies.add("compileOnly", jakartaTransformExtension.mirror)
            project.dependencies.add("compileOnly", "jakarta.validation:jakarta.validation-api:3.0.2")

            jakartaTransformExtension.mirror.configurations.all {
                val configuration = this
                if (arrayOf("api", "implementation", "compileOnly", "runtimeOnly").contains(configuration.name)) {
                    this.dependencies.all {
                        this.group?.let {
                            project.dependencies.create(
                                group = it,
                                name = this.name,
                                version = this.version
                            )
                        }
                            ?.let {
                                project.dependencies.add(
                                    configuration.name,
                                    it
                                )
                            }
                    }
                }
            }

            tasks.getByName("javadoc").apply {
                val options = (this as Javadoc).options
                options.encoding = "UTF-8"
            }
        }

        project.task("copyClasses") {
            doLast {
                val resolve = jakartaTransformExtension.mirror.buildDir.resolve("classes")
                if (resolve.exists()) {
                    resolve.copyRecursively(project.buildDir.resolve("classes"), true)
                }
            }
        }

        project.task("copySources") {
            doLast {
                jakartaTransformExtension.mirror.projectDir.resolve("src")
                    .copyRecursively(project.projectDir.resolve("src"), true)

                project.projectDir.resolve("src").walk().forEach {
                    if (it.isFile && it.name.endsWith(".java")) {
                        it.writeText(it.readText().replace("import javax.validation", "import jakarta.validation"))
                    }
                }
            }
        }

        project.task("cleanSources") {
            doLast {
                project.projectDir.resolve("src").deleteRecursively()
            }
        }

        project.task("transform") {
            doLast {
                project.buildDir.resolve("classes").walk().forEach {
                    if (it.isFile && it.name.endsWith(".class")) {
                        val classReader = ClassReader(it.readBytes())
                        val classWriter = ClassWriter(0)
                        val classRemapping =
                            ClassRemapper(object : ClassVisitor(Opcodes.ASM9, classWriter) {
                            }, object : SimpleRemapper(emptyMap()) {
                                override fun map(key: String): String? {
                                    if (key.startsWith("javax")) {
                                        return "jakarta" + key.substring(5)
                                    }
                                    return null
                                }
                            })
                        classReader.accept(classRemapping, 0)
                        it.writeBytes(classWriter.toByteArray())
                    }
                }
            }
        }

//TODO solution 1: transform bytecode
// Step 1. copy classes
// Step 2. transform classes
// Step 3. copy sources for javadoc
// Step 4. clean sources

//        project.tasks.getByName("compileJava").finalizedBy(project.tasks.getByName("copyClasses"))
//        project.tasks.getByName("copyClasses").finalizedBy(project.tasks.getByName("transform"))
//        project.tasks.getByName("javadoc").dependsOn(project.tasks.getByName("copySources"))
//            .finalizedBy(project.tasks.getByName("cleanSources"))

//TODO solution 2: replace javax with jakarta
//Step 1. copy sources
        project.tasks.getByName("compileJava").dependsOn(project.tasks.getByName("copySources"))
    }
}