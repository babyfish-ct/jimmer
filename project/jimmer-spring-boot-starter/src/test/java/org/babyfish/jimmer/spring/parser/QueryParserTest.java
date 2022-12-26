package org.babyfish.jimmer.spring.parser;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.spring.java.model.Book;
import org.babyfish.jimmer.spring.repository.parser.Context;
import org.babyfish.jimmer.spring.repository.parser.Query;
import org.babyfish.jimmer.spring.repository.parser.Source;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class QueryParserTest {

    @Test
    public void testSimplePredicate() {
        assertQuery(
                Query.of(
                        new Context(),
                        new Source("findByName"),
                        ImmutableType.get(Book.class)
                ),
                "Query{" +
                        "--->action=FIND, " +
                        "--->limit=2147483647, " +
                        "--->distinct=false, " +
                        "--->selectedPath=null, " +
                        "--->predicate=UnresolvedPredicate{" +
                        "--->--->path=name, " +
                        "--->--->op=EQ, " +
                        "--->--->insensitive=false, " +
                        "--->--->likeMode=EXACT" +
                        "--->}, " +
                        "--->orders=[]" +
                        "}"
        );
    }

    @Test
    public void testSimpleOrder() {
        assertQuery(
                Query.of(
                        new Context(),
                        new Source("findOrderByName"),
                        ImmutableType.get(Book.class)
                ),
                "Query{" +
                        "--->action=FIND, " +
                        "--->limit=2147483647, " +
                        "--->distinct=false, " +
                        "--->selectedPath=null, " +
                        "--->predicate=null, " +
                        "--->orders=[" +
                        "--->--->Order{path=name, orderMode=ASC}" +
                        "--->]" +
                        "}"
        );
    }

    @Test
    public void testSelectStoreName() {
        assertQuery(
                Query.of(
                        new Context(),
                        new Source("findStoreNameByName"),
                        ImmutableType.get(Book.class)
                ),
                "Query{" +
                        "--->action=FIND, " +
                        "--->limit=2147483647, " +
                        "--->distinct=false, " +
                        "--->selectedPath=store.name, " +
                        "--->predicate=UnresolvedPredicate{path=name, op=EQ, insensitive=false, likeMode=EXACT}, " +
                        "--->orders=[]" +
                        "}"
        );
    }

    @Test
    public void testSelectDistinctStoreName() {
        assertQuery(
                Query.of(
                        new Context(),
                        new Source("findDistinctStoreNameByName"),
                        ImmutableType.get(Book.class)
                ),
                "Query{" +
                        "--->action=FIND, " +
                        "--->limit=2147483647, " +
                        "--->distinct=true, " +
                        "--->selectedPath=store.name, " +
                        "--->predicate=UnresolvedPredicate{path=name, op=EQ, insensitive=false, likeMode=EXACT}, " +
                        "--->orders=[]" +
                        "}"
        );
    }

    @Test
    public void testSelectStoreNameDistinct() {
        assertQuery(
                Query.of(
                        new Context(),
                        new Source("findStoreNameDistinctByName"),
                        ImmutableType.get(Book.class)
                ),
                "Query{" +
                        "--->action=FIND, " +
                        "--->limit=2147483647, " +
                        "--->distinct=true, " +
                        "--->selectedPath=store.name, " +
                        "--->predicate=UnresolvedPredicate{path=name, op=EQ, insensitive=false, likeMode=EXACT}, " +
                        "--->orders=[]" +
                        "}"
        );
    }

    @Test
    public void testSelectDistinctStoreNameTop10() {
        assertQuery(
                Query.of(
                        new Context(),
                        new Source("findDistinctStoreNameTop10ByName"),
                        ImmutableType.get(Book.class)
                ),
                "Query{" +
                        "--->action=FIND, " +
                        "--->limit=10, " +
                        "--->distinct=true, " +
                        "--->selectedPath=store.name, " +
                        "--->predicate=UnresolvedPredicate{path=name, op=EQ, insensitive=false, likeMode=EXACT}, " +
                        "--->orders=[]" +
                        "}"
        );
    }

    @Test
    public void testSelectStoreNameDistinctTop10() {
        assertQuery(
                Query.of(
                        new Context(),
                        new Source("findStoreNameDistinctTop10ByName"),
                        ImmutableType.get(Book.class)
                ),
                "Query{" +
                        "--->action=FIND, " +
                        "--->limit=10, " +
                        "--->distinct=true, " +
                        "--->selectedPath=store.name, " +
                        "--->predicate=UnresolvedPredicate{path=name, op=EQ, insensitive=false, likeMode=EXACT}, " +
                        "--->orders=[]" +
                        "}"
        );
    }

    @Test
    public void testSelectStoreNameTop10Distinct() {
        assertQuery(
                Query.of(
                        new Context(),
                        new Source("findStoreNameTop10DistinctByName"),
                        ImmutableType.get(Book.class)
                ),
                "Query{" +
                        "--->action=FIND, " +
                        "--->limit=10, " +
                        "--->distinct=true, " +
                        "--->selectedPath=store.name, " +
                        "--->predicate=UnresolvedPredicate{path=name, op=EQ, insensitive=false, likeMode=EXACT}, " +
                        "--->orders=[]" +
                        "}"
        );
    }

    @Test
    public void testSelectTop10StoreNameDistinct() {
        assertQuery(
                Query.of(
                        new Context(),
                        new Source("findTop10StoreNameDistinctByName"),
                        ImmutableType.get(Book.class)
                ),
                "Query{" +
                        "--->action=FIND, " +
                        "--->limit=10, " +
                        "--->distinct=true, " +
                        "--->selectedPath=store.name, " +
                        "--->predicate=UnresolvedPredicate{path=name, op=EQ, insensitive=false, likeMode=EXACT}, " +
                        "--->orders=[]" +
                        "}"
        );
    }

    @Test
    public void testSelectTop10DistinctStoreName() {
        assertQuery(
                Query.of(
                        new Context(),
                        new Source("findTop10DistinctStoreNameByName"),
                        ImmutableType.get(Book.class)
                ),
                "Query{" +
                        "--->action=FIND, " +
                        "--->limit=10, " +
                        "--->distinct=true, " +
                        "--->selectedPath=store.name, " +
                        "--->predicate=UnresolvedPredicate{path=name, op=EQ, insensitive=false, likeMode=EXACT}, " +
                        "--->orders=[]" +
                        "}"
        );
    }

    @Test
    public void testSelectDistinctTop10StoreName() {
        assertQuery(
                Query.of(
                        new Context(),
                        new Source("findDistinctTop10StoreNameByName"),
                        ImmutableType.get(Book.class)
                ),
                "Query{" +
                        "--->action=FIND, " +
                        "--->limit=10, " +
                        "--->distinct=true, " +
                        "--->selectedPath=store.name, " +
                        "--->predicate=UnresolvedPredicate{path=name, op=EQ, insensitive=false, likeMode=EXACT}, " +
                        "--->orders=[]" +
                        "}"
        );
    }

    @Test
    public void testComplexPredicates() {
        assertQuery(
                Query.of(
                        new Context(),
                        new Source("findByNameAndPriceBetweenOrStoreNameStartsWithAndStoreNameIgnoreCase"),
                        ImmutableType.get(Book.class)
                ),
                "" +
                        "Query{" +
                        "--->action=FIND, " +
                        "--->limit=2147483647, " +
                        "--->distinct=false, " +
                        "--->selectedPath=null, " +
                        "--->predicate=OrPredicate{" +
                        "--->--->predicates=[" +
                        "--->--->--->AndPredicate{" +
                        "--->--->--->--->predicates=[" +
                        "--->--->--->--->--->UnresolvedPredicate{path=name, op=EQ, insensitive=false, likeMode=EXACT}, " +
                        "--->--->--->--->--->UnresolvedPredicate{path=price, op=BETWEEN, insensitive=false, likeMode=EXACT}" +
                        "--->--->--->--->]" +
                        "--->--->--->}, " +
                        "--->--->--->AndPredicate{" +
                        "--->--->--->--->predicates=[" +
                        "--->--->--->--->--->UnresolvedPredicate{path=store.name, op=LIKE, insensitive=false, likeMode=START}, " +
                        "--->--->--->--->--->UnresolvedPredicate{path=store.name, op=EQ, insensitive=true, likeMode=EXACT}" +
                        "--->--->--->--->]" +
                        "--->--->--->}" +
                        "--->--->]" +
                        "--->}, " +
                        "--->orders=[]" +
                        "}"
        );
    }

    @Test
    public void testComplexQuery() {
        assertQuery(
                Query.of(
                        new Context(),
                        new Source("findTop10ByNameContainsAndStoreNameStartsWithOrderByNameDescStoreNameAsc"),
                        ImmutableType.get(Book.class)
                ),
                "Query{" +
                        "--->action=FIND, " +
                        "--->limit=10, " +
                        "--->distinct=false, " +
                        "--->selectedPath=null, " +
                        "--->predicate=AndPredicate{" +
                        "--->--->predicates=[" +
                        "--->--->--->UnresolvedPredicate{path=name, op=LIKE, insensitive=false, likeMode=ANYWHERE}, " +
                        "--->--->--->UnresolvedPredicate{path=store.name, op=LIKE, insensitive=false, likeMode=START}" +
                        "--->--->]" +
                        "--->}, " +
                        "--->orders=[" +
                        "--->--->Order{path=name, orderMode=DESC}, " +
                        "--->--->Order{path=store.name, orderMode=ASC}" +
                        "--->]" +
                        "}"
        );
    }

    @Test
    public void testIllegalDelete() {
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Query.of(new Context(), new Source("deleteXByName"), ImmutableType.get(Book.class));
        });
        Assertions.assertEquals(
                "Illegal method name \"delete[X]ByName\"",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalDelete2() {
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Query.of(new Context(), new Source("deleteByNameOrderByName"), ImmutableType.get(Book.class));
        });
        Assertions.assertEquals(
                "Illegal method name \"deleteByName[OrderByName]\"",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalAnd() {
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Query.of(new Context(), new Source("findByAndName"), ImmutableType.get(Book.class));
        });
        Assertions.assertEquals(
                "Cannot resolve the property name \"findBy[AndName]\" " +
                        "by \"org.babyfish.jimmer.spring.java.model.Book\"",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalAnd2() {
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Query.of(new Context(), new Source("findByNameAnd"), ImmutableType.get(Book.class));
        });
        Assertions.assertEquals(
                "Cannot resolve the property name \"findBy[NameAnd]\" " +
                        "by \"org.babyfish.jimmer.spring.java.model.Book\"",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalOr() {
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Query.of(new Context(), new Source("findByOrName"), ImmutableType.get(Book.class));
        });
        Assertions.assertEquals(
                "Cannot resolve the property name \"findBy[OrName]\" " +
                        "by \"org.babyfish.jimmer.spring.java.model.Book\"",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalOr2() {
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Query.of(new Context(), new Source("findByNameOr"), ImmutableType.get(Book.class));
        });
        Assertions.assertEquals(
                "Cannot resolve the property name \"findBy[NameOr]\" " +
                        "by \"org.babyfish.jimmer.spring.java.model.Book\"",
                ex.getMessage()
        );
    }

    @Test
    public void testJoinListFailed() {
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Query.of(new Context(), new Source("findByAuthorsName"), ImmutableType.get(Book.class));
        });
        Assertions.assertEquals(
                "Cannot resolve the property name \"findBy[AuthorsName]\" " +
                        "by \"org.babyfish.jimmer.spring.java.model.Book\"",
                ex.getMessage()
        );
    }

    @Test
    public void testJoinList() {
        assertQuery(
                Query.of(
                        new Context(),
                        new Source("findDistinctIdByAuthorsFirstName"),
                        ImmutableType.get(Book.class)
                ),
                "Query{" +
                        "--->action=FIND, " +
                        "--->limit=2147483647, " +
                        "--->distinct=true, " +
                        "--->selectedPath=id, " +
                        "--->predicate=UnresolvedPredicate{" +
                        "--->--->path=authors.firstName, " +
                        "--->--->op=EQ, " +
                        "--->--->insensitive=false, " +
                        "--->--->likeMode=EXACT" +
                        "--->}, " +
                        "--->orders=[]" +
                        "}"
        );
    }

    private static void assertQuery(Query query, String expectedText) {
        Assertions.assertEquals(
                expectedText
                        .replace("\r", "")
                        .replace("\n", "")
                        .replace("--->", ""),
                query.toString()
        );
    }
}
