package org.babyfish.jimmer.api;

import org.babyfish.jimmer.client.Api;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.client.meta.DefaultFetcherOwner;
import org.babyfish.jimmer.model.Book;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Book Service
 */
@Api
@DefaultFetcherOwner(Tuple.class)
public interface BookService {

    /**
     * Find books by name
     * @param name This is optional
     * @return The query result, a list contains book objects
     */
    List<@FetchBy(value = "A", ownerType = String.class) Book> findById(@Nullable String name);

    /**
     * Find tuple by name
     * @param name This is optional
     * @return The query result, a list contains tuples
     */
    List<Tuple<Tuple<String, Integer>, String>> findTuples(@Nullable String name);
}
