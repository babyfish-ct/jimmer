package org.babyfish.jimmer.apt.client;

import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.MetaException;
import org.babyfish.jimmer.apt.immutable.generator.Annotations;
import org.babyfish.jimmer.error.CodeBasedException;
import org.babyfish.jimmer.error.CodeBasedRuntimeException;
import org.babyfish.jimmer.ClientException;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.*;

public class ClientExceptionContext {

    private final Context context;

    private final Map<TypeElement, ClientExceptionMetadata> metadataMap = new HashMap<>();

    private final Map<Key, TypeElement> nonAbstractElementMap = new HashMap<>();

    public ClientExceptionContext(Context context) {
        this.context = context;
    }

    public ClientExceptionMetadata get(TypeElement typeElement) {
        ClientExceptionMetadata metadata =  metadataMap.get(typeElement);
        if (metadata == null) {
            metadata = create(typeElement);
            metadataMap.put(typeElement, metadata);
            try {
                initSubMetadatas(metadata);
            } catch (RuntimeException | Error ex) {
                metadataMap.remove(typeElement);
                throw ex;
            }
        }
        return metadata;
    }

    private ClientExceptionMetadata create(TypeElement typeElement) {
        AnnotationMirror annotationMirror = Annotations.annotationMirror(typeElement, ClientException.class);
        if (annotationMirror == null) {
            throw new MetaException(
                    typeElement,
                    "the exception type extends \"" +
                            CodeBasedException.class.getName() +
                            "\" or \"" +
                            CodeBasedRuntimeException.class.getName() +
                            "\" must be decorated by \"@" +
                            ClientException.class.getName() +
                            "\""
            );
        }
        String code = Annotations.annotationValue(annotationMirror, "code", null);
        if (code != null && code.isEmpty()) {
            code = null;
        }
        List<Object> subTypes = Annotations.annotationValue(annotationMirror, "subTypes", Collections.emptyList());
        if (code == null && subTypes.isEmpty()) {
            throw new MetaException(
                    typeElement,
                    "it is decorated by @\"" +
                            ClientException.class.getName() +
                            "\" but neither \"code\" nor \"subTypes\" of the annotation is specified"
            );
        }
        if (code != null && !subTypes.isEmpty()) {
            throw new MetaException(
                    typeElement,
                    "it is decorated by @\"" +
                            ClientException.class.getName() +
                            "\" but both \"code\" and \"subTypes\" of the annotation are specified"
            );
        }
        if (code != null && typeElement.getModifiers().contains(Modifier.ABSTRACT)) {
            throw new MetaException(
                    typeElement,
                    "it is decorated by @\"" +
                            ClientException.class.getName() +
                            "\" and the \"code\" of the annotation is specified so that " +
                            "it cannot be abstract"
            );
        }
        if (!subTypes.isEmpty() && !typeElement.getModifiers().contains(Modifier.ABSTRACT)) {
            throw new MetaException(
                    typeElement,
                    "it is decorated by @\"" +
                            ClientException.class.getName() +
                            "\" and the \"subTypes\" of the annotation is specified so that " +
                            "it must be abstract"
            );
        }

        TypeElement superElement = (TypeElement) context.getTypes().asElement(typeElement.getSuperclass());
        ClientExceptionMetadata superMetadata = null;
        if (!superElement.getQualifiedName().toString().equals(CodeBasedException.class.getName()) &&
                !superElement.getQualifiedName().toString().equals(CodeBasedRuntimeException.class.getName())) {
            AnnotationMirror superAnnotationMirror = Annotations.annotationMirror(superElement, ClientException.class);
            if (superAnnotationMirror != null) {
                List<Object> backRefSuperTypes = Annotations.annotationValue(superAnnotationMirror, "subTypes", Collections.emptyList());
                boolean match = false;
                for (Object bacRefType : backRefSuperTypes) {
                    String backRefTypeName = bacRefType.toString();
                    if (backRefTypeName.endsWith(".class")) {
                        backRefTypeName = backRefTypeName.substring(0, backRefTypeName.length() - 6);
                    }
                    if (backRefTypeName.equals(typeElement.getQualifiedName().toString())) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    throw new MetaException(
                            typeElement,
                            "its super type \"" +
                                    superElement.getQualifiedName() +
                                    "\" is decorated by " +
                                    ClientException.class.getName() +
                                    "\" but the \"subTypes\" of the annotation does not contain current type"
                    );
                }
                superMetadata = get(superElement);
            }
        }

        String family = Annotations.annotationValue(annotationMirror, "family", "");
        if (family.isEmpty()) {
            family = superMetadata != null ? superMetadata.getFamily() : "DEFAULT";
        } else if (superMetadata != null && !superMetadata.getFamily().equals(family)) {
            throw new MetaException(
                    typeElement,
                    "Its family is \"" +
                            family +
                            "\" but the family of super exception is \"" +
                            superMetadata.getFamily() +
                            "\""
            );
        }
        if (code != null) {
            TypeElement conflictElement = nonAbstractElementMap.put(new Key(family, code), typeElement);
            if (conflictElement != null && !conflictElement.getQualifiedName().equals(typeElement.getQualifiedName())) {
                throw new MetaException(
                        typeElement,
                        "Duplicated error family \"" +
                                family +
                                "\" and code \"" +
                                code +
                                "\", it is used by another exception type \"" +
                                conflictElement.getQualifiedName() +
                                "\""
                );
            }
        }
        return new ClientExceptionMetadata(
                typeElement,
                family,
                code,
                superMetadata
        );
    }

    private void initSubMetadatas(ClientExceptionMetadata metadata) {
        AnnotationMirror annotationMirror = Annotations.annotationMirror(metadata.getElement(), ClientException.class);
        List<Object> subTypes = Annotations.annotationValue(annotationMirror, "subTypes", Collections.emptyList());
        Set<ClientExceptionMetadata> subMetadatas = new LinkedHashSet<>((subTypes.size() * 4 + 2) / 3);
        for (Object subType : subTypes) {
            String subTypeName = subType.toString();
            if (subTypeName.endsWith(".class")) {
                subTypeName = subTypeName.substring(0, subTypeName.length() - 6);
            }
            TypeElement subElement = context.getElements().getTypeElement(subTypeName);
            Element backRefElement = context.getTypes().asElement(subElement.getSuperclass());
            if (backRefElement != metadata.getElement()) {
                throw new MetaException(
                        metadata.getElement(),
                        "it is decorated by \"@" +
                                ClientException.class.getName() +
                                "\" which specifies the sub type \"" +
                                subElement.getQualifiedName() +
                                "\", " +
                                "but the super type of that sub type is not current type"
                );
            }
            if (subElement.getAnnotation(ClientException.class) == null) {
                throw new MetaException(
                        metadata.getElement(),
                        "it is decorated by \"@" +
                                ClientException.class.getName() +
                                "\" which specifies the sub type \"" +
                                subElement.getQualifiedName() +
                                "\", but that sub type is not decorated by \"@" +
                                ClientException.class.getName() +
                                "\""
                );
            }
            subMetadatas.add(get(subElement));
        }
        metadata.setSubMetdatas(Collections.unmodifiableList(new ArrayList<>(subMetadatas)));
    }

    private static class Key {

        final String family;

        final String code;

        private Key(String family, String code) {
            this.family = family;
            this.code = code;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (!family.equals(key.family)) return false;
            return code.equals(key.code);
        }

        @Override
        public int hashCode() {
            int result = family.hashCode();
            result = 31 * result + code.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Key{" +
                    "family='" + family + '\'' +
                    ", code='" + code + '\'' +
                    '}';
        }
    }
}
