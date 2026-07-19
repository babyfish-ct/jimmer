package org.babyfish.jimmer.dto.compiler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SourceTypeFilterTest {

    @Test
    public void testEmptyFilter() {
        SourceTypeFilter filter = new SourceTypeFilter(null, null);
        Assertions.assertTrue(filter.test("org.example.Book"));
    }

    @Test
    public void testIncludesAreWhitelist() {
        SourceTypeFilter filter = new SourceTypeFilter(
                "org.example.book; org.example.author,org.example.store",
                null
        );
        Assertions.assertTrue(filter.test("org.example.book.Book"));
        Assertions.assertTrue(filter.test("org.example.author.Author"));
        Assertions.assertTrue(filter.test("org.example.store.BookStore"));
        Assertions.assertFalse(filter.test("org.example.customer.Customer"));
    }

    @Test
    public void testExcludesHavePriority() {
        SourceTypeFilter filter = new SourceTypeFilter(
                "org.example",
                "org.example.internal; org.example.legacy"
        );
        Assertions.assertTrue(filter.test("org.example.Book"));
        Assertions.assertFalse(filter.test("org.example.internal.Secret"));
        Assertions.assertFalse(filter.test("org.example.legacy.LegacyBook"));
        Assertions.assertFalse(filter.test("org.other.Author"));
    }
}
