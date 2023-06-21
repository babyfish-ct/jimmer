package org.babyfish.jimmer.client.kotlin.service

import org.babyfish.jimmer.error.ErrorFamily
import org.babyfish.jimmer.error.ErrorField
import java.time.LocalDateTime

@ErrorFamily
enum class KBusinessError {

    DATA_IS_FROZEN,

    @ErrorField(name = "planedResumeTime", type = LocalDateTime::class, nullable = true)
    SERVICE_IS_SUSPENDED,
}