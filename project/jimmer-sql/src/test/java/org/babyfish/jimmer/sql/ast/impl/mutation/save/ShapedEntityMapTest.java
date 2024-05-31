package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookDraft;
import org.babyfish.jimmer.sql.model.embedded.Transform;
import org.babyfish.jimmer.sql.model.embedded.TransformDraft;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class ShapedEntityMapTest extends Tests {

    @Test
    public void testEntity() {
        ShapedEntityMap<Book> bookMap = new ShapedEntityMap<>(ImmutableType.get(Book.class).getKeyProps());
        bookMap.add(
                BookDraft.$.produce(book -> {
                    book.setId(Constants.graphQLInActionId1);
                    book.setName("GraphQL in Action");
                })
        );
        bookMap.add(
                BookDraft.$.produce(book -> {
                    book.setId(Constants.graphQLInActionId2);
                    book.setName("GraphQL in Action");
                })
        );
        bookMap.add(
                BookDraft.$.produce(book -> {
                    book.setName("Learning GraphQL");
                    book.setEdition(1);
                })
        );
        bookMap.add(
                BookDraft.$.produce(book -> {
                    book.setName("Learning GraphQL");
                    book.setEdition(2);
                })
        );
        assertContentEquals(
                "{" +
                        "--->[id, name]: [" +
                        "--->--->{\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\",\"name\":\"GraphQL in Action\"}, " +
                        "--->--->{\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\",\"name\":\"GraphQL in Action\"}" +
                        "--->], " +
                        "--->[name, edition]: [" +
                        "--->--->{\"name\":\"Learning GraphQL\",\"edition\":1}, " +
                        "--->--->{\"name\":\"Learning GraphQL\",\"edition\":2}" +
                        "--->]" +
                        "}",
                bookMap.toString()
        );
    }

    @Test
    public void testEmbedded() {
        ShapedEntityMap<Transform> transformMap = new ShapedEntityMap<>(ImmutableType.get(Transform.class).getKeyProps());
        transformMap.add(
                TransformDraft.$.produce(draft -> {
                    draft.setId(1L);
                    draft.applySource(source -> {
                        source.applyLeftTop(lt -> lt.setX(1));
                        source.applyRightBottom(rb -> rb.setY(2));
                    });
                })
        );
        transformMap.add(
                TransformDraft.$.produce(draft -> {
                    draft.setId(2L);
                    draft.applySource(source -> {
                        source.applyLeftTop(lt -> lt.setY(4));
                        source.applyRightBottom(rb -> rb.setX(8));
                    });
                })
        );
        transformMap.add(
                TransformDraft.$.produce(draft -> {
                    draft.setId(3L);
                    draft.applySource(source -> {
                        source.applyLeftTop(lt -> lt.setX(16));
                        source.applyRightBottom(rb -> rb.setY(32));
                    });
                })
        );
        transformMap.add(
                TransformDraft.$.produce(draft -> {
                    draft.setId(4L);
                    draft.applySource(source -> {
                        source.applyLeftTop(lt -> lt.setY(64));
                        source.applyRightBottom(rb -> rb.setX(128));
                    });
                })
        );
        assertContentEquals(
                "{" +
                        "--->[id, source.leftTop.x, source.rightBottom.y]: [" +
                        "--->--->{\"id\":1,\"source\":{\"leftTop\":{\"x\":1},\"rightBottom\":{\"y\":2}}}, " +
                        "--->--->{\"id\":3,\"source\":{\"leftTop\":{\"x\":16},\"rightBottom\":{\"y\":32}}}" +
                        "--->], " +
                        "--->[id, source.leftTop.y, source.rightBottom.x]: [" +
                        "--->--->{\"id\":2,\"source\":{\"leftTop\":{\"y\":4},\"rightBottom\":{\"x\":8}}}, " +
                        "--->--->{\"id\":4,\"source\":{\"leftTop\":{\"y\":64},\"rightBottom\":{\"x\":128}}}" +
                        "--->]" +
                        "}",
                transformMap.toString()
        );
    }
}
