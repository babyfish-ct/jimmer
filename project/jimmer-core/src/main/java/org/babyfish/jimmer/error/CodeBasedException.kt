package org.babyfish.jimmer.error

abstract class CodeBasedException protected constructor(
    message: String,
    cause: Throwable?
) : Exception(message, cause) {

    private val metadata: ClientExceptionMetadata =
        ClientExceptionMetadata.of(this.javaClass)

    abstract val fields: Map<String, Any?>

    val family: String
        get() = metadata.family

    val code: String
        get() = metadata.code
}
