package org.babyfish.jimmer.sql.example.runtime;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class TenantProvider {

    /*
     * You can use constructor inject in your project, like this
     *
     * ```
     * private final HttpServletRequest request;
     *
     * public TenantProvider(HttpServletRequest request) {
     *     this.request = request;
     * }
     * ```
     *
     * Here, in order to adapt to SpringBoot2's `javax.servlet...` and SpringBoo3's `jakarta.servlet...`
     * at the same time, construct injection is not used, but the relatively cumbersome `RequestContextHolder`
     */

    @Nullable
    public String get() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            String tenant = ((ServletRequestAttributes) requestAttributes).getRequest().getHeader("tenant");
            return tenant == null || tenant.isEmpty() ? null : tenant;
        }
        return null;
    }
}
