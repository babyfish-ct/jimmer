package org.babyfish.jimmer.spring.client;

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KFunction;
import kotlin.reflect.KType;
import org.babyfish.jimmer.client.meta.Metadata;
import org.babyfish.jimmer.client.meta.Operation;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.jetbrains.annotations.Nullable;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.ApplicationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.*;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JakartaMetadataFactoryBean implements FactoryBean<Metadata> {

    private static final Set<String> IGNORED_CLASS_NAMES;

    private final ApplicationContext ctx;

    private final ParameterNameDiscoverer parameterNameDiscoverer;

    public JakartaMetadataFactoryBean(
            ApplicationContext ctx,
            ParameterNameDiscoverer parameterNameDiscoverer
    ) {
        this.ctx = ctx;
        this.parameterNameDiscoverer =
                parameterNameDiscoverer != null ?
                        parameterNameDiscoverer :
                        new DefaultParameterNameDiscoverer();
    }

    @Override
    public Class<?> getObjectType() {
        return Metadata.class;
    }

    @Override
    public Metadata getObject() {
        List<String> packageNames = AutoConfigurationPackages.get(ctx);
        List<Class<?>> serviceTypes = new ArrayList<>();
        for (Object bean : ctx.getBeansWithAnnotation(RestController.class).values()) {
            boolean shouldBeParsed = false;
            for (String packageName : packageNames) {
                if (bean.getClass().getName().startsWith(packageName + '.')) {
                    shouldBeParsed = true;
                    break;
                }
            }
            if (shouldBeParsed) {
                serviceTypes.add(AopUtils.getTargetClass(bean));
            }
        }
        return Metadata
                .newBuilder()
                .addServiceTypes(serviceTypes)
                .setOperationParser(
                        new Metadata.OperationParser() {
                            @Override
                            public Tuple2<String, Operation.HttpMethod> http(AnnotatedElement annotatedElement) {
                                if (annotatedElement instanceof Method) {
                                    GetMapping getMapping = annotatedElement.getAnnotation(GetMapping.class);
                                    if (getMapping != null) {
                                        return new Tuple2<>(text(getMapping.value(), getMapping.path()), Operation.HttpMethod.GET);
                                    }
                                    PostMapping postMapping = annotatedElement.getAnnotation(PostMapping.class);
                                    if (postMapping != null) {
                                        return new Tuple2<>(text(postMapping.value(), postMapping.path()), Operation.HttpMethod.POST);
                                    }
                                    PutMapping putMapping = annotatedElement.getAnnotation(PutMapping.class);
                                    if (putMapping != null) {
                                        return new Tuple2<>(text(putMapping.value(), putMapping.path()), Operation.HttpMethod.PUT);
                                    }
                                    DeleteMapping deleteMapping = annotatedElement.getAnnotation(DeleteMapping.class);
                                    if (deleteMapping != null) {
                                        return new Tuple2<>(text(deleteMapping.value(), deleteMapping.path()), Operation.HttpMethod.DELETE);
                                    }
                                }
                                RequestMapping requestMapping = annotatedElement.getAnnotation(RequestMapping.class);
                                if (requestMapping != null) {
                                    return new Tuple2<>(text(requestMapping.value(), requestMapping.path()),
                                            requestMapping.method().length != 0 ?
                                                    Operation.HttpMethod.valueOf(requestMapping.method()[0].name()) :
                                                    null
                                    );
                                }
                                return null;
                            }

                            @Override
                            public String[] getParameterNames(Method method) {
                                return parameterNameDiscoverer.getParameterNames(method);
                            }

                            @Override
                            public KType kotlinType(KFunction<?> function) {
                                KType type = function.getReturnType();
                                if (JvmClassMappingKt.getKotlinClass(ResponseEntity.class).equals(type.getClassifier())) {
                                    return type.getArguments().get(0).getType();
                                }
                                return type;
                            }

                            @Override
                            public AnnotatedType javaType(Method method) {
                                if (method.getReturnType() == ResponseEntity.class) {
                                    return ((AnnotatedParameterizedType)method.getAnnotatedReturnType()).getAnnotatedActualTypeArguments()[0];
                                }
                                return method.getAnnotatedReturnType();
                            }
                        }
                )
                .setParameterParser(
                        new Metadata.ParameterParser() {
                            @Nullable
                            @Override
                            public Tuple2<String, Boolean> requestParamNameAndNullable(Parameter javaParameter) {
                                RequestParam requestParam = javaParameter.getAnnotation(RequestParam.class);
                                if (requestParam == null) {
                                    return null;
                                }
                                return new Tuple2<>(
                                        notEmpty(requestParam.value(), requestParam.name()),
                                        !requestParam.required() || !requestParam.defaultValue().equals(ValueConstants.DEFAULT_NONE)
                                );
                            }

                            @Nullable
                            @Override
                            public String pathVariableName(Parameter javaParameter) {
                                PathVariable pathVariable = javaParameter.getAnnotation(PathVariable.class);
                                if (pathVariable == null) {
                                    return null;
                                }
                                return notEmpty(pathVariable.value(), pathVariable.name());
                            }

                            @Override
                            public boolean isRequestBody(Parameter javaParameter) {
                                return javaParameter.isAnnotationPresent(RequestBody.class);
                            }

                            @Override
                            public boolean shouldBeIgnored(Parameter javaParameter) {
                                return IGNORED_CLASS_NAMES.contains(javaParameter.getType().getName());
                            }
                        }
                )
                .build();
    }

    private static String text(String[] a, String[] b) {
        for (String value : a) {
            if (!value.isEmpty()) {
                return value;
            }
        }
        for (String path : b) {
            if (!path.isEmpty()) {
                return path;
            }
        }
        return "";
    }

    private static String notEmpty(String a, String b) {
        if (!a.isEmpty()) {
            return a;
        }
        if (!b.isEmpty()) {
            return b;
        }
        return "";
    }

    static {
        Set<String> set = new HashSet<>();
        set.add(HttpServletRequest.class.getName());
        set.add(ServletRequest.class.getName());
        set.add(HttpServletResponse.class.getName());
        set.add(ServletResponse.class.getName());
        set.add(MultipartFile.class.getName());
        set.add(Principal.class.getName());
        IGNORED_CLASS_NAMES = set;
    }
}
