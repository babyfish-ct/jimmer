package org.babyfish.jimmer.client.runtime;

public interface Parameter {

    String getName();

    Type getType();

    String getRequestHeader();

    String getRequestParam();

    String getPathVariable();

    boolean isRequestBody();

    String getDefaultValue();
}
