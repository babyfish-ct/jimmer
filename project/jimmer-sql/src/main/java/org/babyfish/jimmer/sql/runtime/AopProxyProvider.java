package org.babyfish.jimmer.sql.runtime;

public interface AopProxyProvider {

    Class<?> getTargetClass(Object proxy);
}
