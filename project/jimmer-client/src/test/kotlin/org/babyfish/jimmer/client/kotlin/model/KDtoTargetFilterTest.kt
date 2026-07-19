package org.babyfish.jimmer.client.kotlin.model

import org.babyfish.jimmer.client.kotlin.model.dto.filter.KAuthorFromFilteredFile
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class KDtoTargetFilterTest {

    @Test
    fun testAllowedDeclarationFromExcludedDefaultTargetFile() {
        Assertions.assertEquals(
            "org.babyfish.jimmer.client.kotlin.model.dto.filter.KAuthorFromFilteredFile",
            KAuthorFromFilteredFile::class.qualifiedName
        )
    }
}
