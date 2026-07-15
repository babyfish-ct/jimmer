package org.babyfish.jimmer.jackson.v3;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

abstract class PageMixinV3<T> {

    @JsonCreator
    PageMixinV3(
            @JsonProperty("rows") List<T> rows,
            @JsonProperty("totalRowCount") long totalRowCount,
            @JsonProperty("totalPageCount") long totalPageCount
    ) {
    }
}
