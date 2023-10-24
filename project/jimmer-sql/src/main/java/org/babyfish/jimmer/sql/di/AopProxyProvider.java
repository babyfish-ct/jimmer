package org.babyfish.jimmer.sql.di;

public interface AopProxyProvider {

    Class<?> getTargetClass(Object proxy);
}
