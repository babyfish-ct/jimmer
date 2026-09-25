package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.LikeMode;

public class LikePattern {

    private LikePattern() {}

    public static String of(String pattern, LikeMode mode) {
        if (!mode.isStartExact() && !pattern.startsWith("%")) {
            pattern = '%' + pattern;
        }
        if (!mode.isEndExact()) {
            // An existing trailing '%' can be escaped. Appending another wildcard
            // also preserves the meaning of an unescaped '%' without assuming an escape character.
            pattern += '%';
        }
        return pattern;
    }
}
