package org.babyfish.jimmer.sql.event.binlog.impl;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import org.babyfish.jimmer.meta.ImmutableType;

class BinLogModuleV2 extends SimpleModule {

    private final BinLogParser parser;

    BinLogModuleV2(BinLogParser parser) {
        this.parser = parser;
    }

    @Override
    public void setupModule(SetupContext ctx) {
        super.setupModule(ctx);
        ctx.insertAnnotationIntrospector(new Introspector(parser));
        ctx.addDeserializers(new BinLogDeserializersV2(parser));
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
        public Object findDeserializer(Annotated a) {
            if (a instanceof AnnotatedClass) {
                ImmutableType immutableType = ImmutableType.tryGet(a.getRawType());
                if (immutableType != null) {
                    return new BinLogDeserializerV2(parser, immutableType);
                }
            }
            return super.findDeserializer(a);
        }
    }
}
