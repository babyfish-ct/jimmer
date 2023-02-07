package org.babyfish.jimmer.error

abstract class CodeBasedException protected constructor(
    message: String,
    cause: Throwable?
) : RuntimeException(message, cause) {

    abstract val code: Enum<*>
    abstract val fields: Map<String, Any?>

    val exportedError: ExportedError
        get() {
            val code = code
            return ExportedError(
                familyName(code.javaClass.simpleName),
                code.name,
                fields
            )
        }

    companion object {
        private fun familyName(name: String): String {
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
