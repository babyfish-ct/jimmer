package org.babyfish.jimmer.sql.cache;

import java.util.Collections;
import java.util.Map;

public interface CacheFilter {

    default Map<String, Object> toCacheArgs() {
        return Collections.emptyMap();
    }
}
