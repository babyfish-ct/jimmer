package org.babyfish.jimmer.jackson.v2;

import com.fasterxml.jackson.databind.ObjectReader;

/**
 * @deprecated 请使用 {@link org.babyfish.jimmer.json.jackson.v2.JsonReaderV2}。
 */
@Deprecated
public class JsonReaderV2<T> extends org.babyfish.jimmer.json.jackson.v2.JsonReaderV2<T> {

    public JsonReaderV2(ObjectReader objectReader) {
        super(objectReader);
    }
}
