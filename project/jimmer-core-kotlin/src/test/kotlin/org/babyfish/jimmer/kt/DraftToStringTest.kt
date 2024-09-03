package org.babyfish.jimmer.kt

import org.babyfish.jimmer.kt.model.Book
import org.babyfish.jimmer.kt.model.addBy
import kotlin.test.Test
import kotlin.test.expect

class DraftToStringTest {

    @Test
    fun test() {
        val builder = StringBuilder()
        Book {
            name = "SQL in Action"
            store {
                name = "MANNING"
            }
            authors().addBy {
                firstName = "James"
                lastName = "Miller"
            }
            authors().addBy {
                firstName = "Cramer"
                lastName = "London"
            }
            builder.append(this)
        }
        expect(
            """{
                |"name":"SQL in Action",
                |"store":{"name":"MANNING"},
                |"authors":[
                |{"firstName":"James","lastName":"Miller"},
                |{"firstName":"Cramer","lastName":"London"}
                |]
                |}""".trimMargin().replace("\n", "")
        ) {
            builder.toString()
        }
    }
}