package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.model.dto.UserView
import org.babyfish.jimmer.sql.kt.model.filter.User
import testpkg.annotations.Serializable
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
}