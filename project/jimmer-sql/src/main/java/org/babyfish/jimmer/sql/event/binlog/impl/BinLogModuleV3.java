package org.babyfish.jimmer.sql.event.binlog.impl;

import org.babyfish.jimmer.meta.ImmutableType;
import tools.jackson.core.Version;
import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.module.SimpleModule;

class BinLogModuleV3 extends SimpleModule {

    private final BinLogParser parser;

    BinLogModuleV3(BinLogParser parser) {
        this.parser = parser;
    }

    @Override
    public void setupModule(SetupContext ctx) {
        super.setupModule(ctx);
        ctx.insertAnnotationIntrospector(new Introspector(parser));
        ctx.addDeserializers(new BinLogDeserializersV3(parser));
    }

    private static class Introspector extends AnnotationIntrospector {

        private final BinLogParser parser;

        private Introspector(BinLogParser parser) {
            this.parser = parser;
        }

        @Override
        public Version version() {
            return Version.unknownVersion();
        }

        @Override
        public Object findDeserializer(MapperConfig<?> config, Annotated a) {
            if (a instanceof AnnotatedClass) {
                ImmutableType immutableType = ImmutableType.tryGet(a.getRawType());
                return immutableType != null ?
                        new BinLogDeserializerV3(parser, immutableType) :
                        super.findDeserializer(config, a);
            }
            return super.findDeserializer(config, a);
        }
    }
}
