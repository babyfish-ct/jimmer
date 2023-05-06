package org.babyfish.jimmer.sql.meta;

public class SequenceIdGenerator implements IdGenerator {

    private String sequenceName;

    public SequenceIdGenerator(String sequenceName) {
        this.sequenceName = sequenceName;
    }

    public String getSequenceName() {
        return sequenceName;
    }
}
