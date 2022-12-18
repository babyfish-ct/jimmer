package org.babyfish.jimmer.spring.datasource;

public interface TxCallback {

    void open();

    void commit();

    void rollback();
}
