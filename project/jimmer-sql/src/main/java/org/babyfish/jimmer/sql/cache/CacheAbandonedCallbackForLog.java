package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CacheAbandonedCallbackForLog implements CacheAbandonedCallback {

    static final CacheAbandonedCallbackForLog INSTANCE = new CacheAbandonedCallbackForLog();

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheAbandonedCallback.class);

    @Override
    public void abandoned(ImmutableProp prop, Reason reason) {
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn(
                    "Property-level cache is abandoned.\n" +
                            "\tProperty: " +
                            prop +
                            "\n\tReason: " +
                            reason
            );
        }
    }
}
