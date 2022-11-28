package org.babyfish.jimmer;

import org.babyfish.jimmer.input.BookInput;
import org.babyfish.jimmer.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class ConvertTest {

    private static final BookInput INPUT;

    @Test
    public void testIllegalPropName() {
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ImmutableConverter
                    .newBuilder(Book.class, BookInput.class)
                    .map(BookProps.STORE);
        });
        Assertions.assertEquals(
                "Illegal static property name: \"store\", " +
                        "the following methods cannot be found in static type " +
                        "\"org.babyfish.jimmer.input.BookInput\": getStore(), store()",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalType() {
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ImmutableConverter
                    .newBuilder(Book.class, BookInput.class)
                    .map(BookProps.STORE, "storeName");
        });
        Assertions.assertEquals(
                "Cannot map \"org.babyfish.jimmer.model.Book.store\" to " +
                        "\"public java.lang.String org.babyfish.jimmer.input.BookInput.getStoreName()\" " +
                        "without value converter, the return type of jimmer property is " +
                        "\"org.babyfish.jimmer.model.BookStore\" but the return type of the method " +
                        "of static type is \"java.lang.String\"",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalGenericType() {
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ImmutableConverter
                    .newBuilder(Book.class, BookInput.class)
                    .map(BookProps.AUTHORS, "authorNames");
        });
        Assertions.assertEquals(
                "Cannot map \"org.babyfish.jimmer.model.Book.authors\" to " +
                        "\"public java.util.List org.babyfish.jimmer.input.BookInput.getAuthorNames()\" " +
                        "without value converter, the list element type of jimmer property is " +
                        "\"org.babyfish.jimmer.model.Author\" but the list element of return type of " +
                        "the method of static type is \"class java.lang.String\"",
                ex.getMessage()
        );
    }

    @Test
    public void testEmptyConverter() {
        ImmutableConverter<Book, BookInput> converter =
                ImmutableConverter
                        .newBuilder(Book.class, BookInput.class)
                        .build();
        Assertions.assertEquals(
                "{}",
                converter.convert(INPUT).toString()
        );
    }

    @Test
    public void testDefaultConverter() {
        ImmutableConverter<Book, BookInput> converter =
                ImmutableConverter
                        .newBuilder(Book.class, BookInput.class)
                        .autoMapOtherScalars()
                        .build();
        Assertions.assertEquals(
                "{\"name\":\"SQL in Action\",\"price\":49}",
                converter.convert(INPUT).toString()
        );
    }

    @Test
    public void testValueConverter() {
        ImmutableConverter<Book, BookInput> converter =
                ImmutableConverter
                        .newBuilder(Book.class, BookInput.class)
                        .map(BookProps.STORE, "storeName", value ->
                                BookStoreDraft.$.produce(it -> it.setName((String) value))
                        )
                        .mapList(BookProps.AUTHORS, "authorNames", value ->
                                AuthorDraft.$.produce(it -> it.setName((String)value))
                        )
                        .autoMapOtherScalars()
                        .build();
        Assertions.assertEquals(
                "{" +
                        "\"name\":\"SQL in Action\"," +
                        "\"store\":{\"name\":\"MANNING\"}," +
                        "\"price\":49," +
                        "\"authors\":[{\"name\":\"Scott\"},{\"name\":\"Linda\"}]}",
                converter.convert(INPUT).toString()
        );
    }

    @Test
    public void testDraftModifier() {
        ImmutableConverter<Book, BookInput> converter =
                ImmutableConverter
                        .newBuilder(Book.class, BookInput.class)
                        .autoMapOtherScalars()
                        .setDraftModifier((draft, input) -> {
                            BookDraft bookDraft = (BookDraft) draft;
                            if (input.getStoreName() != null) {
                                bookDraft.setStore(store ->
                                        store.setName(input.getStoreName())
                                );
                            }
                            for (String authorName : input.getAuthorNames()) {
                                bookDraft.addIntoAuthors(
                                        author -> author.setName(authorName)
                                );
                            }
                        })
                        .build();
        Assertions.assertEquals(
                "{" +
                        "\"name\":\"SQL in Action\"," +
                        "\"store\":{\"name\":\"MANNING\"}," +
                        "\"price\":49," +
                        "\"authors\":[{\"name\":\"Scott\"},{\"name\":\"Linda\"}]}",
                converter.convert(INPUT).toString()
        );
    }

    static {
        BookInput input = new BookInput();
        input.setName("SQL in Action");
        input.setPrice(49);
        input.setStoreName("MANNING");
        input.setAuthorNames(Arrays.asList("Scott", "Linda"));
        INPUT = input;
    }
}
