package org.babyfish.jimmer.model;

import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.jackson.JsonConverter;
import org.babyfish.jimmer.jackson.LongConverter;
import org.babyfish.jimmer.jackson.LongListConverter;

import java.util.List;

@Immutable
public interface LongData {

    @JsonConverter(LongConverter.class)
    long notNullValue();

    @JsonConverter(LongConverter.class)
    Long nullableValue();

    @JsonConverter(LongListConverter.class)
    List<Long> values();
}
