package org.babyfish.jimmer.sql.event.binlog.impl;

import org.babyfish.jimmer.json.codec.JsonCodecCustomization;
import org.babyfish.jimmer.json.codec.JsonCodecCustomizationTarget;

public class BinLogModuleCustomization implements JsonCodecCustomization {
    private final BinLogParser parser;

    public BinLogModuleCustomization(BinLogParser parser) {
        this.parser = parser;
    }

    @Override
    public void customize(JsonCodecCustomizationTarget target) {
        if (target.acceptsNativeModule(BinLogModuleV2.class)) {
            target.addNativeModule(new BinLogModuleV2(parser));
        } else if (target.acceptsNativeModule(BinLogModuleV3.class)) {
            target.addNativeModule(new BinLogModuleV3(parser));
        }
    }
}
