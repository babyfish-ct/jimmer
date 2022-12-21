package org.babyfish.jimmer.example.kt.graphql.bll.interceptor

import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest

@Component
class TenantProvider(
    private val request: HttpServletRequest
) {
    val tenant: String?
        get() = request
            .getHeader("tenant")
            ?.takeIf { it.isNotEmpty() }
}