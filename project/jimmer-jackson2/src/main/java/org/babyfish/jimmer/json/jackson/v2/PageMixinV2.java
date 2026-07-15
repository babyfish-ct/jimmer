package org.babyfish.jimmer.json.jackson.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

abstract class PageMixinV2<T> {

    @JsonCreator
    PageMixinV2(
            @JsonProperty("rows") List<T> rows,
            @JsonProperty("totalRowCount") long totalRowCount,
            @JsonProperty("totalPageCount") long totalPageCount
    ) {
    }
}
