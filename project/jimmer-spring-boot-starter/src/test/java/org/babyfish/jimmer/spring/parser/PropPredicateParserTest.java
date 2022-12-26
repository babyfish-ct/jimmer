package org.babyfish.jimmer.spring.parser;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.spring.java.model.Book;
import org.babyfish.jimmer.spring.repository.parser.Context;
import org.babyfish.jimmer.spring.repository.parser.PropPredicate;
import org.babyfish.jimmer.spring.repository.parser.Source;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PropPredicateParserTest {

    @Test
    public void testDuplicatedIgnoreCase() {
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () ->
            PropPredicate.of(
                    new Context(),
                    false,
                    true,
                    new Source("AIgnoreCase"),
                    ImmutableType.get(Book.class))
        );
        Assertions.assertEquals(
                "The predicate \"[AIgnoreCase]\" cannot be ignore case when \"AllIgnoreCase\" is already set",
                ex.getMessage()
        );
    }

    @Test
    public void testEq() {
        Assertions.assertEquals(
                "UnresolvedPredicate{path=name, op=EQ, insensitive=false, likeMode=EXACT}",
                PropPredicate.of(
                        new Context(),
                        false,
                        false,
                        new Source("NameIs"),
                        ImmutableType.get(Book.class)
                ).toString()
        );
    }

    @Test
    public void testNe() {
        Assertions.assertEquals(
                "UnresolvedPredicate{path=name, op=NE, insensitive=false, likeMode=EXACT}",
                PropPredicate.of(
                        new Context(),
                        false,
                        false,
                        new Source("NameNot"),
                        ImmutableType.get(Book.class)
                ).toString()
        );
    }

    @Test
    public void testLt() {
        Assertions.assertEquals(
                "UnresolvedPredicate{path=price, op=LT, insensitive=false, likeMode=EXACT}",
                PropPredicate.of(
                        new Context(),
                        false,
                        false,
                        new Source("PriceLessThan"),
                        ImmutableType.get(Book.class)
                ).toString()
        );
    }

    @Test
    public void testLe() {
        Assertions.assertEquals(
                "UnresolvedPredicate{path=price, op=LE, insensitive=false, likeMode=EXACT}",
                PropPredicate.of(
                        new Context(),
                        false,
                        false,
                        new Source("PriceLessThanEqual"),
                        ImmutableType.get(Book.class)
                ).toString()
        );
    }

    @Test
    public void testGt() {
        Assertions.assertEquals(
                "UnresolvedPredicate{path=price, op=GT, insensitive=false, likeMode=EXACT}",
                PropPredicate.of(
                        new Context(),
                        false,
                        false,
                        new Source("PriceGreaterThan"),
                        ImmutableType.get(Book.class)
                ).toString()
        );
    }

    @Test
    public void testGe() {
        Assertions.assertEquals(
                "UnresolvedPredicate{path=price, op=GE, insensitive=false, likeMode=EXACT}",
                PropPredicate.of(
                        new Context(),
                        false,
                        false,
                        new Source("PriceGreaterThanEqual"),
                        ImmutableType.get(Book.class)
                ).toString()
        );
    }

    @Test
    public void testBetween() {
        Assertions.assertEquals(
                "UnresolvedPredicate{path=price, op=BETWEEN, insensitive=false, likeMode=EXACT}",
                PropPredicate.of(
                        new Context(),
                        false,
                        false,
                        new Source("PriceBetween"),
                        ImmutableType.get(Book.class)
                ).toString()
        );
    }

    @Test
    public void testNotBetween() {
        Assertions.assertEquals(
                "UnresolvedPredicate{path=price, op=NOT_BETWEEN, insensitive=false, likeMode=EXACT}",
                PropPredicate.of(
                        new Context(),
                        false,
                        false,
                        new Source("PriceNotBetween"),
                        ImmutableType.get(Book.class)
                ).toString()
        );
    }

    @Test
    public void testIsNull() {
        Assertions.assertEquals(
                "UnresolvedPredicate{path=store, op=NULL, insensitive=false, likeMode=EXACT}",
                PropPredicate.of(
                        new Context(),
                        false,
                        false,
                        new Source("StoreIsNull"),
                        ImmutableType.get(Book.class)
                ).toString()
        );
    }

    @Test
    public void testNotNull() {
        Assertions.assertEquals(
                "UnresolvedPredicate{path=store, op=NOT_NULL, insensitive=false, likeMode=EXACT}",
                PropPredicate.of(
                        new Context(),
                        false,
                        false,
                        new Source("StoreIsNotNull"),
                        ImmutableType.get(Book.class)
                ).toString()
        );
    }

    @Test
    public void testIn() {
        Assertions.assertEquals(
                "UnresolvedPredicate{path=store.id, op=IN, insensitive=false, likeMode=EXACT}",
                PropPredicate.of(
                        new Context(),
                        false,
                        false,
                        new Source("StoreIdIn"),
                        ImmutableType.get(Book.class)
                ).toString()
        );
    }

    @Test
    public void testNotIn() {
        Assertions.assertEquals(
                "UnresolvedPredicate{path=store.id, op=NOT_IN, insensitive=false, likeMode=EXACT}",
                PropPredicate.of(
                        new Context(),
                        false,
                        false,
                        new Source("StoreIdNotIn"),
                        ImmutableType.get(Book.class)
                ).toString()
        );
    }

    @Test
    public void testLike() {
        Assertions.assertEquals(
                "UnresolvedPredicate{path=name, op=LIKE, insensitive=false, likeMode=ANYWHERE}",
                PropPredicate.of(
                        new Context(),
                        false,
                        false,
                        new Source("NameLike"),
                        ImmutableType.get(Book.class)
                ).toString()
        );
    }

    @Test
    public void testNotLike() {
        Assertions.assertEquals(
                "UnresolvedPredicate{path=name, op=NOT_LIKE, insensitive=false, likeMode=ANYWHERE}",
                PropPredicate.of(
                        new Context(),
                        false,
                        false,
                        new Source("NameNotLike"),
                        ImmutableType.get(Book.class)
                ).toString()
        );
    }

    @Test
    public void testContains() {
        Assertions.assertEquals(
                "UnresolvedPredicate{path=name, op=LIKE, insensitive=false, likeMode=ANYWHERE}",
                PropPredicate.of(
                        new Context(),
                        false,
                        false,
                        new Source("NameContains"),
                        ImmutableType.get(Book.class)
                ).toString()
        );
    }

    @Test
    public void testStart() {
        Assertions.assertEquals(
                "UnresolvedPredicate{path=name, op=LIKE, insensitive=false, likeMode=START}",
                PropPredicate.of(
                        new Context(),
                        false,
                        false,
                        new Source("NameStartsWith"),
                        ImmutableType.get(Book.class)
                ).toString()
        );
    }

    @Test
    public void testEnd() {
        Assertions.assertEquals(
                "UnresolvedPredicate{path=name, op=LIKE, insensitive=false, likeMode=END}",
                PropPredicate.of(
                        new Context(),
                        false,
                        false,
                        new Source("NameEndsWith"),
                        ImmutableType.get(Book.class)
                ).toString()
        );
    }
}
