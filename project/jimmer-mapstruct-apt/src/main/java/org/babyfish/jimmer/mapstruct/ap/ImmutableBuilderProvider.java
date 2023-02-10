package org.babyfish.jimmer.mapstruct.ap;

import org.mapstruct.ap.spi.BuilderInfo;
import org.mapstruct.ap.spi.DefaultBuilderProvider;
import org.mapstruct.ap.spi.TypeHierarchyErroneousException;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Collections;

public class ImmutableBuilderProvider extends DefaultBuilderProvider {

    @Override
    public BuilderInfo findBuilderInfo(TypeMirror type) {
        if (type.getKind() == TypeKind.DECLARED) {
            Element element = ((DeclaredType)type).asElement();
            if (ImmutableUtils.isImmutable(element)) {
                String qualifiedName =
                        ((TypeElement) element)
                                .getQualifiedName()
                                .toString() +
                                "Draft.MapStruct";
                TypeElement builderElement = elementUtils.getTypeElement(qualifiedName);
                if (builderElement == null) {
                    // `XxxDraft.MapStruct` has not been generated yet, try later.
                    throw new TypeHierarchyErroneousException(type);
                }
                return parseBuilderInfo(builderElement);
            }
        }
        return super.findBuilderInfo(type);
    }

    private BuilderInfo parseBuilderInfo(TypeElement builderElement) {
        ExecutableElement creator = null;
        ExecutableElement builder = null;
        for (Element memberElement : elementUtils.getAllMembers(builderElement)) {
            if (memberElement.getKind() == ElementKind.CONSTRUCTOR) {
                creator = (ExecutableElement) memberElement;
            }
            if (memberElement.getKind() == ElementKind.METHOD &&
                    ((ExecutableElement)memberElement).getParameters().isEmpty()) {
                builder = (ExecutableElement) memberElement;
            }
        }
        return new BuilderInfo.Builder()
                .builderCreationMethod(creator)
                .buildMethod(Collections.singletonList(builder))
                .build();
    }
}
