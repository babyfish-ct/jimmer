package org.babyfish.jimmer.kt

import org.babyfish.jimmer.kt.model.RegisterRequest
import org.babyfish.jimmer.kt.model.by
import org.junit.Test
import java.lang.IllegalArgumentException
import javax.validation.ValidationException
import kotlin.test.assertFailsWith
import kotlin.test.expect

class RegisterRequestTest {

    @Test
    fun testSetIdCard() {
        val request = new(RegisterRequest::class).by {
            idCard = "123456789123456789"
        }
        expect("""{"idCard":"123456789123456789"}""") {
            request.toString()
        }
    }

    @Test
    fun testSetIdCardFailed() {
        assertFailsWith(
            ValidationException::class
        ) {
            new(RegisterRequest::class).by {
                idCard = "12345678912345678"
            }
        }.let {
            expect("Illegal id-card number") {
                it.message
            }
        }
    }

    @Test
    fun testSetPassword() {
        val request = new(RegisterRequest::class).by {
            password = "abc"
            passwordAgain = "abc"
        }
        expect("""{"password":"abc","passwordAgain":"abc"}""") {
            request.toString()
        }
    }

    @Test
    fun testSetPasswordFailed() {
        assertFailsWith(
            ValidationException::class
        ) {
            new(RegisterRequest::class).by {
                password = "abc"
                passwordAgain = "abC"
            }
        }.let {
            expect("The passwords must be same") {
                it.message
            }
        }
    }
}