package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.fetcher.impl.FetcherFactory;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FactoryTest {

    private static final Fetcher<BookStore> BASE_FETCHER =
            BookStoreFetcher.$
                    .allScalarFields()
                    .avgPrice()
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

    @Test
    public void testFilterType() {
        Fetcher<BookStore> filtered = FetcherFactory.filter(
                BASE_FETCHER,
                (type, path) -> type.getJavaClass() != Author.class,
                null
        );
        Assertions.assertEquals(
                "org.babyfish.jimmer.sql.model.BookStore {\n" +
                        "    id\n" +
                        "    name\n" +
                        "    website\n" +
                        "    version\n" +
                        "    avgPrice\n" +
                        "    books {\n" +
                        "        id\n" +
                        "        name\n" +
                        "        edition\n" +
                        "        price\n" +
                        "    }\n" +
                        "    newestBooks {\n" +
                        "        id\n" +
                        "        name\n" +
                        "        edition\n" +
                        "        price\n" +
                        "    }\n" +
                        "}",
                filtered.toString(true)
        );
    }

    @Test
    public void testFilterProp() {
        Fetcher<BookStore> filtered = FetcherFactory.filter(
                BASE_FETCHER,
                null,
                (prop, path) -> !prop.getName().equals("name")
        );
        Assertions.assertEquals(
                "org.babyfish.jimmer.sql.model.BookStore {\n" +
                        "    id\n" +
                        "    website\n" +
                        "    version\n" +
                        "    avgPrice\n" +
                        "    books {\n" +
                        "        id\n" +
                        "        edition\n" +
                        "        price\n" +
                        "        authors {\n" +
                        "            id\n" +
                        "            firstName\n" +
                        "            lastName\n" +
                        "            gender\n" +
                        "        }\n" +
                        "    }\n" +
                        "    newestBooks {\n" +
                        "        id\n" +
                        "        edition\n" +
                        "        price\n" +
                        "        authors {\n" +
                        "            id\n" +
                        "            firstName\n" +
                        "            lastName\n" +
                        "            gender\n" +
                        "        }\n" +
                        "    }\n" +
                        "}",
                filtered.toString(true)
        );
    }

    @Test
    public void testFilterPropExceptRoot() {
        Fetcher<BookStore> filtered = FetcherFactory.filter(
                BASE_FETCHER,
                null,
                (prop, path) -> !prop.getName().equals("name") || path.isEmpty()
        );
        Assertions.assertEquals(
                "org.babyfish.jimmer.sql.model.BookStore {\n" +
                        "    id\n" +
                        "    name\n" +
                        "    website\n" +
                        "    version\n" +
                        "    avgPrice\n" +
                        "    books {\n" +
                        "        id\n" +
                        "        edition\n" +
                        "        price\n" +
                        "        authors {\n" +
                        "            id\n" +
                        "            firstName\n" +
                        "            lastName\n" +
                        "            gender\n" +
                        "        }\n" +
                        "    }\n" +
                        "    newestBooks {\n" +
                        "        id\n" +
                        "        edition\n" +
                        "        price\n" +
                        "        authors {\n" +
                        "            id\n" +
                        "            firstName\n" +
                        "            lastName\n" +
                        "            gender\n" +
                        "        }\n" +
                        "    }\n" +
                        "}",
                filtered.toString(true)
        );
    }
}
