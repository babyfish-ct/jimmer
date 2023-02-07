package org.babyfish.jimmer.kt

import org.babyfish.jimmer.error.ErrorFamily
import org.babyfish.jimmer.error.ErrorField

@ErrorFamily
enum class BusinessError {

    UNAUTHORIZED,

    @ErrorField(name = "userNames", type = String::class, list = true, nullable = true)
    ILLEGAL_USER_NAME,

    @ErrorField(name = "x", type = Int::class)
    @ErrorField(name = "y", type = Int::class)
    REFERENCE_CYCLE
}