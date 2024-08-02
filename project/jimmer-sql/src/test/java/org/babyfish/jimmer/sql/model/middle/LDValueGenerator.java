package org.babyfish.jimmer.sql.model.middle;

import org.babyfish.jimmer.sql.meta.LogicalDeletedValueGenerator;

import java.util.concurrent.atomic.AtomicLong;

public class LDValueGenerator implements LogicalDeletedValueGenerator<Long> {

    private static final AtomicLong REF = new AtomicLong();

    public static void reset() {
        REF.set(100000L);
    }

    @Override
    public Long generate() {
        return REF.getAndIncrement();
    }
}
