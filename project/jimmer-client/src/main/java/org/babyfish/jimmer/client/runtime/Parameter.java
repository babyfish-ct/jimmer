package org.babyfish.jimmer.client.runtime;

public interface Parameter {

    String getName();

    Type getType();

    String getRequestParam();

    String getPathVariable();

    boolean isRequestBody();
}
