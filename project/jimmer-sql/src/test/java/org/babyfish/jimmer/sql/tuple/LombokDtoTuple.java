package org.babyfish.jimmer.sql.tuple;

import lombok.Data;
import org.babyfish.jimmer.sql.TypedTuple;
import org.babyfish.jimmer.sql.model.dto.BookViewForTupleTest;

@Data
@TypedTuple
public class LombokDtoTuple {

    private BookViewForTupleTest book;

    private long authorCount;
}
