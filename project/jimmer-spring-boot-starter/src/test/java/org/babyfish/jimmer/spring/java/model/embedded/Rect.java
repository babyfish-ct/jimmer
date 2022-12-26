package org.babyfish.jimmer.spring.java.model.embedded;

import org.babyfish.jimmer.sql.Embeddable;
import org.babyfish.jimmer.sql.PropOverride;

@Embeddable
public interface Rect {

    @PropOverride(prop = "x", columnName = "`LEFT`")
    @PropOverride(prop = "y", columnName = "TOP")
    Point leftTop();

    @PropOverride(prop = "x", columnName = "`RIGHT`")
    @PropOverride(prop = "y", columnName = "BOTTOM")
    Point rightBottom();
}
