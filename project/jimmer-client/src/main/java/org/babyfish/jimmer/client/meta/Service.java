package org.babyfish.jimmer.client.meta;

import java.util.List;

public interface Service extends Node {

    Class<?> getJavaType();

    List<Operation> getOperations();
}
