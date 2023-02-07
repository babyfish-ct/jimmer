package org.babyfish.jimmer;

import org.babyfish.jimmer.error.ErrorFamily;
import org.babyfish.jimmer.error.ErrorField;

@ErrorFamily
public enum BusinessError {

    UNAUTHORIZED,

    @ErrorField(name = "userNames", type = String.class, list = true, nullable = true)
    ILLEGAL_USER_NAME,

    @ErrorField(name = "x", type = int.class)
    @ErrorField(name = "y", type = int.class)
    REFERENCE_CYCLE
}
