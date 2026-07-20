package org.babyfish.jimmer.sql.tuple;

import org.babyfish.jimmer.sql.TypedTuple;
import org.babyfish.jimmer.sql.model.Book;

@TypedTuple
public class EntityTuple {

    private final Book book;

    private final long authorCount;

    public EntityTuple(Book book, long authorCount) {
        this.book = book;
        this.authorCount = authorCount;
    }

    public Book getBook() {
        return book;
    }

    public long getAuthorCount() {
        return authorCount;
    }
}
