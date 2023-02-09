package org.babyfish.jimmer.kt

import org.babyfish.jimmer.error.ErrorFamily
import org.babyfish.jimmer.error.ErrorField

@ErrorFamily
enum class BusinessError {

    UNAUTHORIZED,

    @ErrorField(name = "pathNodes", type = String::class, list = true, nullable = true)
    ILLEGAL_PATH_NODES,

    @ErrorField(name = "x", type = Int::class)
    @ErrorField(name = "y", type = Int::class)
    REFERENCE_CYCLE
}