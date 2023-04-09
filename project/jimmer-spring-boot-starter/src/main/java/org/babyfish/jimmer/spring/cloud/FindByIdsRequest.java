package org.babyfish.jimmer.spring.cloud;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class FindByIdsRequest {

    private final String idArrStr;

    private final String fetcherStr;

    @JsonCreator
    public FindByIdsRequest(
            @JsonProperty("idArrStr") String idArrStr,
            @JsonProperty("fetcherStr") String fetcherStr) {
        this.idArrStr = Objects.requireNonNull(idArrStr);
        this.fetcherStr = Objects.requireNonNull(fetcherStr);
    }

    @NotNull
    public String getIdArrStr() {
        return idArrStr;
    }

    @NotNull
    public String getFetcherStr() {
        return fetcherStr;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idArrStr, fetcherStr);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FindByIdsRequest that = (FindByIdsRequest) o;
        return idArrStr.equals(that.idArrStr) && fetcherStr.equals(that.fetcherStr);
    }

    @Override
    public String toString() {
        return "FindByIdsRequest{" +
                "idArrStr='" + idArrStr + '\'' +
                ", fetcherStr='" + fetcherStr + '\'' +
                '}';
    }
}
