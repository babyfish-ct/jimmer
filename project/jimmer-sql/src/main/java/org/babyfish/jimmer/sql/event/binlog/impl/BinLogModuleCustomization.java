package org.babyfish.jimmer.sql.event.binlog.impl;

import org.babyfish.jimmer.json.codec.JsonCodecCustomization;
import org.babyfish.jimmer.json.codec.JsonCodecCustomizationTarget;
import org.babyfish.jimmer.json.codec.JsonCodecFamily;

public class BinLogModuleCustomization implements JsonCodecCustomization {
    private final BinLogParser parser;

    public BinLogModuleCustomization(BinLogParser parser) {
        this.parser = parser;
    }

    @Override
    public void customize(JsonCodecCustomizationTarget target) {
        if (target.family() == JsonCodecFamily.JACKSON2) {
            target.addNativeModule(new BinLogModuleV2(parser));
        } else if (target.family() == JsonCodecFamily.JACKSON3) {
            target.addNativeModule(new BinLogModuleV3(parser));
        }
    }
}
