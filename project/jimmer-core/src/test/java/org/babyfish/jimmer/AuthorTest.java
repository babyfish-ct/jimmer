package org.babyfish.jimmer;

import org.babyfish.jimmer.model.AuthorDraft;
import org.junit.jupiter.api.Assertions;
import org.babyfish.jimmer.support.JacksonBackend;
import org.babyfish.jimmer.support.JacksonTest;
import org.junit.jupiter.api.Test;

import javax.validation.ValidationException;

public class AuthorTest {

    @JacksonTest
    public void testValidEmail(JacksonBackend backend) {
        Assertions.assertEquals(
                "{\"email\":\"tom@gmail.com\"}",
                backend.write(AuthorDraft.$.produce(a -> a.setEmail("tom@gmail.com")))
        );
    }

    @Test
    public void testInvalidEmail() {
        Assertions.assertThrows(ValidationException.class, () -> {
            AuthorDraft.$.produce(a -> a.setEmail("tomgmail.com"));
        });
        Assertions.assertThrows(ValidationException.class, () -> {
            AuthorDraft.$.produce(a -> a.setEmail("tom@@gmail.com"));
        });
        Assertions.assertThrows(ValidationException.class, () -> {
            AuthorDraft.$.produce(a -> a.setEmail("@tomgmail.com"));
        });
        Assertions.assertThrows(ValidationException.class, () -> {
            AuthorDraft.$.produce(a -> a.setEmail("tomgmail.com@"));
        });
    }
}
