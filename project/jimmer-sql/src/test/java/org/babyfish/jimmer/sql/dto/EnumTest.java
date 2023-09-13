package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.enumeration.Approver;
import org.babyfish.jimmer.sql.model.enumeration.Article;
import org.babyfish.jimmer.sql.model.enumeration.Writer;
import org.babyfish.jimmer.sql.model.enumeration.dto.AnotherWriterView;
import org.babyfish.jimmer.sql.model.enumeration.dto.ApproverView;
import org.babyfish.jimmer.sql.model.enumeration.dto.ArticleInput;
import org.babyfish.jimmer.sql.model.enumeration.dto.WriterView;
import org.junit.jupiter.api.Test;

public class EnumTest extends Tests {

    @Test
    public void testWriterView() {
        WriterView view = new WriterView();
        view.setId(1L);
        view.setName("Bob");
        view.setSex(100);

        Writer entity = view.toEntity();
        assertContentEquals(
                "{\"id\":1,\"name\":\"Bob\",\"gender\":\"MALE\"}",
                entity
        );

        WriterView view2 = new WriterView(entity);
        assertContentEquals(
                "WriterView(id=1, name=Bob, sex=100)",
                view2
        );
    }

    @Test
    public void testAnotherWriterView() {
        AnotherWriterView view = new AnotherWriterView();
        view.setId(1L);
        view.setName("Bob");
        view.setSex(null);

        Writer entity = view.toEntity();
        assertContentEquals(
                "{\"id\":1,\"name\":\"Bob\"}",
                entity
        );

        AnotherWriterView view2 = new AnotherWriterView(entity);
        assertContentEquals(
                "AnotherWriterView(id=1, name=Bob, sex=null)",
                view2
        );
    }

    @Test
    public void testApproverView() {
        ApproverView view = new ApproverView();
        view.setId(1L);
        view.setName("Bob");
        view.setSex(null);

        Approver entity = view.toEntity();
        assertContentEquals(
                "{\"id\":1,\"name\":\"Bob\",\"gender\":null}",
                entity
        );

        ApproverView view2 = new ApproverView(entity);
        assertContentEquals(
                "ApproverView(id=1, name=Bob, sex=null)",
                view2
        );
    }

    @Test
    public void testArticleInput() {
        ArticleInput input = new ArticleInput();
        input.setId(1L);
        input.setName("Introduce Jimmer");
        input.setWriterId(1L);
        input.setWriterName("Bob");
        input.setWriterGender("Male");
        input.setApproverId(2L);
        input.setApproverName("Linda");
        input.setApproverGender("Female");

        Article entity = input.toEntity();
        assertContentEquals(
                "{" +
                        "--->\"id\":1," +
                        "--->\"name\":\"Introduce Jimmer\"," +
                        "--->\"writer\":{" +
                        "--->--->\"id\":1," +
                        "--->--->\"name\":\"Bob\"," +
                        "--->--->\"gender\":\"MALE\"" +
                        "--->}," +
                        "--->\"approver\":{" +
                        "--->--->\"id\":2," +
                        "--->--->\"name\":\"Linda\"," +
                        "--->--->\"gender\":\"FEMALE\"" +
                        "--->}" +
                        "}",
                entity
        );

        ArticleInput input2 = new ArticleInput(entity);
        assertContentEquals(
                "ArticleInput(" +
                        "--->id=1, " +
                        "--->name=Introduce Jimmer, " +
                        "--->writerId=1, " +
                        "--->writerName=Bob, " +
                        "--->writerGender=Male, " +
                        "--->approverId=2, " +
                        "--->approverName=Linda, " +
                        "--->approverGender=Female" +
                        ")",
                input2
        );
    }

    @Test
    public void testArticleInput2() {
        ArticleInput input = new ArticleInput();
        input.setId(1L);
        input.setName("Introduce Jimmer");
        input.setWriterId(1L);
        input.setWriterName("Bob");
        input.setWriterGender(null);
        input.setApproverId(2L);
        input.setApproverName("Linda");
        input.setApproverGender(null);

        Article entity = input.toEntity();
        assertContentEquals(
                "{" +
                        "--->\"id\":1," +
                        "--->\"name\":\"Introduce Jimmer\"," +
                        "--->\"writer\":{" +
                        "--->--->\"id\":1," +
                        "--->--->\"name\":\"Bob\"" +
                        "--->}," +
                        "--->\"approver\":{" +
                        "--->--->\"id\":2," +
                        "--->--->\"name\":\"Linda\"," +
                        "--->--->\"gender\":null" +
                        "--->}" +
                        "}",
                entity
        );

        ArticleInput input2 = new ArticleInput(entity);
        assertContentEquals(
                "ArticleInput(" +
                        "--->id=1, " +
                        "--->name=Introduce Jimmer, " +
                        "--->writerId=1, " +
                        "--->writerName=Bob, " +
                        "--->writerGender=null, " +
                        "--->approverId=2, " +
                        "--->approverName=Linda, " +
                        "--->approverGender=null" +
                        ")",
                input2
        );
    }
}
