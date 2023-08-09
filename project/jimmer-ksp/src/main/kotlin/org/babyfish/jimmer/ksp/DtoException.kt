package org.babyfish.jimmer.ksp

import java.lang.RuntimeException

class DtoException(
    message: String, cause: Throwable? = null
): RuntimeException(message, cause)