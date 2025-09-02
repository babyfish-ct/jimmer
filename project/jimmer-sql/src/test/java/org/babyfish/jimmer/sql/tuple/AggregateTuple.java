package org.babyfish.jimmer.sql.tuple;

import lombok.Data;
import org.babyfish.jimmer.sql.TypedTuple;

import java.math.BigDecimal;
import java.util.UUID;

@TypedTuple
@Data
public class AggregateTuple {

    private final UUID storeId;

    private final long bookCount;

    private final BigDecimal minPrice;

    private final BigDecimal maxPrice;

    private final BigDecimal avgPrice;
}
