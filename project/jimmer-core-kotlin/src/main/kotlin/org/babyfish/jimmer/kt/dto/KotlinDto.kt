package org.babyfish.jimmer.kt.dto

/**
 * - This annotation should **NEVER** be used
 * to decorate kotlin type, it is a fake
 * annotation to decorate DTO type.
 * - This annotation is only processed by
 * `jimmer-ksp` of kotlin, it will always
 * be ignored by `jimmer-apt` or java.
 *
 * Jimmer KSP can generate both immutable
 * and mutable DTO classes:
 * - If DTO type is decorated by this annotation
 * and the argument is [KotlinDtoImmutability.IMMUTABLE] or
 * [KotlinDtoImmutability.MUTABLE], generates kotlin DTO
 * according to that configuration.
 * - Otherwise, if the KSP argument `jimmer.dto.mutable`
 * is `true`, generates mutable DTO, otherwise, generates
 * immutable DTO.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(allowedTargets = []) // Decorate DTO type, not kotlin type
annotation class KotlinDto(
    val immutability : KotlinDtoImmutability
)