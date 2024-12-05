package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.model.BookStore;
import org.babyfish.jimmer.sql.model.BookStoreDraft;
import org.babyfish.jimmer.sql.model.dto.BookStoreView;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.babyfish.jimmer.sql.common.Tests.assertContentEquals;

public class BookStoreViewTest {
    @Test
    public void testDtoToEntity() {
        final UUID uuid = UUID.randomUUID();
        BookStoreView view = new BookStoreView();
        view.setAvgPrice(BigDecimal.ONE);
        final BookStoreView.TargetOf_newestBooks newestBooks = new BookStoreView.TargetOf_newestBooks();
        newestBooks.setName("book1");
        newestBooks.setEdition(1);
        view.setNewestBooks(Collections.singletonList(newestBooks));
        view.setNewestBookIds(Collections.singletonList(uuid));
        assertContentEquals(
                "{" +
                        "--->\"avgPrice\":1," +
                        "--->\"newestBooks\":[" +
                        "--->--->{" +
                        "--->--->--->\"name\":\"book1\"," +
                        "--->--->--->\"edition\":1" +
                        "--->--->}" +
                        "--->]," +
                        "--->\"newestBookIds\":[" +
                        "--->--->\"" + uuid + "\"" +
                        "--->]" +
                        "}",
                view.toEntity()
        );
    }


    @Test
    public void testEntityToDto() {
        UUID uuid = UUID.randomUUID();
        BookStore entity = BookStoreDraft.$.produce(draft -> {
            draft.setAvgPrice(BigDecimal.ONE);
            draft.addIntoNewestBooks(book -> {
                book.setName("book1");
                book.setEdition(1);
            });
            draft.setNewestBookIds(Collections.singletonList(uuid));
        });

        BookStoreView view = new BookStoreView(entity);
        assertContentEquals(
                "BookStoreView(" +
                        "--->avgPrice=1, " +
                        "--->newestBooks=[" +
                        "--->--->BookStoreView.TargetOf_newestBooks(" +
                        "--->--->--->name=book1, " +
                        "--->--->--->edition=1" +
                        "--->--->)" +
                        "--->], " +
                        "--->newestBookIds=[" +
                        "--->--->" + uuid +
                        "--->]" +
                        ")",
                view
        );
    }
}
