package org.babyfish.jimmer;

import org.babyfish.jimmer.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
                        "the following non-static methods cannot be found in static type " +
                        "\"org.babyfish.jimmer.ConvertTest$BookInput\": getStore(), store()",
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
                        "\"public java.lang.String org.babyfish.jimmer.ConvertTest$BookInput.getStoreName()\" " +
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
                        "\"public java.util.List org.babyfish.jimmer.ConvertTest$BookInput.getAuthorNames()\" " +
                        "without value converter, the list element type of jimmer property is " +
                        "\"org.babyfish.jimmer.model.Author\" but the list element of return type of " +
                        "the method of static type is \"class java.lang.String\"",
                ex.getMessage()
        );
    }

    @Test
    public void testFullConverter() {
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ImmutableConverter
                    .newBuilder(Book.class, Partial.class)
                    .autoMapOtherScalars()
                    .build();
        });
        Assertions.assertEquals(
                "Cannot automatically map the property " +
                        "\"org.babyfish.jimmer.model.Book.price\", " +
                        "the following non-static methods cannot be found in static type " +
                        "\"org.babyfish.jimmer.ConvertTest$Partial\": getPrice(), price()",
                ex.getMessage()
        );
    }

    @Test
    public void testPartialConverter() {
        Book book = ImmutableConverter
                .newBuilder(Book.class, Partial.class)
                .autoMapOtherScalars(true)
                .build()
                .convert(new Partial("SQL in Action"));
        Assertions.assertEquals(
                "{\"name\":\"SQL in Action\"}",
                book.toString()
        );
    }

    @Test
    public void testCondConverter() {
        Book book = ImmutableConverter
                .newBuilder(Book.class, Partial.class)
                .mapIf(input -> input.getName() != null, BookProps.NAME)
                .build()
                .convert(new Partial(null));
        Assertions.assertEquals(
                "{}",
                book.toString()
        );
    }

    @Test
    public void testDefaultValue() {
        Book book = ImmutableConverter
                .newBuilder(Book.class, Partial.class)
                .map(BookProps.NAME, new ImmutableConverter.ValueConverter() {
                    @Override
                    public Object convert(Object value) {
                        return value;
                    }
                    @Override
                    public Object defaultValue() {
                        return "<NO-NAME>";
                    }
                })
                .build()
                .convert(new Partial(null));
        Assertions.assertEquals(
                "{\"name\":\"<NO-NAME>\"}",
                book.toString()
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
                            } else {
                                bookDraft.setStore((BookStore) null);
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

    public static class BookInput {

        private String name;

        private int price;

        private String storeName;

        private List<String> authorNames;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getPrice() {
            return price;
        }

        public void setPrice(int price) {
            this.price = price;
        }

        public String getStoreName() {
            return storeName;
        }

        public void setStoreName(String storeName) {
            this.storeName = storeName;
        }

        public List<String> getAuthorNames() {
            return authorNames;
        }

        public void setAuthorNames(List<String> authorNames) {
            this.authorNames = authorNames;
        }
    }

    public static class Partial {

        private final String name;

        public Partial(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
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
