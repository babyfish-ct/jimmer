package org.babyfish.jimmer.sql.kt.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.babyfish.jimmer.sql.kt.common.assertContent
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookViewForIssue843
import org.babyfish.jimmer.sql.kt.model.dto.UserView
import org.babyfish.jimmer.sql.kt.model.filter.User
import testpkg.annotations.Serializable
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.expect

class AnnotationTest {

    @Test
    fun testEntityAnnotations() {
        val a1 = UserView::class.annotations.first {
            it.annotationClass == Serializable::class
        } as Serializable?
        val a2 = UserView::name.annotations.first {
            it.annotationClass == Serializable::class
        } as Serializable?
        assertNotNull(a1)
        assertNotNull(a2)
        expect(User::class) { a1.with }
        expect(String::class) { a2.with }
    }

    @Test
    fun testForIssue843() {
        val view = BookViewForIssue843(
            id = 3L,
            name = "RUST programming",
            edition = 3,
            price = BigDecimal("53.4")
        )
        assertContent(
            """{"id":3,"name":"RUST programming","price":53.4}""",
            ObjectMapper().writeValueAsString(view)
        )
    }
}