package org.babyfish.jimmer.client.kotlin.model

/**
 * The gender, which can only be `MALE` or `FEMALE`
 *
 * @property MALE Boys
 */
enum class KGender {
    MALE,

    /**
     * Girls
     */
    FEMALE;

    companion object {
        fun unuseful() {}
    }
}