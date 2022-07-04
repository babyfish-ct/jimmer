package org.babyfish.jimmer.sql.loader;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.model.BookDraft;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.babyfish.jimmer.sql.common.Constants.*;
import static org.babyfish.jimmer.sql.common.Constants.graphQLInActionId2;

public class Entities {

    private Entities() {}

    @SuppressWarnings("unchecked")
    public static final List<ImmutableSpi> BOOKS_WITH_MANY_TO_ONE =
            Collections.unmodifiableList(
                (List<ImmutableSpi>) (List<?>) Arrays.asList(
                    BookDraft.$.produce(book ->
                            book
                                    .setId(learningGraphQLId1)
                                    .setStore(store -> store.setId(oreillyId))
                    ),
                    BookDraft.$.produce(book ->
                            book.setId(learningGraphQLId2)
                    ),
                    BookDraft.$.produce(book ->
                            book
                                    .setId(graphQLInActionId1)
                                    .setStore(store -> store.setId(manningId))
                    ),
                    BookDraft.$.produce(book ->
                            book.setId(graphQLInActionId2)
                    )
                )
        );
}
