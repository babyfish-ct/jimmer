package org.babyfish.jimmer.error

abstract class CodeBasedException protected constructor(
    message: String,
    cause: Throwable?
) : RuntimeException(message, cause) {

    abstract val code: Enum<*>

    abstract val fields: Map<String, Any?>

    // In order to support the overload version for java,
    // the default argument of kotlin is not used
    fun toExportedError(): ExportedError =
        toExportedError(false)

    fun toExportedError(withDebugInfo: Boolean): ExportedError =
        ExportedError(
            familyName(code.javaClass.simpleName),
            code.name,
            fields,
            if (withDebugInfo) ErrorDebugInfo.of(this) else null
        )

    companion object {

        @JvmStatic
        fun familyName(name: String): String {
            var prevLower = false
            val size = name.length
            val builder = StringBuilder()
            for (i in 0 until size) {
                val c = name[i]
                if (Character.isUpperCase(c)) {
                    if (prevLower) {
                        builder.append("_")
                    }
                    prevLower = false
                    builder.append(c)
                } else {
                    prevLower = true
                    builder.append(c.uppercaseChar())
                }
            }
            return builder.toString()
        }
    }
}
