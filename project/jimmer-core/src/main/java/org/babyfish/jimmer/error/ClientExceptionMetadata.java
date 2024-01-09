package org.babyfish.jimmer.error;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.babyfish.jimmer.client.ApiIgnore;
import org.babyfish.jimmer.impl.util.ClassCache;
import org.babyfish.jimmer.impl.util.StringUtil;
import org.babyfish.jimmer.internal.ClientException;
import org.babyfish.jimmer.meta.ModelException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;

class ClientExceptionMetadata {

    private static final Cache CACHE = new Cache(ClientExceptionMetadata::create);

    private final Class<?> exceptionType;

    private final String family;

    private final String code;

    private final Map<String, Method> declaredGetterMap;

    private final Map<String, Method> getterMap;

    private final ClientExceptionMetadata superMetadata;

    private List<ClientExceptionMetadata> subMetadatas;

    private ClientExceptionMetadata(Class<?> exceptionType, String family, String code, ClientExceptionMetadata superMetadata) {
        this.exceptionType = exceptionType;
        this.family = family;
        this.code = code;
        this.superMetadata = superMetadata;
        Map<String, Method> declaredGetterMap = new LinkedHashMap<>();
        for (Method method : exceptionType.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) ||
                    !Modifier.isPublic(method.getModifiers()) ||
                    method.getParameterTypes().length != 0 ||
                    method.getTypeParameters().length != 0 ||
                    method.getReturnType() == void.class ||
                    method.getExceptionTypes().length != 0 ||
                    method.getName().equals("getFamily") ||
                    method.getName().equals("getCode") ||
                    method.isAnnotationPresent(JsonIgnore.class) ||
                    method.isAnnotationPresent(ApiIgnore.class)
            ) {
                continue;
            }
            String propName = StringUtil.propName(method.getName(), method.getReturnType() == boolean.class);
            if (propName == null) {
                continue;
            }
            method.setAccessible(true);
            declaredGetterMap.put(propName, method);
        }
        this.declaredGetterMap = Collections.unmodifiableMap(declaredGetterMap);
        if (superMetadata == null) {
            getterMap = this.declaredGetterMap;
        } else {
            Map<String, Method> getterMap = new LinkedHashMap<>(
                    ((superMetadata.declaredGetterMap.size() + declaredGetterMap.size()) * 4 + 2) / 3
            );
            getterMap.putAll(superMetadata.declaredGetterMap);
            getterMap.putAll(declaredGetterMap);
            this.getterMap = Collections.unmodifiableMap(getterMap);
        }
    }

    public String getFamily() {
        return family;
    }

    public String getCode() {
        return code;
    }

    public Map<String, Method> getDeclaredGetterMap() {
        return declaredGetterMap;
    }

    public Map<String, Method> getGetterMap() {
        return getterMap;
    }

    public ClientExceptionMetadata getSuperMetadata() {
        return superMetadata;
    }

    public List<ClientExceptionMetadata> getSubMetadatas() {
        return subMetadatas;
    }

    public static ClientExceptionMetadata of(Class<?> exceptionType) {
        return CACHE.get(exceptionType);
    }

    private static ClientExceptionMetadata create(Class<?> type) {
        if (!CodeBasedException.class.isAssignableFrom(type) &&
                !CodeBasedRuntimeException.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException(
                    "Illegal type \"" +
                            type.getName() +
                            "\" extends neither \"" +
                            CodeBasedException.class.getName() +
                            "\" nor \"" +
                            CodeBasedRuntimeException.class.getName() +
                            "\""
            );
        }
        ClientException ce = type.getAnnotation(ClientException.class);
        if (ce == null) {
            throw new ModelException(
                    "Illegal type \"" +
                            type.getName() +
                            "\", the exception type extends \"" +
                            CodeBasedException.class.getName() +
                            "\" or \"" +
                            CodeBasedRuntimeException.class.getName() +
                            "\" must be decorated by \"@" +
                            ClientException.class.getName() +
                            "\""
            );
        }
        String code = ce.code();
        Class<?>[] subTypes = ce.subTypes();
        if (code.isEmpty() && subTypes.length == 0) {
            throw new ModelException(
                    "Illegal type \"" +
                            type.getName() +
                            "\", it is decorated by @\"" +
                            ClientException.class.getName() +
                            "\" but neither \"code\" nor \"subTypes\" of the annotation is specified"
            );
        }
        if (!code.isEmpty() && subTypes.length != 0) {
            throw new ModelException(
                    "Illegal type \"" +
                            type.getName() +
                            "\", it is decorated by @\"" +
                            ClientException.class.getName() +
                            "\" but both \"code\" and \"subTypes\" of the annotation are specified"
            );
        }
        if (!code.isEmpty() && Modifier.isAbstract(type.getModifiers())) {
            throw new ModelException(
                    "Illegal type \"" +
                            type.getName() +
                            "\", it is decorated by @\"" +
                            ClientException.class.getName() +
                            "\" and the \"code\" of the annotation is specified so that " +
                            "it cannot be abstract"
            );
        }
        if (subTypes.length != 0 && !Modifier.isAbstract(type.getModifiers())) {
            throw new ModelException(
                    "Illegal type \"" +
                            type.getName() +
                            "\", it is decorated by @\"" +
                            ClientException.class.getName() +
                            "\" and the \"subTypes\" of the annotation is specified so that " +
                            "it must be abstract"
            );
        }

        Class<?> superType = type.getSuperclass();
        ClientExceptionMetadata superMetadata = null;
        if (superType != CodeBasedException.class && superType != CodeBasedRuntimeException.class) {
            ClientException sce = superType.getAnnotation(ClientException.class);
            if (sce != null) {
                boolean match = false;
                for (Class<?> bacRefType : sce.subTypes()) {
                    if (bacRefType == type) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    throw new ModelException(
                            "Illegal type \"" +
                                    type.getName() +
                                    "\", its super type \"" +
                                    superType.getName() +
                                    "\" is decorated by " +
                                    ClientException.class.getName() +
                                    "\" but the \"subTypes\" of the annotation does not contain current type"
                    );
                }
                superMetadata = CACHE.internallyGet(superType);
            }
        }

        String family = ce.family();
        if (family.isEmpty()) {
            family = superMetadata != null ? superMetadata.family : "DEFAULT";
        } else if (superMetadata != null && !superMetadata.getFamily().equals(family)) {
            throw new ModelException(
                    "Illegal type \"" +
                            type.getName() +
                            "\", its family is \"" +
                            family +
                            "\" but the family of super exception is \"" +
                            superMetadata.getFamily() +
                            "\""
            );
        }
        return new ClientExceptionMetadata(
                type,
                family,
                code.isEmpty() ? null : code,
                superMetadata
        );
    }

    void init() {
        ClientException ce = exceptionType.getAnnotation(ClientException.class);
        Class<?>[] subTypes = ce.subTypes();
        Set<ClientExceptionMetadata> metadataSet = new LinkedHashSet<>((subTypes.length * 4 + 2) / 3);
        for (Class<?> subType : subTypes) {
            if (subType.getSuperclass() != exceptionType) {
                throw new ModelException(
                        "Illegal type \"" +
                                exceptionType.getName() +
                                "\", it is decorated by \"@" +
                                ClientException.class.getName() +
                                "\" which specifies the sub type \"" +
                                subType.getName() +
                                "\", " +
                                "but the super type of that sub type is not current type"
                );
            }
            if (subType.getAnnotation(ClientException.class) == null) {
                throw new ModelException(
                        "Illegal type \"" +
                                exceptionType.getName() +
                                "\", it is decorated by \"@" +
                                ClientException.class.getName() +
                                "\" which specifies the sub type \"" +
                                subType.getName() +
                                "\", but that sub type is not decorated by \"@" +
                                ClientException.class.getName() +
                                "\""
                );
            }
            metadataSet.add(CACHE.internallyGet(subType));
        }
        this.subMetadatas = Collections.unmodifiableList(new ArrayList<>(metadataSet));
    }

    private static class Cache extends ClassCache<ClientExceptionMetadata> {

        public Cache(Function<Class<?>, ClientExceptionMetadata> creator) {
            super(creator, false);
        }

        ClientExceptionMetadata internallyGet(Class<?> type) {
            return getWithoutLock(type);
        }

        @Override
        protected void onCreated(Class<?> key, ClientExceptionMetadata value) {
            value.init();
        }
    }
}
