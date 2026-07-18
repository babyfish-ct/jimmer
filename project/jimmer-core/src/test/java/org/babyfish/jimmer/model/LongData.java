package org.babyfish.jimmer.model;

import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.json.JsonConverter;
import org.babyfish.jimmer.json.LongToStringConverter;
import org.babyfish.jimmer.json.LongListToStringListConverter;

import java.util.List;

@Immutable
public interface LongData {

    @JsonConverter(LongToStringConverter.class)
    long notNullValue();

    @JsonConverter(LongToStringConverter.class)
    Long nullableValue();

    @JsonConverter(LongListToStringListConverter.class)
    List<Long> values();
}
