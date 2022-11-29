package org.babyfish.jimmer.sql.model.embedded;

import org.babyfish.jimmer.sql.Embeddable;
import org.babyfish.jimmer.sql.PropOverride;
import org.jetbrains.annotations.Nullable;

@Embeddable
public interface Rect {

    @PropOverride(prop = "x", columnName = "`LEFT`")
    @PropOverride(prop = "y", columnName = "TOP")
    Point leftTop();

    @PropOverride(prop = "x", columnName = "`RIGHT`")
    @PropOverride(prop = "y", columnName = "BOTTOM")
    Point rightBottom();
}
