package org.babyfish.jimmer.meta;

import org.babyfish.jimmer.JimmerVersionsKt;

class GeneratorVersionChecker {
    static void checkGeneratorVersion(String jimmerVersion, String typeName, String generatorName) {
        if (JimmerVersionsKt.compareVersion(jimmerVersion, JimmerVersionsKt.generationVersion()) < 0) {
            throw new IllegalStateException(
                    "The version of the " + generatorName + " for handling type \"" +
                    typeName +
                    "\" is \"" +
                    jimmerVersion +
                    "\", it cannot be less than \"" +
                    JimmerVersionsKt.generationVersion() +
                    "\" which is the last code generation version of jimmer"
            );
        }
    }
}
