package org.babyfish.jimmer.error

abstract class CodeBasedException protected constructor(
    message: String?,
    cause: Throwable?
) : Exception(message, cause) {

    constructor(): this(null, null)

    constructor(message: String?) : this(message, null)

    constructor(cause: Throwable?): this(null, cause)

    private val metadata: ClientExceptionMetadata =
        ClientExceptionMetadata.of(this.javaClass)

    open val fields: Map<String, Any?>
        get() = metadata
            .getterMap
            .takeIf { it.isNotEmpty() }
            ?.let {
                it.mapValues { (_, v) ->
                    v.invoke(this)
                }
            }
            ?: emptyMap()

    val family: String
        get() = metadata.family

    val code: String
        get() = metadata.code
}
