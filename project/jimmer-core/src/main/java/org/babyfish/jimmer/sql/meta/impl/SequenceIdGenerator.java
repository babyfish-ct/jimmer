package org.babyfish.jimmer.sql.meta.impl;

import org.babyfish.jimmer.sql.meta.IdGenerator;

public final class SequenceIdGenerator implements IdGenerator {

    private final String sequenceName;

    public SequenceIdGenerator(String sequenceName) {
        this.sequenceName = sequenceName;
    }

    public String getSequenceName() {
        return sequenceName;
    }
}
