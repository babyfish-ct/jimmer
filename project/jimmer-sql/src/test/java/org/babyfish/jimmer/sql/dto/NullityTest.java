package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookStore;
import org.babyfish.jimmer.sql.model.dto.BookNullableIdInput;
import org.babyfish.jimmer.sql.model.dto.BookNullableInput;
import org.babyfish.jimmer.sql.model.dto.BookStoreNullableIdInput;
import org.babyfish.jimmer.sql.model.dto.BookStoreNullableInput;
import org.babyfish.jimmer.sql.model.enumeration.Article;
import org.babyfish.jimmer.sql.model.enumeration.Gender;
import org.babyfish.jimmer.sql.model.enumeration.dto.ArticleNullableIdInput;
import org.babyfish.jimmer.sql.model.enumeration.dto.ArticleNullableInput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;

public class NullityTest extends Tests {

    @Test
    public void testBookWithNonNullStore() {
        BookNullableInput input = new BookNullableInput();
        input.setName("SQL in Action");
        input.setEdition(1);
        input.setPrice(BigDecimal.TEN);

        BookNullableInput.TargetOf_store store = new BookNullableInput.TargetOf_store();
        store.setName("TURING");
        store.setVersion(1);
        input.setStore(store);

        Book entity = input.toEntity();
        assertContentEquals(
                "{" +
                        "--->\"name\":\"SQL in Action\"," +
                        "--->\"edition\":1," +
                        "--->\"price\":10," +
                        "--->\"store\":{" +
                        "--->--->\"name\":\"TURING\"," +
                        "--->--->\"website\":null," +
                        "--->--->\"version\":1" +
                        "--->}" +
                        "}",
                entity
        );
        assertContentEquals(
                "BookNullableInput(" +
                        "--->id=null, " +
                        "--->name=SQL in Action, " +
                        "--->edition=1, " +
                        "--->price=10, " +
                        "--->store=BookNullableInput.TargetOf_store(" +
                        "--->--->id=null, " +
                        "--->--->name=TURING, " +
                        "--->--->website=null, " +
                        "--->--->version=1" +
                        "--->)" +
                        ")",
                new BookNullableInput(entity)
        );
    }

    @Test
    public void testBookWithNullStore() {
        BookNullableInput input = new BookNullableInput();
        input.setName("SQL in Action");
        input.setEdition(1);
        input.setPrice(BigDecimal.TEN);

        Book entity = input.toEntity();
        assertContentEquals(
                "{" +
                        "--->\"name\":\"SQL in Action\"," +
                        "--->\"edition\":1," +
                        "--->\"price\":10," +
                        "--->\"store\":null" +
                        "}",
                entity
        );
        assertContentEquals(
                "BookNullableInput(" +
                        "--->id=null, " +
                        "--->name=SQL in Action, " +
                        "--->edition=1, " +
                        "--->price=10, " +
                        "--->store=null" +
                        ")",
                new BookNullableInput(entity)
        );
    }

    @Test
    public void testArticleWithNonNullWriter() {
        ArticleNullableInput input = new ArticleNullableInput();
        input.setId(1L);
        input.setName("Introduce Jimmer");

        ArticleNullableInput.TargetOf_writer writer = new ArticleNullableInput.TargetOf_writer();
        writer.setId(2L);
        writer.setName("Bob");
        writer.setGender(Gender.MALE);
        input.setWriter(writer);

        Article entity = input.toEntity();
        assertContentEquals(
                "{" +
                        "--->\"id\":1," +
                        "--->\"name\":\"Introduce Jimmer\"," +
                        "--->\"writer\":{" +
                        "--->--->\"id\":2," +
                        "--->--->\"name\":\"Bob\"," +
                        "--->--->\"gender\":\"MALE\"" +
                        "--->}" +
                        "}",
                entity
        );
        assertContentEquals(
                "ArticleNullableInput(" +
                        "--->id=1, " +
                        "--->name=Introduce Jimmer, " +
                        "--->writer=ArticleNullableInput.TargetOf_writer(" +
                        "--->--->id=2, " +
                        "--->--->name=Bob, " +
                        "--->--->gender=MALE" +
                        "--->)" +
                        ")",
                new ArticleNullableInput(entity)
        );
    }

    @Test
    public void testArticleWithNullWriter() {
        ArticleNullableInput input = new ArticleNullableInput();
        input.setId(1L);
        input.setName("Introduce Jimmer");

        Article entity = input.toEntity();
        assertContentEquals(
                "{\"id\":1,\"name\":\"Introduce Jimmer\"}",
                entity
        );
        assertContentEquals(
                "ArticleNullableInput(" +
                        "--->id=1, " +
                        "--->name=Introduce Jimmer, " +
                        "--->writer=null" +
                        ")",
                new ArticleNullableInput(entity)
        );
    }

    @Test
    public void testBookStoreWithNonNullBooks() {
        BookStoreNullableInput input = new BookStoreNullableInput();
        input.setName("TURING");
        input.setVersion(1);
        input.setBooks(Collections.emptyList());

        BookStore entity = input.toEntity();
        assertContentEquals(
                "{" +
                        "--->\"name\":\"TURING\"," +
                        "--->\"website\":null," +
                        "--->\"version\":1," +
                        "--->\"books\":[]" +
                        "}",
                entity
        );
        assertContentEquals(
                "BookStoreNullableInput(" +
                        "--->id=null, " +
                        "--->name=TURING, " +
                        "--->website=null, " +
                        "--->version=1, " +
                        "--->books=[]" +
                        ")",
                new BookStoreNullableInput(entity)
        );
    }

    @Test
    public void testBookStoreWithNullBooks() {
        BookStoreNullableInput input = new BookStoreNullableInput();
        input.setName("TURING");
        input.setVersion(1);

        BookStore entity = input.toEntity();
        assertContentEquals(
                "{" +
                        "--->\"name\":\"TURING\"," +
                        "--->\"website\":null," +
                        "--->\"version\":1," +
                        "--->\"books\":[]" +
                        "}",
                entity
        );
        assertContentEquals(
                "BookStoreNullableInput(" +
                        "--->id=null, " +
                        "--->name=TURING, " +
                        "--->website=null, " +
                        "--->version=1, " +
                        "--->books=[]" +
                        ")",
                new BookStoreNullableInput(entity)
        );
    }

    //////////////////////////////////////////////////

    @Test
    public void testBookWithNonNullStoreId() {
        BookNullableIdInput input = new BookNullableIdInput();
        input.setName("SQL in Action");
        input.setEdition(1);
        input.setPrice(BigDecimal.TEN);
        input.setStoreId(Constants.manningId);

        Book entity = input.toEntity();
        assertContentEquals(
                "{" +
                        "--->\"name\":\"SQL in Action\"," +
                        "--->\"edition\":1," +
                        "--->\"price\":10," +
                        "--->\"store\":{" +
                        "--->--->\"id\":\"" + Constants.manningId + "\"" +
                        "--->}" +
                        "}",
                entity
        );
        assertContentEquals(
                "BookNullableIdInput(" +
                        "--->id=null, " +
                        "--->name=SQL in Action, " +
                        "--->edition=1, " +
                        "--->price=10, " +
                        "--->storeId=" + Constants.manningId +
                        ")",
                new BookNullableIdInput(entity)
        );
    }

    @Test
    public void testBookWithNullStoreId() {
        BookNullableIdInput input = new BookNullableIdInput();
        input.setName("SQL in Action");
        input.setEdition(1);
        input.setPrice(BigDecimal.TEN);

        Book entity = input.toEntity();
        assertContentEquals(
                "{" +
                        "--->\"name\":\"SQL in Action\"," +
                        "--->\"edition\":1," +
                        "--->\"price\":10," +
                        "--->\"store\":null" +
                        "}",
                entity
        );
        assertContentEquals(
                "BookNullableIdInput(" +
                        "--->id=null, " +
                        "--->name=SQL in Action, " +
                        "--->edition=1, " +
                        "--->price=10, " +
                        "--->storeId=null" +
                        ")",
                new BookNullableIdInput(entity)
        );
    }

    @Test
    public void testArticleWithNonNullWriterId() {
        ArticleNullableIdInput input = new ArticleNullableIdInput();
        input.setId(1L);
        input.setName("Introduce Jimmer");
        input.setWriterId(2L);

        Article entity = input.toEntity();
        assertContentEquals(
                "{" +
                        "--->\"id\":1," +
                        "--->\"name\":\"Introduce Jimmer\"," +
                        "--->\"writer\":{" +
                        "--->--->\"id\":2" +
                        "--->}" +
                        "}",
                entity
        );
        assertContentEquals(
                "ArticleNullableIdInput(" +
                        "--->id=1, " +
                        "--->name=Introduce Jimmer, " +
                        "--->writerId=2" +
                        ")",
                new ArticleNullableIdInput(entity)
        );
    }

    @Test
    public void testArticleWithNullWriterId() {
        ArticleNullableInput input = new ArticleNullableInput();
        input.setId(1L);
        input.setName("Introduce Jimmer");

        Article entity = input.toEntity();
        assertContentEquals(
                "{\"id\":1,\"name\":\"Introduce Jimmer\"}",
                entity
        );
        assertContentEquals(
                "ArticleNullableIdInput(" +
                        "--->id=1, " +
                        "--->name=Introduce Jimmer, " +
                        "--->writerId=null" +
                        ")",
                new ArticleNullableIdInput(entity)
        );
    }

    @Test
    public void testBookStoreWithNonNullBookIds() {
        BookStoreNullableIdInput input = new BookStoreNullableIdInput();
        input.setName("TURING");
        input.setVersion(1);
        input.setBookIds(Collections.emptyList());

        BookStore entity = input.toEntity();
        assertContentEquals(
                "{" +
                        "--->\"name\":\"TURING\"," +
                        "--->\"website\":null," +
                        "--->\"version\":1," +
                        "--->\"books\":[]" +
                        "}",
                entity
        );
        assertContentEquals(
                "BookStoreNullableIdInput(" +
                        "--->id=null, " +
                        "--->name=TURING, " +
                        "--->website=null, " +
                        "--->version=1, " +
                        "--->bookIds=[]" +
                        ")",
                new BookStoreNullableIdInput(entity)
        );
    }

    @Test
    public void testBookStoreWithNullBookIds() {
        BookStoreNullableIdInput input = new BookStoreNullableIdInput();
        input.setName("TURING");
        input.setVersion(1);

        BookStore entity = input.toEntity();
        assertContentEquals(
                "{" +
                        "--->\"name\":\"TURING\"," +
                        "--->\"website\":null," +
                        "--->\"version\":1," +
                        "--->\"books\":[]" +
                        "}",
                entity
        );
        assertContentEquals(
                "BookStoreNullableIdInput(" +
                        "--->id=null, " +
                        "--->name=TURING, " +
                        "--->website=null, " +
                        "--->version=1, " +
                        "--->bookIds=[]" +
                        ")",
                new BookStoreNullableIdInput(entity)
        );
    }
}
