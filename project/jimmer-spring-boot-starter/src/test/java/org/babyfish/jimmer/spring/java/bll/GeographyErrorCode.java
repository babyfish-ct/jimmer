package org.babyfish.jimmer.spring.java.bll;

import org.babyfish.jimmer.error.ErrorFamily;
import org.babyfish.jimmer.error.ErrorField;

import java.math.BigDecimal;

@ErrorFamily
public enum GeographyErrorCode {

    @ErrorField(name = "longitude", type = BigDecimal.class)
    @ErrorField(name ="latitude", type = BigDecimal.class)
    ILLEGAL_POSITION,
}
