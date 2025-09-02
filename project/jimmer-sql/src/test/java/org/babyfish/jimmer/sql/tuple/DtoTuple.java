package org.babyfish.jimmer.sql.tuple;

import org.babyfish.jimmer.sql.TypedTuple;
import org.babyfish.jimmer.sql.model.dto.BookViewForTupleTest;

@TypedTuple
public class DtoTuple {

    private BookViewForTupleTest book;

    private long authorCount;

    public BookViewForTupleTest getBook() {
        return book;
    }

    public void setBook(BookViewForTupleTest book) {
        this.book = book;
    }

    public long getAuthorCount() {
        return authorCount;
    }

    public void setAuthorCount(long authorCount) {
        this.authorCount = authorCount;
    }
}
