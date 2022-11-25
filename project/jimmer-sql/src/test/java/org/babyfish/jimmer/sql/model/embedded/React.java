package org.babyfish.jimmer.sql.model.embedded;

import org.babyfish.jimmer.sql.Embeddable;
import org.babyfish.jimmer.sql.PropOverride;

@Embeddable
public interface React {

    @PropOverride(prop = "x", columnName = "LEFT")
    @PropOverride(prop = "y", columnName = "TOP")
    Point leftTop();

    @PropOverride(prop = "x", columnName = "RIGHT")
    @PropOverride(prop = "x", columnName = "BOTTOM")
    Point rightBottom();
}
