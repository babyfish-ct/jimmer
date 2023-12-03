package org.babyfish.jimmer.client.meta;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface ApiOperation {

    String getName();

    @Nullable
    List<String> getGroups();

    List<ApiParameter> getParameters();

    @Nullable
    TypeRef getReturnType();

    Map<TypeName, List<String>> getErrorMap();

    Doc getDoc();

    List<String> AUTO_OPERATION_ANNOTATIONS = Collections.unmodifiableList(
            Arrays.asList(
                    "org.springframework.web.bind.annotation.RequestMapping",
                    "org.springframework.web.bind.annotation.GetMapping",
                    "org.springframework.web.bind.annotation.PostMapping",
                    "org.springframework.web.bind.annotation.PutMapping",
                    "org.springframework.web.bind.annotation.DeleteMapping"
            )
    );
}
