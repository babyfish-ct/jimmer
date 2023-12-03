package org.babyfish.jimmer.client.meta;

import java.util.Map;

public interface EnumConstant {

    String getName();

    Doc getDoc();

    Map<String, Prop> getErrorPropMap();
}
