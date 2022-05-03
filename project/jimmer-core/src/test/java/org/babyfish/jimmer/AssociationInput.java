package org.babyfish.jimmer;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

@Immutable
public interface AssociationInput {

    @Min(0)
    @Max(100)
    long parentId();

    List<Long> childIds();
}
