package org.babyfish.jimmer.client.java.service;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class PageRequest<S> {

    private final S specification;

    private final int pageIndex;

    private final int pageSize;

    public PageRequest(S specification, int pageIndex, int pageSize) {
        this.specification = specification;
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
    }

    @JsonUnwrapped
    public S getSpecification() {
        return specification;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public int getPageSize() {
        return pageSize;
    }
}
