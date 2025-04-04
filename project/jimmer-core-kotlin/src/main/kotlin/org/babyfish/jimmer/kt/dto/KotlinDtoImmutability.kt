package org.babyfish.jimmer.kt.dto

enum class KotlinDtoImmutability {
    /**
     * No configuration is required.
     * Refer to the global configuration and check whether
     * the KSP argument `jimmer.dto.mutable` is `true` to
     * determine whether the generated DTO should be mutable or immutable.
     */
    AUTO,

    /**
     * Generate immutable DTO class
     */
    IMMUTABLE,

    /**
     * Generate mutable DTO class
     */
    MUTABLE
}