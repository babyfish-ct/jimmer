package org.babyfish.jimmer.sql.api;

import org.babyfish.jimmer.error.ErrorFamily;
import org.babyfish.jimmer.error.ErrorField;

import java.time.LocalDateTime;

@ErrorFamily("SYSTEM_FAMILY")
@ErrorField(name = "tags", type = String.class, list = true, doc = "TagList")
@ErrorField(name = "timestamp", type = LocalDateTime.class, doc = "Error created time")
public enum SystemErrorCode {

    @ErrorField(name = "minBound", type = int.class, doc = "Min Bound value")
    @ErrorField(name = "maxBound", type = int.class, doc = "Max Bound value")
    A,

    @ErrorField(name = "path", type = String.class, doc = "The file path which cannot be accessed")
    B,

    @ErrorField(name = "baseUrl", type = String.class, doc = "The url which cannot be accessed")
    @ErrorField(name = "port", type = int.class, doc = "The port which annot be accessed")
    C
}
