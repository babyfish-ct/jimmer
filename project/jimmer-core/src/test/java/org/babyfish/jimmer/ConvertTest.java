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
                    .forMethods(Book.class, BookInput.class)
                    .map(BookProps.STORE);
        });
        Assertions.assertEquals(
                "Illegal static property name \"store\", " +
                        "available choices are [authorNames, name, price, storeName]",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalType() {
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ImmutableConverter
                    .forMethods(Book.class, BookInput.class)
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
                    .forMethods(Book.class, BookInput.class)
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
    public void testPartialConverter() {
        Book book = ImmutableConverter
                .forFields(Book.class, Partial.class)
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
                .forFields(Book.class, Partial.class)
                .map(BookProps.NAME, mapping ->
                    mapping.useIf(input -> input.name != null)
                )
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
                .forFields(Book.class, Partial.class)
                .map(BookProps.NAME, mapping ->
                        mapping.defaultValue("<NO-NAME>")
                )
                .build()
                .convert(new Partial(null));
        Assertions.assertEquals(
                "{\"name\":\"<NO-NAME>\"}",
                book.toString()
        );
    }

    @Test
    public void testDefaultConverter() {
        ImmutableConverter<Book, BookInput> converter =
                ImmutableConverter
                        .forMethods(Book.class, BookInput.class)
                        .unmapStaticProps("storeName", "authorNames")
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
                        .forMethods(Book.class, BookInput.class)
                        .map(BookProps.STORE, "storeName", mapping ->
                                mapping.valueConverter(value ->
                                        BookStoreDraft.$.produce(it -> it.setName((String)value))
                                )
                        )
                        .mapList(BookProps.AUTHORS, "authorNames", mapping ->
                                mapping.elementConverter(value ->
                                        AuthorDraft.$.produce(it -> it.setName((String)value))
                                )
                        )
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
                        .forMethods(Book.class, BookInput.class)
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
                        .unmapStaticProps("storeName", "authorNames")
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

        private final String name;

        private final Integer price;

        private final String storeName;

        private final List<String> authorNames;

        public BookInput(
                String name,
                Integer price,
                String storeName,
                List<String> authorNames
        ) {
            this.name = name;
            this.price = price;
            this.storeName = storeName;
            this.authorNames = authorNames;
        }

        public String getName() {
            return name;
        }

        public Integer getPrice() {
            return price;
        }

        public String getStoreName() {
            return storeName;
        }

        public List<String> getAuthorNames() {
            return authorNames;
        }

        @Override
        public String toString() {
            return "BookInput{" +
                    "name='" + name + '\'' +
                    ", price=" + price +
                    ", storeName='" + storeName + '\'' +
                    ", authorNames=" + authorNames +
                    '}';
        }
    }

    public static class Partial {

        private final String name;

        public Partial(String name) {
            this.name = name;
        }
    }

    static {
        INPUT = new BookInput(
                "SQL in Action",
                49,
                "MANNING",
                Arrays.asList("Scott", "Linda")
        );
    }
}
