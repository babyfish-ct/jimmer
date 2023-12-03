package org.babyfish.jimmer.sql.kt.api

import org.babyfish.jimmer.error.ErrorFamily
import org.babyfish.jimmer.error.ErrorField
import java.time.LocalDateTime

@ErrorFamily
@ErrorField(name = "tags", type = String::class, list = true, doc = "TagList")
@ErrorField(name = "timestamp", type = LocalDateTime::class, doc = "Error created time")
enum class SystemErrorCode {

    @ErrorField(name = "minBound", type = Int::class, doc = "Min Bound value")
    @ErrorField(name = "maxBound", type = Int::class, doc = "Max Bound value")
    A,

    @ErrorField(name = "path", type = String::class, doc = "The file path which cannot be accessed")
    B,

    @ErrorField(name = "baseUrl", type = String::class, doc = "The url which cannot be accessed")
    @ErrorField(name = "port", type = Int::class, doc = "The port which annot be accessed")
    C
}
