package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookDraft;
import org.babyfish.jimmer.sql.model.dto.BookFoldInsideFlatView;
import org.babyfish.jimmer.sql.model.dto.BookFoldView;
import org.babyfish.jimmer.sql.model.dto.BookNestedFoldView;
import org.babyfish.jimmer.sql.model.dto.BookViewForIssue843;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.babyfish.jimmer.jackson.codec.JsonCodec.jsonCodec;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BookVIewTest extends Tests {

    @Test
    public void testForIssue843() throws Exception {
        BookViewForIssue843 view = new BookViewForIssue843();
        view.setId(Constants.programmingTypeScriptId2);
        view.setName("Programming TypeScript");
        view.setEdition(2);
        view.setPrice(new BigDecimal("59.99"));
        assertContentEquals(
                "{" +
                        "--->\"id\":\"058ecfd0-047b-4979-a7dc-46ee24d08f08\"," +
                        "--->\"name\":\"Programming TypeScript\"," +
                        "--->\"price\":59.99" +
                        "}",
                jsonCodec().writer().writeAsString(view)
        );
    }

    @Test
    public void testFoldView() throws Exception {
        BookFoldView view = new BookFoldView();
        view.setId(Constants.programmingTypeScriptId2);
        BookFoldView.TargetOf_summary summary = new BookFoldView.TargetOf_summary();
        summary.setName("Programming TypeScript");
        summary.setEdition(2);
        view.setSummary(summary);
        assertContentEquals(
                "{" +
                        "--->\"id\":\"058ecfd0-047b-4979-a7dc-46ee24d08f08\"," +
                        "--->\"summary\":{\"name\":\"Programming TypeScript\",\"edition\":2}" +
                        "}",
                jsonCodec().writer().writeAsString(view)
        );
    }

    @Test
    public void testNestedFoldView() throws Exception {
        BookNestedFoldView view = new BookNestedFoldView();
        view.setId(Constants.graphQLInActionId3);
        view.setName("GraphQL in Action");
        BookNestedFoldView.TargetOf_summary summary = new BookNestedFoldView.TargetOf_summary();
        summary.setName("GraphQL in Action");
        BookNestedFoldView.TargetOf_summary.TargetOf_detail detail =
                new BookNestedFoldView.TargetOf_summary.TargetOf_detail();
        detail.setName("GraphQL in Action");
        detail.setEdition(3);
        summary.setDetail(detail);
        view.setSummary(summary);
        assertContentEquals(
                "{" +
                        "--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                        "--->\"name\":\"GraphQL in Action\"," +
                        "--->\"summary\":{" +
                        "--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->\"detail\":{\"name\":\"GraphQL in Action\",\"edition\":3}" +
                        "--->}" +
                        "}",
                jsonCodec().writer().writeAsString(view)
        );
    }

    @Test
    public void testFoldInsideFlatView() throws Exception {
        BookFoldInsideFlatView view = new BookFoldInsideFlatView();
        view.setId(Constants.programmingTypeScriptId2);
        BookFoldInsideFlatView.TargetOf_storeKey storeKey =
                new BookFoldInsideFlatView.TargetOf_storeKey();
        storeKey.setName("MANNING");
        view.setStoreKey(storeKey);
        assertContentEquals(
                "{" +
                        "--->\"id\":\"058ecfd0-047b-4979-a7dc-46ee24d08f08\"," +
                        "--->\"storeKey\":{\"name\":\"MANNING\"}" +
                        "}",
                jsonCodec().writer().writeAsString(view)
        );
    }

    @Test
    public void testFoldInsideFlatViewFromEntityWithNullFlatHead() {
        Book book = BookDraft.$.produce(draft -> {
            draft.setId(Constants.programmingTypeScriptId2);
            draft.setName("Programming TypeScript");
            draft.setEdition(2);
            draft.setPrice(new BigDecimal("59.99"));
        });

        BookFoldInsideFlatView view = new BookFoldInsideFlatView(book);

        assertNull(view.getStoreKey());
    }
}
