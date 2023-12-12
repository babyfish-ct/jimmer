package org.babyfish.jimmer.client.runtime;

import java.util.List;

public interface EnumType extends Type {

    Class<?> getJavaType();

    List<String> getConstants();
}
