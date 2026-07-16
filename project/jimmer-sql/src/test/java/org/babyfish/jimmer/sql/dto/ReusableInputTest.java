package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.dto.BookWithReusableAssociationsInput;
import org.babyfish.jimmer.sql.model.dto.ReusableAuthorInput;
import org.babyfish.jimmer.sql.model.dto.ReusableBookStoreInput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

public class ReusableInputTest {

    @Test
    public void testAssociationInputTypesAndReverseConversion() {
        ReusableBookStoreInput store = new ReusableBookStoreInput();
        store.setId(UUID.randomUUID());
        store.setName("MANNING");

        ReusableAuthorInput author = new ReusableAuthorInput();
        author.setId(UUID.randomUUID());
        author.setFirstName("Samer");
        author.setLastName("Buna");

        BookWithReusableAssociationsInput input = new BookWithReusableAssociationsInput();
        input.setId(UUID.randomUUID());
        input.setName("GraphQL in Action");
        input.setStore(store);
        input.setAuthors(Collections.singletonList(author));

        Book book = input.toImmutable();
        Assertions.assertEquals("MANNING", book.store().name());
        Assertions.assertEquals("Samer", book.authors().get(0).firstName());
    }
}
