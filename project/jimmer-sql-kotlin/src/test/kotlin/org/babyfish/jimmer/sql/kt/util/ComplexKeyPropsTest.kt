package org.babyfish.jimmer.sql.kt.util

import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.kt.common.assertContent
import org.babyfish.jimmer.sql.kt.model.flat.Company
import kotlin.test.Test

class ComplexKeyPropsTest {

    @Test
    fun test() {
        assertContent(
            """{
                |=[
                |--->org.babyfish.jimmer.sql.kt.model.flat.Company.companyName, 
                |--->org.babyfish.jimmer.sql.kt.model.flat.Company.street
                |], 
                |2=[
                |--->org.babyfish.jimmer.sql.kt.model.flat.Company.street, 
                |--->org.babyfish.jimmer.sql.kt.model.flat.Company.value
                |]}""".trimMargin(),
            ImmutableType.get(Company::class.java).keyMatcher.toMap()
        )
    }
}