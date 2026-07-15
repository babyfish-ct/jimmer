package org.babyfish.jimmer.sql.association.meta;

import org.babyfish.jimmer.impl.util.PropCache;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.model.Author;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.inheritance.NamedEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class PropCacheTest {

    @Test
    public void testAssociationProps() {
        AtomicInteger invocationCount = new AtomicInteger();
        PropCache<Object> cache = new PropCache<>(prop -> {
            invocationCount.incrementAndGet();
            return new Object();
        });
        AssociationType bookAuthorsType = AssociationType.of(
                ImmutableType.get(Book.class).getProp("authors")
        );
        AssociationType authorBooksType = AssociationType.of(
                ImmutableType.get(Author.class).getProp("books")
        );
        ImmutableProp bookAuthorsSourceProp = bookAuthorsType.getSourceProp();
        ImmutableProp bookAuthorsTargetProp = bookAuthorsType.getTargetProp();
        ImmutableProp authorBooksSourceProp = authorBooksType.getSourceProp();

        Object bookAuthorsSourceValue = cache.get(bookAuthorsSourceProp);
        Assertions.assertSame(bookAuthorsSourceValue, cache.get(bookAuthorsSourceProp));
        Assertions.assertNotSame(bookAuthorsSourceValue, cache.get(bookAuthorsTargetProp));
        Assertions.assertNotSame(bookAuthorsSourceValue, cache.get(authorBooksSourceProp));
        Assertions.assertEquals(3, invocationCount.get());
    }

    @Test
    public void testNameBasedProp() {
        AtomicInteger invocationCount = new AtomicInteger();
        PropCache<Object> cache = new PropCache<>(prop -> {
            invocationCount.incrementAndGet();
            return new Object();
        });
        ImmutableProp nameProp = ImmutableType.get(NamedEntity.class).getProp("name");

        Assertions.assertEquals(-1, nameProp.getId().asIndex());
        Object value = cache.get(nameProp);
        Assertions.assertSame(value, cache.get(nameProp));
        Assertions.assertEquals(1, invocationCount.get());
    }
}
