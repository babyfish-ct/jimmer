package org.babyfish.jimmer.sql.kt.filter

import org.babyfish.jimmer.sql.kt.filter.impl.toJavaFilter
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import kotlin.test.Test

class FilterJdkProxyTest {

    @Test
    fun testJavaFilterWrapper() {
        val ktFilter = KtFilterImpl()
        val javaFilter = ktFilter.toJavaFilter()
        javaFilter.hashCode()
    }

    private class KtFilterImpl : KFilter<Book>, KAssociationIntegrityAssuranceFilter<Book> {

        override fun filter(args: KFilterArgs<Book>) {
            error("Not implemented")
        }
    }
}