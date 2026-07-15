package org.babyfish.jimmer.jackson;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.model.Book;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

public class ImmutablePropsTest {

    @Test
    public void testJacksonPackageFacade() throws Exception {
        ImmutableType type = ImmutableType.get(Book.class);

        assertSame(
                org.babyfish.jimmer.json.ImmutableProps.get(type, Book.class.getMethod("name")),
                ImmutableProps.get(type, Book.class.getMethod("name"))
        );
    }
}
