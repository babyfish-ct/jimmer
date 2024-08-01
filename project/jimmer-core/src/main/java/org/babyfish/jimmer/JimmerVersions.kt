package org.babyfish.jimmer

import java.io.InputStreamReader
import java.util.regex.Pattern

fun JimmerVersion.asString(): String = "$major.$minor.$patch"

fun currentVersion(): String = JimmerVersion.asString()

fun generationVersion(): String = generationVersion

fun isCurrentVersion(version: String): Boolean = version == currentVersion()

fun compareVersion(a: String, b: String): Int {
    val arr1 = a.split('.')
    val arr2 = b.split('.')
    val cmp1 = arr1[0].toInt() - arr2[0].toInt()
    if (cmp1 != 0) {
        return cmp1
    }
    val cmp2 = arr1[1].toInt() - arr2[1].toInt()
    if (cmp2 != 0) {
        return cmp2
    }
    return arr1[2].toInt() - arr2[2].toInt()
}

private val RESOURCE = "/META-INF/jimmer/code_generation"

private val PATTERN = Pattern.compile("""\d+\.\d+\.\d+""")

private val generationVersion = (
    JimmerVersion::class.java.getResourceAsStream(RESOURCE) ?:
    error("There is no resource file \"$RESOURCE\"")
    ).use { out ->
        InputStreamReader(out).use { reader ->
            reader.readText().trim().also {
                if (!PATTERN.matcher(it).matches()) {
                    throw IllegalArgumentException(
                        "Illegal version text \"$it\" from the resource \"$RESOURCE\""
                    )
                }
            }
        }
    }
