package org.babyfish.jimmer.error

import java.lang.RuntimeException

abstract class CodeBasedRuntimeException protected constructor(
    message: String,
    cause: Throwable?
) : RuntimeException(message, cause) {

    private val metadata: ClientExceptionMetadata =
        ClientExceptionMetadata.of(this.javaClass)

    abstract val fields: Map<String, Any?>

    val family: String
        get() = metadata.family

    val code: String
        get() = metadata.code
}
