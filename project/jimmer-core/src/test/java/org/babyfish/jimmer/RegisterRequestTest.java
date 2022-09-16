package org.babyfish.jimmer;

import org.babyfish.jimmer.model.RegisterRequest;
import org.babyfish.jimmer.model.RegisterRequestDraft;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.validation.ValidationException;

public class RegisterRequestTest {

    @Test
    public void testSetIdCard() {
        RegisterRequest request = RegisterRequestDraft.$.produce(draft -> {
            draft.setIdCard("123456789123456789");
        });
        Assertions.assertEquals(
                "{\"idCard\":\"123456789123456789\"}",
                request.toString()
        );
    }

    @Test
    public void testSetIdCardFailed() {
        ValidationException ex = Assertions.assertThrowsExactly(ValidationException.class, () -> {
            RegisterRequestDraft.$.produce(draft -> {
                draft.setIdCard("1234567891234567");
            });
        });
        Assertions.assertEquals("The id-card number is illegal", ex.getMessage());
    }

    @Test
    public void setSetPassword() {
        RegisterRequest request = RegisterRequestDraft.$.produce(draft -> {
            draft.setPassword("abc");
            draft.setPasswordAgain("abc");
        });
        Assertions.assertEquals(
                "{\"password\":\"abc\",\"passwordAgain\":\"abc\"}",
                request.toString()
        );
    }

    @Test
    public void testSetPasswordFailed() {
        ValidationException ex = Assertions.assertThrowsExactly(ValidationException.class, () -> {
            RegisterRequestDraft.$.produce(draft -> {
                draft.setPassword("abc");
                draft.setPasswordAgain("abC");
            });
        });
        Assertions.assertEquals("Two passwords must be same", ex.getMessage());
    }
}
