package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.fetcher.compiler.FetcherCompileException;
import org.babyfish.jimmer.sql.fetcher.compiler.FetcherCompiler;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CompilerTest {

    @Test
    public void testDeep() {
        BookStoreFetcher fetcher = BookStoreFetcher.$
                .allScalarFields()
                .avgPrice()
                .mostPopularAuthor(
                        AuthorFetcher.$
                                .allScalarFields()
                )
                .books(
                        BookFetcher.$
                                .allScalarFields()
                                .authors(
                                        AuthorFetcher.$
                                                .allScalarFields()
                                )
                )
                .newestBooks(
                        BookFetcher.$
                                .allScalarFields()
                                .authors(
                                        AuthorFetcher.$
                                                .allScalarFields()
                                )
                );

        Fetcher<?> fetcher2 = FetcherCompiler.compile(fetcher.toString(true));
        Assertions.assertEquals(
                fetcher.toString(true),
                fetcher2.toString(true)
        );
    }

    @Test
    public void testWithLimitOffset() {
        BookFetcher fetcher = BookFetcher.$
                .allScalarFields()
                .authors(
                        AuthorFetcher.$
                                .allScalarFields(),
                        cfg -> cfg.batch(1).limit(10, 20)
                );
        Fetcher<?> fetcher2 = FetcherCompiler.compile(fetcher.toString(true));
        Assertions.assertEquals(
                fetcher.toString(true),
                fetcher2.toString(true)
        );
    }

    @Test
    public void testWithDepth() {
        TreeNodeFetcher fetcher = TreeNodeFetcher.$
                .allScalarFields()
                .childNodes(
                        TreeNodeFetcher.$
                                .allScalarFields(),
                        cfg -> cfg.depth(4)
                );
        Fetcher<?> fetcher2 = FetcherCompiler.compile(fetcher.toString(true));
        Assertions.assertEquals(
                fetcher.toString(true),
                fetcher2.toString(true)
        );
    }

    @Test
    public void testWithRecursive() {
        TreeNodeFetcher fetcher = TreeNodeFetcher.$
                .allScalarFields()
                .childNodes(
                        TreeNodeFetcher.$
                                .allScalarFields(),
                        RecursiveListFieldConfig::recursive
                );
        Fetcher<?> fetcher2 = FetcherCompiler.compile(fetcher.toString(true));
        Assertions.assertEquals(
                fetcher.toString(true),
                fetcher2.toString(true)
        );
    }

    @Test
    public void testWithFilter() {
        TreeNodeFetcher fetcher = TreeNodeFetcher.$
                .allScalarFields()
                .childNodes(
                        TreeNodeFetcher.$
                                .allScalarFields(),
                        cfg -> cfg.filter(args -> args.where(args.getTable().name().ne("")))
                );
        Assertions.assertThrows(FetcherCompileException.CodeBasedFilterException.class, () -> {
            FetcherCompiler.compile(fetcher.toString(true));
        });
    }

    @Test
    public void testWithRecursiveCode() {
        TreeNodeFetcher fetcher = TreeNodeFetcher.$
                .allScalarFields()
                .childNodes(
                        TreeNodeFetcher.$
                                .allScalarFields(),
                        cfg -> cfg.recursive(args ->
                            !args.getEntity().name().equals("X")
                        )
                );
        Assertions.assertThrows(FetcherCompileException.CodeBasedRecursionException.class, () -> {
            FetcherCompiler.compile(fetcher.toString(true));
        });
    }

    @Test
    public void testIllegalSyntax() {
        RuntimeException ex = Assertions.assertThrows(FetcherCompileException.class, () -> {
            FetcherCompiler.compile(
                    "EntityType { \n" +
                            "a, \n" +
                            "b, \n" +
                            ", c\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "Cannot compile fetcher(line: 4, position: 0): extraneous input ',' expecting {'}', Identifier}",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalSyntaxAtTail() {
        RuntimeException ex = Assertions.assertThrows(FetcherCompileException.class, () -> {
            FetcherCompiler.compile(
                    "EntityType { \n" +
                            "a, \n" +
                            "b, \n" +
                            "c\n" +
                            "},"
            );
        });
        Assertions.assertEquals(
                "Cannot compile fetcher(line: 5, position: 1): extraneous input ',' expecting <EOF>",
                ex.getMessage()
        );
    }
}
