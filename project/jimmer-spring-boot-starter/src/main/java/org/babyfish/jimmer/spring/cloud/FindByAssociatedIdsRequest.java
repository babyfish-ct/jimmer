package org.babyfish.jimmer.spring.cloud;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class FindByAssociatedIdsRequest {

    private final String prop;

    private final String targetIdArrStr;

    private final String fetcherStr;

    @JsonCreator
    public FindByAssociatedIdsRequest(
            @JsonProperty("prop") String prop,
            @JsonProperty("targetIdArrStr") String targetIdArrStr,
            @JsonProperty("fetcherStr") String fetcherStr
    ) {
        this.prop = Objects.requireNonNull(prop);
        this.targetIdArrStr = Objects.requireNonNull(targetIdArrStr);
        this.fetcherStr = Objects.requireNonNull(fetcherStr);
    }

    @NotNull
    public String getProp() {
        return prop;
    }

    @NotNull
    public String getTargetIdArrStr() {
        return targetIdArrStr;
    }

    @NotNull
    public String getFetcherStr() {
        return fetcherStr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FindByAssociatedIdsRequest that = (FindByAssociatedIdsRequest) o;
        return prop.equals(that.prop) && targetIdArrStr.equals(that.targetIdArrStr) && fetcherStr.equals(that.fetcherStr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prop, targetIdArrStr, fetcherStr);
    }

    @Override
    public String toString() {
        return "FindByAssociatedIdsRequest{" +
                "prop='" + prop + '\'' +
                ", targetIdArrStr='" + targetIdArrStr + '\'' +
                ", fetcherStr='" + fetcherStr + '\'' +
                '}';
    }
}
