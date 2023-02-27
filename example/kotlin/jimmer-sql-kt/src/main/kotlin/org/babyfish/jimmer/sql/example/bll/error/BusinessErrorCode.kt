package org.babyfish.jimmer.sql.example.bll.error

import org.babyfish.jimmer.error.ErrorFamily

@ErrorFamily
enum class BusinessErrorCode {
    GLOBAL_TENANT_REQUIRED
}