import org.babyfish.Vars.jvmMainKspBuildMetaDataDir
import org.babyfish.Vars.jvmMainSourceDir

// 计算各模块目录（使用常量字符串）
//这里的项目以实际名称为主 通常是:backend:server
val serverProject = project(":backend:server")
val modelProject = project(":backend:model")
//val composeProject = project(":composeApp")
//val sharedProject = project(":shared")
val serverSourceDir = serverProject.projectDir.resolve(jvmMainSourceDir).absolutePath
val serverBuildDir = serverProject.projectDir.resolve(jvmMainKspBuildMetaDataDir).absolutePath


val modelSourceDir = modelProject.projectDir.resolve(jvmMainSourceDir).absolutePath
val modelBuildDir = modelProject.projectDir.resolve(jvmMainKspBuildMetaDataDir).absolutePath

//val composeSourceDir = composeProject.projectDir.resolve(commonMainSourceDir).absolutePath
//val composeBuildDir = composeProject.projectDir.resolve(commonMainKspBuildMetaDataDir).absolutePath

//val sharedSourceDir = sharedProject.projectDir.resolve(commonMainSourceDir).absolutePath
//val sharedBuildDir = sharedProject.projectDir.resolve(commonMainKspBuildMetaDataDir).absolutePath
//

plugins {
    id("com.google.devtools.ksp")
}

ksp {
    arg("serverSourceDir", serverSourceDir)
    arg("serverBuildDir", serverBuildDir)

    //我们通常把模型生成在这个目录(ksp处理器jimmer-ksp-jdbc2entity 需要modelSourceDir, 以配置模型生成的源码目录,包路径的配置见ksp4jdbc.gradle.kts)
    arg("modelSourceDir", modelSourceDir)
    arg("modelBuildDir", modelBuildDir)
}












