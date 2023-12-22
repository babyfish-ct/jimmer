package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.SourceWriter;
import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.runtime.Property;

class DocUtils {

    static void doc(Property property, Doc parentDoc, SourceWriter writer) {
        doc(property.getDoc(), property.getName(), parentDoc, writer);
    }

    static void doc(Doc doc, String name, Doc parentDoc, SourceWriter writer) {
        if (doc != null) {
            writer.doc(doc);
        } else {
            if (parentDoc != null) {
                writer.doc(parentDoc.getPropertyValueMap().get(name));
            }
        }
    }
}
