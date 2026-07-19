package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.View;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.fetcher.DtoMetadata;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookFetcher;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.model.dto.BookViewForTupleTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class ReaderCacheTest {

    @Test
    public void testViewReaderIsCachedByClientAndViewType() {
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) JSqlClient.newBuilder().build();
        DtoMetadata<Book, BookViewForTupleTest> metadata = DtoMetadata.of(BookViewForTupleTest.class);
        Selection<?> selection = BookTable.$.fetch(BookViewForTupleTest.class);

        Reader<?> reader = sqlClient.getViewReader(metadata);

        Assertions.assertSame(
                reader,
                sqlClient.getViewReader(metadata)
        );
        Assertions.assertSame(
                reader,
                Readers.createReader(sqlClient, Collections.singletonList(selection), null)
        );
        Assertions.assertNotSame(
                reader,
                Readers.createReader(
                        (JSqlClientImplementor) JSqlClient.newBuilder().build(),
                        Collections.singletonList(selection),
                        null
                )
        );
    }

    @Test
    public void testGeneratedMetadataContainsDtoType() {
        Assertions.assertSame(
                BookViewForTupleTest.class,
                BookViewForTupleTest.METADATA.getDtoType()
        );
    }

    @Test
    public void testMetadataCreatedByOldGeneratorIsResolvedByDtoType() {
        Assertions.assertNull(LegacyBookView.METADATA.getDtoType());

        DtoMetadata<Book, LegacyBookView> metadata = DtoMetadata.of(LegacyBookView.class);

        Assertions.assertNotSame(LegacyBookView.METADATA, metadata);
        Assertions.assertSame(LegacyBookView.class, metadata.getDtoType());
    }

    @Test
    public void testDynamicFetcherReaderIsNotCached() {
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) JSqlClient.newBuilder().build();
        Selection<?> selection = BookTable.$.fetch(BookFetcher.$.name());

        Assertions.assertNotSame(
                Readers.createReader(sqlClient, Collections.singletonList(selection), null),
                Readers.createReader(sqlClient, Collections.singletonList(selection), null)
        );
    }

    public static class LegacyBookView implements View<Book> {

        public static final DtoMetadata<Book, LegacyBookView> METADATA =
                new DtoMetadata<>(BookFetcher.$.name(), LegacyBookView::new);

        private final Book book;

        public LegacyBookView(Book book) {
            this.book = book;
        }

        @Override
        public Book toEntity() {
            return book;
        }
    }
}
