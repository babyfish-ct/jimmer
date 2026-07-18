package org.babyfish.jimmer.jackson.v3;

import tools.jackson.databind.ObjectReader;

/**
 * @deprecated 请使用 {@link org.babyfish.jimmer.json.jackson.v3.JsonReaderV3}。
 */
@Deprecated
public class JsonReaderV3<T> extends org.babyfish.jimmer.json.jackson.v3.JsonReaderV3<T> {

    public JsonReaderV3(ObjectReader objectReader) {
        super(objectReader);
    }
}
