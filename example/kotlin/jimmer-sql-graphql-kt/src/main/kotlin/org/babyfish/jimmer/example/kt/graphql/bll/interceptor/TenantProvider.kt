package org.babyfish.jimmer.example.kt.graphql.bll.interceptor

import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Component
class TenantProvider {

    /*
     * You can use constructor inject in your project, like this
     *
     * ```
     * class TenantProvider(
     *     private val request: HttpServletRequest
     * ) {
     *     ...
     * }
     * ```
     *
     * Here, in order to adapt to SpringBoot2's `javax.servlet...` and SpringBoo3's `jakarta.servlet...`
     * at the same time, construct injection is not used, but the relatively cumbersome `RequestContextHolder`
     */

    val tenant: String?
        get() = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)
            ?.request
            ?.getHeader("tenant")
            ?.takeIf { it.isNotEmpty() }
}