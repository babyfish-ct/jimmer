package org.babyfish.jimmer.ksp.dto

import java.lang.RuntimeException

class DtoException(
    message: String, cause: Throwable? = null
): RuntimeException(message, cause)