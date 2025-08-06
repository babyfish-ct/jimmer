/**
 * 要么全部通过 buildSrc，要么全部通过版本目录  除了benchmark  其余模块 alias(libs.plugins.ksp)  =>    `ksp-convention`
 */
plugins {
    id("com.google.devtools.ksp")
}

