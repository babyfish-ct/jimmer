package org.babyfish.jimmer.lowquery.processor.context

object Settings {
    var jimmerLowQueryGeneratedPackage: String? = null
        private set

    fun fromOptions(options: Map<String, String>) {
        jimmerLowQueryGeneratedPackage = options["jimmerLowQuery.generatedPackage"]
            ?.takeIf(String::isNotBlank)
    }
}
