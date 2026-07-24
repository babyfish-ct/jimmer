package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.client.Description;
import org.babyfish.jimmer.impl.org.objectweb.asm.*;
import org.babyfish.jimmer.sql.model.base.DocumentedBaseDraft;
import org.babyfish.jimmer.sql.model.doc.dto.DocumentedEntityView;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DtoDocumentationTest {

    private static final String DESCRIPTION_DESCRIPTOR =
            "L" + Description.class.getName().replace('.', '/') + ";";

    private static String description(Class<?> type, String methodName) throws IOException {
        AtomicReference<String> resultRef = new AtomicReference<>();
        try (InputStream inputStream = type.getResourceAsStream("/" + type.getName().replace('.', '/') + ".class")) {
            new ClassReader(inputStream).accept(
                    new ClassVisitor(Opcodes.ASM9) {
                        @Override
                        public MethodVisitor visitMethod(
                                int access,
                                String name,
                                String descriptor,
                                String signature,
                                String[] exceptions
                        ) {
                            if (!name.equals(methodName)) {
                                return null;
                            }
                            return new MethodVisitor(Opcodes.ASM9) {
                                @Override
                                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                                    if (!descriptor.equals(DESCRIPTION_DESCRIPTOR)) {
                                        return null;
                                    }
                                    return new AnnotationVisitor(Opcodes.ASM9) {
                                        @Override
                                        public void visit(String name, Object value) {
                                            if ("value".equals(name)) {
                                                resultRef.set((String) value);
                                            }
                                        }
                                    };
                                }
                            };
                        }
                    },
                    ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES
            );
        }
        return resultRef.get();
    }

    @Test
    public void testMappedSuperclassDocumentationFromDependency() throws IOException {
        assertEquals(
                "The user who created the object",
                description(DocumentedBaseDraft.class, "setCreatedBy")
        );
        assertEquals(
                "The user who created the object\n",
                description(DocumentedEntityView.class, "getCreatedBy")
        );
    }
}
