package org.babyfish.jimmer.sql.loader;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.model.AuthorDraft;
import org.babyfish.jimmer.sql.model.BookDraft;
import org.babyfish.jimmer.sql.model.BookStoreDraft;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class Entities {

    private Entities() {}

    @SuppressWarnings("unchecked")
    public static final List<ImmutableSpi> BOOKS_FOR_MANY_TO_ONE =
            Collections.unmodifiableList(
                (List<ImmutableSpi>) (List<?>) Arrays.asList(
                    BookDraft.$.produce(book ->
                            book
                                    .setId(learningGraphQLId1)
                                    .applyStore(store -> store.setId(oreillyId))
                    ),
                    BookDraft.$.produce(book ->
                            book.setId(learningGraphQLId2)
                    ),
                    BookDraft.$.produce(book ->
                            book
                                    .setId(graphQLInActionId1)
                                    .applyStore(store -> store.setId(manningId))
                    ),
                    BookDraft.$.produce(book ->
                            book.setId(graphQLInActionId2)
                    )
                )
        );

    @SuppressWarnings("unchecked")
    public static final List<ImmutableSpi> BOOK_STORES =
            Collections.unmodifiableList(
                    (List<ImmutableSpi>) (List<?>) Arrays.asList(
                            BookStoreDraft.$.produce(store ->
                                    store.setId(oreillyId)
                            ),
                            BookStoreDraft.$.produce(store ->
                                    store.setId(manningId)
                            )
                    )
            );

    @SuppressWarnings("unchecked")
    public static final List<ImmutableSpi> BOOKS_FOR_MANY_TO_MANY =
            Collections.unmodifiableList(
                    (List<ImmutableSpi>) (List<?>) Arrays.asList(
                            BookDraft.$.produce(
                                    book -> book.setId(learningGraphQLId3)
                            ),
                            BookDraft.$.produce(
                                    book -> book.setId(graphQLInActionId3)
                            )
                    )
            );

    @SuppressWarnings("unchecked")
    public static final List<ImmutableSpi> AUTHORS_FOR_MANY_TO_MANY =
            Collections.unmodifiableList(
                    (List<ImmutableSpi>) (List<?>) Arrays.asList(
                            AuthorDraft.$.produce(
                                    author -> author.setId(alexId)
                            ),
                            AuthorDraft.$.produce(
                                    author -> author.setId(danId)
                            )
                    )
            );

    public static final List<ImmutableSpi> AUTHORS_FOR_MANY_TO_ONE =
            AUTHORS_FOR_MANY_TO_MANY;
}
