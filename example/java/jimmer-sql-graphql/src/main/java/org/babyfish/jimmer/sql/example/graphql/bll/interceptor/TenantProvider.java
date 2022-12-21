package org.babyfish.jimmer.sql.example.graphql.bll.interceptor;

import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class TenantProvider {

    private final HttpServletRequest request;

    public TenantProvider(HttpServletRequest request) {
        this.request = request;
    }

    public String get() {
        String tenant = request.getHeader("tenant");
        return "".equals(tenant) ? null : tenant;
    }
}
