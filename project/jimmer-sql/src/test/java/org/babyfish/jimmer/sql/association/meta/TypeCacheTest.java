package org.babyfish.jimmer.sql.association.meta;

import org.babyfish.jimmer.impl.util.TypeCache;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.model.Author;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.middle.Shop;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class TypeCacheTest {

    @Test
    public void testAssociationTypes() {
        AtomicInteger invocationCount = new AtomicInteger();
        TypeCache<Object> cache = new TypeCache<>(type -> {
            invocationCount.incrementAndGet();
            return new Object();
        });
        AssociationType bookAuthorsType = AssociationType.of(
                ImmutableType.get(Book.class).getProp("authors")
        );
        AssociationType authorBooksType = AssociationType.of(
                ImmutableType.get(Author.class).getProp("books")
        );
        ImmutableType shopType = ImmutableType.get(Shop.class);
        AssociationType shopCustomersType = AssociationType.of(shopType.getProp("customers"));
        AssociationType shopVendorsType = AssociationType.of(shopType.getProp("vendors"));

        Object bookAuthorsValue = cache.get(bookAuthorsType);
        Assertions.assertSame(bookAuthorsValue, cache.get(bookAuthorsType));
        Assertions.assertNotSame(bookAuthorsValue, cache.get(authorBooksType));
        Object shopValue = cache.get(shopType);
        Object shopCustomersValue = cache.get(shopCustomersType);
        Object shopVendorsValue = cache.get(shopVendorsType);
        Assertions.assertNotSame(shopValue, shopCustomersValue);
        Assertions.assertNotSame(shopCustomersValue, shopVendorsValue);
        Assertions.assertSame(shopCustomersValue, cache.get(shopCustomersType));
        Assertions.assertSame(shopVendorsValue, cache.get(shopVendorsType));
        Assertions.assertEquals(5, invocationCount.get());
    }
}
