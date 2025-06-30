package org.babyfish.jimmer.ksp.util

import java.io.File

fun guessResourceFile(file: File?, name: String): File? =
    guessResourceDir(file)?.let { File(it, name) }

private fun guessResourceDir(file: File?): File? {
    return tryGetResourceDir(file) ?: guessResourceDir(file?.parentFile ?: return null)
}

private fun tryGetResourceDir(file: File?): File? =
    file
        ?.takeIf(File::isDirectory)
        ?.let { File(it, "generated") }
        ?.takeIf(File::isDirectory)
        ?.let { File(it, "ksp") }
        ?.takeIf(File::isDirectory)
        ?.let {
            File(it, "main").takeIf(File::isDirectory)
                ?: File(it, "test").takeIf(File::isDirectory)
                ?: File(it, "debug").takeIf(File::isDirectory)
                ?: File(it, "release").takeIf(File::isDirectory)
        }
        ?.let { File(it, "resources") }
        ?.let { File(it, "META-INF") }
        ?.let { File(it, "jimmer") }
