package org.babyfish.jimmer.model;

import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.Scalar;

import java.util.List;

@Immutable
public interface Data {

    @Scalar
    List<Long> list();

    @Scalar
    List<List<Long>> nestedList();

    @Scalar
    long[] arr();

    @Scalar
    long[][] nestedArr();
}
