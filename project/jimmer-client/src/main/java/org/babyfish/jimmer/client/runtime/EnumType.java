package org.babyfish.jimmer.client.runtime;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.EnumConstant;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface EnumType extends NamedType {

    Class<?> getJavaType();

    List<String> getSimpleNames();

    List<Constant> getConstants();

    @Nullable
    Doc getDoc();

    class Constant {

        private final String name;

        @Nullable
        private final Doc doc;

        public Constant(String name, @Nullable Doc doc) {
            this.name = name;
            this.doc = doc;
        }

        public String getName() {
            return name;
        }

        @Nullable
        public Doc getDoc() {
            return doc;
        }
    }
}
