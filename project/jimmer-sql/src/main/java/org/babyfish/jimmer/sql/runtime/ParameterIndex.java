package org.babyfish.jimmer.sql.runtime;

public class ParameterIndex {

    private int index;

    public int get() {
        return ++index;
    }
}
