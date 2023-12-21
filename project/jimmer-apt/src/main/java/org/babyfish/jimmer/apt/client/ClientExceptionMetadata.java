package org.babyfish.jimmer.apt.client;

import javax.lang.model.element.TypeElement;
import java.util.List;

public class ClientExceptionMetadata {

    private final TypeElement element;

    private final String family;

    private final String code;

    private final ClientExceptionMetadata superMetadata;

    private List<ClientExceptionMetadata> subMetdatas;

    public ClientExceptionMetadata(TypeElement element, String family, String code, ClientExceptionMetadata superMetadata) {
        this.element = element;
        this.family = family;
        this.code = code;
        this.superMetadata = superMetadata;
    }

    public TypeElement getElement() {
        return element;
    }

    public String getFamily() {
        return family;
    }

    public String getCode() {
        return code;
    }

    public ClientExceptionMetadata getSuperMetadata() {
        return superMetadata;
    }

    public List<ClientExceptionMetadata> getSubMetdatas() {
        return subMetdatas;
    }

    void setSubMetdatas(List<ClientExceptionMetadata> subMetdatas) {
        this.subMetdatas = subMetdatas;
    }
}
