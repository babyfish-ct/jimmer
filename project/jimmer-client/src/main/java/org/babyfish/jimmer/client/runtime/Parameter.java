package org.babyfish.jimmer.client.runtime;

public interface Parameter {

    String getName();

    Type getType();

    String getRequestHeader();

    String getRequestParam();

    String getPathVariable();

    String getRequestPart();

    boolean isRequestBody();

    String getDefaultValue();
}
