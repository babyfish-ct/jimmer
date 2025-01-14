package org.babyfish.jimmer.client.java.service;

public record PageQuery<T>(
        Integer pageIndex,
        Integer pageSize,
        T spec
) {

}