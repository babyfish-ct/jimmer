package org.babyfish.jimmer.client.kotlin.util

import org.babyfish.jimmer.client.meta.Doc
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DocTest {

    @Test
    fun test() {
        val docText =
            """/**
                |@param A First
                |@param
                |@param B
                |@param
                |*/""".trimMargin()
        val doc = Doc.parse(docText)
        Assertions.assertEquals(
            mapOf(
                "A" to "First",
                "B" to ""
            ),
            doc.parameterValueMap
        )
    }
}